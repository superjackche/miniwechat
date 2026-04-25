# Dependency Modernization Research

## Snapshot Date

2026-04-25

## Starting Project Versions

- Android Gradle Plugin: `8.13.1`
- Gradle wrapper: `8.13`
- Kotlin: `2.0.21`
- Compose BOM: `2024.09.00`
- Java bytecode target: `17`
- compileSdk / minSdk / targetSdk: `36`

## Latest Stable Signals Found

- Google Maven metadata checked on 2026-04-25 shows `androidx.compose:compose-bom` stable versions through `2026.04.01`; Compose artifacts should stay BOM-managed.
- Google Maven metadata shows AndroidX latest stable versions used here: Core KTX `1.18.0`, Activity Compose `1.13.0`, Lifecycle `2.10.0`, Navigation Compose `2.9.8`, DataStore `1.2.1`, AppCompat `1.7.1`, AndroidX Test JUnit `1.3.0`, and Espresso `3.7.0`.
- Google Maven metadata shows `com.google.android.gms:play-services-nearby` stable versions through `19.3.0`.
- Maven Central metadata shows Kotlin Gradle plugins stable through `2.3.21`, with `2.4.0-Beta*` excluded as pre-release.
- Maven Central metadata shows kotlinx.coroutines stable through `1.10.2` and `1.11.0-rc01` excluded as pre-release.
- Maven Central metadata shows kotlinx.serialization JSON stable through `1.11.0` and Coil 2.x `io.coil-kt:coil-compose` stable through `2.7.0`.
- Official Gradle metadata shows Gradle `9.4.1` as current stable, but the wrapper remains on `8.13` to avoid combining a wrapper/toolchain migration with this dependency-catalog task.
- Google Maven metadata shows AGP stable versions through `9.2.0`; this task keeps the compatible AGP 8.13 line and upgrades to `8.13.2` rather than adopting AGP 9.x without a broader Gradle wrapper migration.

## Applied Version Decisions

| Area | Decision | Reason |
| --- | --- | --- |
| Android Gradle Plugin | `8.13.1` → `8.13.2` | Latest stable patch on the current AGP 8.13 line; avoids AGP 9.x wrapper/toolchain migration. |
| Kotlin plugins | `2.0.21` → `2.3.21` | Latest stable Kotlin plugin metadata entry; matching Android, Compose, and serialization plugin versions. |
| Compose | BOM `2024.09.00` → `2026.04.01` | Latest stable Google Maven BOM; Compose UI/material artifacts remain versionless and BOM-managed. |
| AndroidX Core | `1.17.0` → `1.18.0` | Latest stable metadata entry; excludes `1.19.0-alpha01`. |
| AndroidX Activity | `1.11.0` → `1.13.0` | Latest stable metadata entry. |
| AndroidX Lifecycle | `2.9.4` → `2.10.0` | Latest stable shared Lifecycle version; excludes `2.11.0-alpha/beta`. |
| AndroidX Navigation | `2.8.0` → `2.9.8` | Latest stable metadata entry; excludes `2.10.0-alpha*`. |
| AndroidX DataStore | `1.1.1` → `1.2.1` | Latest stable metadata entry; excludes `1.3.0-alpha*`. |
| Google Nearby | `18.4.0` → `19.3.0` | Latest stable Google Maven metadata entry. |
| kotlinx.coroutines | `1.8.1` → `1.10.2` | Latest stable metadata entry; excludes `1.11.0-rc01`. |
| kotlinx.serialization JSON | `1.7.3` → `1.11.0` | Latest stable metadata entry; excludes RC versions. |
| AppCompat | `1.7.0` → `1.7.1` | Latest stable metadata entry; excludes `1.8.0-alpha01`. |
| Coil Compose | Keep `2.7.0` | Latest stable for existing `io.coil-kt:coil-compose` artifact; Coil 3 is a different artifact family and is not a drop-in catalog-only upgrade. |
| Gradle wrapper | Keep `8.13` | Avoids unrelated wrapper migration; validation should use JDK 17/21 due known JDK 25 failure. |

## Centralization Changes

- Added catalog aliases for `androidx.lifecycle:lifecycle-process` and `androidx.compose.material:material`.
- Replaced inline dependencies in `app/build.gradle.kts` with catalog aliases: Lifecycle Process, Compose Material, Coil Compose, and Material Icons Extended.
- Removed the hard-coded `composeOptions.kotlinCompilerExtensionVersion = "1.5.1"`; Kotlin 2.x uses the `org.jetbrains.kotlin.plugin.compose` plugin, so the compiler extension should not be pinned separately.

## Upgrade Direction

1. Use JDK 17 or a Kotlin/Gradle-supported JDK before running builds.
2. Move hard-coded dependencies from `app/build.gradle.kts` to `gradle/libs.versions.toml` before version bumps.
3. Prefer AGP `8.13.2` for this task because it is the latest stable patch on the current compatible AGP line.
4. Use Kotlin `2.3.21` with matching Kotlin Android, Compose, and Serialization plugins; do not add a separate Compose compiler extension version.
5. Update Compose via BOM, not individual Compose artifact versions.
6. Avoid RC/Beta/Alpha artifacts unless a separate PRD explicitly opts in.

## Sources

- Android Gradle plugin release notes: https://developer.android.com/studio/releases/gradle-plugin
- Kotlin 2.3.20 release notes: https://kotlinlang.org/docs/whatsnew2320.html
- Kotlin 2.3.20 blog: https://blog.jetbrains.com/kotlin/2026/03/kotlin-2-3-20-released/
- Compose release notes: https://developer.android.com/jetpack/androidx/releases/compose
- Compose BOM docs: https://developer.android.com/jetpack/compose/bom
- Compose BOM Maven listing: https://mvnrepository.com/artifact/androidx.compose/compose-bom
- Google Maven metadata: `https://dl.google.com/dl/android/maven2/.../maven-metadata.xml`
- Maven Central metadata: `https://repo1.maven.org/maven2/.../maven-metadata.xml`
- Gradle current version metadata: https://services.gradle.org/versions/current

## Validation Notes

- Build validation should run with JDK 17 or 21. The PRD and quality guidelines both note that the local default JDK 25.0.2 fails during Kotlin DSL compilation before tests run with `IllegalArgumentException: 25.0.2`.
- If validation is attempted under JDK 25, record the blocker rather than treating dependency changes as the root cause.
