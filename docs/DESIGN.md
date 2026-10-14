# Help authoring design

## Boundary

`qbit-help` is an extension around QQQ core #1177. It owns only authoring metadata and action/widget renderers. Core owns `HelpContent`, `QHelpContent`, database customizers, targeting, `HelpCoverageAction`, `HelpTargetCatalog`, `HelpSearchAction`, and `QInstanceHelpContentManager`. Search is therefore not reimplemented here.

The sole data table is core `helpContent`, an approved pre-standard naming exception. The producer uses `HelpContentMetaDataProvider.withTargetingFields(true)` only when the table is absent. In that case a registered backend is required. Existing storage remains the host's responsibility. The table customizer runs before the qbit applies visibility, capabilities, and permissions; it cannot rename the table. Existing field defaults are retained, otherwise role/format default to ALL_SCREENS/MARKDOWN.

## Metadata and permissions

One app defaults to name `help`, label Help, and contains only `helpContent` as a navigation child. The three actions carry `tableName=helpContent` and are not app children. Preview requires one selected record. Apply-now has min/max input count zero, so the table menu launches it with no selection. Key checking uses the selected records or current query through `ExtractViaQueryStep`.

Permissions use only standard `QPermissionRules`. The table uses `READ_INSERT_EDIT_DELETE_PERMISSIONS`, base `help`, generating `help.read`, `help.insert`, `help.edit`, and `help.delete`. The app/recent widget use `HAS_ACCESS_PERMISSION`, base `help`, generating `help.hasAccess`. Coverage widgets and all three processes use `HAS_ACCESS_PERMISSION`, base `help.author`, generating `help.author.hasAccess`. Core always appends `.hasAccess` for these non-table checks; it cannot reuse the exact `help.read` name via a base-name override. Authors need all six grants; viewers need read and app access. Core's subtype-aware table metadata checks now accurately deny viewer write flags without core changes.

The producer applies table rules when it defines the table or `applyPermissionRulesToHelpContent` is true (the default). Opting out on an existing table preserves host/customizer rules. Host record locks remain intact. Widget rendering checks standard widget permissions; core process execution checks process rules. Core coverage additionally requires table insert or edit permission. No denied-client rules are introduced.

## Dashboard

All widgets are typed TABLE data at 12 grid columns. Coverage totals are a compact table, with per-kind rows and a fixed overall row. No HTML payload is emitted; row actions are CompositeWidgetData containing TextBlockData with typed links.

A coverage render requests the complete core coverage action result, removes hidden entries unless configured otherwise, and recomputes totals from those same entries. Thus the missing list and totals have the same denominator and audience. Summary-only entries count. All ten requested kinds appear even when their count is zero; empty scopes have N/A coverage. Core may also supply declared instance slots.

The missing list is deterministic, bounded to 200 rows, with 20-row pages and a total count in its label. Recent changes uses QueryAction, descending modifyDate and id, limited to 20. The qbit does not add a storage cache.

Next's links use `AiLinkBuilder` for `/app/{table}` and record URLs. Create defaults follow the JSON/URI-encoded `#defaultValues=` convention found in `AbstractHTMLWidgetRenderer.linkTableCreateWithDefaultValues`. That older helper's app-hierarchy path is not used for Next. Spaces are `%20`, not `+`, because Next decodes hash values with decodeURIComponent. The UI accesses data only via its standard `/qqq/v1` widgets, table, and process endpoints.

## Actions

Every action is a StreamedETLWithFrontendProcess with full validation and the standard Review and Result components. Execution does not write authoring records. The row source for preview/check is the normal query extractor with heavy content fields enabled. The reload extractor emits one in-memory work item so an empty table still reloads and clears old help. No files are read or written by the runtime code.

Preview's transform clones the run's Review/Result frontend steps and attaches a QHelpContent built from the selected record's summary, content, and format. The adjustment is sent through ProcessMetaDataAdjustment.withUpdatedFrontendStepList. Each step retains its normal summary component and adds a VIEW_FORM for the record's key/role/format/summary/targeting. Only the preview payload uses ALL_SCREENS without audience targeting, so an author can preview an EDIT_SCREEN or permission-targeted entry; saved data and shared metadata remain unchanged. No HELP_TEXT fallback is needed.

Check-keys builds a set from HelpCoverageAction plus core's full HelpTargetCatalog (for explicitly declared nonstandard slots). Hidden targets remain valid. A leading-star process name means a nonempty suffix and must match a real canonical target including its exact step/field/slot. Bad keys appear in standard error summary lines; this diagnostic does not mutate them.

Apply-now first performs an ordinary backend read so read failures surface. It then calls core reload and compares the resulting data-applied help on matching targets with each visible source row (content, summary, format, role, permission, client). A wildcard row counts once even if applied to several targets. Confirmed rows count toward `N help entries applied`; unconfirmed nonempty rows produce error lines. Empty rows are removal instructions, not applied entries. Reload scope is the running instance; other nodes use core polling. Core's current void/catching reload method prevents a complete success/atomicity guarantee.

## Verification

Tests use a real memory backend, core instance enrichment/validation, explicit core permission enforcement, ordinary table actions, and direct backend writes to simulate out-of-band edits. The ProcessRunner follows a real UI run through Review and Result. Tests cover canonical and wildcard keys, hidden targets, bounded dashboard data, Next link/default encoding, metadata ownership, saved targeting versus preview presentation, reload insert/update/delete, malformed keys, read/write permissions, viewer/author metadata flags, and table permission ownership.

A live browser/host/database deployment has not been exercised in this repository. No frontend implementation or core files are part of this change.
