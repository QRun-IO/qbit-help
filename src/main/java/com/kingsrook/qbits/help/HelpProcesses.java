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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.help.HelpCoverageAction;
import com.kingsrook.qqq.backend.core.actions.help.HelpCoverageEntry;
import com.kingsrook.qqq.backend.core.actions.help.HelpCoverageInput;
import com.kingsrook.qqq.backend.core.actions.help.HelpTarget;
import com.kingsrook.qqq.backend.core.actions.help.HelpTargetCatalog;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessMetaDataAdjustment;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.helpcontent.HelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.help.HelpFormat;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpRole;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QComponentType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractExtractStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.BackendStepPostRunInput;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.BackendStepPostRunOutput;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ProcessSummaryProviderInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;


/*******************************************************************************
 ** Standard streamed actions: Review, execution, and Result.
 *******************************************************************************/
public final class HelpProcesses
{
   public static final String PREVIEW = "help_preview";
   public static final String CHECK = "help_check_keys";
   private static final List<String> PREVIEW_FIELDS = List.of("summary", "key", "role", "format", "permissionName", "clientChannel");



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private HelpProcesses()
   {
   }



   /*******************************************************************************
    ** Table-bound processes are discovered as actions, never as app children.
    *******************************************************************************/
   public static List<QProcessMetaData> metadata()
   {
      return (List.of(
         etl(PREVIEW, "Preview help", ExtractViaQueryStep.class, PreviewTransform.class, ReadOnlyLoad.class)
            .withMinInputRecords(1).withMaxInputRecords(1),
         etl(CHECK, "Check help keys", ExtractViaQueryStep.class, CheckTransform.class, ReadOnlyLoad.class)));
   }



   /*******************************************************************************
    ** Shared streamed ETL definition; full validation covers every selected row.
    *******************************************************************************/
   private static QProcessMetaData etl(String name, String label, Class<? extends AbstractExtractStep> extract,
      Class<? extends AbstractTransformStep> transform, Class<? extends AbstractLoadStep> load)
   {
      Map<String, Serializable> defaults = new HashMap<>();
      defaults.put(StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE, HelpContent.TABLE_NAME);
      defaults.put(StreamedETLWithFrontendProcess.FIELD_DESTINATION_TABLE, HelpContent.TABLE_NAME);
      defaults.put(StreamedETLWithFrontendProcess.FIELD_SUPPORTS_FULL_VALIDATION, true);
      defaults.put(StreamedETLWithFrontendProcess.FIELD_DO_FULL_VALIDATION, true);
      defaults.put(StreamedETLWithFrontendProcess.FIELD_FETCH_HEAVY_FIELDS, true);
      defaults.put(StreamedETLWithFrontendProcess.FIELD_TRANSACTION_LEVEL, StreamedETLWithFrontendProcess.TRANSACTION_LEVEL_AUTO_COMMIT);
      defaults.put(StreamedETLWithFrontendProcess.FIELD_PREVIEW_MESSAGE, label);
      return (StreamedETLWithFrontendProcess.defineProcessMetaData(extract, transform, load, defaults)
         .withName(name).withLabel(label).withIcon(new QIcon("help_outline"))
         .withTableName(HelpContent.TABLE_NAME).withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
            .withPermissionBaseName("help.author")));
   }



   /*******************************************************************************
    ** Canonical catalog, including hidden objects and explicitly declared slots.
    *******************************************************************************/
   static Set<String> keys() throws QException
   {
      Set<String> keys = new HelpCoverageAction().execute(new HelpCoverageInput()).getEntries().stream()
         .map(HelpCoverageEntry::getKey).collect(Collectors.toSet());
      HelpTargetCatalog.collect(QContext.getQInstance()).stream().map(HelpTarget::key).forEach(keys::add);
      return (keys);
   }



   /*******************************************************************************
    ** Resolve canonical keys or core's leading-star process suffix wildcard.
    *******************************************************************************/
   static boolean resolves(String key, Set<String> keys)
   {
      if(key == null)
      {
         return (false);
      }
      if(keys.contains(key))
      {
         return (true);
      }
      if(key.startsWith("process:*"))
      {
         int separator = key.indexOf(';');
         if(separator > "process:*".length())
         {
            String suffix = key.substring("process:*".length(), separator);
            String target = key.substring(separator);
            return (keys.stream().anyMatch(candidate -> candidate.startsWith("process:")
               && candidate.endsWith(suffix + target)));
         }
      }
      return (false);
   }



   /*******************************************************************************
    ** A standard count line with stable Review and Result wording.
    *******************************************************************************/
   private static ProcessSummaryLine line(Status status, int count, String message)
   {
      return (new ProcessSummaryLine(status, count, message));
   }



   /*******************************************************************************
    ** Key validation never writes data; bad keys are standard error lines.
    *******************************************************************************/
   public static class CheckTransform extends AbstractTransformStep
   {
      private Set<String> keys;
      private int good;
      private final List<String> bad = new ArrayList<>();



      /*******************************************************************************
       ** Validate a page against one catalog snapshot.
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         if(keys == null)
         {
            keys = keys();
         }
         for(QRecord record : input.getRecords())
         {
            String key = record.getValueString("key");
            if(resolves(key, keys))
            {
               good++;
            }
            else
            {
               bad.add(String.valueOf(key));
            }
            output.addRecord(record);
         }
      }



      /*******************************************************************************
       ** Both totals remain visible even when one count is zero.
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean result)
      {
         ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();
         lines.add(line(Status.OK, good, "keys resolve"));
         lines.add(line(bad.isEmpty() ? Status.INFO : Status.ERROR, bad.size(), "keys do not match any object"));
         for(String key : bad)
         {
            lines.add(new ProcessSummaryLine(Status.ERROR, null, key));
         }
         return (lines);
      }
   }



   /*******************************************************************************
    ** Preview a single selected record through the client's normal help renderer.
    *******************************************************************************/
   public static class PreviewTransform extends AbstractTransformStep
   {
      private QRecord selected;



      /*******************************************************************************
       ** Keep exactly one record, including its heavy content field.
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         for(QRecord record : input.getRecords())
         {
            if(selected != null)
            {
               throw (new QException("Preview help requires exactly one record"));
            }
            selected = record;
            output.addRecord(record);
         }
      }



      /*******************************************************************************
       ** Adjust only this process run, never shared process metadata.
       ** Targeting is displayed as data; the preview is visible to the author.
       *******************************************************************************/
      @Override
      public void postRun(BackendStepPostRunInput input, BackendStepPostRunOutput output) throws QException
      {
         if(selected == null)
         {
            throw (new QException("Preview help requires exactly one record"));
         }
         QHelpContent help = new QHelpContent().withContent(selected.getValueString("content"))
            .withSummary(selected.getValueString("summary")).withRole(QHelpRole.ALL_SCREENS);
         String format = selected.getValueString("format");
         if(format != null && !format.isBlank())
         {
            help.withFormat(HelpFormat.valueOf(format));
         }
         List<QFrontendStepMetaData> steps = new ArrayList<>();
         for(String name : List.of(StreamedETLWithFrontendProcess.STEP_NAME_REVIEW, StreamedETLWithFrontendProcess.STEP_NAME_RESULT))
         {
            QFrontendStepMetaData step = QContext.getQInstance().getProcess(PREVIEW).getFrontendStep(name).clone();
            step.setHelpContents(List.of(help));
            step.withComponent(new QFrontendComponentMetaData().withType(QComponentType.VIEW_FORM));
            for(String field : PREVIEW_FIELDS)
            {
               QFieldMetaData source = QContext.getQInstance().getTable(HelpContent.TABLE_NAME).getFields().get(field);
               step.withViewField(source == null ? new QFieldMetaData(field, QFieldType.STRING).withLabel(field) : source.clone());
               output.addValue(field, selected.getValue(field));
            }
            steps.add(step);
         }
         output.setProcessMetaDataAdjustment(new ProcessMetaDataAdjustment().withUpdatedFrontendStepList(steps));
      }



      /*******************************************************************************
       ** Standard read-only preview summary.
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean result)
      {
         return (new ArrayList<>(List.of(line(Status.INFO, selected == null ? 0 : 1, "help entry previewed"))));
      }
   }



   /*******************************************************************************
    ** Read-only execution keeps the transform's standard summary.
    *******************************************************************************/
   public static class ReadOnlyLoad extends AbstractLoadStep implements ProcessSummaryProviderInterface
   {
      /*******************************************************************************
       ** Pass records through without writing them.
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         output.getRecords().addAll(input.getRecords());
      }



      /*******************************************************************************
       ** Reuse validation results.
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean result)
      {
         return (getTransformStep().getProcessSummary(output, result));
      }
   }



}
