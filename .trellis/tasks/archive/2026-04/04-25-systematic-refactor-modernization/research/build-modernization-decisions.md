# Build Modernization Decisions

## Snapshot Date

2026-04-25

## Changes Applied

- Centralized remaining hard-coded dependency coordinates from `app/build.gradle.kts` into `gradle/libs.versions.toml`.
- Upgraded Android Gradle Plugin from `8.13.1` to stable patch `8.13.2` while keeping the existing Gradle 8.13 wrapper line.
- Did not jump to AGP `9.2.0` because its release notes require Gradle `9.4.1`; that is a wrapper/toolchain migration outside this safe refactor pass.
- Upgraded Kotlin from `2.0.21` to stable `2.3.21`.
- Upgraded Compose BOM from `2024.09.00` to stable `2026.04.01`; Compose artifacts remain BOM-managed and do not pin per-artifact versions.
- Upgraded AndroidX stable lines represented in the catalog, including Core KTX `1.18.0`, Lifecycle `2.10.0`, Activity Compose `1.13.0`, Navigation Compose `2.9.8`, DataStore `1.2.1`, Kotlinx Coroutines `1.10.2`, Kotlinx Serialization JSON `1.11.0`, Google Play Services Nearby `19.3.0`, and AppCompat `1.7.1`.
- Removed the legacy `composeOptions.kotlinCompilerExtensionVersion = "1.5.1"` pin because the project uses the Kotlin Compose compiler plugin.

## Compatibility Notes

- The product constraint remains API 36-only: `compileSdk`, `minSdk`, and `targetSdk` stay at `36`.
- JDK bytecode target remains `17`.
- Local validation is blocked by the host JDK `25.0.2`; Gradle/Kotlin reject it before Kotlin compilation. Use JDK 17 or 21 to run `bash ./gradlew test` and `bash ./gradlew assembleDebug`.

## Sources

- Android Gradle Plugin release notes: https://developer.android.com/studio/releases/gradle-plugin
- Kotlin releases: https://kotlinlang.org/docs/releases.html
- Jetpack Compose April 2026 stable BOM announcement: https://developer.android.com/blog/posts/whats-new-in-the-jetpack-compose-april-26-release
- Activity release notes: https://developer.android.com/jetpack/androidx/releases/activity
- Lifecycle release metadata: https://developer.android.com/jetpack/androidx/releases/lifecycle
