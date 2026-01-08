package com.xiaomi.imybatis.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

/**
 * Database metadata information
 */
data class TableMetadata(
    val name: String,
    val columns: List<ColumnMetadata>,
    val primaryKeys: List<String>
)

/**
 * Column metadata information
 */
data class ColumnMetadata(
    val name: String,
    val type: String,
    val jdbcType: Int,
    val nullable: Boolean,
    val isPrimaryKey: Boolean,
    val comment: String? = null
)

/**
 * Provider for database metadata
 */
class MetadataProvider(private val dialect: DatabaseDialect) {

    /**
     * Get tables from database
     */
    fun getTables(connection: Connection, schema: String? = null): List<String> {
        val query = dialect.getTableMetadataQuery(schema)
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(query)
        val tables = mutableListOf<String>()

        while (resultSet.next()) {
            tables.add(resultSet.getString(1))
        }

        resultSet.close()
        statement.close()
        return tables
    }

    /**
     * Get table metadata
     */
    fun getTableMetadata(connection: Connection, tableName: String, schema: String? = null): TableMetadata? {
        val query = dialect.getColumnMetadataQuery(schema, tableName)
        val statement = connection.createStatement()
        val resultSet = statement.executeQuery(query)
        val columns = mutableListOf<ColumnMetadata>()
        val primaryKeys = mutableListOf<String>()

        while (resultSet.next()) {
            val columnName = resultSet.getString("COLUMN_NAME") ?: resultSet.getString("column_name")
            val dataType = resultSet.getString("DATA_TYPE") ?: resultSet.getString("data_type")
            val typeName = resultSet.getString("COLUMN_TYPE") 
                ?: resultSet.getString("udt_name") 
                ?: dataType
            val nullable = (resultSet.getString("IS_NULLABLE") ?: resultSet.getString("is_nullable")) == "YES"
            val isPrimaryKey = resultSet.getBoolean("is_primary_key") 
                || (resultSet.getString("COLUMN_KEY") ?: "").contains("PRI")

            val comment = try {
                resultSet.getString("COLUMN_COMMENT") ?: resultSet.getString("column_comment")
            } catch (e: Exception) {
                null
            }

            // Determine JDBC type (simplified)
            val jdbcType = getJdbcType(dataType, typeName)

            columns.add(
                ColumnMetadata(
                    name = columnName,
                    type = typeName,
                    jdbcType = jdbcType,
                    nullable = nullable,
                    isPrimaryKey = isPrimaryKey,
                    comment = comment
                )
            )

            if (isPrimaryKey) {
                primaryKeys.add(columnName)
            }
        }

        resultSet.close()
        statement.close()

        return if (columns.isEmpty()) null else TableMetadata(
            name = tableName,
            columns = columns,
            primaryKeys = primaryKeys
        )
    }

    /**
     * Get JDBC type from database type name
     */
    private fun getJdbcType(dataType: String?, typeName: String?): Int {
        val type = (typeName ?: dataType ?: "").uppercase()
        return when {
            type.contains("INT") -> java.sql.Types.INTEGER
            type.contains("BIGINT") -> java.sql.Types.BIGINT
            type.contains("DECIMAL") || type.contains("NUMERIC") -> java.sql.Types.DECIMAL
            type.contains("FLOAT") -> java.sql.Types.FLOAT
            type.contains("DOUBLE") -> java.sql.Types.DOUBLE
            type.contains("BOOLEAN") || type.contains("BIT") -> java.sql.Types.BOOLEAN
            type.contains("CHAR") || type.contains("TEXT") || type.contains("VARCHAR") -> java.sql.Types.VARCHAR
            type.contains("DATE") -> java.sql.Types.DATE
            type.contains("TIME") -> java.sql.Types.TIME
            type.contains("TIMESTAMP") -> java.sql.Types.TIMESTAMP
            type.contains("BLOB") || type.contains("BYTEA") -> java.sql.Types.BLOB
            else -> java.sql.Types.VARCHAR
        }
    }

    /**
     * Convert column type to Java type
     */
    fun columnTypeToJavaType(columnMetadata: ColumnMetadata): String {
        return dialect.jdbcTypeToJavaType(columnMetadata.jdbcType, columnMetadata.type)
    }
}
