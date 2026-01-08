package com.xiaomi.imybatis.core.navigation

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.xiaomi.imybatis.core.index.IndexManager

/**
 * Navigation provider for Entity classes
 * Handles navigation between ResultMap properties and Entity fields
 */
class EntityNavigationProvider : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?
    ): Array<PsiElement>? {
        if (sourceElement == null) return null

        val project = sourceElement.project

        // Check if we're in a ResultMap property attribute
        val xmlTag = PsiTreeUtil.getParentOfType(sourceElement, XmlTag::class.java) ?: return null
        if (xmlTag.name == "result" || xmlTag.name == "id") {
            val property = xmlTag.getAttributeValue("property") ?: return null
            if (sourceElement is XmlAttributeValue && sourceElement.value == property) {
                // Find the resultMap type
                val resultMapTag = findResultMapTag(xmlTag) ?: return null
                val type = resultMapTag.getAttributeValue("type") ?: return null
                
                // Navigate to Entity field
                return navigateToEntityField(project, type, property)
            }
        }

        return null
    }

    /**
     * Find the parent resultMap tag
     */
    private fun findResultMapTag(tag: XmlTag): XmlTag? {
        var current: XmlTag? = tag
        while (current != null) {
            if (current.name == "resultMap") {
                return current
            }
            current = current.parent as? XmlTag
        }
        return null
    }

    /**
     * Navigate from ResultMap property to Entity field
     */
    private fun navigateToEntityField(project: Project, entityType: String, fieldName: String): Array<PsiElement>? {
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        val psiClass = javaPsiFacade.findClass(
            entityType,
            com.intellij.psi.search.GlobalSearchScope.allScope(project)
        ) ?: return null

        val field = psiClass.findFieldByName(fieldName, false) ?: return null
        return arrayOf(field)
    }
}
