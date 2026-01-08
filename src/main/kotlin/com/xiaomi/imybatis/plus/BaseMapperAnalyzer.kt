package com.xiaomi.imybatis.plus

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.xiaomi.imybatis.core.index.IndexManager
import com.xiaomi.imybatis.core.index.model.EntityInfo

/**
 * Analyzer for BaseMapper interfaces
 * Identifies BaseMapper<T> generic types and establishes entity-table mappings
 */
class BaseMapperAnalyzer(private val indexManager: IndexManager) {

    /**
     * Analyze BaseMapper interface and extract entity type
     */
    fun analyzeBaseMapper(psiClass: PsiClass): EntityInfo? {
        if (!psiClass.isInterface) return null

        // Check if it extends BaseMapper
        val baseMapperType = findBaseMapperType(psiClass) ?: return null
        val entityType = extractEntityTypeFromBaseMapper(baseMapperType) ?: return null

        // Find entity in index
        return indexManager.findEntity(entityType)
    }

    /**
     * Find BaseMapper type in super types
     */
    private fun findBaseMapperType(psiClass: PsiClass): PsiClassType? {
        val superTypes = psiClass.superTypes
        for (superType in superTypes) {
            val canonicalText = superType.canonicalText
            if (canonicalText.contains("BaseMapper")) {
                return superType as? PsiClassType
            }
        }
        return null
    }

    /**
     * Extract entity type from BaseMapper<T>
     */
    private fun extractEntityTypeFromBaseMapper(baseMapperType: PsiClassType): String? {
        val parameters = baseMapperType.parameters
        if (parameters.isEmpty()) return null

        val firstParameter = parameters[0]
        return when (firstParameter) {
            is PsiClassType -> firstParameter.canonicalText
            is PsiType -> firstParameter.canonicalText
            else -> null
        }
    }

    /**
     * Check if a class extends BaseMapper
     */
    fun isBaseMapper(psiClass: PsiClass): Boolean {
        if (!psiClass.isInterface) return false
        return findBaseMapperType(psiClass) != null
    }

    /**
     * Get all BaseMapper interfaces in project
     */
    fun findAllBaseMappers(): List<Pair<PsiClass, EntityInfo?>> {
        val result = mutableListOf<Pair<PsiClass, EntityInfo?>>()
        val mappers = indexManager.findAllMappers()

        // This is a simplified version - in a real implementation,
        // we would scan all interfaces in the project
        return result
    }
}
