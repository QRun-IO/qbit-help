# QBit Help

The authoring experience for QQQ in-app help: a **Help** app, coverage dashboard, and standard record/table actions. Storage, application, targeting, rendering, and search stay in QQQ core. This qbit adds no tables, filesystem storage, HTTP routes, HTML widgets, or other-qbit dependencies.

## Requirements and installation

- Java 21 and `qbit-build-parent` 2.0.0.
- QQQ with help bundles (QRun-IO/qqq#1198). Verification uses the locally installed `4.1.0-help2-SNAPSHOT`; the checked-in pin is `4.1.0-RC.7`, with the stand-in removed by RC.8. This is not a claim that published RC.7 contains the bundle APIs.
- A QQQ UI using `/qqq/v1`, including its widget and process endpoints. Next supplies the help renderer and in-context author mode.
- A host backend supporting the standard core `helpContent` table. The host owns database schema creation/migration; the producer defines metadata, not database DDL.

```xml
<dependency>
   <groupId>com.kingsrook.qbits</groupId>
   <artifactId>qbit-help</artifactId>
   <version>0.1.0-SNAPSHOT</version>
</dependency>
```

This version has not been published. Build/install it locally or use your organization's development artifact repository.

```java
new HelpQBitProducer()
   .withQBitConfig(new HelpQBitConfig()
      .withBackendName("rdbms"))
   .produce(qInstance);

// Then run the host's normal QInstance enrichment/validation and startup.
```

If `helpContent` is already registered, `backendName` may be omitted. The existing table, backend mapping, fields, record locks, and core change customizers are retained. The producer sets its visibility, authoring permissions, and standard capabilities. Configure `tableMetaDataCustomizer` for backend-specific details or host customization; it must return the same logical `helpContent` table and preserve core's required fields/customizers.

| Configuration | Default | Purpose |
|---|---|---|
| `backendName` | unset | Required only when the host has no `helpContent` table; must name an existing backend. |
| `appName` | `help` | App identity. |
| `appLabel` | `Help` | App label. |
| `includeHiddenInCoverage` | `false` | Include hidden targets in both coverage totals and missing-help rows. |
| `applyPermissionRulesToHelpContent` | `true` | Apply help table rules to existing host storage; newly defined storage always receives them. |
| `tableMetaDataCustomizer` | unset | Customize the existing or newly defined core table. |

The app's only navigation child is **Help Content**. Preview, key checking, and core bundle sync/accept/keep processes are actions on that table. The defaults for new rows are `ALL_SCREENS` and `MARKDOWN`, unless the host has supplied other defaults.

## Permissions

Only standard `QPermissionRules` are used; there is no custom permission checker.

| Permission | Grants |
|---|---|
| `help.read` | Reading/exporting help rows. |
| `help.insert`, `help.edit`, `help.delete` | Corresponding help row operations. |
| `help.hasAccess` | Help app and recent-changes widget. |
| `help.author.hasAccess` | Coverage and bundle review widgets, preview, key checking, and core sync/accept/keep actions. |

Give viewers `help.read` and `help.hasAccess`. Give authors both viewer grants plus `help.insert`, `help.edit`, `help.delete`, and `help.author.hasAccess`; author access does not imply any other grant. Core coverage additionally requires insert or edit access to `helpContent`.

The table uses `READ_INSERT_EDIT_DELETE_PERMISSIONS` with base name `help`. App/recent changes use `HAS_ACCESS_PERMISSION` with base `help`; coverage/processes use the same level with base `help.author`. Core's `PermissionsHelper.getEffectivePermissionSubType` retains `HAS_ACCESS` for non-table checks and `getPermissionName` always appends the subtype suffix, so a base of `help.read` would generate `help.read.hasAccess`, not reuse `help.read`. All six names above are discoverable through standard permission metadata. Viewer table metadata reports read allowed and insert/edit/delete denied; authors receive all four flags.

By default the qbit applies its table rules even to an existing host table. Set `withApplyPermissionRulesToHelpContent(false)` to preserve the rules on an existing table (including customizer changes). A table newly defined by this qbit always receives the standard help rules. When preserving host rules, grant the host's corresponding table permissions instead.

The `/qqq/v1` middleware enforces permissions. For direct Java action callers, configure the host's `qInstance.withEnforcePermissionsInCoreActions(true)` as usual; this qbit does not change that instance-wide policy. No assistant/client-channel wall is added. Core enforces individual help-row audience targeting when serving help.

## Coverage report

The app home has seven full-width typed `TABLE` widgets:

- `help_coverage_summary`: total targets, targets with help, and percentage for apps, tables, sections, fields, processes, steps, process fields, reports, widgets, and possible-value sources, plus Overall. Empty kinds show `N/A`; a summary alone counts as help.
- `help_coverage_missing`: kind, label, parent key, canonical key, and **Add help**. It displays at most 200 missing targets, with a visible "Showing N of M" count and pagination. Links open Next's standard create screen with `key` and `ALL_SCREENS` prefilled using QQQ's `#defaultValues=` URL mechanism.
- `help_recent_changes`: the 20 most recently modified help rows, newest first, with links to their full record screens.

- `help_bundle_coverage`: each registered bundle's id, version, declared entry count, and rows applied. Here "rows applied" means persisted, resolving `CURRENT` rows attributed to that bundle/version; pending, retired, old-version, and unresolved rows are excluded. It is not a historical sync-run count or proof of cluster-wide reload.
- `help_upstream_changed`: pending rows with key, bundle, current/upstream summaries, a short change indicator, and record links.
- `help_retired`: edited bundle rows retained by core after retirement, with record links.
- `help_unresolved`: rows whose keys no longer resolve using core's `HelpBundleAssertions.resolves`, including its extensible slot and wildcard rules.

Pending records also have a full-width `help_upstream_diff` TABLE section: Current and Upstream columns, Summary and Content rows. Values are literal text, including Markdown/HTML source. The widget fetches the secured record by id and only returns a comparison for `UPSTREAM_CHANGED`. Other statuses return core's typed hidden-widget response. The section is hidden on edit screens. All review widgets require `help.author.hasAccess` in addition to normal table access.

Coverage comes from `HelpCoverageAction`. It reflects the current session/client's help visibility, not every audience's combined coverage. Hidden targets are excluded from both numerator and denominator unless configured otherwise. Declared instance-level slots, if present, appear as an additional kind and contribute to Overall. The report counts canonical standard slots; it is not a matrix of every role or targeting variant.

## Authoring workflow

1. Open **Help** and use **Add help** in the missing list, or add `?helpHelp` to a Next page to see target keys and in-context **Add help** links. Next implements author mode; no frontend code is bundled here.
2. Edit the standard help record: key, role, summary, format, content, and optional permission/client targeting. Prefer a short plain summary and Markdown for longer content.
3. On the saved record, choose **Preview help**. Its standard Review and Result steps carry the selected content to Next's normal help renderer; open the step's help icon to view it. A view form shows summary, key, role, format, permission, and client channel. The preview makes the content visible to its author regardless of its intended screen/audience; the original targeting is shown in the form and is not modified.
4. Choose **Check help keys** for selected rows or the current query. Review and Result show `N keys resolve`, `N keys do not match any object`, and individual bad keys as standard error lines. This checks target existence, including hidden targets and process suffix wildcards; it does not rewrite records or validate content quality.
5. Use **Sync help from bundles** from the Help Content table to seed registered bundles or check newer versions. Core owns its standard Review/Result lines and reload behavior. This replaces the qbit's former **Apply help changes now** action; `help_apply_now` is removed.
6. Open an **Upstream changed** row, compare Current and Upstream, then use **Accept upstream text** or **Keep my text**. These are core record actions with Review/Result screens. Core protects edits and rejects a stale pending review if the bundle changes again. Retired and unresolved rows remain available for an author to inspect and edit/delete using normal table actions.

Normal QQQ inserts/edits/deletes apply through core customizers. Hosts needing a reload after direct database writes can call core's `QInstanceHelpContentManager.reloadHelpContent`; this qbit no longer supplies a separate reload UI action. Cross-node propagation remains core's responsibility.

## Bulk edit, import and export

Use QQQ's standard table actions: **Bulk Insert** from file, **Bulk Edit**, **Bulk Edit With File**, and **Export**. The table enables the normal insert/update/delete/query/get/count/export capabilities; core enrichment supplies the bulk processes. This qbit implements no file parser, upload endpoint, or export writer.

An import should include `key`, `role`, `format`, `summary`, and `content`, plus optional `permissionName` and `clientChannel`. Use the standard edit/import mapping for updating existing rows. Core's unique key is `(key, role)`; targeting is not an extra uniqueness dimension. Run **Check help keys** after an import.

## Key reference

Names are metadata names, not display labels. Keys are case-sensitive. Copy canonical keys from the coverage list whenever possible.

| Target | Key |
|---|---|
| App | `app:{app};slot:help` |
| Table | `table:{table};slot:help` |
| Section | `table:{table};section:{section}` |
| Field | `table:{table};field:{field}` |
| Process | `process:{process};slot:help` |
| Frontend process step | `process:{process};step:{step}` |
| Process field | `process:{process};field:{field}` |
| Report | `report:{report};slot:help` |
| Widget title | `widget:{widget};slot:label` |
| Possible-value source | `possibleValueSource:{source};slot:help` |
| Declared instance slot | `instanceLevel:true;slot:{slot}` |
| Process suffix wildcard | `process:*{suffix};step:{step}` (also field/slot keys) |

Explicitly declared custom slots are accepted by key checking through core's target catalog. Unknown application/plugin-specific key grammars are outside this checker's scope. A wildcard must match at least one real process and the specified step/field/slot; `*` alone is not a valid suffix.

Roles: `ALL_SCREENS`, `READ_SCREENS`, `WRITE_SCREENS`, `QUERY_SCREEN`, `VIEW_SCREEN`, `EDIT_SCREEN`, `INSERT_SCREEN`, `PROCESS_SCREEN`, `APP_SCREEN`, `TABLE_ACTION_MENU`. Formats: `TEXT`, `MARKDOWN`, `HTML`. Core and the client own rendering and sanitization.

`helpContent` is a documented **pre-standard naming exception**: this core table predates QBit snake_case naming, and existing applications depend on its name. Do not rename it or create a qbit-prefixed replacement.

## Build and design

```sh
./mvnw -o -Dqqq.version=4.1.0-help2-SNAPSHOT verify
```

On Windows use `mvnw.cmd`. The wrapper pins Maven 3.9.11 because Maven 3.10 rejects the current build parent's unresolved `${revision}` POM. `mvn -o verify` works with Maven 3.9.11 on PATH. Verification runs JUnit 5/AssertJ memory-backend integration tests, Checkstyle, and JaCoCo gates of 70% instructions and 90% classes.

See [design](docs/DESIGN.md), [core follow-ups](docs/CORE-FOLLOWUPS.md), and [session state](docs/SESSION-STATE.md). Apache-2.0; see [LICENSE](LICENSE).
