package com.xiaomi.imybatis.plus

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.xiaomi.imybatis.core.index.IndexManager
import com.xiaomi.imybatis.core.index.model.EntityFieldInfo
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.JavaElementVisitor

/**
 * Completion support for MyBatis Plus Wrapper classes
 * Provides column name completion for QueryWrapper and LambdaQueryWrapper
 */
class WrapperSupport : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            WrapperCompletionProvider()
        )
    }

    private class WrapperCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
        ) {
            val element = parameters.position
            val psiMethodCall = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression::class.java) ?: return

            // Check if this is a Wrapper method call
            val methodName = psiMethodCall.methodExpression.referenceName ?: return
            if (methodName !in listOf("eq", "ne", "gt", "ge", "lt", "le", "like", "in", "between")) {
                return
            }

            // Find the Wrapper type and extract entity type
            val wrapperType = findWrapperType(psiMethodCall) ?: return
            val entityType = extractEntityTypeFromWrapper(wrapperType) ?: return

            // Get entity fields from index
            val project = element.project
            val indexManager = IndexManager.getInstance(project)
            val entityInfo = indexManager.findEntity(entityType) ?: return

            // Add column name completions
            for (field in entityInfo.fields) {
                result.addElement(
                    LookupElementBuilder.create("\"${field.columnName}\"")
                        .withTypeText("Column (${field.name}: ${field.type})")
                )
            }
        }

        /**
         * Find Wrapper type from method call context
         */
        private fun findWrapperType(psiMethodCall: PsiMethodCallExpression): PsiType? {
            val qualifier = psiMethodCall.methodExpression.qualifierExpression
            return qualifier?.type
        }

        /**
         * Extract entity type from Wrapper generic type
         */
        private fun extractEntityTypeFromWrapper(wrapperType: PsiType?): String? {
            if (wrapperType !is PsiClassType) return null

            val canonicalText = wrapperType.canonicalText
            if (!canonicalText.contains("Wrapper")) return null

            // Try to extract generic parameter
            val parameters = wrapperType.parameters
            if (parameters.isNotEmpty()) {
                val firstParameter = parameters[0]
                return when (firstParameter) {
                    is PsiClassType -> firstParameter.canonicalText
                    is PsiType -> firstParameter.canonicalText
                    else -> null
                }
            }

            return null
        }
    }
}

/**
 * Inspection for Wrapper column name validation
 */
class WrapperColumnInspection : com.intellij.codeInspection.LocalInspectionTool() {
    override fun buildVisitor(
        holder: com.intellij.codeInspection.ProblemsHolder,
        isOnTheFly: Boolean
    ): PsiElementVisitor {
        return object : JavaElementVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                val methodName = expression.methodExpression.referenceName ?: return
                if (methodName !in listOf("eq", "ne", "gt", "ge", "lt", "le", "like", "in", "between")) {
                    return
                }

                // Validate column name
                val arguments = expression.argumentList.expressions
                if (arguments.isEmpty()) return

                val firstArgument = arguments[0]
                if (firstArgument is PsiLiteralExpression) {
                    val columnName = firstArgument.value as? String ?: return
                    // Check if column name exists in entity
                    // This would require context about which entity the wrapper is for
                    // For now, this is a placeholder
                }
            }
        }
    }
}
