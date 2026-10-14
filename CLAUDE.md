# qbit-help

QQQ in-app help authoring only. Core owns storage, targeting, search, application, and coverage enumeration. Read README.md, docs/DESIGN.md, and docs/CORE-FOLLOWUPS.md before changing behavior.

## Build

Java 21, `qbit-build-parent` 2.0.0, `qqq.version=4.1.0-RC.7`; verify bundle work with local `-Dqqq.version=4.1.0-help2-SNAPSHOT` (QRun-IO/qqq#1198), stand-in deadline RC.8. Use `./mvnw -o verify` (`mvnw.cmd` on Windows), then online if missing artifacts. Maven 3.10 cannot load the current parent's unflattened `${revision}`; wrapper is 3.9.11.

JUnit 5 + AssertJ, real QQQ memory backend and streamed process runner. Checkstyle is copied from qbit-webhooks. JaCoCo requires 70% instruction and 90% class coverage. Do not skip gates or exclude authoring classes.

## Source map

- `HelpQBitConfig`: backend/app/coverage/table customization.
- `HelpQBitProducer`: reuse or define core table; app, widgets, actions.
- Permissions: standard help CRUD rules, `help.hasAccess`, and `help.author.hasAccess`; host-table opt-out in config.
- `HelpWidgets`: seven full-width typed home tables and the conditional record comparison; Next links and standard default-values hash.
- `HelpProcesses`: streamed Review/Result workflows and per-run preview metadata.

## Invariants

- Add no tables; use `helpContent`, the approved pre-standard naming exception.
- Do not replace host backend details, record locks, or core lifecycle customizers.
- One Help app; navigation contains only the help table. Processes are table/record actions.
- No HTML widgets, runtime filesystem I/O, other-qbit dependencies, custom routes, or non-UI API paths.
- Use QQQ's typed help renderer and standard import/export. No custom dry-run forms.
- No global per-instance config cache. Widget metadata carries its own coverage flag.
- Do not mutate shared process help for a preview. Use runtime frontend-step adjustments.
- Do not wall help off from the assistant.
- Preserve user-owned changes. No commits, pushes, PRs, or edits outside this repository without new authorization.

## Style

3 spaces, Allman braces, `return (x);`, flower-box Javadoc, three blank lines between methods, sorted imports, fluent `withX()` accessors, reference Apache license header. Favor core primitives and short classes over new abstractions.
