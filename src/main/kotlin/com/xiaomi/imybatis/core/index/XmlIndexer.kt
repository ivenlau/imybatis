package com.xiaomi.imybatis.core.index

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileContent
import com.xiaomi.imybatis.core.index.model.XmlMapperInfo
import com.xiaomi.imybatis.core.index.model.XmlStatementInfo

/**
 * Indexer for Mapper XML files
 * Scans XML files to find mapper statements and index them
 */
class XmlIndexer : DataIndexer<String, Void?, FileContent> {

    override fun map(inputData: FileContent): Map<String, Void?> {
        val result = mutableMapOf<String, Void?>()
        val psiFile = inputData.psiFile as? XmlFile ?: return result

        val rootTag = psiFile.rootTag ?: return result
        if (rootTag.name != "mapper") return result

        val namespace = rootTag.getAttributeValue("namespace") ?: return result
        result[namespace] = null

        return result
    }
}

/**
 * Extract XML Mapper information from an XmlFile
 */
fun extractXmlMapperInfo(xmlFile: XmlFile): XmlMapperInfo? {
    val rootTag = xmlFile.rootTag ?: return null
    if (rootTag.name != "mapper") return null

    val namespace = rootTag.getAttributeValue("namespace") ?: return null
    val statements = mutableListOf<XmlStatementInfo>()
    val resultMaps = mutableListOf<String>()
    val sqlFragments = mutableListOf<String>()

    // Extract statements (select, insert, update, delete)
    extractStatements(rootTag, statements)

    // Extract resultMaps
    extractResultMaps(rootTag, resultMaps)

    // Extract sql fragments
    extractSqlFragments(rootTag, sqlFragments)

    return XmlMapperInfo(
        namespace = namespace,
        statements = statements,
        resultMaps = resultMaps,
        sqlFragments = sqlFragments,
        virtualFile = xmlFile.virtualFile
    )
}

/**
 * Extract SQL statements from mapper XML
 */
private fun extractStatements(rootTag: XmlTag, statements: MutableList<XmlStatementInfo>) {
    val statementTypes = listOf("select", "insert", "update", "delete")
    
    for (statementType in statementTypes) {
        val tags = rootTag.findSubTags(statementType)
        for (tag in tags) {
            val id = tag.getAttributeValue("id") ?: continue
            val parameterType = tag.getAttributeValue("parameterType")
            val resultType = tag.getAttributeValue("resultType")
            val resultMap = tag.getAttributeValue("resultMap")
            
            statements.add(
                XmlStatementInfo(
                    id = id,
                    type = statementType,
                    parameterType = parameterType,
                    resultType = resultType,
                    resultMap = resultMap,
                    xmlTag = tag
                )
            )
        }
    }
}

/**
 * Extract resultMap definitions
 */
private fun extractResultMaps(rootTag: XmlTag, resultMaps: MutableList<String>) {
    val resultMapTags = rootTag.findSubTags("resultMap")
    for (tag in resultMapTags) {
        val id = tag.getAttributeValue("id") ?: continue
        resultMaps.add(id)
    }
}

/**
 * Extract SQL fragments
 */
private fun extractSqlFragments(rootTag: XmlTag, sqlFragments: MutableList<String>) {
    val sqlTags = rootTag.findSubTags("sql")
    for (tag in sqlTags) {
        val id = tag.getAttributeValue("id") ?: continue
        sqlFragments.add(id)
    }
}
