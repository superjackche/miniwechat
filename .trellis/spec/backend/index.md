# Backend Development Guidelines

> Best practices for backend development in this project.

---

## Overview

This directory contains project-specific guidelines for NearbyChater's Android/Kotlin backend-style layers: data, storage, services, repositories, logging, and app architecture boundaries. The app is a single-module Jetpack Compose project, so these guidelines also document cross-layer rules that keep UI, ViewModel, repository, platform, and SQLite code separated.

---

## Guidelines Index

| Guide | Description | Status |
|-------|-------------|--------|
| [Directory Structure](./directory-structure.md) | Module organization and file layout | Filled |
| [Database Guidelines](./database-guidelines.md) | SQLite patterns, queries, migrations | Filled |
| [Error Handling](./error-handling.md) | Diagnostics events, state transitions, handling strategies | Filled |
| [Quality Guidelines](./quality-guidelines.md) | Kotlin/Android standards, dependency policy, forbidden patterns | Filled |
| [Logging Guidelines](./logging-guidelines.md) | Android Log and persistent diagnostics conventions | Filled |

---

## Pre-Development Checklist

Before changing app code, read:

1. [Directory Structure](./directory-structure.md)
2. [Quality Guidelines](./quality-guidelines.md)

Also read the topic-specific guide for the area being changed:

- Persistence/schema/query work: [Database Guidelines](./database-guidelines.md)
- Nearby, repository, service, or failure-state work: [Error Handling](./error-handling.md)
- Diagnostics, log viewer, or troubleshooting work: [Logging Guidelines](./logging-guidelines.md)

Always read shared thinking guides in `.trellis/spec/guides/index.md` and follow the pre-modification search rule before changing constants, settings keys, schema values, or dependency versions.

---

## How to Use These Guidelines

These docs describe the current project reality plus near-term refactor direction. Match existing behavior first, then improve structure incrementally when the task scope allows it.

---

**Language**: All documentation should be written in **English**.
