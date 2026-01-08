package com.xiaomi.imybatis.core.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.util.ProcessingContext
import com.xiaomi.imybatis.core.index.IndexManager

/**
 * Completion contributor for MyBatis parameter placeholders (#{} and ${})
 */
class ParameterCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().inFile(PlatformPatterns.psiFile(XmlFile::class.java)),
            ParameterCompletionProvider()
        )
    }

    private class ParameterCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val element = parameters.position
            val xmlFile = element.containingFile as? XmlFile ?: return

            // Check if we're inside a SQL statement
            val xmlTag = PsiTreeUtil.getParentOfType(element, com.intellij.psi.xml.XmlTag::class.java) ?: return
            if (xmlTag.name !in listOf("select", "insert", "update", "delete")) return

            // Find the mapper namespace
            val rootTag = xmlFile.rootTag ?: return
            if (rootTag.name != "mapper") return
            val namespace = rootTag.getAttributeValue("namespace") ?: return

            // Find the statement id
            val statementId = xmlTag.getAttributeValue("id") ?: return

            // Get Mapper method parameters
            val project = element.project
            val indexManager = IndexManager.getInstance(project)
            val mapperInfo = indexManager.findMapper(namespace) ?: return

            val method = mapperInfo.methods.find { it.name == statementId } ?: return

            // Add parameter completions
            for ((type, name) in method.parameters) {
                result.addElement(
                    LookupElementBuilder.create("$name")
                        .withTypeText(type)
                )

                // If parameter is a POJO, add nested property completions
                if (isPojoType(type)) {
                    addPojoProperties(project, type, result, "")
                }
            }
        }

        private fun isPojoType(type: String): Boolean {
            // Simple check: if type doesn't start with java.lang or is a primitive, it might be a POJO
            return !type.startsWith("java.lang.") &&
                    type !in listOf("int", "long", "double", "float", "boolean", "char", "byte", "short", "String")
        }

        private fun addPojoProperties(
            project: Project,
            pojoType: String,
            result: CompletionResultSet,
            prefix: String
        ) {
            val javaPsiFacade = JavaPsiFacade.getInstance(project)
            val psiClass = javaPsiFacade.findClass(
                pojoType,
                com.intellij.psi.search.GlobalSearchScope.allScope(project)
            ) ?: return

            for (field in psiClass.fields) {
                val fieldName = field.name ?: continue
                val fieldType = field.type.canonicalText
                val lookupText = if (prefix.isEmpty()) fieldName else "$prefix.$fieldName"

                result.addElement(
                    LookupElementBuilder.create(lookupText)
                        .withTypeText(fieldType)
                )
            }
        }
    }
}
