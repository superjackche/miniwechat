# Quality Guidelines

> Code quality standards for NearbyChater Android/Kotlin development.

---

## Overview

NearbyChater is a Kotlin 2.x, Android Gradle Plugin, Jetpack Compose, Google Nearby Connections app targeting Android API 36 only. Code should favor modern stable Android/Kotlin APIs, lifecycle-aware flows, explicit repository boundaries, and small focused modules.

The current codebase contains some large files and tutorial-style comments. Future refactors should preserve behavior while moving toward smaller components, clearer state boundaries, and stable latest dependencies through the Gradle version catalog.

---

## Dependency Policy

- Prefer the latest stable versions for Kotlin, Android Gradle Plugin, Jetpack Compose BOM, AndroidX, coroutines, serialization, Coil, and Google Play Services.
- Avoid alpha, beta, RC, EAP, and snapshot dependencies unless a PRD explicitly justifies them.
- Declare dependency versions in `gradle/libs.versions.toml`; avoid hard-coded versions in `app/build.gradle.kts`.
- Prefer Compose BOM-managed artifacts without per-artifact Compose UI versions.
- When updating dependencies, check official release notes or Maven metadata, update the version catalog first, then run build/tests.
- Embrace stable new platform/library features when they reduce code or improve safety, but keep `minSdk = 36` and `targetSdk = 36` constraints explicit unless a task changes the product target.

---

## Required Patterns

- Use `StateFlow`/`SharedFlow` for long-lived UI state and events; expose immutable/read-only flow types from repositories and ViewModels.
- Collect flows in Compose with lifecycle-aware APIs such as `collectAsStateWithLifecycle`.
- Keep blocking IO on `Dispatchers.IO` or an injected dispatcher.
- Keep platform integrations behind wrappers/repositories: Nearby in `NearbyChatService`, persistence in DAOs, settings in `SettingsRepository`, logging in `LogManager`.
- Use `kotlinx.serialization` models for mesh payloads and shared cache/network contracts.
- Use `remember`, `LaunchedEffect`, and ViewModel intent functions carefully; Compose UI should render state and dispatch events, not own business workflows.
- Use stable IDs (`messageId`, `conversationId`, `memberId`) for persistence and UI state.
- Keep permission and foreground-service handling compatible with Android API 36 requirements.

---

## Forbidden Patterns

- Do not add new hard-coded dependency versions to `app/build.gradle.kts`; add aliases to `gradle/libs.versions.toml` instead.
- Do not introduce deprecated Android APIs when stable modern APIs exist.
- Do not perform database, file, bitmap, or network work on the main thread.
- Do not call Google Nearby Connections directly from UI or ViewModel layers.
- Do not mutate public `MutableStateFlow` from outside its owning class.
- Do not log raw message text, Base64 attachment data, secrets, or stable device identifiers.
- Do not add more unrelated responsibilities to `MainActivity.kt`, `ChatRepository.kt`, `ChatScreen.kt`, or `ConversationListScreen.kt`; extract instead.
- Do not leave generated, backup, or experimental files in production source directories.

---

## Testing Requirements

- Add JVM unit tests for pure Kotlin logic, mapping helpers, repository state transitions, retry policy decisions, and settings defaults.
- Add instrumentation or Compose tests for permission-sensitive UI, diagnostics bubble behavior, chat send/retry/cancel actions, and logs screen actions.
- For database schema changes, test fresh database creation and upgrade paths when feasible.
- For dependency upgrades, run at least `./gradlew test` and an Android build task; run instrumentation tests when UI, permissions, or platform behavior changes.
- Prefer small targeted tests around changed behavior before running broad suites.
- Use a Gradle/Kotlin-supported JDK for local verification. This project targets Java 17 bytecode; JDK 17 or 21 is safer than bleeding-edge JDKs. JDK 25 currently fails during Kotlin DSL compilation with `IllegalArgumentException: 25.0.2` before tests run.

---

## Code Review Checklist

- Does the change keep UI, ViewModel, repository, DAO, and platform wrapper responsibilities separated?
- Are dependency versions centralized and stable?
- Are new flows lifecycle-safe and exposed read-only?
- Are IO operations off the main thread?
- Are database schema changes accompanied by version increments and migrations?
- Are errors represented through diagnostics/status state instead of silent failures?
- Are logs useful, searchable, and free of sensitive payloads?
- Does the change reduce or avoid duplication rather than adding another copy of existing patterns?
- Are tests updated for changed behavior or documented when not feasible?
- Was verification run with a supported JDK rather than a newer JDK that the Kotlin tooling cannot parse?

---

## Current Refactor Targets

- Split large Compose screens into focused components and state/event contracts.
- Split `ChatRepository.kt` into smaller collaborators for queue flushing, notifications, mesh handling, and conversation summaries.
- Move remaining hard-coded dependencies in `app/build.gradle.kts` into the version catalog.
- Remove or relocate `ChatRepository.kt.backup` from production source control.
- Standardize comments: keep comments that explain intent or Android-specific constraints; remove tutorial comments that restate syntax.

---

## Scenario: Dependency Catalog Modernization

### 1. Scope / Trigger
- Trigger: dependency upgrades, Gradle plugin changes, or adding a new library.

### 2. Signatures
- Version declarations: `gradle/libs.versions.toml` `[versions]`, `[libraries]`, and `[plugins]`.
- Module usage: `app/build.gradle.kts` should use `libs.*` aliases and `platform(libs.androidx.compose.bom)`.

### 3. Contracts
- No new hard-coded dependency coordinates with versions in `app/build.gradle.kts`.
- Compose artifacts should be BOM-managed unless they are not covered by the BOM.
- Kotlin Compose compiler plugin owns Compose compiler integration; do not add stale `composeOptions.kotlinCompilerExtensionVersion` pins.
- Keep API 36-only constraints unchanged unless a PRD explicitly changes `compileSdk`, `minSdk`, or `targetSdk`.

### 4. Validation & Error Matrix
- Alpha/beta/RC/EAP/snapshot version -> reject unless PRD explicitly allows it.
- Gradle/Kotlin unsupported JDK -> build fails before task execution; switch to JDK 17 or 21.
- AGP major upgrade requiring a new Gradle wrapper -> treat as a separate wrapper/toolchain migration unless scoped.

### 5. Good/Base/Bad Cases
- Good: add `androidx-lifecycle-process` in `libs.versions.toml` and use `implementation(libs.androidx.lifecycle.process)`.
- Base: update Compose by changing `composeBom`, not individual `androidx.compose.ui` versions.
- Bad: `implementation("androidx.lifecycle:lifecycle-process:2.10.0")` inside `app/build.gradle.kts`.

### 6. Tests Required
- Run `bash ./gradlew test` with JDK 17 or 21.
- Run `bash ./gradlew assembleDebug` when dependencies or Android configuration change.
- If local JDK blocks validation, record `java -version`, `/usr/libexec/java_home -V`, command, and exact error.

### 7. Wrong vs Correct

#### Wrong
```kotlin
implementation("androidx.compose.material:material:1.7.5")
```

#### Correct
```kotlin
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.material)
```
