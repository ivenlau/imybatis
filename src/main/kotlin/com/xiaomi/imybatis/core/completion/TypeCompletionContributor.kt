package com.xiaomi.imybatis.core.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlFile
import com.intellij.util.ProcessingContext
import com.xiaomi.imybatis.core.index.IndexManager
import com.intellij.openapi.project.Project

/**
 * Completion contributor for type attributes (parameterType, resultType, resultMap)
 */
class TypeCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().inFile(PlatformPatterns.psiFile(XmlFile::class.java)),
            TypeCompletionProvider()
        )
    }

    private class TypeCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val element = parameters.position
            val xmlFile = element.containingFile as? XmlFile ?: return

            val xmlAttributeValue = PsiTreeUtil.getParentOfType(element, XmlAttributeValue::class.java) ?: return
            val xmlTag = PsiTreeUtil.getParentOfType(element, com.intellij.psi.xml.XmlTag::class.java) ?: return

            val project = element.project
            val indexManager = IndexManager.getInstance(project)

            // Check attribute name - get parent XmlAttribute and extract its name
            val xmlAttribute = PsiTreeUtil.getParentOfType(xmlAttributeValue, com.intellij.psi.xml.XmlAttribute::class.java)
            val attributeName = xmlAttribute?.name ?: return

            when (attributeName) {
                "parameterType", "resultType" -> {
                    // Add Java type completions
                    addJavaTypeCompletions(project, result)
                }
                "resultMap" -> {
                    // Add resultMap ID completions
                    addResultMapCompletions(project, xmlFile, result)
                }
                "type" -> {
                    // For resultMap type attribute
                    addJavaTypeCompletions(project, result)
                }
            }
        }

        private fun addJavaTypeCompletions(project: Project, result: CompletionResultSet) {
            // Add common Java types
            val commonTypes = listOf(
                "java.lang.String",
                "java.lang.Integer",
                "java.lang.Long",
                "java.lang.Double",
                "java.lang.Float",
                "java.lang.Boolean",
                "java.util.List",
                "java.util.Map",
                "java.util.Date",
                "java.time.LocalDateTime",
                "java.time.LocalDate"
            )

            for (type in commonTypes) {
                result.addElement(
                    LookupElementBuilder.create(type)
                )
            }

            // Add Entity types from index
            val indexManager = IndexManager.getInstance(project)
            val entities = indexManager.findAllEntities()
            for (entity in entities) {
                result.addElement(
                    LookupElementBuilder.create(entity.qualifiedName)
                        .withTypeText("Entity")
                )
            }
        }

        private fun addResultMapCompletions(
            project: Project,
            xmlFile: XmlFile,
            result: CompletionResultSet
        ) {
            val rootTag = xmlFile.rootTag ?: return
            if (rootTag.name != "mapper") return

            val namespace = rootTag.getAttributeValue("namespace") ?: return
            val indexManager = IndexManager.getInstance(project)
            val xmlMapper = indexManager.findXmlMapper(namespace) ?: return

            for (resultMapId in xmlMapper.resultMaps) {
                result.addElement(
                    LookupElementBuilder.create(resultMapId)
                        .withTypeText("ResultMap")
                )
            }
        }
    }
}
