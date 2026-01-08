package com.xiaomi.imybatis.core.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.xiaomi.imybatis.core.index.IndexManager

/**
 * Quick fix for creating missing ResultMap
 */
class CreateResultMapQuickFix(private val resultMapId: String, private val type: String) : LocalQuickFix {
    override fun getName(): String = "Create ResultMap '$resultMapId'"

    override fun getFamilyName(): String = "Create ResultMap"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val xmlTag = PsiTreeUtil.getParentOfType(element, XmlTag::class.java) ?: return
        val xmlFile = xmlTag.containingFile as? com.intellij.psi.xml.XmlFile ?: return
        val rootTag = xmlFile.rootTag ?: return

        // Create ResultMap tag
        val resultMapTag = rootTag.createChildTag("resultMap", rootTag.namespace, "", false)
        resultMapTag.setAttribute("id", resultMapId)
        resultMapTag.setAttribute("type", type)

        // Add basic result elements (this is a simplified version)
        // In a real implementation, we would extract fields from the Entity class
        rootTag.addSubTag(resultMapTag, false)
    }
}

/**
 * Quick fix for fixing parameterType
 */
class FixParameterTypeQuickFix(private val correctType: String) : LocalQuickFix {
    override fun getName(): String = "Fix parameterType to '$correctType'"

    override fun getFamilyName(): String = "Fix parameterType"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val xmlTag = PsiTreeUtil.getParentOfType(element, XmlTag::class.java) ?: return

        xmlTag.setAttribute("parameterType", correctType)
    }
}

/**
 * Quick fix for fixing resultType
 */
class FixResultTypeQuickFix(private val correctType: String) : LocalQuickFix {
    override fun getName(): String = "Fix resultType to '$correctType'"

    override fun getFamilyName(): String = "Fix resultType"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val xmlTag = PsiTreeUtil.getParentOfType(element, XmlTag::class.java) ?: return

        xmlTag.setAttribute("resultType", correctType)
    }
}
