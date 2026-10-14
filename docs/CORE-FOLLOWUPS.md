# Core follow-ups at 4.1.0-help2-SNAPSHOT

## Recommended: explicit reload result and failure propagation

`QInstanceHelpContentManager.reloadHelpContent` returns void and catches/logs exceptions. It also logs/skips invalid targets. Consequently callers cannot obtain an authoritative applied-row count, skipped keys, or success/failure status.

The qbit now delegates sync to core and no longer exposes a separate reload action. Its bundle coverage counts current persisted provenance, not reload success.

A core result containing applied row count, rejected rows/reasons, and failure status (or a throwing reload variant) would remove that limitation. Cross-node propagation remains core's responsibility.

## Build-parent follow-up (outside QQQ core)

The locally cached qbit-build-parent 2.0.0 POM has `<version>${revision}</version>`. Maven 3.10 rejects it before compilation. The copied reference Maven 3.9.11 wrapper handles it (with a parent warning). Publish a flattened parent POM to remove this toolchain limitation. No global Maven cache or installation was changed for this task.
