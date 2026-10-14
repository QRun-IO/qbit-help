# Changelog

## [Unreleased]

### Added

- Author-only upstream-changed, retired, unresolved-key and per-bundle coverage tables; conditional plain-text Current/Upstream record comparison.

- Help app over core's visible `helpContent` table, with configurable backend, app name/label, hidden-target coverage, and table customization.
- Full-width typed coverage totals, missing-help links with standard create defaults, and recent changes.
- Standard streamed actions: Preview help using runtime help metadata and Check help keys including process suffix wildcards.
- Standard `help.read`/`help.insert`/`help.edit`/`help.delete`, `help.hasAccess`, and `help.author.hasAccess` permissions with host-table opt-out; standard bulk edit, import, and export capabilities.
- JUnit 5/AssertJ memory-backend integration tests, Checkstyle, JaCoCo gates, Maven 3.9.11 wrapper, and authoring/design documentation.

### Changed

- Replaced the extension-template scaffold with `com.kingsrook.qbits.help`, artifact `qbit-help`, version `0.1.0-SNAPSHOT`.
- Replaced Apply help changes now with core bundle sync, accept upstream, and keep mine actions, retaining standard Review/Result screens.
- Verified against core #1198 `4.1.0-help2-SNAPSHOT`; restored `qqq.version` to `4.1.0-RC.7` with the RC.8 stand-in deadline.

### Known limitations

- Bundle rows applied counts current persisted provenance, not historical sync execution or cluster reload success. Live host/browser validation remains outstanding.
