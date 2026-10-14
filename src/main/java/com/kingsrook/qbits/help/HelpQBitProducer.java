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


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.helpcontent.HelpContent;
import com.kingsrook.qqq.backend.core.model.helpcontent.HelpContentMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Produces the authoring app around core's existing helpContent table.
 *******************************************************************************/
public class HelpQBitProducer implements QBitMetaDataProducer<HelpQBitConfig>
{
   private HelpQBitConfig qBitConfig;
   private String namespace;



   /*******************************************************************************
    ** Supply configuration explicitly; package scanning alone is disabled.
    *******************************************************************************/
   public HelpQBitProducer withQBitConfig(HelpQBitConfig config)
   {
      qBitConfig = config;
      return (this);
   }



   /*******************************************************************************
    ** Configuration.
    *******************************************************************************/
   @Override
   public HelpQBitConfig getQBitConfig()
   {
      return (qBitConfig);
   }



   /*******************************************************************************
    ** Namespace for QBit identity (storage and action names remain stable).
    *******************************************************************************/
   public HelpQBitProducer withNamespace(String namespace)
   {
      this.namespace = namespace;
      return (this);
   }



   /*******************************************************************************
    ** Namespace.
    *******************************************************************************/
   @Override
   public String getNamespace()
   {
      return (namespace);
   }



   /*******************************************************************************
    ** QBit identity.
    *******************************************************************************/
   @Override
   public QBitMetaData getQBitMetaData()
   {
      return (new QBitMetaData().withGroupId("com.kingsrook.qbits").withArtifactId("qbit-help")
         .withVersion("0.1.0-SNAPSHOT").withNamespace(namespace).withConfig(qBitConfig));
   }



   /*******************************************************************************
    ** Only explicitly configured producers run.
    *******************************************************************************/
   @Override
   public boolean isEnabled()
   {
      return (qBitConfig != null);
   }



   /*******************************************************************************
    ** Install metadata into the instance, preserving host storage and customizers.
    *******************************************************************************/
   @Override
   public MetaDataProducerMultiOutput produce(QInstance instance) throws QException
   {
      if(qBitConfig == null)
      {
         throw (new QException("Help qbit requires HelpQBitConfig"));
      }
      List<String> errors = new ArrayList<>();
      qBitConfig.validate(instance, errors);
      if(!errors.isEmpty())
      {
         throw (new QException("Help qbit configuration is invalid: " + String.join("; ", errors)));
      }
      boolean definesHelpContent = instance.getTable(HelpContent.TABLE_NAME) == null;
      if(definesHelpContent)
      {
         new HelpContentMetaDataProvider().withTargetingFields(true).defineAll(instance, qBitConfig.getBackendName(), null);
      }
      QTableMetaData table = instance.getTable(HelpContent.TABLE_NAME);
      if(qBitConfig.getTableMetaDataCustomizer() != null)
      {
         table = qBitConfig.getTableMetaDataCustomizer().customizeMetaData(instance, table);
      }
      if(table == null || !HelpContent.TABLE_NAME.equals(table.getName()))
      {
         throw (new QException("tableMetaDataCustomizer must return the helpContent table without renaming it"));
      }
      if(definesHelpContent || qBitConfig.getApplyPermissionRulesToHelpContent())
      {
         table.withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS)
            .withPermissionBaseName("help"));
      }
      table.withIsHidden(false)
         .withCapabilities(Capability.TABLE_QUERY, Capability.TABLE_GET, Capability.TABLE_COUNT, Capability.TABLE_INSERT,
            Capability.TABLE_UPDATE, Capability.TABLE_DELETE, Capability.TABLE_EXPORT);
      if(table.getField("format").getDefaultValue() == null)
      {
         table.getField("format").withDefaultValue("MARKDOWN");
      }
      if(table.getField("role").getDefaultValue() == null)
      {
         table.getField("role").withDefaultValue("ALL_SCREENS");
      }
      instance.getTables().put(HelpContent.TABLE_NAME, table);
      QBitMetaData qBit = getQBitMetaData();
      instance.addQBit(qBit);
      MetaDataProducerMultiOutput output = new MetaDataProducerMultiOutput();
      HelpWidgets.metadata(qBitConfig).forEach(output::add);
      HelpProcesses.metadata().forEach(process ->
      {
         process.setSourceQBitName(qBit.getName());
         output.add(process);
      });
      output.add(new QAppMetaData().withName(qBitConfig.getAppName()).withLabel(qBitConfig.getAppLabel())
         .withIcon(new QIcon("help_outline")).withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION).withPermissionBaseName("help"))
         .withWidgets(List.of(HelpWidgets.SUMMARY, HelpWidgets.MISSING, HelpWidgets.RECENT)).withChild(table));
      output.addSelfToInstance(instance);
      return (new MetaDataProducerMultiOutput());
   }
}
