# Field QSO — 完整项目重建报告

## 📌 重建概述

我们已根据您的所有新要求，从零全新构建了完整的 **Field QSO** 业余无线电户外通联日志 Android 原生工程。

---

## 🎯 4 大新要求落实情况

### 1. 详细信息的独立 APK 文件生成（不覆盖历史版本）
- **配置文件**：[app/build.gradle.kts](file:///d:/Dev/AntiGravity/Android%20QSO/app/build.gradle.kts)
- **命名规范**：`FieldQSO_v<版本号>_<构建类型>_<年月日_时分>.apk`
- **生成样例**：`FieldQSO_v1.0.0_debug_20260821_0645.apk`
- **优势**：每次在 Android Studio 中打包或调试运行，都会在 `app/build/outputs/apk/debug/` 下生成一份带独立时间戳的新 APK，不会覆盖先前的编译产物。

### 2. 丰富通联记录项目（自定义时间、时区、设备、天线、功率、QTH、海拔）
- **数据层**：[QSOEntity.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/data/model/QSOEntity.kt)
  - `timestampUtc`（支持手动指定通联时刻，精确到秒）
  - `timeZoneId`（支持选择 UTC、Asia/Shanghai、Asia/Tokyo 等）
  - `qth`（对方地理位置描述）
  - `altitudeMeters`（海拔高度）
  - `theirRig`（对方电台设备型号）
  - `theirAntenna`（对方天线类型）
  - `theirPowerWatts`（对方发射功率）
- **UI 交互**：[LoggingScreen.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/ui/screens/log/LoggingScreen.kt) 提供独立可折叠卡片与快速微调按钮（±1分、±10分、回到当前）。
- **标准互通**：[AdifExporter.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/domain/adif/AdifExporter.kt) 完整导出 `<QTH>`, `<ALTITUDE>`, `<RIG>`, `<ANTENNA>`, `<RX_PWR>` 标签。

### 3. 对方网格后期可编辑与换算
- **录入时**：对方网格允许直接留空，可在"备注"或"QTH"中临时记录地名；
- **历史记录中**：[HistoryScreen.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/ui/screens/history/HistoryScreen.kt) 支持点击编辑按钮唤起弹窗，支持实时补录网格，并自带 [MaidenheadUtils.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/domain/utils/MaidenheadUtils.kt) 的 4位/6位网格格式校验。
- **筛选功能**：历史记录支持一键勾选"仅未定网格"，方便出站后集中批量补全网格。

### 4. 强制 Android 16+ 运行环境
- **版本配置**：
  - `compileSdk = 36` (Android 16)
  - `targetSdk = 36` (Android 16)
  - `minSdk = 36` (强制 Android 16 专属，非 Android 16 设备不可安装)
- **技术栈配置**：
  - Gradle 8.11.1 + AGP 8.7.3
  - Kotlin 2.0.21 + KSP 2.0.21-1.0.28
  - Java Toolchain 17（彻底解决 Java 与 Kotlin JVM Target 不一致的问题）
  - Room 2.7.1
  - Material Design 3 (Compose BOM 2025.05.01)

---

## 📂 核心文件清单

| 模块 | 文件路径 | 功能说明 |
| :--- | :--- | :--- |
| **构建系统** | [libs.versions.toml](file:///d:/Dev/AntiGravity/Android%20QSO/gradle/libs.versions.toml) | Version Catalog 依赖版本管理 |
| | [app/build.gradle.kts](file:///d:/Dev/AntiGravity/Android%20QSO/app/build.gradle.kts) | APK 自动命名、Toolchain 17 与 API 36 |
| | [gradle.properties](file:///d:/Dev/AntiGravity/Android%20QSO/gradle.properties) | Android 16 编译环境参数 |
| **数据层** | [QSOEntity.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/data/model/QSOEntity.kt) | 扩展通联实体模型 |
| | [SessionEntity.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/data/model/SessionEntity.kt) | 户外活动/架台会话模型 |
| | [AppDatabase.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/data/local/AppDatabase.kt) | Room 本地数据库单例 |
| | [QSORepository.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/data/repository/QSORepository.kt) | 仓储层，防重通联检测与会话切换 |
| **核心算法** | [MaidenheadUtils.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/domain/utils/MaidenheadUtils.kt) | GPS/经纬度/网格互转、大圆距离与天线方位角计算 |
| | [AdifExporter.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/domain/adif/AdifExporter.kt) | ADIF 3.1.4 与 CSV 格式生成器 |
| | [AdifImporter.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/domain/adif/AdifImporter.kt) | ADIF 文件解析导入器 |
| **界面层** | [MainActivity.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/MainActivity.kt) | 导航栏与整体页面入口 |
| | [LoggingScreen.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/ui/screens/log/LoggingScreen.kt) | 快速录入、时钟、防重告警、时间/时区调节、详细字段 |
| | [HistoryScreen.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/ui/screens/history/HistoryScreen.kt) | 历史列表、网格编辑弹窗、分享/导入 ADIF |
| | [SessionListScreen.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/ui/screens/session/SessionListScreen.kt) | 架台与出站活动管理 |
| | [GridCalculatorScreen.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/ui/screens/tools/GridCalculatorScreen.kt) | GPS 网格计算器、天线指向角度与 Q 简语速查 |
| | [SettingsScreen.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/main/java/com/ham/qso/ui/screens/settings/SettingsScreen.kt) | 户外 Sunshine 强光高对比度模式与深色模式 |
| **单元测试** | [MaidenheadAndAdifTest.kt](file:///d:/Dev/AntiGravity/Android%20QSO/app/src/test/java/com/ham/qso/MaidenheadAndAdifTest.kt) | 网格算法、天线方位角与 ADIF 往返测试 |
