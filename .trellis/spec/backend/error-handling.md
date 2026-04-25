# Error Handling

> Error and diagnostics patterns for NearbyChater.

---

## Overview

NearbyChater does not currently define a large custom exception hierarchy. Runtime failures are surfaced through a small diagnostics model, Android logs, repository state updates, and persistent log files.

Primary mechanisms:

- `DiagnosticsEvent` captures a `code`, user/developer-facing `message`, optional `Throwable`, and timestamp.
- `ChatRepository` emits diagnostics through a `MutableSharedFlow` and persists important events through `LogManager`.
- `NearbyChatService` wraps Google Nearby Connections callbacks and reports connection/payload failures as events.
- ViewModels update UI state or call repository retry/cancel methods rather than throwing errors to Composables.
- Settings and log operations run on IO dispatchers and should fail gracefully without crashing the app.

---

## Error Types

- Use `DiagnosticsEvent` for recoverable runtime problems that should be visible in diagnostics UI or logs.
- Use message status (`QUEUED`, `SENDING`, `SENT`, `DELIVERED`, `CANCELLED`, `FAILED`) for chat delivery outcomes.
- Use nullable return values only when absence is expected and handled immediately, such as optional attachments or optional settings.
- Throw exceptions only for programmer errors or unrecoverable platform failures; prefer converting expected platform failures into diagnostics and state transitions.

---

## Error Handling Patterns

- Catch failures at platform boundaries: Nearby callbacks, payload parsing, notification/service startup, file IO, and database access.
- Convert send failures into message status changes so the UI can show retry/cancel affordances.
- Use `withContext(ioDispatcher)` for blocking or IO work and handle exceptions inside that coroutine boundary when user-visible state must be updated.
- Emit diagnostics with stable codes that can be searched in logs. Prefer codes such as `nearby_send_failed`, `payload_decode_failed`, or `log_read_failed` over free-form strings.
- Keep flows alive after individual operation failures. A failed settings read or send attempt should not cancel shared repository scopes.
- Use `SupervisorJob` for repository-level scopes when sibling coroutine failure should not take down unrelated streams.

---

## UI Error Responses

This app does not expose HTTP/API responses. User-facing errors should be represented in app state:

- Chat delivery failures appear through `MessageStatus.FAILED` and retry/cancel actions.
- Nearby/connectivity failures appear as diagnostics events and optional diagnostics bubbles.
- Connectivity prerequisites are handled by permission requests and connectivity warning UI in `MainActivity.kt`.
- Logs are available through `LogsScreen` even when on-screen diagnostics are disabled.

---

## Logging Failures

- If a diagnostic includes a cause, log the cause class and message; avoid dumping sensitive message content unless it is necessary for debugging.
- File logging should be best-effort. Failure to write diagnostics should not prevent chat or settings flows from continuing.
- Use Android `Log` for platform/runtime debugging and `LogManager` for persistent diagnostics that the user can inspect.

---

## Common Mistakes

- Swallowing exceptions without updating message status or emitting diagnostics.
- Throwing from Compose event handlers instead of delegating to ViewModel/repository methods.
- Letting a child coroutine cancel a long-lived repository scope unexpectedly.
- Logging raw message payloads, attachment Base64 data, endpoint IDs, or device identifiers unnecessarily.
- Treating permission denial, Bluetooth/Wi-Fi disabled state, or peer disconnects as crashes instead of expected recoverable states.
