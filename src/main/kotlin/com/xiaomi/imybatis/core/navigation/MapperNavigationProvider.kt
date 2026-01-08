package com.xiaomi.imybatis.core.navigation

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.xiaomi.imybatis.core.index.IndexManager
import com.xiaomi.imybatis.core.index.model.XmlStatementInfo

/**
 * Navigation provider for Mapper interfaces
 * Handles navigation from Mapper methods to XML statements
 */
class MapperNavigationProvider : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?
    ): Array<PsiElement>? {
        if (sourceElement == null) return null

        val project = sourceElement.project
        val psiMethod = PsiTreeUtil.getParentOfType(sourceElement, PsiMethod::class.java) ?: return null
        val psiClass = psiMethod.containingClass ?: return null

        // Check if this is a Mapper interface
        if (!isMapperInterface(psiClass)) return null

        val mapperFqn = psiClass.qualifiedName ?: return null
        val methodName = psiMethod.name

        // Find corresponding XML statements
        val indexManager = IndexManager.getInstance(project)
        val xmlMapper = indexManager.findXmlMapper(mapperFqn) ?: return null

        val statements = xmlMapper.statements.filter { it.id == methodName }
        if (statements.isEmpty()) return null

        return statements.mapNotNull { statement ->
            statement.xmlTag
        }.toTypedArray()
    }

    private fun isMapperInterface(psiClass: PsiClass): Boolean {
        if (!psiClass.isInterface) return false

        // Check for @Mapper annotation
        val annotations = psiClass.modifierList?.annotations ?: return false
        for (annotation in annotations) {
            val qualifiedName = annotation.qualifiedName
            if (qualifiedName == "org.apache.ibatis.annotations.Mapper" ||
                qualifiedName == "org.mybatis.spring.annotation.Mapper" ||
                qualifiedName?.endsWith(".Mapper") == true) {
                return true
            }
        }

        // Check if it extends BaseMapper
        val superTypes = psiClass.superTypes
        for (superType in superTypes) {
            val superTypeName = superType.canonicalText
            if (superTypeName.contains("BaseMapper")) {
                return true
            }
        }

        return false
    }
}
