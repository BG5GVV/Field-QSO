# Android 构建与运行环境规则 (Build & Environment Rules)

## 1. JDK 与 Gradle 编译规范
- **Java 版本**：当前宿主运行环境为 **Java 25 (OpenJDK/Oracle JDK 25.0.3)**。
- **Gradle 版本**：Gradle 8.x 不支持 Java 25，本项目统一采用 **Gradle 9.7.0**。
- **项目 Wrapper**：已固化根目录 `gradlew.bat` 与 `gradlew`，使用 `.\gradlew.bat <tasks>` 即可直接调用 Gradle 9.7.0。
- **Gradle 9.7.0 本地路径**：
  `C:\Users\FrankChan\.gradle\wrapper\dists\gradle-9.7.0-bin\d4tj7w02tcgubx9zk9hbippn6\gradle-9.7.0\bin\gradle.bat`

## 2. Release 编译与安装指令
- **编译 Release APK**：
  ```powershell
  .\gradlew.bat assembleRelease
  ```
  *(注：已在 app/build.gradle.kts 中配置 `lint { checkReleaseBuilds = false }`，避免 Java 25 下 lintVitalAnalyzeRelease 的已知异常)*
- **APK 产物位置**：
  - 自动备份归档目录：`apk_history/FieldQSO_v1.0.0_release_yyyyMMdd_HHmmss.apk`
  - 默认构建目录：`app/build/outputs/apk/release/app-release.apk`
- **ADB 安装与运行**：
  ```powershell
  # 安装最新 APK
  adb install -r "apk_history/<最新文件>.apk"
  # 启动主界面
  adb shell am start -n com.ham.qso/.MainActivity
  ```
