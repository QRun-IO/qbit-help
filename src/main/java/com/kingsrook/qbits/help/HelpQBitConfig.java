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


import java.util.List;
import com.kingsrook.qqq.backend.core.model.helpcontent.HelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfig;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Configuration for the Help authoring app.
 *******************************************************************************/
public class HelpQBitConfig implements QBitConfig
{
   private String backendName;
   private String appName = "help";
   private String appLabel = "Help";
   private boolean includeHiddenInCoverage = false;
   private boolean applyPermissionRulesToHelpContent = true;
   private MetaDataCustomizerInterface<QTableMetaData> tableMetaDataCustomizer;



   /*******************************************************************************
    ** Get backendName.
    *******************************************************************************/
   public String getBackendName()
   {
      return (backendName);
   }



   /*******************************************************************************
    ** Set backendName.
    *******************************************************************************/
   public void setBackendName(String backendName)
   {
      this.backendName = backendName;
   }



   /*******************************************************************************
    ** Fluent setter for backendName.
    *******************************************************************************/
   public HelpQBitConfig withBackendName(String backendName)
   {
      this.backendName = backendName;
      return (this);
   }



   /*******************************************************************************
    ** Get appName.
    *******************************************************************************/
   public String getAppName()
   {
      return (appName);
   }



   /*******************************************************************************
    ** Set appName.
    *******************************************************************************/
   public void setAppName(String appName)
   {
      this.appName = appName;
   }



   /*******************************************************************************
    ** Fluent setter for appName.
    *******************************************************************************/
   public HelpQBitConfig withAppName(String appName)
   {
      this.appName = appName;
      return (this);
   }



   /*******************************************************************************
    ** Get appLabel.
    *******************************************************************************/
   public String getAppLabel()
   {
      return (appLabel);
   }



   /*******************************************************************************
    ** Set appLabel.
    *******************************************************************************/
   public void setAppLabel(String appLabel)
   {
      this.appLabel = appLabel;
   }



   /*******************************************************************************
    ** Fluent setter for appLabel.
    *******************************************************************************/
   public HelpQBitConfig withAppLabel(String appLabel)
   {
      this.appLabel = appLabel;
      return (this);
   }



   /*******************************************************************************
    ** Get applyPermissionRulesToHelpContent.
    *******************************************************************************/
   public boolean getApplyPermissionRulesToHelpContent()
   {
      return (applyPermissionRulesToHelpContent);
   }



   /*******************************************************************************
    ** Set applyPermissionRulesToHelpContent.
    *******************************************************************************/
   public void setApplyPermissionRulesToHelpContent(boolean applyPermissionRulesToHelpContent)
   {
      this.applyPermissionRulesToHelpContent = applyPermissionRulesToHelpContent;
   }



   /*******************************************************************************
    ** Fluent setter for applyPermissionRulesToHelpContent.
    *******************************************************************************/
   public HelpQBitConfig withApplyPermissionRulesToHelpContent(boolean applyPermissionRulesToHelpContent)
   {
      this.applyPermissionRulesToHelpContent = applyPermissionRulesToHelpContent;
      return (this);
   }



   /*******************************************************************************
    ** Get includeHiddenInCoverage.
    *******************************************************************************/
   public boolean getIncludeHiddenInCoverage()
   {
      return (includeHiddenInCoverage);
   }



   /*******************************************************************************
    ** Set includeHiddenInCoverage.
    *******************************************************************************/
   public void setIncludeHiddenInCoverage(boolean includeHiddenInCoverage)
   {
      this.includeHiddenInCoverage = includeHiddenInCoverage;
   }



   /*******************************************************************************
    ** Fluent setter for includeHiddenInCoverage.
    *******************************************************************************/
   public HelpQBitConfig withIncludeHiddenInCoverage(boolean includeHiddenInCoverage)
   {
      this.includeHiddenInCoverage = includeHiddenInCoverage;
      return (this);
   }



   /*******************************************************************************
    ** Get tableMetaDataCustomizer.
    *******************************************************************************/
   public MetaDataCustomizerInterface<QTableMetaData> getTableMetaDataCustomizer()
   {
      return (tableMetaDataCustomizer);
   }



   /*******************************************************************************
    ** Set tableMetaDataCustomizer.
    *******************************************************************************/
   public void setTableMetaDataCustomizer(MetaDataCustomizerInterface<QTableMetaData> tableMetaDataCustomizer)
   {
      this.tableMetaDataCustomizer = tableMetaDataCustomizer;
   }



   /*******************************************************************************
    ** Fluent setter for tableMetaDataCustomizer.
    *******************************************************************************/
   public HelpQBitConfig withTableMetaDataCustomizer(MetaDataCustomizerInterface<QTableMetaData> tableMetaDataCustomizer)
   {
      this.tableMetaDataCustomizer = tableMetaDataCustomizer;
      return (this);
   }



   /*******************************************************************************
    ** Validate the backend only when core help storage must be defined.
    *******************************************************************************/
   @Override
   public void validate(QInstance instance, List<String> errors)
   {
      if(instance.getTable(HelpContent.TABLE_NAME) == null)
      {
         assertCondition(backendName != null && instance.getBackend(backendName) != null,
            "backendName must name an existing backend when helpContent is absent", errors);
      }
      assertCondition(appName != null && !appName.isBlank(), "appName must not be blank", errors);
      assertCondition(appLabel != null && !appLabel.isBlank(), "appLabel must not be blank", errors);
   }
}
