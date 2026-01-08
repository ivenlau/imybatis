package com.xiaomi.imybatis.database

import java.sql.Types

/**
 * MySQL/MariaDB dialect implementation
 */
class MySqlDialect : DatabaseDialect {
    override fun getName(): String = "MySQL"

    override fun getJdbcUrlPattern(): String = "jdbc:mysql://"

    override fun getDriverClassName(): String = "com.mysql.cj.jdbc.Driver"

    override fun getSqlKeywords(): List<String> {
        return listOf(
            "SELECT", "FROM", "WHERE", "JOIN", "INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN",
            "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
            "ORDER BY", "GROUP BY", "HAVING", "LIMIT", "OFFSET",
            "AND", "OR", "NOT", "IN", "LIKE", "BETWEEN", "IS NULL", "IS NOT NULL",
            "COUNT", "SUM", "AVG", "MAX", "MIN", "DISTINCT",
            "AS", "ON", "USING", "UNION", "UNION ALL",
            "CASE", "WHEN", "THEN", "ELSE", "END",
            "IF", "IFNULL", "COALESCE", "NULLIF"
        )
    }

    override fun getSqlFunctions(): List<String> {
        return listOf(
            "CONCAT", "SUBSTRING", "LENGTH", "UPPER", "LOWER", "TRIM",
            "DATE_FORMAT", "NOW", "CURDATE", "CURTIME",
            "YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND"
        )
    }

    override fun jdbcTypeToJavaType(jdbcType: Int, typeName: String): String {
        return when (jdbcType) {
            Types.INTEGER -> "Integer"
            Types.BIGINT -> "Long"
            Types.DECIMAL, Types.NUMERIC -> "java.math.BigDecimal"
            Types.FLOAT, Types.REAL -> "Float"
            Types.DOUBLE -> "Double"
            Types.BOOLEAN, Types.BIT -> "Boolean"
            Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR -> "String"
            Types.DATE -> "java.time.LocalDate"
            Types.TIME -> "java.time.LocalTime"
            Types.TIMESTAMP -> "java.time.LocalDateTime"
            Types.BLOB -> "byte[]"
            Types.CLOB -> "String"
            else -> {
                // Handle MySQL-specific types
                when (typeName.uppercase()) {
                    "TINYINT" -> if (typeName.contains("(1)")) "Boolean" else "Integer"
                    "MEDIUMINT" -> "Integer"
                    "SMALLINT" -> "Integer"
                    "TEXT", "MEDIUMTEXT", "LONGTEXT" -> "String"
                    "DATETIME" -> "java.time.LocalDateTime"
                    "YEAR" -> "Integer"
                    else -> "String"
                }
            }
        }
    }

    override fun getTableMetadataQuery(schema: String?): String {
        return if (schema != null) {
            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '$schema'"
        } else {
            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE()"
        }
    }

    override fun getColumnMetadataQuery(schema: String?, tableName: String): String {
        val schemaCondition = if (schema != null) "AND TABLE_SCHEMA = '$schema'" else "AND TABLE_SCHEMA = DATABASE()"
        return """
            SELECT
                COLUMN_NAME,
                DATA_TYPE,
                COLUMN_TYPE,
                IS_NULLABLE,
                COLUMN_KEY,
                COLUMN_COMMENT,
                CASE WHEN COLUMN_KEY = 'PRI' THEN TRUE ELSE FALSE END AS IS_PRIMARY_KEY
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_NAME = '$tableName' $schemaCondition
            ORDER BY ORDINAL_POSITION
        """.trimIndent()
    }
}
