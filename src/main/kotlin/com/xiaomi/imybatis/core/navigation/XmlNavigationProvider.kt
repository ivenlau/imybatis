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
 * Navigation provider for Mapper XML files
 * Handles navigation from XML statements to Mapper interface methods
 */
class XmlNavigationProvider : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?
    ): Array<PsiElement>? {
        if (sourceElement == null) return null

        val project = sourceElement.project

        // Check if we're in a mapper XML file
        val xmlTag = PsiTreeUtil.getParentOfType(sourceElement, XmlTag::class.java) ?: return null
        val xmlFile = xmlTag.containingFile as? com.intellij.psi.xml.XmlFile ?: return null

        val rootTag = xmlFile.rootTag ?: return null
        if (rootTag.name != "mapper") return null

        val namespace = rootTag.getAttributeValue("namespace") ?: return null

        // Handle navigation from statement id to Mapper method
        if (xmlTag.name in listOf("select", "insert", "update", "delete")) {
            val id = xmlTag.getAttributeValue("id") ?: return null
            return navigateToMapperMethod(project, namespace, id)
        }

        // Handle navigation from resultMap reference to definition
        if (sourceElement is XmlAttributeValue && sourceElement.parent is XmlTag) {
            val parentTag = sourceElement.parent as? XmlTag ?: return null
            if (parentTag.name in listOf("select", "insert", "update", "delete")) {
                val xmlAttribute = PsiTreeUtil.getParentOfType(sourceElement, com.intellij.psi.xml.XmlAttribute::class.java)
                val attributeName = xmlAttribute?.name
                if (attributeName == "resultMap") {
                    val resultMapId = sourceElement.value
                    return navigateToResultMap(project, namespace, resultMapId, xmlFile)
                }
            }
        }

        return null
    }

    /**
     * Navigate from XML statement id to Mapper interface method
     */
    private fun navigateToMapperMethod(project: Project, namespace: String, methodName: String): Array<PsiElement>? {
        val indexManager = IndexManager.getInstance(project)
        val mapperInfo = indexManager.findMapper(namespace) ?: return null

        val psiClass = findPsiClass(project, namespace) ?: return null
        val methods = psiClass.findMethodsByName(methodName, false)

        return if (methods.isEmpty()) null else methods as Array<PsiElement>
    }

    /**
     * Navigate to ResultMap definition
     */
    private fun navigateToResultMap(
        project: Project,
        namespace: String,
        resultMapId: String,
        xmlFile: com.intellij.psi.xml.XmlFile
    ): Array<PsiElement>? {
        val rootTag = xmlFile.rootTag ?: return null
        val resultMapTags = rootTag.findSubTags("resultMap")

        for (tag in resultMapTags) {
            val id = tag.getAttributeValue("id") ?: continue
            if (id == resultMapId) {
                return arrayOf(tag)
            }
        }

        return null
    }

    /**
     * Find PsiClass by qualified name
     */
    private fun findPsiClass(project: Project, qualifiedName: String): PsiClass? {
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        return javaPsiFacade.findClass(qualifiedName, com.intellij.psi.search.GlobalSearchScope.allScope(project))
    }
}
