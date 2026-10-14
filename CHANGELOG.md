# Changelog

## [Unreleased]

### Added

- Help app over core's visible `helpContent` table, with configurable backend, app name/label, hidden-target coverage, and table customization.
- Full-width typed coverage totals, missing-help links with standard create defaults, and recent changes.
- Standard streamed actions: Preview help using runtime help metadata, Apply help changes now, and Check help keys including process suffix wildcards.
- Standard `help.read`/`help.insert`/`help.edit`/`help.delete`, `help.hasAccess`, and `help.author.hasAccess` permissions with host-table opt-out; standard bulk edit, import, and export capabilities.
- JUnit 5/AssertJ memory-backend integration tests, Checkstyle, JaCoCo gates, Maven 3.9.11 wrapper, and authoring/design documentation.

### Changed

- Replaced the extension-template scaffold with `com.kingsrook.qbits.help`, artifact `qbit-help`, version `0.1.0-SNAPSHOT`.
- Pinned the core #1177 stand-in to `4.1.0-help-SNAPSHOT` through `qqq.version`.

### Known limitations

- Core's void reload API does not report failures or applied counts. The qbit preflights reads and confirms live entries, but cannot prove an atomic successful reload.
