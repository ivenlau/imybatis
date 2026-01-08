package com.xiaomi.imybatis.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Settings UI for imybatis plugin
 */
class MyBatisSettingsConfigurable : Configurable {

    private val settings = MyBatisSettings.getInstance()
    
    private val enableNavigationCheckBox = JBCheckBox("启用代码导航")
    private val enableCompletionCheckBox = JBCheckBox("启用代码补全")
    private val enableInspectionCheckBox = JBCheckBox("启用代码检查")
    private val indexOnStartupCheckBox = JBCheckBox("启动时自动索引")
    private val maxIndexTimeField = JBTextField()

    override fun getDisplayName(): String = "imybatis"

    override fun createComponent(): JComponent {
        return FormBuilder.createFormBuilder()
            .addComponent(enableNavigationCheckBox)
            .addComponent(enableCompletionCheckBox)
            .addComponent(enableInspectionCheckBox)
            .addComponent(indexOnStartupCheckBox)
            .addLabeledComponent(JBLabel("最大索引时间（秒）:"), maxIndexTimeField, 1, false)
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
