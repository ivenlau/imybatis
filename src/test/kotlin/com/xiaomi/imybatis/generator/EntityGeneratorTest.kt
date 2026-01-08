package com.xiaomi.imybatis.generator

import com.xiaomi.imybatis.database.ColumnMetadata
import com.xiaomi.imybatis.database.TableMetadata
import com.xiaomi.imybatis.template.TemplateEngine
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test for Entity generator
 */
class EntityGeneratorTest {

    @Test
    fun testEntityGeneration() {
        val templateEngine = TemplateEngine()
        val generator = EntityGenerator(templateEngine)

        val columns = listOf(
            ColumnMetadata("id", "BIGINT", java.sql.Types.BIGINT, false, true, "主键"),
            ColumnMetadata("name", "VARCHAR", java.sql.Types.VARCHAR, true, false, "名称"),
            ColumnMetadata("created_at", "TIMESTAMP", java.sql.Types.TIMESTAMP, true, false, "创建时间")
        )

        val tableMetadata = TableMetadata(
            name = "user",
            columns = columns,
            primaryKeys = listOf("id")
        )

        val code = generator.generate(
            tableMetadata = tableMetadata,
            packageName = "com.example",
            className = "User",
            useLombok = true
        )

        assertNotNull(code)
        assertTrue(code.contains("package com.example"))
        assertTrue(code.contains("class User"))
        assertTrue(code.contains("@TableName"))
    }
}
