package com.xiaomi.imybatis.database

import java.sql.Types

/**
 * PostgreSQL dialect implementation
 */
class PostgreSqlDialect : DatabaseDialect {
    override fun getName(): String = "PostgreSQL"

    override fun getJdbcUrlPattern(): String = "jdbc:postgresql://"

    override fun getDriverClassName(): String = "org.postgresql.Driver"

    override fun getSqlKeywords(): List<String> {
        return listOf(
            "SELECT", "FROM", "WHERE", "JOIN", "INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN",
            "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
            "ORDER BY", "GROUP BY", "HAVING", "LIMIT", "OFFSET",
            "AND", "OR", "NOT", "IN", "LIKE", "BETWEEN", "IS NULL", "IS NOT NULL",
            "COUNT", "SUM", "AVG", "MAX", "MIN", "DISTINCT",
            "AS", "ON", "USING", "UNION", "UNION ALL",
            "CASE", "WHEN", "THEN", "ELSE", "END",
            "COALESCE", "NULLIF", "CAST", "::"
        )
    }

    override fun getSqlFunctions(): List<String> {
        return listOf(
            "CONCAT", "SUBSTRING", "LENGTH", "UPPER", "LOWER", "TRIM",
            "TO_CHAR", "TO_DATE", "TO_TIMESTAMP", "NOW", "CURRENT_DATE", "CURRENT_TIME",
            "EXTRACT", "DATE_PART",
            "ARRAY_AGG", "STRING_AGG", "JSON_AGG"
        )
    }

    override fun jdbcTypeToJavaType(jdbcType: Int, typeName: String): String {
        return when (jdbcType) {
            Types.INTEGER -> "Integer"
            Types.BIGINT -> "Long"
            Types.DECIMAL, Types.NUMERIC -> "java.math.BigDecimal"
            Types.REAL -> "Float"
            Types.DOUBLE -> "Double"
            Types.BOOLEAN -> "Boolean"
            Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR -> "String"
            Types.DATE -> "java.time.LocalDate"
            Types.TIME -> "java.time.LocalTime"
            Types.TIMESTAMP -> "java.time.LocalDateTime"
            Types.BLOB -> "byte[]"
            Types.CLOB -> "String"
            Types.ARRAY -> "java.sql.Array"
            else -> {
                // Handle PostgreSQL-specific types
                when (typeName.uppercase()) {
                    "SMALLINT" -> "Integer"
                    "SERIAL", "BIGSERIAL" -> "Long"
                    "TEXT" -> "String"
                    "TIMESTAMP", "TIMESTAMPTZ" -> "java.time.LocalDateTime"
                    "DATE" -> "java.time.LocalDate"
                    "TIME", "TIMETZ" -> "java.time.LocalTime"
                    "UUID" -> "java.util.UUID"
                    "JSON", "JSONB" -> "String"
                    "BYTEA" -> "byte[]"
                    else -> "String"
                }
            }
        }
    }

    override fun getTableMetadataQuery(schema: String?): String {
        val schemaCondition = if (schema != null) "AND table_schema = '$schema'" else "AND table_schema = 'public'"
        return """
            SELECT table_name 
            FROM information_schema.tables 
            WHERE table_type = 'BASE TABLE' $schemaCondition
            ORDER BY table_name
        """.trimIndent()
    }

    override fun getColumnMetadataQuery(schema: String?, tableName: String): String {
        val schemaCondition = if (schema != null) "AND table_schema = '$schema'" else "AND table_schema = 'public'"
        return """
            SELECT
                column_name,
                data_type,
                udt_name,
                is_nullable,
                column_default,
                (SELECT COUNT(*) > 0
                 FROM information_schema.table_constraints tc
                 JOIN information_schema.key_column_usage kcu
                   ON tc.constraint_name = kcu.constraint_name
                 WHERE tc.table_name = '$tableName'
                   AND tc.constraint_type = 'PRIMARY KEY'
                   AND kcu.column_name = c.column_name) as is_primary_key
            FROM information_schema.columns c
            WHERE table_name = '$tableName' $schemaCondition
            ORDER BY ordinal_position
        """.trimIndent()
    }
}
