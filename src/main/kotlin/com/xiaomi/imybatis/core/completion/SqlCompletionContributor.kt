package com.xiaomi.imybatis.core.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlText
import com.intellij.util.ProcessingContext
import com.xiaomi.imybatis.database.MetadataProvider

/**
 * Completion contributor for SQL keywords, table names, and column names
 */
class SqlCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().inFile(PlatformPatterns.psiFile(XmlFile::class.java)),
            SqlCompletionProvider()
        )
    }

    private class SqlCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val element = parameters.position
            val xmlFile = element.containingFile as? XmlFile ?: return

            // Check if this is a MyBatis mapper file (rootTag must be "mapper")
            val rootTag = xmlFile.rootTag ?: return
            if (rootTag.name != "mapper") return

            // Check if we're inside XML text content (not in attributes)
            val xmlText = PsiTreeUtil.getParentOfType(element, XmlText::class.java)
            if (xmlText == null) return

            // Check if we're inside a SQL statement tag
            val xmlTag = PsiTreeUtil.getParentOfType(element, com.intellij.psi.xml.XmlTag::class.java) ?: return
            if (xmlTag.name !in listOf("select", "insert", "update", "delete")) return

            val project = element.project

            // Add SQL keyword completions
            addSqlKeywords(result)

            // Try to add table/column completions if we have database connection
            // This will be enhanced when database support is implemented
            addTableCompletions(project, result)
        }

        private fun addSqlKeywords(result: CompletionResultSet) {
            val mysqlKeywords = listOf(
                "SELECT", "FROM", "WHERE", "JOIN", "INNER JOIN", "LEFT JOIN", "RIGHT JOIN",
                "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
                "ORDER BY", "GROUP BY", "HAVING", "LIMIT", "OFFSET",
                "AND", "OR", "NOT", "IN", "LIKE", "BETWEEN", "IS NULL", "IS NOT NULL",
                "COUNT", "SUM", "AVG", "MAX", "MIN", "DISTINCT",
                "AS", "ON", "USING"
            )

            for (keyword in mysqlKeywords) {
                result.addElement(
                    LookupElementBuilder.create(keyword)
                        .withTypeText("SQL Keyword")
                )
            }
        }

        private fun addTableCompletions(project: Project, result: CompletionResultSet) {
            // This will be implemented when database support is implemented
            // For now, we can add Entity table names from index
            val indexManager = com.xiaomi.imybatis.core.index.IndexManager.getInstance(project)
            val entities = indexManager.findAllEntities()

            for (entity in entities) {
                val tableName = entity.tableName ?: continue
                result.addElement(
                    LookupElementBuilder.create(tableName)
                        .withTypeText("Table (${entity.qualifiedName})")
                )

                // Add column names from entity fields
                for (field in entity.fields) {
                    result.addElement(
                        LookupElementBuilder.create(field.columnName)
                            .withTypeText("Column (${field.type})")
                    )
                }
            }
        }
    }
}
