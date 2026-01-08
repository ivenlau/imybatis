package com.xiaomi.imybatis.core.index.model

import com.intellij.openapi.vfs.VirtualFile

/**
 * Information about an Entity class
 */
data class EntityInfo(
    val qualifiedName: String,
    val tableName: String?,
    val fields: List<EntityFieldInfo>,
    val virtualFile: VirtualFile?
)

/**
 * Information about an Entity field
 */
data class EntityFieldInfo(
    val name: String,
    val type: String,
    val columnName: String,
    val isPrimaryKey: Boolean = false
)
