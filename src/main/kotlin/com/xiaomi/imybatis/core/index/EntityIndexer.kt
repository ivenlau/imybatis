package com.xiaomi.imybatis.core.index

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileContent
import com.xiaomi.imybatis.core.index.model.EntityInfo
import com.xiaomi.imybatis.core.index.model.EntityFieldInfo

/**
 * Indexer for Entity classes
 * Scans Java files to find entity classes and index their fields
 */
class EntityIndexer : DataIndexer<String, Void?, FileContent> {

    override fun map(inputData: FileContent): Map<String, Void?> {
        val result = mutableMapOf<String, Void?>()
        val psiFile = inputData.psiFile as? PsiJavaFile ?: return result

        // Avoid using PsiTreeUtil.getChildrenOfType as it may trigger additional PSI resolution
        // Instead, directly access the classes from the PsiJavaFile
        val classes = psiFile.classes

        for (psiClass in classes) {
            if (isEntityClass(psiClass)) {
                val entityFqn = psiClass.qualifiedName ?: continue
                result[entityFqn] = null
            }
        }

        return result
    }

    /**
     * Check if a class is an Entity class
     * NOTE: This is called during indexing, so we must avoid any operations that might
     * trigger PSI resolution or index lookups to prevent IndexNotReadyException.
     */
    private fun isEntityClass(psiClass: PsiClass): Boolean {
        if (psiClass.isInterface) return false

        // Check for @Entity annotation (JPA) - use text representation to avoid resolution
        val annotations = psiClass.modifierList?.annotations ?: return false
        for (annotation in annotations) {
            // Get the reference text instead of qualifiedName to avoid triggering index lookup
            val referenceText = annotation.nameReferenceElement?.text ?: continue

            // Check for common entity-related annotation names
            if (referenceText == "Entity" || referenceText == "TableName") {
                return true
            }
        }

        return false
    }
}

/**
 * Extract Entity information from a PsiClass
 */
fun extractEntityInfo(psiClass: PsiClass): EntityInfo? {
    if (psiClass.isInterface) return null

    val entityFqn = psiClass.qualifiedName ?: return null
    val tableName = extractTableName(psiClass)
    val fields = mutableListOf<EntityFieldInfo>()

    // Extract fields
    for (field in psiClass.fields) {
        val fieldInfo = extractFieldInfo(field)
        if (fieldInfo != null) {
            fields.add(fieldInfo)
        }
    }

    return EntityInfo(
        qualifiedName = entityFqn,
        tableName = tableName,
        fields = fields,
        virtualFile = psiClass.containingFile?.virtualFile
    )
}

/**
 * Extract table name from entity class
 */
private fun extractTableName(psiClass: PsiClass): String? {
    val annotations = psiClass.modifierList?.annotations ?: return null
    
    for (annotation in annotations) {
        val qualifiedName = annotation.qualifiedName
        when {
            qualifiedName == "com.baomidou.mybatisplus.annotation.TableName" -> {
                val value = annotation.findAttributeValue("value")
                return value?.text?.removeSurrounding("\"")
            }
            qualifiedName == "javax.persistence.Table" ||
            qualifiedName == "jakarta.persistence.Table" -> {
                val value = annotation.findAttributeValue("name")
                return value?.text?.removeSurrounding("\"")
            }
        }
    }

    // Default: use class name (converted to snake_case)
    return psiClass.name?.let { camelToSnakeCase(it) }
}

/**
 * Extract field information from a PsiField
 */
fun extractFieldInfo(psiField: PsiField): EntityFieldInfo? {
    val fieldName = psiField.name ?: return null
    val fieldType = psiField.type.canonicalText
    val columnName = extractColumnName(psiField) ?: camelToSnakeCase(fieldName)
    val isPrimaryKey = isPrimaryKey(psiField)

    return EntityFieldInfo(
        name = fieldName,
        type = fieldType,
        columnName = columnName,
        isPrimaryKey = isPrimaryKey
    )
}

/**
 * Extract column name from field
 */
private fun extractColumnName(psiField: PsiField): String? {
    val annotations = psiField.modifierList?.annotations ?: return null
    
    for (annotation in annotations) {
        val qualifiedName = annotation.qualifiedName
        when {
            qualifiedName == "com.baomidou.mybatisplus.annotation.TableField" -> {
                val value = annotation.findAttributeValue("value")
                return value?.text?.removeSurrounding("\"")
            }
            qualifiedName == "javax.persistence.Column" ||
            qualifiedName == "jakarta.persistence.Column" -> {
                val value = annotation.findAttributeValue("name")
                return value?.text?.removeSurrounding("\"")
            }
        }
    }
    
    return null
}

/**
 * Check if field is primary key
 */
private fun isPrimaryKey(psiField: PsiField): Boolean {
    val annotations = psiField.modifierList?.annotations ?: return false
    
    for (annotation in annotations) {
        val qualifiedName = annotation.qualifiedName
        if (qualifiedName == "javax.persistence.Id" ||
            qualifiedName == "jakarta.persistence.Id" ||
            qualifiedName == "com.baomidou.mybatisplus.annotation.TableId") {
            return true
        }
    }
    
    return false
}

/**
 * Convert camelCase to snake_case
 */
private fun camelToSnakeCase(str: String): String {
    return str.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
}
