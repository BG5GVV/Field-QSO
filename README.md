# Field QSO - 业余无线电户外通联日志 (Android App)

一款专为业余无线电爱好者（HAM）打造的户外架台/野台/POTA/SOTA 通联记录 Android 原生 App。采用现代 **Jetpack Compose + Material Design 3 (MD3)** 规范开发，专为 **Android 15+ (API 35+)** 与 Android 16 设备量身定制。

> 📄 **完整技术架构、UI 规范与使用手册详见本地文档**：[PROJECT_DOCUMENTATION.md](file:///d:/Dev/AntiGravity/Android%20QSO/PROJECT_DOCUMENTATION.md)

---

## 🌟 核心特性与设计亮点

1. **极速单手通联录入流 (Field Burst Logging)**：
   - 呼号自动转大写输入，同架台同波段下**防重通联即时亮红告警 (Dupe Alert)**。
   - **单行三并列紧凑选择器**：`[ 波段 40m ▼ ]` + `[ 模式 SSB ▼ ]` + `[ 频率 7.050 MHz ]` 并列呈现，空间利用率提升 65%。
   - 双方信号报告（我给 Sent / 对方给 Rcvd）与模式智能预设胶囊（59, 599, FT8 信噪比等）。
   - **双重置顶与置底 (Dual Pinning)**：
     - 📌 **顶部固定**：UTC 实时跳动时钟 + 当前架台 + 本会话通联条数与独立呼号去重统计。
     - 💾 **底部固定**：即时防重告警条 + 记录通联 (LOG QSO) 醒目大按钮。
     - 🖱️ **全局空白处点击**：自动收起软键盘并结束编辑状态。
   - **全页面统一输入设计规范 (`HamInputField`)**：44dp 紧凑高度、圆角 8dp、左上 11sp 微标签、正文 15~18sp，字体与版面完全统一协调。

2. **通联日志中心与标准数据互通**：
   - 全量日志检索与多重过滤（呼号/网格/波段/模式/架台）。
   - 通联卡片支持随时查看详情、二次编辑补录与删除。
   - **标准 ADIF 3.1.4 格式导出**：完整覆盖 `<QTH>`, `<ALTITUDE>`, `<RIG>`, `<ANTENNA>`, `<RX_PWR>` 标签，无缝同步至 QRZ.com, LoTW, ClubLog, Hamlog, N1MM 等平台。
   - **CSV 表格导出** 与 Android 原生多途径分享面板（微信、QQ、网盘、邮件等）。
   - **ADIF 文件一键导入**。

3. **架台与活动会话管理 (Field Session)**：
   - 多架台会话属性隔离管理（我的呼号、我的网格、我的 QTH、发射功率、电台型号、架设天线、POTA/SOTA/WWFF 编号）。
   - 通联自动继承架台参数，独立统计有效呼号数。

4. **无线电实用工具箱 (Radio Utilities)**：
   - **GPS ⇄ 梅登黑德网格 (Maidenhead Grid)**：离线实时计算 4 位 / 6 位高精度网格。
   - **网格距离与天线大圆方位角 (Azimuth / Bearing)**：两地直线距离与旋转天线指向角度精确测算。
   - **常用无线电 Q 简语速查词典**（QTH, QRM, QRN, QSB, QSL, 73, 88...）支持即时搜索。

5. **主题与视觉体系**：
   - Material Design 3 现代规范，支持深色夜间模式。
   - 专为户外强光直射打造的 **Sunshine 阳光高对比度模式**（纯黑底色 + 高亮荧光色）。

6. **编译 APK 自动持久化归档机制**：
   - 每次编译自动将生成携带版本号、构建类型及精确时间戳的独立安装包备份至项目根目录的 **`apk_history/`** 文件夹中（如 `FieldQSO_v1.0.0_debug_20260821_134510.apk`），永久累加归档，历史版本绝不丢失。

---

## 🛠️ 技术栈与环境依赖

- **支持系统**：Android 15+ 与 Android 16（minSdk = 35, targetSdk = 35, compileSdk = 35）
- **UI 框架**：Jetpack Compose (Material3 1.3+)
- **架构模式**：MVVM + Repository + StateFlow (UDF)
- **本地数据库**：Android Jetpack Room 2.7.1 + KSP
- **构建系统**：Gradle 8.11.1 + AGP 8.7.3 + Java 17/21 兼容

---

## 👨‍💻 开发者与技术支持

- **开发者 / 呼号**：`BG5GVV`
- **开发方式**：`Vibe code with Gemini ✨` (AI 全流程辅助驱动开发)
- **联系邮箱**：`BG5GVV@outlook.com`

---

73 & Good DX! *(Vibe code with Gemini)*
