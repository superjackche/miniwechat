# Dependency Policy Research

## Goal

Capture the dependency policy requested during bootstrap: use the latest stable dependency/package versions and actively adopt stable modern Android/Kotlin features.

## Findings

- The project uses Gradle Version Catalogs in `gradle/libs.versions.toml`; this should be the single source of truth for dependency versions.
- `app/build.gradle.kts` still contains several hard-coded dependencies. Future dependency cleanup should move them into `libs.versions.toml`.
- Current project constraints are Kotlin 2.x, Android Gradle Plugin 8.x, Jetpack Compose, Java 17, and Android API 36-only support.
- Latest-stable checks are time-sensitive. Before any dependency upgrade task, verify official release notes or Maven metadata rather than relying on memory.
- Local verification is also toolchain-sensitive: the current machine default is JDK 25.0.2, and `./gradlew test` fails before task execution because Kotlin/Gradle cannot parse that Java version. Use JDK 17 or another Gradle/Kotlin-supported stable JDK for builds until the toolchain supports JDK 25.

## Policy Added to Specs

- Prefer latest stable releases for Kotlin, AGP, Compose BOM, AndroidX, coroutines, serialization, Coil, and Google Play Services.
- Avoid alpha, beta, RC, EAP, and snapshot artifacts unless a PRD explicitly justifies them.
- Prefer stable new platform/library APIs when they simplify code or improve safety.
- Run build/tests after dependency upgrades.

## Future Refactor Notes

- Move `androidx.lifecycle:lifecycle-process`, `androidx.compose.material:material`, `io.coil-kt:coil-compose`, and direct material icons dependency declarations into the version catalog.
- Re-check whether legacy Material 2 dependencies are still required once Compose Material 3 usage is consolidated.
