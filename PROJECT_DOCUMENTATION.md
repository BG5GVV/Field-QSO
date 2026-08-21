# Field QSO - 业余无线电户外通联日志系统 (技术与用户全景文档)

> **版本**：v1.0.0 (build 2026.08.21)  
> **适用平台**：Android 15+ (API 35+) 与 Android 16  
> **开发者 / 呼号**：BG5GVV  
> **开发方式**：Vibe code with Gemini ✨ (AI 全流程辅助驱动开发)  
> **联系邮箱**：BG5GVV@outlook.com  
> **开发框架**：Kotlin 2.0 + Jetpack Compose + Material Design 3 (MD3) + Room 2.7 (KSP)  

---

## 📖 1. 项目简介与设计哲学

**Field QSO** 是一部专为业余无线电爱好者（HAM）量身定制的现代化野台、POTA/SOTA 户外通联记录与辅助工具应用。

### 核心设计哲学：
1. **户外极速单手盲操流 (Field Burst Logging)**：针对户外强光直射、时间紧迫（如比赛堆叠 Pileup）的环境，输入流极致精简，所有关键操作均在一屏内完成。
2. **零留白紧凑视觉体系 (Zero-Waste Compact Design)**：全面重构原生 MD3 控件，消除冗余边距与超高输入框，实现一屏总览通联核心五要素（波段、模式、频率、对方呼号、双方信号报告）。
3. **数据开放与标准化 (Open Data & ADIF 3.1.4)**：原生遵循业余无线电 ADIF 3.1.4 规范，通联记录可无缝同步导出至 QRZ.com, LoTW, ClubLog, Hamlog, N1MM 等国内外主流平台。

---

## 📱 2. 界面布局与交互系统规范

```
┌────────────────────────────────────────────────────────┐
│  Field QSO (TopAppBar)                        ℹ️ ⚙️     │
├────────────────────────────────────────────────────────┤
│  📌 [置顶卡片: UTC 时钟 + 当前架台 + 本会话通联/独立呼号]│
├────────────────────────────────────────────────────────┤
│  📻 [单行并列三控件: 波段 40m ▼ | 模式 SSB ▼ | 7.050 MHz]│
│  🎯 [对方呼号输入框: BH4XYZ                     (清空)]│
│  📶 [双方信号报告: 我给 59 | 对方给 59]                │
│     [快捷 RST 胶囊: 59 | 58 | 57 | 55 | 53 | 44]      │
│  🔽 [折叠详细信息面板: 对方网格/姓名/QTH/设备/天线/功率] │
│  🕒 [横向滑动: 最近通联记录流水条]                     │
├────────────────────────────────────────────────────────┤
│  ⚠️ [防重通联即时告警条 (检测到重复时出现)]             │
│  💾 [置底大按钮: 记录通联 (LOG QSO)]                   │
├────────────────────────────────────────────────────────┤
│  🔘 [底部四大功能导航栏: 极速录入 | 通联日志 | 架台会话 | 工具箱] │
└────────────────────────────────────────────────────────┘
```

### 交互与视觉优化特性：
- **双重置顶与置底 (Dual Pinning)**：
  - **顶部固定**：UTC 实时秒级跳动时钟 + 架台名称 + 本会话总通联数与独立呼号数。
  - **底部固定**：防重通联告警条 + 记录通联（LOG QSO）醒目大按钮。
  - **中间自适应**：自适应不同屏幕尺寸（`Modifier.weight(1f)`），中间内容自由滚动，无上下多余空白。
- **单行并列三紧凑选择器 (Compact Selector)**：
  - 将波段（Band）、模式（Mode）、频率（MHz）压缩至同一行内，高度统一为 `44dp`，点击即弹出列表点选，省下 65% 垂直空间。
- **全页面统一输入框规范 (`HamInputField`)**：
  - 彻底解决原生 `OutlinedTextField` 高度过高（56dp+）和文字忽大忽小的问题。
  - 统一规范：圆角 `8dp`、高度 `44dp`（呼号框 `48dp`）、左上微型标签 `11sp`、正文输入 `15~16sp`（呼号 `18sp Monospace`）。
- **全局空白点击收起软键盘**：
  - 挂载 `LocalFocusManager` 与手势识别，点击任意空白区域、卡片间隙均能立即结束编辑状态并收起键盘。
- **内置版本与开发者信息弹窗 (`AboutDialog`)**：
  - 动态显示构建时间戳与版本号（如 `1.0.0 build 2026.08.21`），支持直接通过顶部 `ℹ️` 图标或工具箱入口随时查看。

---

## 🗂️ 3. 四大核心模块功能详述

### 3.1 极速通联录入 (`LoggingScreen`)
- **呼号自动大写与去重**：输入字母自动转大写，同架台同波段下呼号重复时即刻亮红告警（Dupe Alert）。
- **频率与波段自动联动**：切换波段自动填充对应波段中心频率（如 40m -> 7.050 MHz，2m -> 144.200 MHz），也可直接手动修改精确频率。
- **RST 双方信号报告与预设**：根据通联模式智能切换快捷预设（SSB 预设 59/58/57..，CW 预设 599/579..，FT8 预设 -05/-10/-15..）。
- **详细信息展开面板**：支持对方网格 (Grid)、对方姓名/OP、QTH 地名文字描述、对方电台型号 (Rig)、天线类型 (Antenna)、发射功率 (W)、海拔高度 (m)、通联备忘备注。
- **最近通联流水**：支持一键回溯最近 10 条通联。

### 3.2 通联日志中心 (`LogbookScreen`)
- **全量日志卡片与检索**：支持按呼号、网格、架台关键字搜索，支持按波段/模式多重过滤筛选。
- **通联详情查看与编辑**：点击任意日志卡片可查看完整通联细节，支持二次编辑（补录网格、修改信号报告等）与删除。
- **ADIF 3.1.4 规范导出**：一键生成标准 `.adi` 格式文本，包含所有标准 ADIF 标签。
- **CSV 表格导出**：方便在 Excel / WPS 中生成通联台账。
- **系统原生分享面板**：支持微信、QQ、网盘、邮件、隔空传送等一键分享文件。
- **ADIF 外部文件导入**：支持从手机本地存储导入 ADIF 历史日志。

### 3.3 架台与活动会话管理 (`SessionScreen`)
- **多架台属性隔离**：支持管理多个户外架台（如 "莲花山 POTA CN-0123"、"海边野外设台"、"周末公园出台"）。
- **独立台站属性配置**：
  - 架台名称、我的呼号 (My Callsign)、我的网格 (My Grid)、我的 QTH。
  - 本台发射功率 (W)、本台电台型号 (Rig)、架设天线 (Antenna)。
  - POTA / SOTA / WWFF 专项活动参考编号。
- **属性自动继承**：在极速录入时自动将当前活动架台的所有硬件和地理属性赋给新通联。
- **独立呼号去重统计**：实时统计每个架台下的通联总数与独立有效呼号数。

### 3.4 无线电实用工具箱 (`ToolsScreen`)
- **GPS 经纬度 ⇄ 梅登黑德网格换算**：调用系统 GPS 实时计算高精度 6 位梅登黑德网格（如 `PM95xm`），无网络亦可离线使用。
- **网格间距与天线大圆方位角计算 (Azimuth / Bearing)**：
  - 输入双方网格，实时计算直线距离（公里数）与八木/旋转天线应指向的大圆方位角（0° ~ 360°）。
- **常用业余无线电 Q 简语速查词典**：
  - 内置 QTH, QRM, QRN, QSB, QSL, QSO, QRP, QSY, 73, 88 等常用无线电术语，支持拼音与字母即时模糊搜索。
- **关于软件与开发者信息 (About App)**：
  - 展示版本号、动态构建信息、开发者呼号 `BG5GVV`、联系邮箱 `BG5GVV@outlook.com` 及开源协议。

---

## 🏗️ 4. 技术栈与工程架构

```
app/src/main/java/com/ham/qso/
├── QSOApplication.kt               # 全局 Application 单例容器
├── MainActivity.kt                 # 主入口 Activity (MD3 Scaffold, 导航栏, 主题切换, 关于弹窗)
├── data/
│   ├── local/                      # Room 本地持久化
│   │   ├── AppDatabase.kt          # 数据库定义 (v1)
│   │   ├── QSODao.kt               # 通联记录 DAO
│   │   └── SessionDao.kt           # 架台会话 DAO
│   ├── model/                      # 数据实体与枚举
│   │   ├── QSOEntity.kt            # 通联记录实体
│   │   ├── SessionEntity.kt        # 架台会话实体
│   │   ├── Band.kt                 # 波段枚举 (160m ~ 70cm)
│   │   └── Mode.kt                 # 模式枚举 (SSB, CW, FT8, FM, AM...)
│   └── repository/
│       └── QSORepository.kt        # 统一数据仓库
├── domain/
│   ├── adif/
│   │   ├── AdifExporter.kt         # ADIF 3.1.4 格式生成器
│   │   └── AdifImporter.kt         # ADIF 3.1.4 格式解析器
│   ├── grid/
│   │   └── MaidenheadUtils.kt      # 梅登黑德网格算法 / 大圆方位角
│   └── model/
│       └── QCodeData.kt            # Q 简语字典数据源
└── ui/
    ├── theme/                      # 主题与排版设计系统
    │   ├── Color.kt                # 调色板 (包含 Sunshine 阳光高对比度模式)
    │   ├── Theme.kt                # 动态色彩 / 户外高对比模式支持
    │   └── Type.kt                 # 等宽字体与呼号样式
    ├── navigation/
    │   ├── Screen.kt               # 路由定义
    │   └── NavGraph.kt             # 页面导航宿主
    ├── components/                 # 核心复用 UI 组件
    │   ├── AboutDialog.kt          # 关于软件与开发者信息弹窗
    │   ├── HamInputField.kt        # 统一规范输入框组件 (44dp 紧凑型)
    │   ├── UtcClockHeader.kt       # 置顶 UTC 时钟与会话统计卡片
    │   ├── BandModeSelector.kt     # 单行并列波段/模式/频率紧凑选择器
    │   ├── RstPicker.kt            # 紧凑型双方信号报告与预设组件
    │   ├── DupeAlertCard.kt        # 防重通联亮红告警卡片
    │   └── QSOCard.kt              # 通联日志卡片
    └── screens/                    # 四大业务页面 (MVVM)
        ├── log/                    # 极速通联录入
        ├── logbook/                # 通联日志与导出
        ├── session/                # 架台会话管理
        └── tools/                  # 无线电工具箱
```

---

## 🛠️ 5. 构建与环境配置规范

| 依赖项 | 规范配置 | 说明 |
| :--- | :--- | :--- |
| **Gradle 版本** | `8.11.1` | 项目根构建工具 |
| **Android Gradle Plugin** | `8.7.3` | AGP 插件 |
| **Java 兼容版本** | `Java 17` / `Java 21` | sourceCompatibility = VERSION_17 |
| **Kotlin 版本** | `2.0.21` | 支持 Compose Compiler 插件 |
| **Min SDK** | `35` (Android 15+) | 专为 Android 15+ 与 Android 16 设备量身定制 |
| **Target & Compile SDK** | `35` (Android 15) | 稳定兼容 Android 15/16 运行时 |
### 编译 APK 自动持久化归档规范：
已在 [app/build.gradle.kts](file:///d:/Dev/AntiGravity/Android%20QSO/app/build.gradle.kts) 中配置了自动化产物备份与持久化归档机制：
每次编译完成后，Gradle 会自动将生成的 APK 拷贝备份到项目根目录下的 **`apk_history/`** 文件夹中：
- **永久归档路径**：`d:\Dev\AntiGravity\Android QSO\apk_history\`（完全脱离 Gradle 临时 build 目录，每次编译自动累加保留，历史安装包永久保存不会丢失）。
- **产物示例**：`FieldQSO_v1.0.0_debug_20260821_134510.apk`、`FieldQSO_v1.0.0_release_20260821_134530.apk`。

---

## 🚀 6. 编译与打包操作手册

1. **导入项目**：
   - 打开 Android Studio，点击 **Open**，选择目录 `d:\Dev\AntiGravity\Android QSO`。
2. **构建 APK**：
   - 点击顶部菜单栏 **Build** -> **Generate App Bundles or APKs** -> **Generate APKs**。
   - 或点击 **Build** -> **Build Bundle(s) / APK(s)** -> **Build APK(s)**。
3. **获取安装包**：
   - 构建完成后，点击右下角浮窗的 **locate** 按钮，即可在文件夹中找到带时间戳的安装包，直接安装至手机即可。

---

## 👨‍💻 7. 开发者与技术支持

- **开发者 / 呼号**：`BG5GVV`
- **开发方式**：`Vibe code with Gemini ✨ (AI 全流程辅助驱动开发)`
- **联系邮箱**：`BG5GVV@outlook.com`
- **开源许可**：Apache-2.0

---

**Field QSO Project Team (BG5GVV)**  
*Vibe code with Gemini · 73 & Good DX!*
