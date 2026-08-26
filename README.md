# Field QSO 📻 — 业余无线电户外通联日志 (Android App)

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%2016%20(API%2036)-3DDC84.svg?style=flat-square&logo=android" alt="Android 16">
  <img src="https://img.shields.io/badge/JDK-Java%2025-ED8B00.svg?style=flat-square&logo=openjdk" alt="Java 25">
  <img src="https://img.shields.io/badge/Gradle-9.7.0-02303A.svg?style=flat-square&logo=gradle" alt="Gradle 9.7.0">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20MD3-4285F4.svg?style=flat-square&logo=jetpackcompose" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/AI%20Driven-Google%20Antigravity%20%7C%20Gemini-8E75B2.svg?style=flat-square&logo=google" alt="AI Driven">
</p>

专为业余无线电爱好者（HAM）打造的现代化野外架台 / POTA / SOTA / 通联比赛快速记录 Android 原生应用。采用现代 **Jetpack Compose + Material Design 3** 构建，深度适配 **Android 16 (API 36)** 平台特性与 **Java 25** 运行环境。

> [!IMPORTANT]
> **系统要求**：本项目专为 **Android 16 (API 36)** 平台打造（`minSdk = 36`）。在低于 Android 16 的系统上安装时，系统包管理器会提示“解析错误”或拒绝安装（`INSTALL_FAILED_OLDER_SDK`）。

> 💡 **AI 代码驱动开发 (AI-Driven Development)**：本项目核心架构、UI 交互流、传感器算法与 ADIF 编解码引擎均基于 **Google Antigravity & Gemini** 进行 AI 全流程代码驱动构建（Vibe Coding），体现了现代 AI 与 Android 原生技术的深度融合。

---

## ✨ 核心特性与设计亮点

### 1. ⚡ 极速单手通联录入流 (Field Burst Logging)
- **智能呼号输入**：输入自动转大写，同架台同波段同模式下**即时亮红防重通联告警 (Dupe Alert)**。
- **单行紧凑三联选择器**：`[ 波段 40m ▼ ]` + `[ 模式 SSB ▼ ]` + `[ 频率 7.050 MHz ]` 并列极简排布，极大提升屏幕纵向利用率。
- **双重置顶与置底 (Dual Pinning)**：
  - 📌 **顶部固定状态栏**：UTC 实时跳动时钟 + 当前架台状态 + 通联总数与唯一呼号去重统计。
  - 💾 **底部固定操作条**：即时防重告警指示 + 大尺寸沉浸式 **LOG QSO** 提交按钮。
  - 🖱️ **全局空白手势**：点击屏幕任意空白处自动收起软键盘并保存编辑态。
- **标准化统一输入组件 (`HamInputField`)**：44dp 紧凑高度、圆角 8dp、微标签与正文比例精心调优，兼顾户外戴手套与单手快速输入。

### 2. 🧭 天线方位指南针与传感器校准 (Antenna Compass & Calibration)
- **真北 / 磁北自动解算**：基于手机地磁与加速度传感器，实时计算天线旋转与指向方位角。
- **8 字绕环动态校准向导**：内置平滑动画与磁场精度检测，随时一键矫正传感器偏差。

### 3. 🌐 梅登黑德网格与大圆测距 (Maidenhead Grid & Bearing)
- **GPS ⇄ 网格离线即时解算**：支持经纬度快速转换 4 位 / 6 位高精度 Maidenhead 网格坐标。
- **大圆航线与天线仰角/方位角计算**：精准测算当前架台到对方台站的直线距离（km）及定向天线指向方位角。

### 4. 📊 标准 ADIF 3.1.4 & CSV 互通中心
- **全量日志检索与多维筛选**：支持按呼号、网格、波段、模式、架台及时间范围快速过滤。
- **标准 ADIF 3.1.4 规范导出**：完整支持 `<QTH>`, `<ALTITUDE>`, `<RIG>`, `<ANTENNA>`, `<RX_PWR>`, `<MY_GRIDSQUARE>` 等标准字段，无缝导入 **QRZ.com**、**LoTW**、**ClubLog**、**N1MM**、**Hamlog** 等平台。
- **CSV 导出与系统原生多渠道分享**：一键导出并调用 Android 原生分享面板（微信、QQ、网盘、邮件等）。
- **ADIF 日志一键导入**：支持从第三方软件无损迁移历史日志。

### 5. 📻 活动与架台多维度管理 (Field Sessions)
- 支持多活动会话属性隔离：我的呼号、我的网格、我的 QTH、发射功率、电台型号、天线类型、POTA/SOTA/WWFF/BOTA 活动编号等。
- 录入自动继承架台参数，独立统计有效通联数。

### 6. 📖 业余无线电 Q 简语速查词典
- 离线内置完整常用无线电 Q 简语（QTH, QRM, QRN, QSB, QSL, 73, 88...）并支持即时模糊搜索。

### 7. ☀️ 户外阳光高对比度模式 (Sunshine Mode)
- 专为强光直射下的户外野台环境打造，提供深黑底色 + 高饱和高对比度荧光视觉，确保烈日下清晰易读。

### 8. 📦 自动化构建归档机制 (APK Auto-Archiving)
- 每次编译自动生成携带版本号、构建类型及精确时间戳的独立 APK（如 `FieldQSO_v1.0.0_release_20260826_202602.apk`），永久累加归档于 `apk_history/` 目录。

---

## 🛠️ 技术栈与环境规范

| 维度 | 规格 / 技术选型 |
| :--- | :--- |
| **Target OS / SDK** | **Android 16** (`compileSdk = 36`, `targetSdk = 36`, `minSdk = 36`) |
| **JDK 环境** | **Java 25** (`Java 25.0.3`) |
| **构建系统** | **Gradle 9.7.0** + Android Gradle Plugin 8.9+ |
| **编程语言** | Kotlin 2.0+ (Coroutines, Flow) |
| **UI 架构** | Jetpack Compose + Material 3 (Material You) |
| **设计模式** | MVVM + Clean Architecture + Unidirectional Data Flow (UDF) |
| **本地持久化** | Jetpack Room 2.7+ with KSP (Kotlin Symbol Processing) |
| **代码生成与开发** | **AI-Driven (Google Antigravity & Gemini)** |

---

## 🚀 快速构建与安装

项目根目录下已配置好 Gradle Wrapper (Gradle 9.7.0)：

```powershell
# 编译 Debug 版本并自动归档 APK
.\gradlew.bat assembleDebug

# 编译 Release 版本并自动归档 APK
.\gradlew.bat assembleRelease

# 安装最新生成的 APK 到连接的 Android 16+ 设备
adb install -r "apk_history/<latest_apk>.apk"

# 启动应用
adb shell am start -n com.ham.qso/.MainActivity
```

---

## 👨‍💻 作者与技术支持

- **开发者 / 呼号**：`BG5GVV`
- **联系方式**：`BG5GVV@outlook.com`
- **代码构建方式**：`AI-Driven with Google Antigravity & Gemini ✨`

---

<p align="center">
  <b>73 & Good DX! Have fun on the airwaves! 📻📡</b>
</p>
