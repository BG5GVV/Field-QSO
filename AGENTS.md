# Field QSO Agent Instructions & Environment Guidelines

## 🛠️ Build & Environment Guidelines
- **Java / JDK**: Java 25 (`Java 25.0.3`).
- **Gradle Version**: **Gradle 9.7.0** (required for Java 25 compatibility).
- **Gradle Wrapper**: Always use `.\gradlew.bat` in the root directory (configured with Gradle 9.7.0).
- **Gradle 9.7.0 Direct Path**: `C:\Users\FrankChan\.gradle\wrapper\dists\gradle-9.7.0-bin\d4tj7w02tcgubx9zk9hbippn6\gradle-9.7.0\bin\gradle.bat`

## 📦 Build & Deployment Commands
- **Compile Release**: `.\gradlew.bat assembleRelease`
- **Compile Debug**: `.\gradlew.bat assembleDebug`
- **Install APK**: `adb install -r "apk_history/<latest_apk>.apk"`
- **Launch App**: `adb shell am start -n com.ham.qso/.MainActivity`
