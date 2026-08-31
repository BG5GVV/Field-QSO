# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Room entities
-keep class com.ham.qso.data.model.** { *; }

# Keep ADIF data classes
-keepclassmembers class com.ham.qso.domain.adif.** { *; }

# Keep Enum values for Room TypeConverters
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
