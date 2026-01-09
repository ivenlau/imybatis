package com.xiaomi.imybatis.core.navigation

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.diagnostic.Logger
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

    companion object {
        private val LOG = Logger.getInstance(EntityNavigationProvider::class.java)
    }

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?
    ): Array<PsiElement>? {
        if (sourceElement == null) return null

        LOG.info("===== EntityNavigationProvider called =====")
        LOG.info("Source element: ${sourceElement.javaClass.simpleName}, text: '${sourceElement.text}'")

        val project = sourceElement.project

        // Check if we're in a ResultMap property attribute
        val xmlTag = PsiTreeUtil.getParentOfType(sourceElement, XmlTag::class.java)
        LOG.info("Parent XML tag: ${xmlTag?.name}, tag text: '${xmlTag?.text?.take(100)}'")

        if (xmlTag != null && (xmlTag.name == "result" || xmlTag.name == "id")) {
            val property = xmlTag.getAttributeValue("property")
            LOG.info("Property attribute value: $property")

            if (property == null) {
                LOG.warn("Property attribute value is null")
                return null
            }

            // Check if cursor is on the property value (either XmlAttributeValue or XmlToken)
            val isOnPropertyValue = when {
                sourceElement is XmlAttributeValue -> {
                    LOG.info("Source is XmlAttributeValue, value: '${sourceElement.value}'")
                    sourceElement.value == property
                }
                sourceElement.text == property -> {
                    LOG.info("Source text matches property value: '$property'")
                    true
                }
                else -> {
                    false
                }
            }

            if (isOnPropertyValue) {
                LOG.info("Cursor is on property value, navigating to entity field: $property")

                // Find the resultMap type
                val resultMapTag = findResultMapTag(xmlTag)
                LOG.info("ResultMap tag found: ${resultMapTag != null}")

                if (resultMapTag != null) {
                    val type = resultMapTag.getAttributeValue("type")
                    LOG.info("ResultMap type: $type")

                    if (type != null) {
                        // Navigate to Entity field
                        return navigateToEntityField(project, type, property)
                    } else {
                        LOG.warn("ResultMap type is null")
                    }
                }
            } else {
                LOG.info("Cursor is NOT on property value")
                // Check if cursor is on the attribute name
                val sourceText = sourceElement.text.trim()
                if (sourceText == "property") {
                    LOG.info("Cursor is on 'property' attribute name, navigating to: $property")
                    val resultMapTag = findResultMapTag(xmlTag)
                    if (resultMapTag != null) {
                        val type = resultMapTag.getAttributeValue("type")
                        if (type != null) {
                            return navigateToEntityField(project, type, property)
                        }
                    }
                }
            }
        } else {
            LOG.info("Not in a result/id tag or xmlTag is null")
        }

        LOG.info("===== EntityNavigationProvider returning null =====")
        return null
    }

    /**
     * Find the parent resultMap tag
     */
    private fun findResultMapTag(tag: XmlTag): XmlTag? {
        var current: XmlTag? = tag
        var depth = 0
        while (current != null && depth < 10) {
            if (current.name == "resultMap") {
                LOG.info("Found resultMap tag at depth $depth")
                return current
            }
            current = current.parent as? XmlTag
            depth++
        }
        LOG.warn("resultMap tag not found")
        return null
    }

    /**
     * Navigate from ResultMap property to Entity field
     */
    private fun navigateToEntityField(project: Project, entityType: String, fieldName: String): Array<PsiElement>? {
        LOG.info("Navigating to entity field: type=$entityType, field=$fieldName")

        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        val psiClass = javaPsiFacade.findClass(
            entityType,
            com.intellij.psi.search.GlobalSearchScope.allScope(project)
        )

        LOG.info("Found PsiClass: ${psiClass?.qualifiedName}")
        if (psiClass == null) {
            LOG.warn("Class not found: $entityType")
            return null
        }

        val field = psiClass.findFieldByName(fieldName, false)
        LOG.info("Found field: ${field?.name}")

        return if (field != null) arrayOf(field) else {
            LOG.warn("Field '$fieldName' not found in class ${psiClass.qualifiedName}")
            null
        }
    }
}
