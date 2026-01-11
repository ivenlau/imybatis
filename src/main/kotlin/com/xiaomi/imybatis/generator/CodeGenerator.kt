package com.xiaomi.imybatis.generator

import com.xiaomi.imybatis.database.MetadataProvider
import com.xiaomi.imybatis.database.TableMetadata
import com.xiaomi.imybatis.template.TemplateEngine
import java.sql.Connection

/**
 * Main code generator that coordinates all generators
 */
class CodeGenerator(
    private val templateEngine: TemplateEngine,
    private val metadataProvider: MetadataProvider
) {
    private val entityGenerator = EntityGenerator(templateEngine)
    private val mapperGenerator = MapperGenerator(templateEngine)
    private val xmlGenerator = XmlGenerator(templateEngine)
    private val serviceGenerator = ServiceGenerator(templateEngine)

    /**
     * Generate all code for a table
     */
    fun generateAll(
        tableName: String,
        basePackage: String,
        entityPackage: String,
        mapperPackage: String,
        xmlPackage: String,
        servicePackage: String,
        controllerPackage: String,
        className: String,
        generateService: Boolean = true,
        generateController: Boolean = false,
        useMyBatisPlus: Boolean = true,
        useLombok: Boolean = true,
        generateBatchOperations: Boolean = false,
        generateInsertOnDuplicateUpdate: Boolean = false,
        useLocalDateTime: Boolean = true
    ): GeneratedCode {
        val tableMetadata = metadataProvider.getTableMetadata(tableName)
            ?: throw IllegalArgumentException("Table not found: $tableName")

        val entityCode = entityGenerator.generate(
            tableMetadata = tableMetadata,
            packageName = entityPackage,
            className = className,
            useLombok = useLombok,
            useMyBatisPlus = useMyBatisPlus,
            useLocalDateTime = useLocalDateTime
        )

        val mapperCode = mapperGenerator.generate(
            packageName = mapperPackage,
            mapperName = "${className}Mapper",
            entityName = className,
            entityPackage = entityPackage,
            useMyBatisPlus = useMyBatisPlus,
            tableMetadata = tableMetadata,
            useLocalDateTime = useLocalDateTime,
            generateBatchOperations = generateBatchOperations,
            generateInsertOnDuplicateUpdate = generateInsertOnDuplicateUpdate
        )

        val xmlCode = xmlGenerator.generate(
            namespace = "$mapperPackage.${className}Mapper",
            tableName = tableName,
            entityName = className,
            entityPackage = entityPackage,
            tableMetadata = tableMetadata,
            useMyBatisPlus = useMyBatisPlus,
            generateBatchOperations = generateBatchOperations,
            generateInsertOnDuplicateUpdate = generateInsertOnDuplicateUpdate
        )

        val serviceCode = if (generateService) {
            serviceGenerator.generateServiceInterface(
                packageName = servicePackage,
                serviceName = "I${className}Service",
                entityName = className,
                entityPackage = entityPackage,
                useMyBatisPlus = useMyBatisPlus
            )
        } else null

        val serviceImplCode = if (generateService) {
            serviceGenerator.generateServiceImpl(
                packageName = "$servicePackage.impl",
                serviceImplName = "${className}ServiceImpl",
                serviceName = "I${className}Service",
                servicePackage = servicePackage,
                mapperName = "${className}Mapper",
                mapperPackage = mapperPackage,
                entityName = className,
                entityPackage = entityPackage,
                useMyBatisPlus = useMyBatisPlus
            )
        } else null

        val controllerCode = if (generateController) {
            serviceGenerator.generateController(
                packageName = controllerPackage,
                controllerName = "${className}Controller",
                serviceName = "I${className}Service",
                servicePackage = servicePackage,
                entityName = className,
                entityPackage = entityPackage,
                useMyBatisPlus = useMyBatisPlus
            )
        } else null

        return GeneratedCode(
            entity = entityCode,
            mapper = mapperCode,
            xml = xmlCode,
            service = serviceCode,
            serviceImpl = serviceImplCode,
            controller = controllerCode
        )
    }
}

/**
 * Generated code result
 */
data class GeneratedCode(
    val entity: String,
    val mapper: String,
    val xml: String,
    val service: String?,
    val serviceImpl: String?,
    val controller: String?
)
