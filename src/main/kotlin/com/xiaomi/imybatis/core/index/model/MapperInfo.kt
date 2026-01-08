package com.xiaomi.imybatis.core.index.model

import com.intellij.openapi.vfs.VirtualFile

/**
 * Information about a Mapper interface
 */
data class MapperInfo(
    val qualifiedName: String,
    val methods: List<MapperMethodInfo>,
    val virtualFile: VirtualFile?
)

/**
 * Information about a Mapper method
 */
data class MapperMethodInfo(
    val name: String,
    val returnType: String,
    val parameters: List<Pair<String, String>>, // (type, name)
    val hasSqlAnnotation: Boolean = false,
    val sqlAnnotations: List<String> = emptyList()
)
