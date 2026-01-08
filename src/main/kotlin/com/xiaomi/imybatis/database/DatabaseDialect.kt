package com.xiaomi.imybatis.database

import java.sql.Connection

/**
 * Database dialect interface
 * Provides database-specific functionality
 */
interface DatabaseDialect {
    /**
     * Get dialect name
     */
    fun getName(): String

    /**
     * Get JDBC URL pattern
     */
    fun getJdbcUrlPattern(): String

    /**
     * Get driver class name
     */
    fun getDriverClassName(): String

    /**
     * Get SQL keywords for this dialect
     */
    fun getSqlKeywords(): List<String>

    /**
     * Get SQL functions for this dialect
     */
    fun getSqlFunctions(): List<String>

    /**
     * Convert JDBC type to Java type
     */
    fun jdbcTypeToJavaType(jdbcType: Int, typeName: String): String

    /**
     * Get table metadata query
     */
    fun getTableMetadataQuery(schema: String?): String

    /**
     * Get column metadata query
     */
    fun getColumnMetadataQuery(schema: String?, tableName: String): String
}
