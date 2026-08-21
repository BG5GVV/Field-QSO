# Field QSO 业余无线电户外通联日志 - 完成总结

根据项目 `README.md` 与实施计划，已完整构建 **Field QSO** 原生 Android 应用程序。

---

## 🌟 核心功能与模块一览

### 1. 极速户外通联录入 (Field Burst Logging)
- **文件**：[LoggingScreen.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/ui/screens/log/LoggingScreen.kt), [LoggingViewModel.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/ui/screens/log/LoggingViewModel.kt)
- **特性**：
  - 顶部常驻 **UTC 实时秒级跳动时钟** 与当前激活架台会话指示。
  - 呼号输入自动转为大写，支持等宽大字体展示。
  - **防重通联即时告警 (Dupe Alert)**：在同架台会话同波段下，当输入已通联过的呼号时即时标红警示。
  - **波段与模式快速 Chips**：160m ~ 70cm 波段与 SSB/CW/FT8/FM 等模式水平滑动快速点选。
  - **RST 预设器**：根据当前模式自动提供常用信号报告（如 59, 599, FT8 -10dB 等）。
  - **可折叠高级详情**：支持记录对方网格、姓名、QTH 地名文字、海拔高度、对方电台、对方天线、对方功率及通联备注。
  - **Burst 模式自动保留**：提交一条 QSO 后，自动清空对方字段，保留当前工作波段、模式、频率和 RST。

### 2. 架台与活动会话管理 (Field Session)
- **文件**：[SessionScreen.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/ui/screens/session/SessionScreen.kt), [SessionViewModel.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/ui/screens/session/SessionViewModel.kt)
- **特性**：
  - 支持多活动会话管理（如 "莲花山 POTA CN-0123"、"海边野台"）。
  - 独立配置我方呼号、我方网格、发射功率、电台型号、架设天线、POTA/SOTA/WWFF 编号。
  - 一键切换当前激活会话，录入时自动继承该会话参数。
  - 实时统计每个会话的总 QSO 数与独立呼号数。

### 3. 通联日志检索与数据互通 (Logbook & Interoperability)
- **文件**：[LogbookScreen.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/ui/screens/logbook/LogbookScreen.kt), [LogbookViewModel.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/ui/screens/logbook/LogbookViewModel.kt), [AdifExporter.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/domain/adif/AdifExporter.kt), [AdifParser.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/domain/adif/AdifParser.kt)
- **特性**：
  - 支持按会话、按波段、按模式及关键词（呼号/网格/姓名/备注）多维实时组合筛选。
  - 单条 QSO 详情查看、编辑与删除。
  - **ADIF 3.1.4 标准格式导出与分享**（支持通过系统面板分享至微信、QQ、网盘、邮件，或无缝导入 LoTW / QRZ / ClubLog）。
  - **CSV 表格导出与分享**。
  - **ADIF 文件导入**：支持从本地 `.adi` 文件解析历史记录并批量导入到指定会话。

### 4. 无线电实用工具箱 (Radio Utilities)
- **文件**：[ToolsScreen.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/ui/screens/tools/ToolsScreen.kt), [ToolsViewModel.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/ui/screens/tools/ToolsViewModel.kt), [MaidenheadUtils.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/domain/utils/MaidenheadUtils.kt), [QCodeData.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/domain/model/QCodeData.kt)
- **特性**：
  - **GPS -> 梅登黑德网格定位**：实时获取手机 GPS 坐标，自动换算 4 位粗略与 6 位高精网格，并支持一键应用至当前架台会话。
  - **天线大圆方位角与距离测算**：输入两地网格即可实时计算大圆距离（km/miles）与旋转天线指向角度（Bearing 0°~360°及八方位指示）。
  - **常用无线电 Q 简语速查**：离线内置 QTH, QSL, QSO, QRZ, QSY, QRM, QRN, QSB, QRO, QRP, QRT, 73, 88 等代码与中文对照并支持实时检索。

### 5. MD3 主题与户外阳光高对比度模式 (Sunshine Mode)
- **文件**：[Theme.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/ui/theme/Theme.kt), [Color.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/ui/theme/Color.kt), [Type.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/ui/theme/Type.kt)
- **特性**：
  - 顶部栏支持四种主题模式快速切换：**跟随系统**、**日常浅色**、**夜间深色**、**户外阳光高对比度模式 (Sunshine)**。
  - Sunshine 模式采用 AMOLED 纯黑背景 + 高饱和太阳金黄重点色与高对比文字，专为户外强光直射下的清晰读写设计。

---

## 🏛️ 项目文件结构

```
Android QSO/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   └── java/com/ham/qso/
│   │   │       ├── QSOApplication.kt
│   │   │       ├── MainActivity.kt
│   │   │       ├── data/
│   │   │       │   ├── local/ (AppDatabase, QSODao, SessionDao, Converters)
│   │   │       │   ├── model/ (QSOEntity, SessionEntity, Band, Mode)
│   │   │       │   └── repository/ (QSORepository)
│   │   │       ├── domain/
│   │   │       │   ├── adif/ (AdifExporter, AdifParser)
│   │   │       │   ├── model/ (QCodeData)
│   │   │       │   └── utils/ (MaidenheadUtils)
│   │   │       └── ui/
│   │   │           ├── components/ (UtcClockHeader, BandModeSelector, RstPicker, DupeAlertCard)
│   │   │           ├── navigation/ (Screen, NavGraph)
│   │   │           ├── screens/
│   │   │           │   ├── log/ (LoggingScreen, LoggingViewModel)
│   │   │           │   ├── logbook/ (LogbookScreen, LogbookViewModel)
│   │   │           │   ├── session/ (SessionScreen, SessionViewModel)
│   │   │           │   └── tools/ (ToolsScreen, ToolsViewModel)
│   │   │           ├── theme/ (Color, Theme, Type)
│   │   │           └── viewmodel/ (AppViewModelFactory)
│   │   └── test/
│   │       └── java/com/ham/qso/
│   │           ├── MaidenheadAndAdifTest.kt
│   │           └── DupeCheckAndDomainTest.kt
│   └── build.gradle.kts
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       └── gradle-wrapper.properties
└── README.md
```

---

## 🚀 如何在 Android Studio 中打开与运行

1. 打开 **Android Studio** (推荐 Koala / Ladybug 或更高版本)。
2. 选择 **Open**，导航并选中本目录 `d:\Dev\AntiGravity\Android QSO`。
3. 等待 Gradle Sync 完成。
4. 连接 Android 设备或启动模拟器，点击 **Run 'app'** 即可完整体验！
