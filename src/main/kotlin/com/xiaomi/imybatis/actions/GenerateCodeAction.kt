package com.xiaomi.imybatis.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.Project
import com.xiaomi.imybatis.ImybatisBundle
import com.xiaomi.imybatis.ui.CodeGenerationWizard

/**
 * Action to open code generation wizard
 */
class GenerateCodeAction : AnAction() {

    init {
        val templatePresentation = templatePresentation
        templatePresentation.text = ImybatisBundle.message("action.generate.title")
        templatePresentation.description = ImybatisBundle.message("action.generate.title")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val wizard = CodeGenerationWizard(project)
        wizard.show()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
}
