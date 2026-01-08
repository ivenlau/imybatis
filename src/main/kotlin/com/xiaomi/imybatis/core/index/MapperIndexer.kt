package com.xiaomi.imybatis.core.index

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.xiaomi.imybatis.core.index.model.MapperMethodInfo
import com.xiaomi.imybatis.core.index.model.MapperInfo

/**
 * Indexer for Mapper interfaces
 * Scans Java files to find Mapper interfaces and index their methods
 */
class MapperIndexer : DataIndexer<String, Void?, FileContent> {

    override fun map(inputData: FileContent): Map<String, Void?> {
        val result = mutableMapOf<String, Void?>()
        val psiFile = inputData.psiFile

        psiFile.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitClass(psiClass: PsiClass) {
                super.visitClass(psiClass)
                if (isMapperInterface(psiClass)) {
                    psiClass.qualifiedName?.let {
                        result[it] = null
                    }
                }
            }
        })

        return result
    }

    /**
     * Check if a class is a Mapper interface
     * NOTE: This is called during indexing, so we must avoid any operations that might
     * trigger PSI resolution or index lookups to prevent IndexNotReadyException.
     */
    private fun isMapperInterface(psiClass: PsiClass): Boolean {
        if (!psiClass.isInterface) return false

        // Check for @Mapper annotation - use text representation to avoid resolution
        psiClass.modifierList?.annotations?.forEach { annotation ->
            // Get the reference text instead of qualifiedName to avoid triggering index lookup
            val referenceText = annotation.nameReferenceElement?.text
            if (referenceText == "Mapper") {
                return true
            }
        }

        // Check if it extends a known Mapper interface (like BaseMapper from MyBatis Plus)
        // Use reference text from extends list to avoid resolution
        psiClass.extendsList?.referenceElements?.forEach { referenceElement ->
            // Using .text is safer than resolving to qualified name during indexing
            val referenceName = referenceElement.text
            if (referenceName.contains("BaseMapper") || referenceName.contains("Mapper")) {
                return true
            }
        }

        return false
    }
}

/**
 * Extract Mapper information from a PsiClass
 */
fun extractMapperInfo(psiClass: PsiClass): MapperInfo? {
    if (!psiClass.isInterface) return null

    val mapperFqn = psiClass.qualifiedName ?: return null
    val methods = mutableListOf<MapperMethodInfo>()

    for (method in psiClass.methods) {
        val methodInfo = extractMethodInfo(method)
        if (methodInfo != null) {
            methods.add(methodInfo)
        }
    }

    return MapperInfo(
        qualifiedName = mapperFqn,
        methods = methods,
        virtualFile = psiClass.containingFile?.virtualFile
    )
}

/**
 * Extract method information from a PsiMethod
 */
fun extractMethodInfo(psiMethod: PsiMethod): MapperMethodInfo? {
    val methodName = psiMethod.name
    val returnType = psiMethod.returnType?.canonicalText ?: "void"
    val parameters = psiMethod.parameterList.parameters.map { param ->
        param.type.canonicalText to param.name
    }

    // Check for SQL annotations
    val annotations = psiMethod.modifierList?.annotations?.mapNotNull { annotation ->
        val annotationName = annotation.qualifiedName
        when {
            annotationName?.contains("Select") == true -> "Select"
            annotationName?.contains("Insert") == true -> "Insert"
            annotationName?.contains("Update") == true -> "Update"
            annotationName?.contains("Delete") == true -> "Delete"
            else -> null
        }
    } ?: emptyList()

    return MapperMethodInfo(
        name = methodName,
        returnType = returnType,
        parameters = parameters,
        hasSqlAnnotation = annotations.isNotEmpty(),
        sqlAnnotations = annotations
    )
}
