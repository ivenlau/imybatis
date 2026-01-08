package com.xiaomi.imybatis.generator

import com.xiaomi.imybatis.database.ColumnMetadata
import com.xiaomi.imybatis.database.TableMetadata
import com.xiaomi.imybatis.template.TemplateEngine

/**
 * Generator for Mapper XML files
 */
class XmlGenerator(private val templateEngine: TemplateEngine) {

    /**
     * Generate Mapper XML code
     */
    fun generate(
        namespace: String,
        tableName: String,
        entityName: String,
        entityPackage: String,
        tableMetadata: TableMetadata,
        useMyBatisPlus: Boolean = true
    ): String {
        val primaryKeyColumns = tableMetadata.columns.filter { it.isPrimaryKey }.map {
            ColumnInfo(it.name, camelCase(it.name), it.type, it.isPrimaryKey)
        }
        val nonPrimaryKeyColumns = tableMetadata.columns.filter { !it.isPrimaryKey }.map {
            ColumnInfo(it.name, camelCase(it.name), it.type, it.isPrimaryKey)
        }
        val allColumns = tableMetadata.columns.map {
            ColumnInfo(it.name, camelCase(it.name), it.type, it.isPrimaryKey)
        }

        val dataModel = mapOf(
            "namespace" to namespace,
            "tableName" to tableName,
            "entityName" to entityName,
            "entityPackage" to entityPackage,
            "entityFqn" to "$entityPackage.$entityName",
            "primaryKeyColumns" to primaryKeyColumns,
            "nonPrimaryKeyColumns" to nonPrimaryKeyColumns,
            "allColumns" to allColumns,
            "useMyBatisPlus" to useMyBatisPlus
        )

        val template = getDefaultXmlTemplate()
        return templateEngine.processString(template, dataModel)
    }

    private fun camelCase(name: String): String {
        val parts = name.split("_")
        return if (parts.isEmpty()) name else {
            parts[0].lowercase() + parts.drop(1).joinToString("") { 
                it.lowercase().replaceFirstChar { char -> char.uppercaseChar() }
            }
        }
    }

    private fun getDefaultXmlTemplate(): String {
        return """
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="${'$'}{namespace}">
    <!-- ResultMap -->
        <resultMap id="BaseResultMap" type="${'$'}{entityFqn}">
<#list primaryKeyColumns as column>
        <id property="${'$'}{column.propertyName}" column="${'$'}{column.columnName}" jdbcType="${'$'}{column.type}"/>
</#list>
<#list nonPrimaryKeyColumns as column>
        <result property="${'$'}{column.propertyName}" column="${'$'}{column.columnName}" jdbcType="${'$'}{column.type}"/>
</#list>
    </resultMap>

    <!-- Base Column List -->
    <sql id="Base_Column_List">
<#list allColumns as column>
        ${'$'}{column.columnName}<#if column_has_next>,</#if>
</#list>
    </sql>

<#if !useMyBatisPlus>
    <!-- Select by Primary Key -->
    <select id="selectById" resultMap="BaseResultMap">
        SELECT
        <include refid="Base_Column_List"/>
        FROM ${'$'}{tableName}
        WHERE <#list primaryKeyColumns as pk>${'$'}{pk.columnName} = ${'$'}{"#"}{${'$'}{pk.propertyName?string}}<#if pk_has_next> AND </#if></#list>
    </select>

    <!-- Select All -->
    <select id="selectList" resultMap="BaseResultMap">
        SELECT
        <include refid="Base_Column_List"/>
        FROM ${'$'}{tableName}
    </select>

    <!-- Insert -->
    <insert id="insert" parameterType="${'$'}{entityFqn}">
        INSERT INTO ${'$'}{tableName}
        (<#list allColumns as column>${'$'}{column.columnName}<#if column_has_next>, </#if></#list>)
        VALUES
        (<#list allColumns as column>${'$'}{"#"}{${'$'}{column.propertyName?string}}<#if column_has_next>, </#if></#list>)
    </insert>

    <!-- Update by Primary Key -->
    <update id="updateById" parameterType="${'$'}{entityFqn}">
        UPDATE ${'$'}{tableName}
        SET
<#list nonPrimaryKeyColumns as column>
            ${'$'}{column.columnName} = ${'$'}{"#"}{${'$'}{column.propertyName?string}}<#if column_has_next>,</#if>
</#list>
        WHERE <#list primaryKeyColumns as pk>${'$'}{pk.columnName} = ${'$'}{"#"}{${'$'}{pk.propertyName?string}}<#if pk_has_next> AND </#if></#list>
    </update>

    <!-- Delete by Primary Key -->
    <delete id="deleteById">
        DELETE FROM ${'$'}{tableName}
        WHERE <#list primaryKeyColumns as pk>${'$'}{pk.columnName} = ${'$'}{"#"}{${'$'}{pk.propertyName?string}}<#if pk_has_next> AND </#if></#list>
    </delete>
</#if>
</mapper>
        """.trimIndent()
    }
}

/**
 * Column information for template
 */
data class ColumnInfo(
    val columnName: String,
    val propertyName: String,
    val type: String,
    val isPrimaryKey: Boolean
)
