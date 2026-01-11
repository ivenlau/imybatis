package com.xiaomi.imybatis.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.xiaomi.imybatis.ImybatisBundle
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Settings UI for imybatis plugin
 */
class MyBatisSettingsConfigurable : Configurable {

    private val settings = MyBatisSettings.getInstance()

    private val enableNavigationCheckBox = JBCheckBox(ImybatisBundle.message("settings.enableNavigation"))
    private val enableCompletionCheckBox = JBCheckBox(ImybatisBundle.message("settings.enableCompletion"))
    private val enableInspectionCheckBox = JBCheckBox(ImybatisBundle.message("settings.enableInspection"))
    private val indexOnStartupCheckBox = JBCheckBox(ImybatisBundle.message("settings.indexOnStartup"))
    private val maxIndexTimeField = JBTextField()

    override fun getDisplayName(): String = ImybatisBundle.message("settings.title")

    override fun createComponent(): JComponent {
        return FormBuilder.createFormBuilder()
            .addComponent(enableNavigationCheckBox)
            .addComponent(enableCompletionCheckBox)
            .addComponent(enableInspectionCheckBox)
            .addComponent(indexOnStartupCheckBox)
            .addLabeledComponent(JBLabel(ImybatisBundle.message("settings.maxIndexTime") + ":"), maxIndexTimeField, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun isModified(): Boolean {
        val state = settings.state
        return enableNavigationCheckBox.isSelected != state.enableNavigation ||
                enableCompletionCheckBox.isSelected != state.enableCompletion ||
                enableInspectionCheckBox.isSelected != state.enableInspection ||
                indexOnStartupCheckBox.isSelected != state.indexOnStartup ||
                maxIndexTimeField.text.toIntOrNull() != state.maxIndexTime
    }

    override fun apply() {
        val state = settings.state
        state.enableNavigation = enableNavigationCheckBox.isSelected
        state.enableCompletion = enableCompletionCheckBox.isSelected
        state.enableInspection = enableInspectionCheckBox.isSelected
        state.indexOnStartup = indexOnStartupCheckBox.isSelected
        state.maxIndexTime = maxIndexTimeField.text.toIntOrNull() ?: 60
    }

    override fun reset() {
        val state = settings.state
        enableNavigationCheckBox.isSelected = state.enableNavigation
        enableCompletionCheckBox.isSelected = state.enableCompletion
        enableInspectionCheckBox.isSelected = state.enableInspection
        indexOnStartupCheckBox.isSelected = state.indexOnStartup
        maxIndexTimeField.text = state.maxIndexTime.toString()
    }
}
