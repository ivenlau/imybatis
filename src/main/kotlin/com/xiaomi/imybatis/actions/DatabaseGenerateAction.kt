package com.xiaomi.imybatis.actions

import com.intellij.database.psi.DbDataSource
import com.intellij.database.psi.DbElement
import com.intellij.database.psi.DbTable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.psi.PsiElement
import com.xiaomi.imybatis.ImybatisBundle
import com.xiaomi.imybatis.database.PsiMetadataProvider
import com.xiaomi.imybatis.ui.CodeGenerationWizard

class DatabaseGenerateAction : AnAction(
    { ImybatisBundle.message("action.database.generate") },
    { ImybatisBundle.message("action.database.generate") }
) {

    override fun update(e: AnActionEvent) {
        // 只要有项目就显示菜单项
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiElements = e.getData(LangDataKeys.PSI_ELEMENT_ARRAY)
            ?: e.getData(PlatformDataKeys.PSI_ELEMENT)?.let { arrayOf(it) }
            ?: return

        if (psiElements.isEmpty()) return

        val firstElement = psiElements[0]

        when {
            // 情况1：选中的是表，获取同级所有表，并高亮选中的表
            firstElement is DbTable -> {
                val selectedTables = psiElements.filterIsInstance<DbTable>()

                val parent = firstElement.parent
                val allTables = mutableListOf<DbTable>()

                if (parent != null) {
                    collectTables(parent, allTables, maxDepth = 1)
                }

                if (allTables.isEmpty()) {
                    allTables.addAll(selectedTables)
                }

                val provider = PsiMetadataProvider(allTables)
                val wizard = CodeGenerationWizard(project)
                wizard.setInitialState(provider, selectedTables.map { it.name })
                wizard.show()
            }
            // 情况2：选中的是列或表下面的元素，只显示当前表
            firstElement is DbElement && findParentTable(firstElement) != null -> {
                val table = findParentTable(firstElement)!!
                val tables = listOf(table)

                val provider = PsiMetadataProvider(tables)
                val wizard = CodeGenerationWizard(project)
                wizard.setInitialState(provider, listOf(table.name))
                wizard.show()
            }
            // 情况3：选中的是其他元素（数据源、schema等），递归向下找所有表
            firstElement is DbElement -> {
                val tables = mutableListOf<DbTable>()
                collectTables(firstElement, tables)

                if (tables.isNotEmpty()) {
                    val provider = PsiMetadataProvider(tables)
                    val wizard = CodeGenerationWizard(project)
                    wizard.setInitialState(provider)
                    wizard.show()
                }
            }
        }
    }

    private fun findParentTable(element: PsiElement): DbTable? {
        var current: PsiElement? = element
        while (current != null) {
            if (current is DbTable) {
                return current
            }
            try {
                current = current.parent
            } catch (e: Exception) {
                return null
            }
        }
        return null
    }

    private fun collectTables(element: PsiElement, tables: MutableList<DbTable>, depth: Int = 0, maxDepth: Int = 2) {
        if (depth > maxDepth) return

        element.children.forEach { child ->
            if (child is DbTable) {
                tables.add(child)
            } else if (child is DbElement) {
                collectTables(child, tables, depth + 1, maxDepth)
            }
        }
    }
}
