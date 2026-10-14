/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qbits.help;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.actions.ai.output.AiLinkBuilder;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.help.HelpCoverageAction;
import com.kingsrook.qqq.backend.core.actions.help.HelpCoverageEntry;
import com.kingsrook.qqq.backend.core.actions.help.HelpCoverageInput;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.AlertData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.CompositeWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.QWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.text.TextBlockData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.text.TextValues;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.helpcontent.HelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.testing.HelpBundleAssertions;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;


/*******************************************************************************
 ** Typed, full-width home widgets served by the standard UI widget endpoint.
 *******************************************************************************/
public class HelpWidgets extends AbstractWidgetRenderer
{
   public static final String SUMMARY = "help_coverage_summary";
   public static final String MISSING = "help_coverage_missing";
   public static final String RECENT = "help_recent_changes";
   public static final String UPSTREAM = "help_upstream_changed";
   public static final String RETIRED = "help_retired";
   public static final String UNRESOLVED = "help_unresolved";
   public static final String BUNDLES = "help_bundle_coverage";
   public static final String DIFF = "help_upstream_diff";
   public static final List<String> HOME = List.of(SUMMARY, BUNDLES, UPSTREAM, RETIRED, UNRESOLVED, MISSING, RECENT);
   public static final int MISSING_LIMIT = 200;
   private static final Map<String, String> KIND_LABELS = Map.ofEntries(
      Map.entry("app", "Apps"), Map.entry("table", "Tables"), Map.entry("section", "Sections"),
      Map.entry("field", "Fields"), Map.entry("process", "Processes"), Map.entry("step", "Steps"),
      Map.entry("processField", "Process fields"), Map.entry("report", "Reports"),
      Map.entry("widget", "Widgets"), Map.entry("possibleValueSource", "Possible-value sources"));
   private static final List<String> KINDS = List.of("app", "table", "section", "field", "process", "step",
      "processField", "report", "widget", "possibleValueSource");



   /*******************************************************************************
    ** Full-width metadata with configuration carried per instance, not globally.
    *******************************************************************************/
   public static List<QWidgetMetaData> metadata(HelpQBitConfig config)
   {
      List<QWidgetMetaData> widgets = new ArrayList<>();
      for(String name : List.of(SUMMARY, MISSING, RECENT, BUNDLES, UPSTREAM, RETIRED, UNRESOLVED, DIFF))
      {
         String label = switch(name)
         {
            case SUMMARY -> "Help coverage";
            case MISSING -> "Missing help";
            case BUNDLES -> "Help bundle coverage";
            case UPSTREAM -> "Upstream changed";
            case RETIRED -> "Retired";
            case UNRESOLVED -> "Keys that no longer resolve";
            case DIFF -> "Current and upstream help";
            default -> "Recent changes";
         };
         widgets.add(new QWidgetMetaData().withName(name).withLabel(label).withType(WidgetType.TABLE.getType())
            .withCodeReference(new QCodeReference(HelpWidgets.class)).withGridColumns(12)
            .withDefaultValue("includeHidden", config.getIncludeHiddenInCoverage()).withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
               .withPermissionBaseName(RECENT.equals(name) ? "help" : "help.author")));
      }
      return (widgets);
   }



   /*******************************************************************************
    ** Render using core actions, with no new HTTP surface.
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      PermissionsHelper.checkWidgetPermissionThrowing(input, input.getWidgetMetaData().getName());
      if(RECENT.equals(input.getWidgetMetaData().getName()))
      {
         return (new RenderWidgetOutput(recent()));
      }
      String name = input.getWidgetMetaData().getName();
      if(DIFF.equals(name))
      {
         return (new RenderWidgetOutput(diff(input)));
      }
      if(List.of(BUNDLES, UPSTREAM, RETIRED, UNRESOLVED).contains(name))
      {
         List<QRecord> records = new QueryAction().execute(new QueryInput(HelpContent.TABLE_NAME)
            .withShouldFetchHeavyFields(true).withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id", true)))).getRecords();
         return (new RenderWidgetOutput(BUNDLES.equals(name) ? bundles(records) : review(name, records)));
      }
      boolean hidden = Boolean.TRUE.equals(input.getWidgetMetaData().getDefaultValues().get("includeHidden"));
      List<HelpCoverageEntry> entries = new HelpCoverageAction().execute(new HelpCoverageInput()).getEntries().stream()
         .filter(entry -> hidden || !entry.getHidden()).toList();
      return (new RenderWidgetOutput(MISSING.equals(input.getWidgetMetaData().getName()) ? missing(entries) : summary(entries)));
   }



   /*******************************************************************************
    ** Recount filtered entries so hidden targets affect neither numerator nor denominator.
    *******************************************************************************/
   private static TableData summary(List<HelpCoverageEntry> entries)
   {
      List<Map<String, Object>> rows = new ArrayList<>();
      List<String> kinds = new ArrayList<>(KINDS);
      entries.stream().map(HelpCoverageEntry::getKind).distinct().filter(kind -> !kinds.contains(kind)).sorted().forEach(kinds::add);
      for(String kind : kinds)
      {
         List<HelpCoverageEntry> scoped = entries.stream().filter(entry -> kind.equals(entry.getKind())).toList();
         rows.add(total(KIND_LABELS.getOrDefault(kind, kind), scoped));
      }
      rows.add(total("Overall", entries));
      return (new TableData("Targets with help", List.of(column("Kind", "kind"), column("Total", "total"),
         column("With help", "withHelp"), column("Coverage", "percent")), rows)
         .withRowsPerPage(rows.size()).withHidePaginationDropdown(true).withFixedStickyLastRow(true));
   }



   /*******************************************************************************
    ** One totals row. Empty scopes have no coverage percentage.
    *******************************************************************************/
   private static Map<String, Object> total(String label, List<HelpCoverageEntry> entries)
   {
      long withHelp = entries.stream().filter(entry -> entry.getHasContent() || entry.getHasSummary()).count();
      return (Map.of("kind", label, "total", entries.size(), "withHelp", withHelp, "percent",
         entries.isEmpty() ? "N/A" : String.format(Locale.ROOT, "%.1f%%", 100.0 * withHelp / entries.size())));
   }



   /*******************************************************************************
    ** A bounded missing list with the full missing count, and core default-value links.
    *******************************************************************************/
   private static TableData missing(List<HelpCoverageEntry> entries) throws QException
   {
      List<HelpCoverageEntry> missing = entries.stream().filter(entry -> !entry.getHasContent() && !entry.getHasSummary()).toList();
      List<Map<String, Object>> rows = new ArrayList<>();
      for(HelpCoverageEntry entry : missing.stream().limit(MISSING_LIMIT).toList())
      {
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("kind", KIND_LABELS.getOrDefault(entry.getKind(), entry.getKind()));
         row.put("label", entry.getLabel());
         row.put("parent", entry.getParentKey());
         row.put("key", entry.getKey());
         String defaults = JsonUtils.toJson(Map.of("key", entry.getKey(), "role", "ALL_SCREENS"));
         String url = AiLinkBuilder.objectHref(HelpContent.TABLE_NAME) + "/create#defaultValues="
            + URLEncoder.encode(defaults, StandardCharsets.UTF_8).replace("+", "%20");
         row.put("action", link("Add help", url));
         rows.add(row);
      }
      return (new TableData("Showing " + rows.size() + " of " + missing.size() + " targets missing help",
         List.of(column("Kind", "kind"), column("Label", "label"), column("Parent", "parent"), column("Key", "key"),
            column("Action", "action").withType("composite")), rows).withRowsPerPage(20));
   }



   /*******************************************************************************
    ** Latest standard records; QueryAction enforces host record security as well.
    *******************************************************************************/
   private static TableData recent() throws QException
   {
      List<QRecord> records = new QueryAction().execute(new QueryInput().withTableName(HelpContent.TABLE_NAME)
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("modifyDate", false))
            .withOrderBy(new QFilterOrderBy("id", false)).withLimit(20))).getRecords();
      List<Map<String, Object>> rows = new ArrayList<>();
      for(QRecord record : records)
      {
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("key", record.getValue("key"));
         row.put("summary", record.getValue("summary"));
         row.put("role", record.getValue("role"));
         row.put("modified", record.getValueString("modifyDate"));
         row.put("record", link("View help", AiLinkBuilder.recordHref(HelpContent.TABLE_NAME, record.getValue("id"))));
         rows.add(row);
      }
      return (new TableData("Most recent " + records.size() + " help entries", List.of(column("Key", "key"),
         column("Summary", "summary"), column("Role", "role"), column("Modified", "modified"),
         column("Record", "record").withType("composite")), rows).withRowsPerPage(20));
   }



   /*******************************************************************************
    ** Pending and retired rows remain ordinary record links with core actions.
    *******************************************************************************/
   private static TableData review(String name, List<QRecord> records)
   {
      List<Map<String, Object>> rows = new ArrayList<>();
      for(QRecord record : records)
      {
         boolean include = switch(name)
         {
            case UPSTREAM -> "UPSTREAM_CHANGED".equals(record.getValueString("upstreamStatus"));
            case RETIRED -> "RETIRED".equals(record.getValueString("upstreamStatus"));
            default -> !resolves(record);
         };
         if(!include)
         {
            continue;
         }
         Map<String, Object> row = new LinkedHashMap<>();
         for(String field : List.of("key", "bundleId", "summary", "upstreamSummary"))
         {
            row.put(field, record.getValue(field));
         }
         List<String> changes = new ArrayList<>();
         for(String field : List.of("summary", "content"))
         {
            String upstream = "upstream" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
            if(!Objects.equals(record.getValueString(field), record.getValueString(upstream)))
            {
               changes.add(field + " changed");
            }
         }
         row.put("changes", String.join(", ", changes));
         row.put("record", link("View help", AiLinkBuilder.recordHref(HelpContent.TABLE_NAME, record.getValue("id"))));
         rows.add(row);
      }
      List<TableData.Column> columns = new ArrayList<>(List.of(column("Key", "key"), column("Bundle", "bundleId"), column("Current summary", "summary")));
      if(UPSTREAM.equals(name))
      {
         columns.add(column("Upstream summary", "upstreamSummary"));
         columns.add(column("Changes", "changes"));
      }
      columns.add(column("Record", "record").withType("composite"));
      return (new TableData(rows.size() + " help entries", columns, rows).withRowsPerPage(20));
   }



   /*******************************************************************************
    ** Counts persisted current rows of each registered bundle version.
    *******************************************************************************/
   private static TableData bundles(List<QRecord> records)
   {
      List<Map<String, Object>> rows = new ArrayList<>();
      for(var bundle : HelpBundles.of(QContext.getQInstance()).getBundles())
      {
         long applied = records.stream().filter(record -> bundle.getId().equals(record.getValueString("bundleId"))
            && bundle.getVersion().equals(record.getValueString("bundleVersion"))
            && "CURRENT".equals(record.getValueString("upstreamStatus"))
            && resolves(record)).count();
         rows.add(Map.of("bundle", bundle.getId(), "version", bundle.getVersion(), "entries", bundle.getEntries().size(), "applied", applied));
      }
      return (new TableData("Registered bundles (rows applied at the current version)", List.of(column("Bundle", "bundle"),
         column("Version", "version"), column("Entries", "entries"), column("Rows applied", "applied")), rows).withRowsPerPage(20));
   }



   /*******************************************************************************
    ** Core owns key grammar; malformed legacy rows can have a null key.
    *******************************************************************************/
   private static boolean resolves(QRecord record)
   {
      String key = record.getValueString("key");
      return (key != null && HelpBundleAssertions.resolves(QContext.getQInstance(), key));
   }



   /*******************************************************************************
    ** Fetch the secured record, never trust status or content from query parameters.
    ** A typed hidden response suppresses the widget outside pending reviews.
    *******************************************************************************/
   private static QWidgetData diff(RenderWidgetInput input) throws QException
   {
      String id = input.getQueryParams().get("id");
      QRecord record = id == null || id.isBlank() ? null : new GetAction().execute(new GetInput(HelpContent.TABLE_NAME)
         .withPrimaryKey(id).withShouldFetchHeavyFields(true)).getRecord();
      if(record == null || !"UPSTREAM_CHANGED".equals(record.getValueString("upstreamStatus")))
      {
         return (new AlertData().withHideWidget(true));
      }
      List<Map<String, Object>> rows = new ArrayList<>();
      for(String field : List.of("Summary", "Content"))
      {
         rows.add(Map.of("current", field + ":\n" + Objects.toString(record.getValueString(field.toLowerCase(Locale.ROOT)), ""),
            "upstream", field + ":\n" + Objects.toString(record.getValueString("upstream" + field), "")));
      }
      return (new TableData("Compare Summary and Content before accepting upstream or keeping yours",
         List.of(column("Current", "current"), column("Upstream", "upstream")), rows)
         .withRowsPerPage(2).withHidePaginationDropdown(true));
   }



   /*******************************************************************************
    ** Standard plain-text column.
    *******************************************************************************/
   private static TableData.Column column(String label, String accessor)
   {
      return (new TableData.Column("default", label, accessor, null, "left"));
   }



   /*******************************************************************************
    ** Typed text/link blocks, never HTML markup.
    *******************************************************************************/
   private static CompositeWidgetData link(String label, String url)
   {
      return (new CompositeWidgetData().withLayout(CompositeWidgetData.Layout.FLEX_ROW)
         .withBlock(new TextBlockData().withValues(new TextValues(label)).withLink(url)));
   }
}
