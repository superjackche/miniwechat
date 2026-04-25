# Systematic Project Refactor and Modernization

## Goal

Systematically refactor NearbyChater to improve maintainability, modularity, build hygiene, testability, and dependency freshness while preserving current product behavior: nearby mesh chat, offline retry, diagnostics logging, settings, and Compose UI.

## User Request

- Completely refactor and optimize the project systematically.
- Finish Bootstrap Guidelines first, then use those guidelines for the refactor.
- Dependencies and packages should use latest stable releases.
- Actively adopt stable new Kotlin/Android/Jetpack features when they improve safety, simplicity, or maintainability.

## What We Know

- This is a single-module Android/Kotlin project under `app/`.
- Package root is `com.example.nearbychater`.
- Current layers are `core/`, `data/`, `ui/`, and root app bootstrapping files.
- Persistence uses framework SQLite via `SQLiteOpenHelper`, `ChatDao`, `SettingsDao`, and cursor mapping helpers.
- Messaging uses Google Nearby Connections through `NearbyChatService` and repository orchestration in `ChatRepository`.
- UI is Jetpack Compose with ViewModels exposing `StateFlow`.
- Some source files are very large and should be split: `ChatScreen.kt`, `ConversationListScreen.kt`, `ChatRepository.kt`, and `MainActivity.kt`.
- `app/build.gradle.kts` contains hard-coded dependency versions that should move into `gradle/libs.versions.toml`.
- Local Gradle verification currently fails on default JDK 25.0.2 before tests run; use JDK 17 or another Gradle/Kotlin-supported JDK for validation.

## Requirements

### R1: Preserve Behavior

- Existing chat, conversation list, settings, logs, diagnostics, foreground service, permission flow, and Nearby mesh behavior must remain functionally equivalent unless explicitly changed.
- Message ordering must remain ascending by timestamp in conversation views, with the newest message at the bottom.
- Diagnostics must continue to be logged persistently even when the diagnostics bubble is disabled.

### R2: Dependency Modernization

- Move hard-coded dependency versions from `app/build.gradle.kts` into `gradle/libs.versions.toml`.
- Verify latest stable versions from official sources or Maven metadata before upgrading.
- Avoid alpha, beta, RC, EAP, and snapshot dependencies unless explicitly justified.
- Prefer Compose BOM-managed versions for Compose artifacts.
- Keep Android API 36-only product constraint unless separately approved.

### R3: Structural Refactor

- Split large UI files into smaller focused components without changing UI behavior.
- Split `ChatRepository` into focused collaborators where appropriate, such as queue flushing, notification handling, mesh event handling, and conversation summary generation.
- Keep raw SQL and cursor mapping in `data/storage/`.
- Keep Google Nearby integration behind `NearbyChatService` or a focused data-layer wrapper.
- Keep Composables rendering state and dispatching intents; business logic belongs in ViewModels/repositories.

### R4: Quality and Safety

- Remove production backup/temporary files such as `ChatRepository.kt.backup` if not needed.
- Reduce tutorial-style comments that restate Kotlin syntax; preserve comments explaining Android/platform constraints, non-obvious behavior, schema decisions, and routing logic.
- Add or update tests around changed behavior when feasible.
- Run targeted tests/build checks with a supported JDK and document any environment blockers.

## Acceptance Criteria

- [ ] Dependency versions are centralized in `gradle/libs.versions.toml` where feasible.
- [ ] Latest-stable dependency decisions are documented in task research notes.
- [ ] Large classes/screens are decomposed into smaller focused files with no intended behavior regression.
- [ ] Storage, Nearby, repository, ViewModel, and Compose responsibilities match `.trellis/spec/backend/` guidelines.
- [ ] Obsolete backup files are removed or explicitly justified.
- [ ] Project builds/tests pass under a supported JDK, or blockers are documented with exact commands and errors.
- [ ] Specs are updated if the refactor establishes new conventions.

## Out of Scope

- Changing the core product concept or network protocol semantics beyond safe refactoring.
- Introducing unstable dependency channels by default.
- Replacing SQLiteOpenHelper with Room unless a follow-up PRD approves a database architecture migration.
- Rewriting the entire app in a different architecture or language.
- Changing Android API 36-only support unless approved.

## Technical Notes

- Follow `.trellis/spec/backend/index.md` and all topic-specific guideline files.
- Follow `.trellis/spec/guides/index.md`, especially the pre-modification search rule before changing constants, schema, settings keys, or dependency versions.
- Bootstrap research note: `.trellis/tasks/archive/2026-04/00-bootstrap-guidelines/research/dependency-policy.md`.
