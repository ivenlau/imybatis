package com.xiaomi.imybatis.core.index.model

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.xml.XmlTag

/**
 * Information about a Mapper XML file
 */
data class XmlMapperInfo(
    val namespace: String,
    val statements: List<XmlStatementInfo>,
    val resultMaps: List<String>,
    val sqlFragments: List<String>,
    val virtualFile: VirtualFile?
)

/**
 * Information about a SQL statement in XML
 */
data class XmlStatementInfo(
    val id: String,
    val type: String, // select, insert, update, delete
    val parameterType: String?,
    val resultType: String?,
    val resultMap: String?,
    val xmlTag: XmlTag
)
