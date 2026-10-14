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


import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.help.HelpCoverageAction;
import com.kingsrook.qqq.backend.core.actions.help.HelpCoverageInput;
import com.kingsrook.qqq.backend.core.actions.metadata.TableMetaDataAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.CompositeWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.helpcontent.HelpContentMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.help.HelpFormat;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpRole;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryInsertAction;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Memory-backend integration tests for metadata, widgets and real process runs.
 *******************************************************************************/
class HelpQBitTest
{
   private QInstance instance;



   /*******************************************************************************
    ** Build and validate a fresh memory instance before every test.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      MemoryRecordStore.fullReset();
      instance = base();
      new HelpQBitProducer().withQBitConfig(new HelpQBitConfig().withBackendName("memory")).produce(instance);
      new QInstanceValidator().validate(instance);
      as("help.read", "help.insert", "help.edit", "help.delete", "help.hasAccess", "help.author.hasAccess");
   }



   /*******************************************************************************
    ** A host instance with ordinary, visible and hidden field targets.
    *******************************************************************************/
   private static QInstance base()
   {
      QInstance instance = new QInstance().withEnforcePermissionsInCoreActions(true);
      instance.withInstanceDefaultAuthentication(new QAuthenticationMetaData().withName("anonymous").withType(QAuthenticationType.FULLY_ANONYMOUS));
      instance.addBackend(new QBackendMetaData().withName("memory").withBackendType(MemoryBackendModule.class));
      instance.addTable(new QTableMetaData().withName("person").withLabel("People").withBackendName("memory")
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("hidden", QFieldType.STRING).withIsHidden(true))
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED)));
      return (instance);
   }



   /*******************************************************************************
    ** Run as a session with exactly the supplied permissions.
    *******************************************************************************/
   private void as(String... permissions)
   {
      QContext.init(instance, new QSession().withPermissions(List.of(permissions)));
   }



   /*******************************************************************************
    ** A persisted-help fixture with a stable modification timestamp.
    *******************************************************************************/
   private static QRecord help(int id, String key)
   {
      return (new QRecord().withValue("id", id).withValue("key", key).withValue("role", "ALL_SCREENS")
         .withValue("format", "MARKDOWN").withValue("summary", "Short help " + id)
         .withValue("content", "Read **this** first.").withValue("modifyDate", Instant.parse("2026-10-13T10:00:00Z")));
   }



   /*******************************************************************************
    ** Render a typed home widget in the active context.
    *******************************************************************************/
   private TableData widget(String name) throws Exception
   {
      return ((TableData) new HelpWidgets().render(new RenderWidgetInput().withWidgetMetaData(instance.getWidget(name))).getWidgetData());
   }



   /*******************************************************************************
    ** Read a typed link from a table cell.
    *******************************************************************************/
   private static String href(Map<String, Object> row, String field)
   {
      return (((CompositeWidgetData) row.get(field)).getBlocks().getFirst().getLink().getHref());
   }



   /*******************************************************************************
    ** Metadata Uses Only Core Table And Standard Actions.
    *******************************************************************************/
   @Test
   void metadataUsesOnlyCoreTableAndStandardActions() throws Exception
   {
      assertThat(instance.getTables()).containsOnlyKeys("person", "helpContent");
      assertThat(instance.getApp("help").getLabel()).isEqualTo("Help");
      assertThat(instance.getApp("help").getChildren()).containsExactly(instance.getTable("helpContent"));
      assertThat(instance.getApp("help").getWidgets()).containsExactlyElementsOf(HelpWidgets.HOME);
      assertThat(instance.getWidgets().values()).allSatisfy(widget ->
      {
         assertThat(widget.getType()).isEqualTo(WidgetType.TABLE.getType());
         assertThat(widget.getGridColumns()).isEqualTo(12);
      });
      assertThat(instance.getProcesses()).containsKeys(HelpProcesses.PREVIEW, "helpBundleSync", HelpProcesses.CHECK,
         "helpAcceptUpstream", "helpKeepMine", "helpContent.bulkInsert", "helpContent.bulkEdit", "helpContent.bulkEditWithFile");
      assertThat(List.of(instance.getProcess(HelpProcesses.PREVIEW), instance.getProcess("helpBundleSync"), instance.getProcess("helpAcceptUpstream"), instance.getProcess("helpKeepMine"), instance.getProcess(HelpProcesses.CHECK))).allSatisfy(process ->
      {
         assertThat(process.getTableName()).isEqualTo("helpContent");
         assertThat(process.getFrontendStep("review")).isNotNull();
         assertThat(process.getFrontendStep("result")).isNotNull();
      });
      assertThat(instance.getProcess(HelpProcesses.PREVIEW).getMinInputRecords()).isEqualTo(1);
      assertThat(instance.getProcess(HelpProcesses.PREVIEW).getMaxInputRecords()).isEqualTo(1);
      assertThat(instance.getProcesses()).doesNotContainKey("help_apply_now");
      var table = instance.getTable("helpContent");
      assertThat(table.getIsHidden()).isFalse();
      assertThat(table.getFields()).containsKeys("summary", "permissionName", "clientChannel");
      assertThat(table.getCustomizers()).isNotEmpty();
      assertThat(table.getField("format").getDefaultValue()).isEqualTo("MARKDOWN");
      assertThat(table.getField("role").getDefaultValue()).isEqualTo("ALL_SCREENS");
      assertThat(table.getEnabledCapabilities()).contains(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_EXPORT);
      assertThat(PermissionsHelper.getAllAvailablePermissions(instance)).extracting(permission -> permission.getName())
         .contains("help.read", "help.insert", "help.edit", "help.delete", "help.hasAccess", "help.author.hasAccess")
         .doesNotContain("help.view", "help.author", "help.read.hasAccess");
   }



   /*******************************************************************************
    ** Existing Table Is Reused Without Backend Config.
    *******************************************************************************/
   @Test
   void existingTableIsReusedWithoutBackendConfig() throws Exception
   {
      QInstance host = base();
      new HelpContentMetaDataProvider().defineAll(host, "memory", null);
      QTableMetaData table = host.getTable("helpContent");
      table.withLabel("Host help").withField(new QFieldMetaData("hostField", QFieldType.STRING));
      var customizers = table.getCustomizers();
      new HelpQBitProducer().withQBitConfig(new HelpQBitConfig().withAppName("support").withAppLabel("Support")
         .withTableMetaDataCustomizer((q, t) -> t.withRecordLabelFormat("Help %s"))).produce(host);
      assertThat(host.getTable("helpContent")).isSameAs(table);
      assertThat(table.getLabel()).isEqualTo("Host help");
      assertThat(table.getField("hostField")).isNotNull();
      assertThat(table.getCustomizers()).isSameAs(customizers);
      assertThat(host.getApp("support").getLabel()).isEqualTo("Support");
   }



   /*******************************************************************************
    ** Invalid Config Fails Clearly Before Creating Anything.
    *******************************************************************************/
   @Test
   void invalidConfigFailsClearlyBeforeCreatingAnything()
   {
      QInstance empty = base();
      assertThatThrownBy(() -> new HelpQBitProducer().produce(empty)).hasMessageContaining("HelpQBitConfig");
      assertThatThrownBy(() -> new HelpQBitProducer().withQBitConfig(new HelpQBitConfig()).produce(empty))
         .hasMessageContaining("backendName").hasMessageContaining("helpContent is absent");
      assertThat(empty.getTable("helpContent")).isNull();
      assertThatThrownBy(() -> new HelpQBitProducer().withQBitConfig(new HelpQBitConfig().withBackendName("memory")
         .withAppName(" ").withAppLabel(null)).produce(empty)).hasMessageContaining("appName").hasMessageContaining("appLabel");
   }



   /*******************************************************************************
    ** Config And Producer Identity.
    *******************************************************************************/
   @Test
   void configAndProducerIdentity()
   {
      HelpQBitConfig config = new HelpQBitConfig();
      assertThat(config.getIncludeHiddenInCoverage()).isFalse();
      config.setBackendName("memory");
      config.setAppName("support");
      config.setAppLabel("Support");
      config.setIncludeHiddenInCoverage(true);
      config.setTableMetaDataCustomizer((q, t) -> t);
      assertThat(config.getBackendName()).isEqualTo("memory");
      assertThat(config.getAppName()).isEqualTo("support");
      assertThat(config.getAppLabel()).isEqualTo("Support");
      assertThat(config.getTableMetaDataCustomizer()).isNotNull();
      HelpQBitProducer producer = new HelpQBitProducer();
      assertThat(producer.isEnabled()).isFalse();
      producer.withQBitConfig(config).withNamespace("host");
      assertThat(producer.isEnabled()).isTrue();
      assertThat(producer.getQBitConfig()).isSameAs(config);
      assertThat(producer.getNamespace()).isEqualTo("host");
      assertThat(producer.getQBitMetaData().getArtifactId()).isEqualTo("qbit-help");
   }



   /*******************************************************************************
    ** Coverage Uses Core Totals And Missing Keys With Default Values.
    *******************************************************************************/
   @Test
   void coverageUsesCoreTotalsAndMissingKeysWithDefaultValues() throws Exception
   {
      new InsertAction().execute(new InsertInput("helpContent").withRecord(help(1, "table:person;field:name")));
      TableData totals = widget(HelpWidgets.SUMMARY);
      assertThat(totals.getRows()).hasSize(11);
      var fields = totals.getRows().stream().filter(row -> row.get("kind").equals("Fields")).findFirst().orElseThrow();
      assertThat(fields.get("withHelp")).isEqualTo(1L);
      var core = new HelpCoverageAction().execute(new HelpCoverageInput()).getEntries().stream().filter(entry -> !entry.getHidden()).toList();
      var overall = totals.getRows().getLast();
      assertThat(overall.get("total")).isEqualTo(core.size());
      assertThat(overall.get("withHelp")).isEqualTo(1L);
      TableData missing = widget(HelpWidgets.MISSING);
      assertThat(missing.getRows()).noneSatisfy(row -> assertThat(row.get("key")).isIn("table:person;field:name", "table:person;field:hidden"));
      var row = missing.getRows().stream().filter(r -> "table:person;slot:help".equals(r.get("key"))).findFirst().orElseThrow();
      String url = URLDecoder.decode(href(row, "action"), StandardCharsets.UTF_8);
      assertThat(url).contains("/app/helpContent/create#defaultValues=", "\"key\":\"table:person;slot:help\"", "\"role\":\"ALL_SCREENS\"");
      assertThat(missing.getLabel()).contains("of " + (core.size() - 1) + " targets missing help");
   }



   /*******************************************************************************
    ** Hidden Targets Are Opt In And Missing List Is Bounded.
    *******************************************************************************/
   @Test
   void hiddenTargetsAreOptInAndMissingListIsBounded() throws Exception
   {
      for(int i = 0; i < 250; i++)
      {
         instance.getTable("person").withField(new QFieldMetaData("extra" + i, QFieldType.STRING));
      }
      instance.getWidget(HelpWidgets.MISSING).withDefaultValue("includeHidden", true);
      assertThat(widget(HelpWidgets.MISSING).getRows()).hasSize(200);
      instance.getWidget(HelpWidgets.SUMMARY).withDefaultValue("includeHidden", true);
      int withHidden = (int) widget(HelpWidgets.SUMMARY).getRows().getLast().get("total");
      instance.getWidget(HelpWidgets.SUMMARY).withDefaultValue("includeHidden", false);
      assertThat((int) widget(HelpWidgets.SUMMARY).getRows().getLast().get("total")).isLessThan(withHidden);
   }



   /*******************************************************************************
    ** Recent Changes Are Ordered And Linked.
    *******************************************************************************/
   @Test
   void recentChangesAreOrderedAndLinked() throws Exception
   {
      new MemoryInsertAction().execute(new InsertInput("helpContent").withRecords(List.of(
         help(1, "table:person;field:name"), help(2, "table:person;slot:help").withValue("modifyDate", Instant.parse("2026-10-14T10:00:00Z")))));
      TableData recent = widget(HelpWidgets.RECENT);
      assertThat(recent.getRows()).hasSize(2);
      assertThat(recent.getRows().getFirst().get("key")).isEqualTo("table:person;slot:help");
      assertThat(href(recent.getRows().getFirst(), "record")).isEqualTo("/app/helpContent/2");
   }



   /*******************************************************************************
    ** Preview Carries Rendered Help And View Fields Without Changing Shared Metadata.
    *******************************************************************************/
   @Test
   void previewCarriesRenderedHelpAndViewFieldsWithoutChangingSharedMetadata() throws Exception
   {
      QRecord record = help(1, "table:person;field:name").withValue("role", "EDIT_SCREEN")
         .withValue("permissionName", "finance.view").withValue("clientChannel", "web");
      new InsertAction().execute(new InsertInput("helpContent").withRecord(record));
      RunProcessOutput review = review(HelpProcesses.PREVIEW, "1");
      assertThat(review.getException()).isEmpty();
      assertThat(review.getProcessMetaDataAdjustment()).isNotNull();
      var adjusted = review.getProcessMetaDataAdjustment().getUpdatedFrontendStepList();
      assertThat(adjusted).hasSize(2);
      QHelpContent content = adjusted.getFirst().getHelpContents().getFirst();
      assertThat(content.getContent()).isEqualTo("Read **this** first.");
      assertThat(content.getContentAsHtml()).contains("<strong>this</strong>");
      assertThat(content.getSummary()).isEqualTo("Short help 1");
      assertThat(content.getFormat()).isEqualTo(HelpFormat.MARKDOWN);
      assertThat(content.getRoles()).containsExactly(QHelpRole.ALL_SCREENS);
      assertThat(review.getValues()).containsEntry("role", "EDIT_SCREEN").containsEntry("permissionName", "finance.view").containsEntry("clientChannel", "web");
      assertThat(adjusted.getFirst().getViewFields()).extracting(QFieldMetaData::getName)
         .containsExactly("summary", "key", "role", "format", "permissionName", "clientChannel");
      assertThat(instance.getProcess(HelpProcesses.PREVIEW).getFrontendStep("review").getHelpContents()).isNullOrEmpty();
      var lines = ProcessRunner.run(HelpProcesses.PREVIEW, 1);
      assertThat(lines.review()).contains("1 help entry previewed");
      assertThat(lines.result()).contains("1 help entry previewed");
   }



   /*******************************************************************************
    ** Start a real process and stop at its frontend Review step.
    *******************************************************************************/
   private static RunProcessOutput review(String name, String ids) throws Exception
   {
      RunProcessInput input = new RunProcessInput();
      input.setProcessName(name);
      input.addValue("recordsParam", "recordIds");
      input.addValue("recordIds", ids);
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.BREAK);
      return (new RunProcessAction().execute(input));
   }



   /*******************************************************************************
    ** Preview Rejects Multiple Records.
    *******************************************************************************/
   @Test
   void previewRejectsMultipleRecords() throws Exception
   {
      new InsertAction().execute(new InsertInput("helpContent").withRecords(List.of(help(1, "table:person;field:name"), help(2, "table:person;slot:help"))));
      assertThatThrownBy(() -> review(HelpProcesses.PREVIEW, "1,2")).hasMessageContaining("exactly one record");
   }



   /*******************************************************************************
    ** Check Keys Reports Good Bad Hidden And Wildcard Keys.
    *******************************************************************************/
   @Test
   void checkKeysReportsGoodBadHiddenAndWildcardKeys() throws Exception
   {
      List<QRecord> rows = List.of(help(1, "table:person;field:name"), help(2, "table:gone;field:x"),
         help(3, "process:*preview;step:review"), help(4, "table:person;field:hidden"));
      new MemoryInsertAction().execute(new InsertInput("helpContent").withRecords(rows));
      var lines = ProcessRunner.run(HelpProcesses.CHECK, 1, 2, 3, 4);
      assertThat(lines.review()).contains("3 keys resolve", "1 keys do not match any object", "table:gone;field:x");
      assertThat(lines.result()).contains("3 keys resolve", "1 keys do not match any object", "table:gone;field:x");
      assertThat(new QueryAction().execute(new QueryInput("helpContent")).getRecords()).hasSize(4);
      assertThat(HelpProcesses.resolves(null, Set.of())).isFalse();
      assertThat(HelpProcesses.resolves("process:*;step:review", HelpProcesses.keys())).isFalse();
      assertThat(HelpProcesses.resolves("process:*missing;step:review", HelpProcesses.keys())).isFalse();
      assertThat(HelpProcesses.resolves("process:*preview;step:absent", HelpProcesses.keys())).isFalse();
   }



   /*******************************************************************************
    ** Viewer Can Read But Cannot Author Or Run Coverage.
    *******************************************************************************/
   @Test
   void viewerCanReadButCannotAuthorOrRunCoverage() throws Exception
   {
      new InsertAction().execute(new InsertInput("helpContent").withRecord(help(1, "table:person;field:name")));
      as("help.read", "help.hasAccess");
      QContext.getQSession().withClientChannel("assistant");
      assertThat(new QueryAction().execute(new QueryInput("helpContent")).getRecords()).hasSize(1);
      assertThat(widget(HelpWidgets.RECENT).getRows()).hasSize(1);
      assertThatThrownBy(() -> new InsertAction().execute(new InsertInput("helpContent").withRecord(help(2, "table:person;slot:help"))))
         .isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> new UpdateAction().execute(new UpdateInput("helpContent").withRecord(help(1, "table:person;field:name"))))
         .isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> new DeleteAction().execute(new DeleteInput("helpContent").withPrimaryKeys(List.of(1))))
         .isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> widget(HelpWidgets.SUMMARY)).isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> widget(HelpWidgets.MISSING)).isInstanceOf(QPermissionDeniedException.class);
      for(String process : List.of(HelpProcesses.PREVIEW, "helpBundleSync", HelpProcesses.CHECK))
      {
         assertThat(PermissionsHelper.hasProcessPermission(new QueryInput(), process)).isFalse();
         assertThatThrownBy(() -> ProcessRunner.run(process)).isInstanceOf(QPermissionDeniedException.class);
      }
      assertThatThrownBy(() -> new HelpCoverageAction().execute(new HelpCoverageInput())).isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> ProcessRunner.run("helpBundleSync")).isInstanceOf(QPermissionDeniedException.class);
      PermissionsHelper.checkAppPermissionThrowing(new QueryInput(), "help");
      as();
      assertThatThrownBy(() -> new QueryAction().execute(new QueryInput("helpContent"))).isInstanceOf(QPermissionDeniedException.class);
   }



   /*******************************************************************************
    ** Recent Changes Are Bounded.
    *******************************************************************************/
   @Test
   void recentChangesAreBounded() throws Exception
   {
      for(int i = 1; i <= 25; i++)
      {
         new MemoryInsertAction().execute(new InsertInput("helpContent").withRecord(help(i, "table:person;field:name")));
      }
      TableData recent = widget(HelpWidgets.RECENT);
      assertThat(recent.getRows()).hasSize(20);
      assertThat(href(recent.getRows().getFirst(), "record")).isEqualTo("/app/helpContent/25");
      assertThat(href(recent.getRows().getLast(), "record")).isEqualTo("/app/helpContent/6");
   }



   /*******************************************************************************
    ** Preview Rejects Empty Selection And Supports Plain Text.
    *******************************************************************************/
   @Test
   void previewRejectsEmptySelectionAndSupportsPlainText() throws Exception
   {
      assertThatThrownBy(() -> review(HelpProcesses.PREVIEW, "")).hasMessageContaining("No records were selected");
      new MemoryInsertAction().execute(new InsertInput("helpContent").withRecord(help(1, "table:person;field:name").withValue("format", "TEXT")));
      QHelpContent help = review(HelpProcesses.PREVIEW, "1").getUpdatedFrontendStepList().getFirst().getHelpContents().getFirst();
      assertThat(help.getFormat()).isEqualTo(HelpFormat.TEXT);
      assertThat(help.getContent()).isEqualTo("Read **this** first.");
   }



   /*******************************************************************************
    ** Viewer Metadata Shows Only Read Permission.
    *******************************************************************************/
   @Test
   void viewerMetadataShowsOnlyReadPermission() throws Exception
   {
      as("help.read", "help.hasAccess");
      TableMetaDataInput input = new TableMetaDataInput();
      input.setTableName("helpContent");
      var table = new TableMetaDataAction().execute(input).getTable();
      assertThat(table.getReadPermission()).isTrue();
      assertThat(table.getInsertPermission()).isFalse();
      assertThat(table.getEditPermission()).isFalse();
      assertThat(table.getDeletePermission()).isFalse();

   }



   /*******************************************************************************
    ** Author Metadata Shows All Table Permissions And Authoring Actions.
    *******************************************************************************/
   @Test
   void authorMetadataShowsAllPermissions() throws Exception
   {
      TableMetaDataInput input = new TableMetaDataInput();
      input.setTableName("helpContent");
      var table = new TableMetaDataAction().execute(input).getTable();
      assertThat(table.getReadPermission()).isTrue();
      assertThat(table.getInsertPermission()).isTrue();
      assertThat(table.getEditPermission()).isTrue();
      assertThat(table.getDeletePermission()).isTrue();
      for(String process : List.of(HelpProcesses.PREVIEW, "helpBundleSync", HelpProcesses.CHECK))
      {
         assertThat(PermissionsHelper.hasProcessPermission(input, process)).isTrue();
      }
      for(String widget : List.of(HelpWidgets.SUMMARY, HelpWidgets.MISSING, HelpWidgets.RECENT))
      {
         assertThat(PermissionsHelper.hasWidgetPermission(input, widget)).isTrue();
      }
   }



   /*******************************************************************************
    ** Host Rules Are Preserved Only When Opting Out On An Existing Table.
    *******************************************************************************/
   @Test
   void permissionRulesRespectTableOwnership() throws Exception
   {
      HelpQBitConfig config = new HelpQBitConfig().withBackendName("memory");
      assertThat(config.getApplyPermissionRulesToHelpContent()).isTrue();
      config.setApplyPermissionRulesToHelpContent(false);
      assertThat(config.getApplyPermissionRulesToHelpContent()).isFalse();
      QInstance host = base();
      new HelpQBitProducer().withQBitConfig(config).produce(host);
      assertThat(host.getTable("helpContent").getPermissionRules().getLevel())
         .isEqualTo(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS);
      for(boolean applyRules : List.of(false, true))
      {
         host = base();
         new HelpContentMetaDataProvider().defineAll(host, "memory", null);
         QPermissionRules hostRules = new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS)
            .withPermissionBaseName("hostHelp");
         host.getTable("helpContent").withPermissionRules(hostRules);
         new HelpQBitProducer().withQBitConfig(new HelpQBitConfig().withApplyPermissionRulesToHelpContent(applyRules)).produce(host);
         QPermissionRules actual = host.getTable("helpContent").getPermissionRules();
         if(applyRules)
         {
            assertThat(actual.getLevel()).isEqualTo(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS);
            assertThat(actual.getPermissionBaseName()).isEqualTo("help");
         }
         else
         {
            assertThat(actual).isSameAs(hostRules);
         }
      }
   }



   /*******************************************************************************
    ** Customizer Cannot Rename Core Storage.
    *******************************************************************************/
   @Test
   void customizerCannotRenameCoreStorage()
   {
      assertThatThrownBy(() -> new HelpQBitProducer().withQBitConfig(new HelpQBitConfig().withBackendName("memory")
         .withTableMetaDataCustomizer((q, t) -> t.withName("renamed"))).produce(base()))
         .hasMessageContaining("without renaming");
   }

   /*******************************************************************************
    ** Review status filtering, literal diff content, links, and permissions.
    *******************************************************************************/
   @Test
   void bundleReviewWidgetsAndRecordDiff() throws Exception
   {
      new MemoryInsertAction().execute(new InsertInput("helpContent").withRecords(List.of(
         help(1, "table:person;field:name").withValue("bundleId", "test").withValue("bundleVersion", "1")
            .withValue("upstreamStatus", "UPSTREAM_CHANGED").withValue("upstreamSummary", "New summary")
            .withValue("upstreamContent", "<script>plain text</script>"),
         help(2, "table:gone;slot:help").withValue("upstreamStatus", "RETIRED"),
         help(3, "table:person;slot:help").withValue("upstreamStatus", "CURRENT"),
         help(4, "process:*preview;step:review"), help(5, "table:person;field:hidden"))));
      var changed = widget(HelpWidgets.UPSTREAM).getRows();
      assertThat(changed).hasSize(1);
      assertThat(changed.getFirst()).containsEntry("summary", "Short help 1").containsEntry("upstreamSummary", "New summary")
         .containsEntry("changes", "summary changed, content changed").containsEntry("bundleId", "test");
      assertThat(href(changed.getFirst(), "record")).isEqualTo("/app/helpContent/1");
      assertThat(widget(HelpWidgets.RETIRED).getRows()).singleElement().satisfies(row -> assertThat(row.get("key")).isEqualTo("table:gone;slot:help"));
      assertThat(widget(HelpWidgets.UNRESOLVED).getRows()).hasSize(1);
      var input = new RenderWidgetInput().withWidgetMetaData(instance.getWidget(HelpWidgets.DIFF)).withUrlParams(Map.of("id", "1"));
      var diff = (TableData) new HelpWidgets().render(input).getWidgetData();
      assertThat(diff.getColumns()).hasSize(2);
      assertThat(diff.getRows()).containsExactly(Map.of("current", "Summary:\nShort help 1", "upstream", "Summary:\nNew summary"),
         Map.of("current", "Content:\nRead **this** first.", "upstream", "Content:\n<script>plain text</script>"));
      for(String id : List.of("2", "3", "999", ""))
      {
         var hidden = (com.kingsrook.qqq.backend.core.model.dashboard.widgets.AlertData) new HelpWidgets().render(input.withUrlParams(Map.of("id", id))).getWidgetData();
         assertThat(hidden.getHideWidget()).isTrue();
      }
      assertThat(instance.getTable("helpContent").getSections()).anySatisfy(section ->
      {
         assertThat(section.getWidgetName()).isEqualTo(HelpWidgets.DIFF);
         assertThat(section.getGridColumns()).isEqualTo(12);
      });
      as("help.read", "help.hasAccess");
      for(String name : List.of(HelpWidgets.UPSTREAM, HelpWidgets.RETIRED, HelpWidgets.UNRESOLVED, HelpWidgets.BUNDLES, HelpWidgets.DIFF))
      {
         assertThatThrownBy(() -> widget(name)).isInstanceOf(QPermissionDeniedException.class);
      }
      for(String name : List.of("helpBundleSync", "helpAcceptUpstream", "helpKeepMine"))
      {
         assertThat(PermissionsHelper.hasProcessPermission(new QueryInput(), name)).isFalse();
      }
   }


   /*******************************************************************************
    ** Bundle coverage separates versions, pending rows and unresolved targets.
    *******************************************************************************/
   @Test
   void bundleCountsSeparateVersionsAndPendingRows() throws Exception
   {
      com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles.register(instance, "help/review-test.yaml");
      var registry = com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles.of(instance);
      var bundle = registry.getBundles().getLast();
      new MemoryInsertAction().execute(new InsertInput("helpContent").withRecords(List.of(
         help(1, "table:person;slot:help").withValue("bundleId", bundle.getId()).withValue("bundleVersion", bundle.getVersion()).withValue("upstreamStatus", "CURRENT"),
         help(2, "table:person;field:name").withValue("bundleId", bundle.getId()).withValue("bundleVersion", bundle.getVersion()).withValue("upstreamStatus", "UPSTREAM_CHANGED"),
         help(3, "table:person;field:name").withValue("bundleId", bundle.getId()).withValue("bundleVersion", "old").withValue("upstreamStatus", "CURRENT"),
         help(4, "table:gone;slot:help").withValue("bundleId", bundle.getId()).withValue("bundleVersion", bundle.getVersion()).withValue("upstreamStatus", "CURRENT"))));
      assertThat(widget(HelpWidgets.BUNDLES).getRows()).hasSize(2);
      assertThat(widget(HelpWidgets.BUNDLES).getRows().getFirst().get("applied")).isEqualTo(0L);
      assertThat(widget(HelpWidgets.BUNDLES).getRows().stream().filter(row -> bundle.getId().equals(row.get("bundle"))).toList()).singleElement().satisfies(row ->
         assertThat(row).containsEntry("bundle", bundle.getId()).containsEntry("version", bundle.getVersion())
            .containsEntry("entries", bundle.getEntries().size()).containsEntry("applied", 1L));
   }



   /*******************************************************************************
    ** Empty tables and legacy null keys remain safe to inspect.
    *******************************************************************************/
   @Test
   void emptyReviewAndSummaryOnlyChange() throws Exception
   {
      for(String name : List.of(HelpWidgets.UPSTREAM, HelpWidgets.RETIRED, HelpWidgets.UNRESOLVED))
      {
         assertThat(widget(name).getRows()).isEmpty();
      }
      new MemoryInsertAction().execute(new InsertInput("helpContent").withRecords(List.of(
         help(1, "table:person;slot:custom").withValue("upstreamStatus", "UPSTREAM_CHANGED")
            .withValue("upstreamSummary", "New").withValue("upstreamContent", "Read **this** first."),
         help(2, null))));
      assertThat(widget(HelpWidgets.UPSTREAM).getRows()).singleElement().satisfies(row -> assertThat(row.get("changes")).isEqualTo("summary changed"));
      assertThat(widget(HelpWidgets.UNRESOLVED).getRows()).singleElement().satisfies(row -> assertThat(row.get("key")).isNull());
      assertThat(((com.kingsrook.qqq.backend.core.model.dashboard.widgets.AlertData) new HelpWidgets().render(
         new RenderWidgetInput().withWidgetMetaData(instance.getWidget(HelpWidgets.DIFF))).getWidgetData()).getHideWidget()).isTrue();
   }


   /*******************************************************************************
    ** Core sync has a read-only review and populates persisted bundle counts.
    *******************************************************************************/
   @Test
   void coreSyncSuppliesReviewAndResultAndPopulatesBundleCoverage() throws Exception
   {
      com.kingsrook.qqq.backend.core.model.metadata.help.bundles.HelpBundles.register(instance, "help/review-test.yaml");
      var review = review("helpBundleSync", "");
      assertThat(review.getException()).isEmpty();
      assertThat(new QueryAction().execute(new QueryInput("helpContent")).getRecords()).isEmpty();
      var lines = ProcessRunner.run("helpBundleSync");
      assertThat(lines.review()).isNotEmpty();
      assertThat(lines.result()).isNotEmpty();
      assertThat(widget(HelpWidgets.BUNDLES).getRows().stream().filter(row -> "review-test".equals(row.get("bundle"))).toList())
         .singleElement().satisfies(row -> assertThat(row).containsEntry("entries", 1).containsEntry("applied", 1L));
   }

}
