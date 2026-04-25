# Database Guidelines

> SQLite persistence patterns and conventions for NearbyChater.

---

## Overview

The project currently uses Android framework SQLite directly through `SQLiteOpenHelper`, not Room. `AppDatabaseHelper` owns the database name, version, schema creation, migrations, and WAL mode. DAOs under `data/storage/` own all table access.

Current database facts:

- Database name: `nearbychater.db`.
- Current version: `3`.
- Tables: `members`, `conversations`, `conversation_members`, `messages`, and `settings`.
- Indexes: `idx_messages_conversation` on `(conversation_id, timestamp)`.
- Booleans are stored as `INTEGER` values (`0`/`1`).
- Timestamps are stored as epoch milliseconds in `INTEGER` columns.
- Enum values are stored as their Kotlin enum names, e.g. `MessageStatus.SENT` and `MessageType.TEXT`.

---

## Query Patterns

- Keep all SQL in DAO/helper files. UI and ViewModel layers should call repository methods, not SQL directly.
- Use parameterized `query`, `update`, `delete`, or `rawQuery` calls with selection arguments. Do not concatenate user or message content into SQL strings.
- Convert database rows through cursor extension helpers in `DatabaseExtensions.kt`, such as `toChatMessage()` and `toMemberProfile()`.
- Use `ContentValues` for inserts/updates and conflict strategies where appropriate. Settings use `CONFLICT_REPLACE` for key-value upserts.
- Preserve deterministic ordering for messages with `timestamp ASC` so latest messages render at the bottom of the chat surface.
- Wrap multi-table writes in explicit transactions when updating related rows, such as conversation metadata plus members plus messages.
- Run DAO work from repositories on `Dispatchers.IO` or an injected `CoroutineDispatcher`; do not block the main thread.

---

## Migrations

- Increment `DB_VERSION` whenever the schema changes.
- Add forward-only `if (oldVersion < N)` blocks in `onUpgrade` for every new schema version.
- Use `ALTER TABLE ... ADD COLUMN ... DEFAULT ...` for additive migrations when possible.
- Keep `onCreate` and `onUpgrade` logically aligned: a fresh install at the latest version should have the same schema as an upgraded install.
- Add or update tests for schema-sensitive changes, especially message status, attachments, conversation metadata, and settings keys.

Existing migration examples:

- Version 2 added `conversations.pinned`.
- Version 3 added `messages.message_type` with default `TEXT`.

---

## Naming Conventions

- Tables and columns use `snake_case`, e.g. `conversation_id`, `message_type`, `setting_key`.
- Primary keys are stable IDs from the domain model (`message_id`, `member_id`, `conversation_id`) rather than auto-increment IDs.
- Join tables use both entity names, such as `conversation_members`.
- Index names use `idx_<table>_<purpose>`, such as `idx_messages_conversation`.
- Settings keys use readable strings and prefixes for grouped settings; conversation aliases use the `alias:` prefix.

---

## Data Mapping Rules

- Keep model serialization annotations in `core/model/ChatModels.kt` and storage mapping in `DatabaseExtensions.kt`.
- Provide safe cursor access helpers for optional columns and backward-compatible reads.
- When adding a nullable model field, decide explicitly whether it needs a database column, a default cursor value, and a migration.
- For attachments, store metadata (`attachment_type`, `attachment_mime`) separately from payload (`attachment_data`) to keep mapping explicit.

---

## Common Mistakes

- Forgetting to increment `DB_VERSION` after changing schema.
- Updating `onCreate` but not `onUpgrade`, creating fresh-install vs upgraded-install mismatches.
- Reading cursor columns directly throughout the code instead of using mapping helpers.
- Performing database reads/writes from the main thread.
- Storing large Base64 attachments without considering file-size, memory, and migration impact.
- Adding settings keys without documenting defaults and synchronization behavior in `SettingsRepository`.
