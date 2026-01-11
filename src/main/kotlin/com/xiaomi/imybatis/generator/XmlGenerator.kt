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
        useMyBatisPlus: Boolean = true,
        generateBatchOperations: Boolean = false,
        generateInsertOnDuplicateUpdate: Boolean = false
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
            "useMyBatisPlus" to useMyBatisPlus,
            "generateBatchOperations" to generateBatchOperations,
            "generateInsertOnDuplicateUpdate" to generateInsertOnDuplicateUpdate
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
    <insert id="insert" parameterType="${'$'}{entityFqn}"<#if generateInsertOnDuplicateUpdate> useGeneratedKeys="true" keyProperty="<#list primaryKeyColumns as pk>${'$'}{pk.propertyName}<#if pk_has_next>,</#if></#list>"</#if>>
        INSERT INTO ${'$'}{tableName}
        (<#list allColumns as column>${'$'}{column.columnName}<#if column_has_next>, </#if></#list>)
        VALUES
        (<#list allColumns as column>${'$'}{"#"}{${'$'}{column.propertyName?string}}<#if column_has_next>, </#if></#list>)
<#if generateInsertOnDuplicateUpdate>
        ON DUPLICATE KEY UPDATE
<#list nonPrimaryKeyColumns as column>
        ${'$'}{column.columnName} = VALUES(${'$'}{column.columnName})<#if column_has_next>,</#if>
</#list>
</#if>
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

<#if generateBatchOperations>
    <!-- Batch Insert -->
    <insert id="batchInsert" parameterType="java.util.List">
        INSERT INTO ${'$'}{tableName}
        (<#list allColumns as column>${'$'}{column.columnName}<#if column_has_next>, </#if></#list>)
        VALUES
        <foreach collection="list" item="item" separator=",">
        (<#list allColumns as column>${'$'}{"#"}{item.${'$'}{column.propertyName?string}}<#if column_has_next>, </#if></#list>)
        </foreach>
    </insert>

<#if generateInsertOnDuplicateUpdate>
    <!-- Batch Insert On Duplicate Update -->
    <insert id="batchInsertOnDuplicate" parameterType="java.util.List">
        INSERT INTO ${'$'}{tableName}
        (<#list allColumns as column>${'$'}{column.columnName}<#if column_has_next>, </#if></#list>)
        VALUES
        <foreach collection="list" item="item" separator=",">
        (<#list allColumns as column>${'$'}{"#"}{item.${'$'}{column.propertyName?string}}<#if column_has_next>, </#if></#list>)
        </foreach>
        ON DUPLICATE KEY UPDATE
<#list nonPrimaryKeyColumns as column>
        ${'$'}{column.columnName} = VALUES(${'$'}{column.columnName})<#if column_has_next>,</#if>
</#list>
    </insert>
</#if>

    <!-- Batch Update -->
    <update id="batchUpdate" parameterType="java.util.List">
        <foreach collection="list" item="item" separator=";">
        UPDATE ${'$'}{tableName}
        SET
<#list nonPrimaryKeyColumns as column>
            ${'$'}{column.columnName} = ${'$'}{"#"}{item.${'$'}{column.propertyName?string}}<#if column_has_next>,</#if>
</#list>
        WHERE <#list primaryKeyColumns as pk>${'$'}{pk.columnName} = ${'$'}{"#"}{item.${'$'}{pk.propertyName?string}}<#if pk_has_next> AND </#if></#list>
        </foreach>
    </update>

    <!-- Batch Delete -->
    <delete id="batchDelete" parameterType="java.util.List">
        DELETE FROM ${'$'}{tableName}
        WHERE
        <foreach collection="list" item="item" separator=" OR ">
        (<#list primaryKeyColumns as pk>${'$'}{pk.columnName} = ${'$'}{"#"}{item.${'$'}{pk.propertyName?string}}<#if pk_has_next> AND </#if></#list>)
        </foreach>
    </delete>

    <!-- Batch Delete by IDs -->
    <delete id="batchDeleteByIds">
        DELETE FROM ${'$'}{tableName}
        WHERE <#list primaryKeyColumns as pk>${'$'}{pk.columnName} IN
        <foreach collection="list" item="id" open="(" separator="," close=")">
            ${'$'}{"#"}{id}
        </foreach>
        <#if pk_has_next> AND </#if></#list>
    </delete>
</#if>
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
