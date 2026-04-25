# Directory Structure

> Android/Kotlin module organization for NearbyChater.

---

## Overview

NearbyChater is a single-module Android app under `app/`. The Kotlin package root is `com.example.nearbychater`, and code is organized by responsibility rather than by screen alone:

- `core/` holds domain models and cross-cutting primitives.
- `data/` holds persistence, Nearby Connections, repository orchestration, settings, and foreground service integration.
- `ui/` holds Jetpack Compose screens, theme, and ViewModels.
- root package files (`MainActivity.kt`, `MiniwechatApplication.kt`) bootstrap the Android app, dependency graph, permissions, navigation, and service startup.

Business rules should live in repositories or service wrappers, not directly inside Composables. UI should consume `StateFlow` through lifecycle-aware collection and call ViewModel intent methods.

---

## Directory Layout

```text
app/src/main/java/com/example/nearbychater/
├── MainActivity.kt                         # Activity, permission flow, root navigation shell
├── MiniwechatApplication.kt                # Application-level repository/service wiring
├── core/
│   ├── logging/LogManager.kt               # Persistent diagnostics log files
│   └── model/ChatModels.kt                 # Serializable domain/network models
├── data/
│   ├── chat/ChatRepository.kt              # Chat orchestration, cache, mesh events, queue flush
│   ├── nearby/NearbyChatService.kt         # Google Nearby Connections wrapper
│   ├── service/ChatForegroundService.kt    # Long-running foreground service
│   ├── settings/SettingsRepository.kt      # Settings state and persistence facade
│   └── storage/                            # SQLiteOpenHelper DAOs and cursor mappers
├── ui/
│   ├── ChatScreen.kt                       # Conversation UI
│   ├── ConversationListScreen.kt           # Conversation list UI
│   ├── LogsScreen.kt                       # Diagnostics log viewer
│   ├── SettingsScreen.kt                   # Settings UI
│   ├── state/                              # ViewModels and factories
│   └── theme/                              # Compose Material theme
└── util/ClipboardUtils.kt                  # Small Android utility helpers
```

Tests follow Android defaults:

```text
app/src/test/java/com/example/nearbychater/          # JVM unit tests
app/src/androidTest/java/com/example/nearbychater/   # Instrumentation/Compose tests
```

---

## Module Organization

- Add new domain objects to `core/model/` when they are shared across UI, repository, storage, or network payloads.
- Add persistence-specific code to `data/storage/`; keep raw SQL and cursor conversion there.
- Add platform/network APIs behind `data/nearby/` or another `data/<capability>/` package; do not call platform SDKs directly from UI.
- Add orchestration that combines multiple data sources to `data/chat/` or the closest repository package.
- Add screen-level UI to `ui/` and state holders to `ui/state/`; ViewModels expose immutable `StateFlow` or read-only flow types.
- Keep app bootstrapping in `MiniwechatApplication.kt` and root app wiring in `MainActivity.kt`. Avoid creating repositories inside Composables except through existing factories or application wiring.

---

## Naming Conventions

- Models use noun-based names such as `ChatMessage`, `MemberProfile`, `ConversationSnapshot`, and `DiagnosticsEvent`.
- Repositories use `<Feature>Repository`; DAOs use `<Feature>Dao`; platform wrappers use descriptive service names like `NearbyChatService`.
- Compose screens use `<Feature>Screen`; small private Composables can use descriptive names in the same file when they are not reused.
- Mutable flow backing properties use `_name`; public read-only properties use `name`.
- SQLite tables and columns use lowercase `snake_case`; Kotlin properties use `camelCase`. Cursor mapping belongs in `DatabaseExtensions.kt`.
- Constants use `private const val` near the code that owns them unless they are part of a public model contract.

---

## Examples

- `core/model/ChatModels.kt` is the canonical place for serializable chat, member, attachment, mesh, and diagnostics models.
- `data/storage/AppDatabaseHelper.kt` owns schema creation, DB versioning, and migrations.
- `data/storage/ChatDao.kt` owns chat persistence operations and should be the pattern for new SQLite DAOs.
- `data/chat/ChatRepository.kt` shows the current repository pattern: combine DAO cache, Nearby events, retry queue, notifications, and diagnostics into UI-facing flows.
- `ui/state/ChatViewModel.kt` shows the UI-state boundary: expose flows, keep Android/IO work off the main thread, and delegate business work to repositories.

---

## Anti-Patterns

- Do not put raw SQL or cursor column names in ViewModels or Composables.
- Do not add more large responsibilities to already-large files without first considering extraction; `ChatScreen.kt`, `ConversationListScreen.kt`, `ChatRepository.kt`, and `MainActivity.kt` are candidates for future decomposition.
- Do not duplicate dependency construction in multiple UI entry points; prefer application-level wiring or an explicit factory.
- Do not keep backup source files such as `*.kt.backup` in production source trees.

---

## Scenario: Post-Refactor Responsibility Boundaries

### 1. Scope / Trigger
- Trigger: large-file decomposition, app-shell extraction, and repository collaborator extraction.

### 2. Signatures
- Root app shell: `MainActivity.onCreate()`, `NearbyChaterApp()`, `NearbyChaterNavHost(...)`, `requiredPermissions()`, `handleForegroundService(...)`.
- UI screens: `ChatScreen(...)` and `ConversationListScreen(...)` remain route-level entry points.
- Repository facade: `ChatRepository` public methods and flows remain the data contract for `ChatViewModel`.

### 3. Contracts
- Routes stay stable: `home`, `chat/{conversationId}`, `settings`, and `logs`.
- One shared `ChatViewModel` instance is passed from the app shell to route screens; extracted child composables receive state and callbacks, not repositories or whole ViewModels.
- `ChatRepository` stays a facade; helpers such as summary builders, outbound queues, dedupe policy, and notification presenters are internal collaborators.

### 4. Validation & Error Matrix
- Missing route name or changed path argument -> existing deep navigation breaks.
- Child Composable depends on a ViewModel unnecessarily -> UI becomes harder to preview and test.
- Helper changes a public repository method signature -> `ChatViewModel` and app wiring break.

### 5. Good/Base/Bad Cases
- Good: `ChatScreen` collects flows, computes screen state, and calls `MessageList(messages, onRetry, onCancel, ...)`.
- Base: `ChatRepository.sendMessage(...)` persists, updates memory, and delegates queue/notification details internally.
- Bad: extracted UI component imports `ChatRepository`, raw SQL constants, or `NearbyChatService`.

### 6. Tests Required
- Navigation smoke test asserts route names and `conversationId` argument behavior.
- Compose tests assert child components dispatch callbacks for send/retry/cancel/delete/pin.
- Repository tests assert ACK delivery, retry/cancel state transitions, and self-conversation behavior through the facade.

### 7. Wrong vs Correct

#### Wrong
```kotlin
@Composable
fun MessageList(viewModel: ChatViewModel) { /* collects and mutates business state */ }
```

#### Correct
```kotlin
@Composable
fun MessageList(
    messages: List<ChatMessage>,
    onRetry: (String) -> Unit,
    onCancel: (String) -> Unit
) { /* renders state and emits user intents */ }
```
