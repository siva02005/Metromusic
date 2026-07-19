plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    application
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
}

compose.desktop {
    application {
        mainClass = "com.metromusic.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = "Metromusic"
            packageVersion = "1.0.0"
            description = "Metromusic - Open Source Music Player"
            vendor = "Metromusic"

            windows {
                menuGroup = "Metromusic"
                upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                iconFile.set(project.file("icon.ico"))
            }
        }
    }
}
