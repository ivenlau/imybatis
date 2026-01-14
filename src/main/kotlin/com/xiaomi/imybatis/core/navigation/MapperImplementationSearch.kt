package com.xiaomi.imybatis.core.navigation

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiClass
import com.intellij.psi.search.searches.DefinitionsScopedSearch
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor
import com.xiaomi.imybatis.core.index.IndexManager

/**
 * Implementation search for Mapper interfaces
 * Handles navigation from Mapper methods to XML statements via Ctrl+Alt+B
 */
class MapperImplementationSearch : QueryExecutor<PsiElement, DefinitionsScopedSearch.SearchParameters> {

    override fun execute(
        queryParameters: DefinitionsScopedSearch.SearchParameters,
        consumer: Processor<in PsiElement>
    ): Boolean {
        val sourceElement = queryParameters.element
        if (sourceElement !is PsiMethod) return true

        return ReadAction.compute<Boolean, RuntimeException> {
            val psiClass = sourceElement.containingClass ?: return@compute true

            // Check if this is a Mapper interface
            if (!isMapperInterface(psiClass)) return@compute true

            val mapperFqn = psiClass.qualifiedName ?: return@compute true
            val methodName = sourceElement.name
            val project = sourceElement.project

            // Find corresponding XML statements
            val indexManager = IndexManager.getInstance(project)
            val xmlMapper = indexManager.findXmlMapper(mapperFqn) ?: return@compute true

            val statements = xmlMapper.statements.filter { it.id == methodName }
            
            for (statement in statements) {
                val xmlTag = statement.xmlTag
                if (xmlTag != null) {
                    if (!consumer.process(xmlTag)) return@compute false
                }
            }
            true
        }
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
