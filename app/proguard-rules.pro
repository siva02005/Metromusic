# Metromusic ProGuard Rules

# Keep Media3 classes
-keep class androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }

# Keep Room entities
-keep class com.metromusic.app.data.local.entity.** { *; }

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class com.metromusic.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.metromusic.app.**$$serializer {
    *** INSTANCE;
}

# Keep Retrofit interfaces
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Wi-Fi Aware
-keep class android.net.wifi.aware.** { *; }

# Timber
-dontwarn org.jetbrains.annotations.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# General
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
