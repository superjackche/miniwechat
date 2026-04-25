# Logging Guidelines

> Runtime and persistent diagnostics conventions for NearbyChater.

---

## Overview

The project uses two logging channels:

- Android `Log` for development/runtime diagnostics such as display refresh rate and platform callback debugging.
- `core/logging/LogManager.kt` for persistent on-device diagnostics under the app files directory.

Diagnostics are part of the product experience. Even when the diagnostics bubble is disabled in settings, important events should still be written to persistent logs when they help support demos and debugging.

---

## Log Levels

- `DEBUG`: noisy development-only details, temporary investigation, refresh-rate capability output, payload routing traces.
- `INFO`: normal lifecycle and user-visible operational events, such as mesh start/stop, peer connected/disconnected, queue flush start/end, settings changes.
- `WARN`: recoverable abnormal states, such as send retry scheduled, peer unavailable, Bluetooth/Wi-Fi disabled, permission missing.
- `ERROR`: operation failed and needs attention, such as payload decode failure, database write failure, log file failure, or unrecoverable Nearby API callback errors.

Prefer persistent `LogManager` entries for `INFO`, `WARN`, and `ERROR` events that would help diagnose user/device behavior later. Keep `DEBUG` mostly in Android `Log` unless actively needed in the in-app log viewer.

---

## Structured Logging

- Use a stable tag or code for searchable diagnostics. Diagnostics events should include a stable `code` plus concise message.
- Include context fields in the message when useful: conversation ID, message ID, status transition, peer count, retry count, or operation name.
- Avoid multiline log messages except stack traces or explicit diagnostic dumps.
- Keep log entries concise; large payloads and Base64 attachments do not belong in logs.
- Preserve timestamps from `DiagnosticsEvent` or `LogManager` instead of manually formatting time at call sites.

---

## What to Log

- Nearby lifecycle: advertising/discovery start and stop, endpoint discovery, connection request, accept/reject, connection loss.
- Message flow: enqueue, send attempt, send success, delivery/ack, retry, cancellation, failure.
- Queue behavior: pending queue size, retry loop trigger, peer availability changes.
- Settings changes: diagnostics enabled/disabled, background service enabled/disabled, alias updates.
- Persistence operations when they fail or when migrations run.
- Foreground service lifecycle and notification setup failures.

---

## What NOT to Log

- Raw chat message content unless the user explicitly enables a debug-only diagnostic mode.
- Attachment Base64 data or image bytes.
- Secrets, tokens, keystore material, or signing configuration.
- Full device identifiers, Android ID, endpoint IDs, or stable member IDs when a short hash or suffix is enough.
- Excessively frequent Compose recomposition details.

---

## Common Mistakes

- Using Android `Log` for important support events that should be visible in `LogsScreen`.
- Logging only the exception message without a stable diagnostic code.
- Logging sensitive payload data while debugging Nearby send/receive problems.
- Emitting repeated identical logs inside retry loops without throttling or useful context.
- Treating persistent logging failure as fatal to chat functionality.
