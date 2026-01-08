package com.xiaomi.imybatis.template.model

/**
 * Template information
 */
data class TemplateInfo(
    val name: String,
    val type: TemplateType,
    val content: String,
    val description: String = "",
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * Template type
 */
enum class TemplateType {
    ENTITY,
    MAPPER,
    XML,
    SERVICE,
    CONTROLLER
}
