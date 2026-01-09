package com.xiaomi.imybatis.database

import com.intellij.database.psi.DbTable
import com.intellij.database.util.DasUtil

class PsiMetadataProvider(
    private val tables: List<DbTable>
) : MetadataProvider {

    override fun getTables(): List<String> {
        return tables.map { it.name }
    }

    override fun getTableMetadata(tableName: String): TableMetadata? {
        val table = tables.find { it.name == tableName } ?: return null
        
        val columns = mutableListOf<ColumnMetadata>()
        val primaryKeys = mutableListOf<String>()
        
        DasUtil.getColumns(table).forEach { column ->
            val isPrimaryKey = DasUtil.isPrimary(column)
            val typeName = column.dataType.typeName
            
            val jdbcTypeInt = getJdbcType(typeName)
            
            columns.add(ColumnMetadata(
                name = column.name,
                type = typeName,
                jdbcType = jdbcTypeInt,
                nullable = !column.isNotNull,
                isPrimaryKey = isPrimaryKey,
                comment = column.comment
            ))
            
            if (isPrimaryKey) {
                primaryKeys.add(column.name)
            }
        }
        
        return TableMetadata(
            name = tableName,
            columns = columns,
            primaryKeys = primaryKeys
        )
    }
    
    private fun getJdbcType(typeName: String): Int {
        val type = typeName.uppercase()
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
}
