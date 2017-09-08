/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.application.options.codeStyle;

import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import com.intellij.psi.codeStyle.presentation.CodeStyleBoundedIntegerSettingPresentation;
import com.intellij.psi.codeStyle.presentation.CodeStyleSelectSettingPresentation;
import com.intellij.psi.codeStyle.presentation.CodeStyleSettingPresentation;
import com.intellij.psi.codeStyle.presentation.CodeStyleSoftMarginsPresentation;
import com.intellij.ui.components.fields.CommaSeparatedIntegersField;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

public class WrappingAndBracesPanel extends OptionTableWithPreviewPanel {
  private MultiMap<String, String> myGroupToFields = new MultiMap<>();
  private Map<String, SettingsGroup> myFieldNameToGroup;
  private final CommaSeparatedIntegersField mySoftMarginsEditor =
    new CommaSeparatedIntegersField(0, CodeStyleSettings.MAX_RIGHT_MARGIN, "Optional");

  public WrappingAndBracesPanel(CodeStyleSettings settings) {
    super(settings);
    init();
  }

  @Override
  public LanguageCodeStyleSettingsProvider.SettingsType getSettingsType() {
    return LanguageCodeStyleSettingsProvider.SettingsType.WRAPPING_AND_BRACES_SETTINGS;
  }

  @Override
  protected void addOption(@NotNull String fieldName, @NotNull String title, @Nullable String groupName) {
    super.addOption(fieldName, title, groupName);
    if (groupName != null) {
      myGroupToFields.putValue(groupName, fieldName);
    }
  }

  @Override
  protected void addOption(@NotNull String fieldName, @NotNull String title, @Nullable String groupName,
                           @NotNull String[] options, @NotNull int[] values) {
    super.addOption(fieldName, title, groupName, options, values);
    if (groupName == null) {
      myGroupToFields.putValue(title, fieldName);
    }
  }

  @Override
  protected void initTables() {
    for (Map.Entry<CodeStyleSettingPresentation.SettingsGroup, List<CodeStyleSettingPresentation>> entry :
      CodeStyleSettingPresentation.getStandardSettings(getSettingsType()).entrySet()) {
      CodeStyleSettingPresentation.SettingsGroup group = entry.getKey();
      for (CodeStyleSettingPresentation setting : entry.getValue()) {
        //TODO this is ugly, but is the fastest way to make Options UI API and settings representation API connect
        String fieldName = setting.getFieldName();
        String uiName = setting.getUiName();
        if (setting instanceof CodeStyleBoundedIntegerSettingPresentation) {
          CodeStyleBoundedIntegerSettingPresentation intSetting = (CodeStyleBoundedIntegerSettingPresentation)setting;
          int defaultValue = intSetting.getDefaultValue();
          addOption(fieldName, uiName, group.name, intSetting.getLowerBound(), intSetting.getUpperBound(), defaultValue,
                    intSetting.getValueUiName(
                      defaultValue));
        }
        else if (setting instanceof CodeStyleSelectSettingPresentation) {
          CodeStyleSelectSettingPresentation selectSetting = (CodeStyleSelectSettingPresentation)setting;
          addOption(fieldName, uiName, group.name, selectSetting.getOptions(), selectSetting.getValues());
        }
        else if (setting instanceof CodeStyleSoftMarginsPresentation) {
          addSoftMarginsOption(fieldName, uiName, group.name);
          showOption(fieldName);
        }
        else {
          addOption(fieldName, uiName, group.name);
        }
      }
    }
  }

  protected SettingsGroup getAssociatedSettingsGroup(String fieldName) {
    if (myFieldNameToGroup == null) {
      myFieldNameToGroup = ContainerUtil.newHashMap();
      Set<String> groups = myGroupToFields.keySet();
      for (String group : groups) {
        Collection<String> fields = myGroupToFields.get(group);
        SettingsGroup settingsGroup = new SettingsGroup(group, fields);
        for (String field : fields) {
          myFieldNameToGroup.put(field, settingsGroup);
        }
      }
    }
    return myFieldNameToGroup.get(fieldName);
  }

  @Override
  protected String getTabTitle() {
    return ApplicationBundle.message("wrapping.and.braces");
  }

  protected static class SettingsGroup {
    public final String title;
    public final Collection<String> commonCodeStyleSettingFieldNames;

    public SettingsGroup(@NotNull String title,
                         @NotNull Collection<String> commonCodeStyleSettingFieldNames) {
      this.title = title;
      this.commonCodeStyleSettingFieldNames = commonCodeStyleSettingFieldNames;
    }
  }


  private void addSoftMarginsOption(@NotNull String optionName, @NotNull String title, @Nullable String groupName) {
    Language language = getDefaultLanguage();
    if (language != null) {
      addCustomOption(new SoftMarginsOption(language, optionName, title, groupName));
    }
  }

  private static class SoftMarginsOption extends Option {

    private Language myLanguage;

    protected SoftMarginsOption(@NotNull Language language,
                                @NotNull String optionName,
                                @NotNull String title,
                                @Nullable String groupName) {
      super(optionName, title, groupName, null, null);
      myLanguage = language;
    }

    @Override
    public Object getValue(CodeStyleSettings settings) {
      CommonCodeStyleSettings langSettings = settings.getCommonSettings(myLanguage);
      return langSettings.getSoftMargins();
    }

    @Override
    public void setValue(Object value, CodeStyleSettings settings) {
      settings.setSoftMargins(myLanguage, castToIntList(value));
    }

    @Override
    public boolean isEnabled() {
      return true;
    }
  }

  private static List<Integer> castToIntList(@Nullable Object value) {
    if (value instanceof List && ((List)value).size() > 0 && ((List)value).get(0) instanceof Integer) {
      //noinspection unchecked
      return (List<Integer>)value;
    }
    return Collections.emptyList();
  }

  @Nullable
  @Override
  protected JComponent getCustomValueRenderer(@NotNull String optionName, @NotNull Object value) {
    if (CodeStyleSoftMarginsPresentation.OPTION_NAME.equals(optionName)) {
      return new JLabel(getSoftMarginsString(castToIntList(value)));
    }
    return super.getCustomValueRenderer(optionName, value);
  }

  @NotNull
  private String getSoftMarginsString(@NotNull List<Integer> intList) {
    if (intList.size() > 0) {
      return CommaSeparatedIntegersField.intListToString(intList);
    }
    List<Integer> defaultMargins = getSettings().getDefaultSoftMargins();
    String defaultsStr = defaultMargins.size() > 0 ? CommaSeparatedIntegersField.intListToString(defaultMargins) : "None";
    return "Default: " + defaultsStr;
  }

  @Nullable
  @Override
  protected JComponent getCustomNodeEditor(@NotNull MyTreeNode node) {
    String optionName = node.getKey().getOptionName();
    if (CodeStyleSoftMarginsPresentation.OPTION_NAME.equals(optionName)) {
      mySoftMarginsEditor.setValue(castToIntList(node.getValue()));
      return mySoftMarginsEditor;
    }
    return super.getCustomNodeEditor(node);
  }

  @Nullable
  @Override
  protected Object getCustomNodeEditorValue(@NotNull JComponent customEditor) {
    if (customEditor instanceof CommaSeparatedIntegersField) {
      return ((CommaSeparatedIntegersField)customEditor).getValue();
    }
    return super.getCustomNodeEditorValue(customEditor);
  }
}