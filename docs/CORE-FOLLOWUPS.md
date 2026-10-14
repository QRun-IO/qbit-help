# Core follow-ups at 4.1.0-help-SNAPSHOT

## Recommended: explicit reload result and failure propagation

`QInstanceHelpContentManager.reloadHelpContent` returns void and catches/logs exceptions. It also logs/skips invalid targets. Consequently callers cannot obtain an authoritative applied-row count, skipped keys, or success/failure status.

The qbit preflights a database read, calls reload, and confirms source rows against data-applied live targets, reporting unconfirmed rows as errors. This avoids claiming that every database row was applied. It cannot prove atomic success, distinguish an unchanged old matching entry from a newly applied entry after a caught failure, or authoritatively confirm deletions across a failed reload.

A core result containing applied row count, rejected rows/reasons, and failure status (or a throwing reload variant) would remove that limitation. Cross-node propagation remains core's responsibility.

## Build-parent follow-up (outside QQQ core)

The locally cached qbit-build-parent 2.0.0 POM has `<version>${revision}</version>`. Maven 3.10 rejects it before compilation. The copied reference Maven 3.9.11 wrapper handles it (with a parent warning). Publish a flattened parent POM to remove this toolchain limitation. No global Maven cache or installation was changed for this task.
