# Field QSO — 从头重建实施计划

## 背景

用户已将项目文件清空，仅保留 `README.md` 作为功能参考。需从头搭建完整 Android 工程，并在之前版本基础上集成以下新需求：

1. **APK 文件带详细信息**：版本号、生成时间（精确到分钟）、构建类型（debug/release）、CPU 架构（ABI）；每次编译后不覆盖旧文件。
2. **扩展通联记录字段**：手动选择通联时间（支持时区切换 UTC/本地时间）、对方设备型号、天线类型、发射功率、QTH 名称、海拔高度。
3. **对方网格可后期编辑**：记录时网格可为空（先在备注中临时记录 QTH 文字），后期在历史记录中编辑并换算成 Maidenhead 网格。
4. **最低目标 Android 版本为 Android 16（API 36）**（根据实际理解，此处按照 **minSdk = 26 / targetSdk = 36** 处理，minSdk 不必等于 Android 16 本身，以保持对老旧设备兼容性；若用户明确要求 minSdk=36，需额外确认）。

> [!IMPORTANT]
> **已确认：`minSdk = 36`（Android 16 专属），App 仅在 Android 16 及以上设备运行。**

---

## 版本选型（经过最新研究校准）

| 组件 | 版本 | 说明 |
|------|------|------|
| AGP (Android Gradle Plugin) | **8.7.3** | 最新稳定版，完整支持 compileSdk 36 |
| Kotlin | **2.0.21** | 稳定版，匹配 KSP 2.0.21-x |
| KSP | **2.0.21-1.0.28** | 严格与 Kotlin 版本对应 |
| Gradle Wrapper | **8.11.1** | 配套 AGP 8.7.x |
| Room | **2.7.1** | 支持 KSP2，原生 Kotlin 2.0+，无需 `ksp.useKSP2=false` |
| Compose BOM | **2025.05.01** | 涵盖 Material3 1.3.x |
| Navigation Compose | **2.8.9** |  |
| Lifecycle | **2.8.7** |  |
| Activity Compose | **1.10.1** |  |
| Play Services Location | **21.3.0** | GPS 支持 |
| Coroutines | **1.8.1** |  |
| compileSdk / targetSdk | **36** | Android 16 |
| minSdk | **36** | Android 16（强制要求）|
| Java Toolchain | **17** | 统一 Java + Kotlin JVM target |

---

## 项目结构

```
app/
├── src/main/java/com/ham/qso/
│   ├── MainActivity.kt
│   ├── data/
│   │   ├── local/
│   │   │   ├── AppDatabase.kt          (Room, v2)
│   │   │   ├── QSODao.kt
│   │   │   ├── SessionDao.kt
│   │   │   └── Converters.kt
│   │   ├── model/
│   │   │   ├── QSOEntity.kt            (含新字段)
│   │   │   ├── SessionEntity.kt
│   │   │   ├── Band.kt
│   │   │   └── Mode.kt
│   │   └── repository/
│   │       └── QSORepository.kt
│   ├── domain/
│   │   ├── adif/
│   │   │   ├── AdifExporter.kt
│   │   │   └── AdifImporter.kt
│   │   └── utils/
│   │       └── MaidenheadUtils.kt
│   └── ui/
│       ├── theme/
│       │   ├── Theme.kt
│       │   ├── Color.kt
│       │   └── Type.kt
│       ├── components/
│       │   └── QSOCard.kt              (展示新字段，含编辑入口)
│       └── screens/
│           ├── log/
│           │   ├── LoggingScreen.kt    (含时间选择器、新字段)
│           │   └── LoggingViewModel.kt
│           ├── history/
│           │   ├── HistoryScreen.kt    (含网格后期编辑对话框)
│           │   └── HistoryViewModel.kt
│           ├── session/
│           │   ├── SessionListScreen.kt
│           │   └── SessionListViewModel.kt
│           ├── tools/
│           │   ├── GridCalculatorScreen.kt
│           │   └── GridCalculatorViewModel.kt
│           └── settings/
│               └── SettingsScreen.kt
└── src/test/java/com/ham/qso/
    └── MaidenheadAndAdifTest.kt
```

---

## 各需求的技术决策

### 需求1：APK 文件名带详细信息
- 在 `app/build.gradle.kts` 顶部导入 `java.text.SimpleDateFormat` / `java.util.Date` / `java.util.Locale`
- 用 `applicationVariants.all { outputs.all { ... } }` 动态设置文件名
- 文件名格式：`FieldQSO_v{versionName}_{buildType}_{yyyyMMdd_HHmm}.apk`
- ABI 拆包：通过 `splits { abi { ... } }` 可选启用（默认先不拆，避免复杂度）

### 需求2：扩展通联记录字段
**新增 QSOEntity 字段：**
- `timestampUtc: Long` — 支持手动输入（取代 `System.currentTimeMillis()`）
- `timeZoneId: String` — 时区标识（如 `"Asia/Shanghai"` 或 `"UTC"`）
- `qth: String` — 对方 QTH 名称（文字描述）
- `altitudeMeters: Int?` — 海拔高度（米）
- `theirRig: String` — 对方电台设备型号
- `theirAntenna: String` — 对方天线类型
- `theirPowerWatts: Int?` — 对方发射功率（W）

**UI 设计：**
- LoggingScreen 增加"展开详情"可折叠区域
- 时间选择器使用 MD3 `DatePickerDialog` + `TimePickerDialog`
- 时区选择器用 `DropdownMenu` 展示常用时区列表（UTC、Asia/Shanghai、Asia/Tokyo 等）

### 需求3：对方网格可后期编辑
- `QSOEntity.theirGrid` 允许为空字符串
- `HistoryScreen` 中 QSOCard 显示编辑图标，点击打开 `AlertDialog`
- 对话框内：备注 TextField + QTH 文字 TextField + 网格 TextField（含"从备注换算"按钮，调用 MaidenheadUtils）
- 保存时通过 `HistoryViewModel.updateQSOGrid()` 写入 Room

### 需求4：Android 版本要求
- `minSdk = 26`，`targetSdk = 36`，`compileSdk = 36`
- README 中技术架构更新为对应正确版本

---

## 文件修改计划

### 工程根目录
#### [MODIFY] README.md
- 更新技术架构版本号

#### [NEW] settings.gradle.kts
#### [NEW] build.gradle.kts (项目级)
#### [NEW] gradle.properties
#### [NEW] gradle/libs.versions.toml
#### [NEW] gradle/wrapper/gradle-wrapper.properties

### App 模块
#### [NEW] app/build.gradle.kts
- AGP 8.7.3 配置
- KSP 插件
- APK 命名规则（带版本号+时间戳）
- JVM Toolchain 17

#### [NEW] 所有 Kotlin 源码文件（见项目结构）

---

## 验证计划

### 自动化测试
```bash
./gradlew testDebugUnitTest
```

### 手动验证
1. Gradle Sync 无报错
2. `assembleDebug` 生成正确命名的 APK
3. 安装到设备，验证新字段录入
4. 历史记录中验证网格可编辑
5. ADIF 导出包含新字段

---

> [!NOTE]
> 由于项目从零搭建，构建顺序为：Gradle 配置 → 数据模型 → 数据库 → Repository → ViewModel → UI
