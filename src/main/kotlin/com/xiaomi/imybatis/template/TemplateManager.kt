package com.xiaomi.imybatis.template

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.xiaomi.imybatis.template.model.TemplateInfo

/**
 * Template manager for code generation templates
 */
@State(name = "imybatis.TemplateManager", storages = [Storage("imybatis-templates.xml")])
@Service(Service.Level.PROJECT)
class TemplateManager(private val project: Project) {

    private val templates = mutableMapOf<String, TemplateInfo>()
    private val templateEngine = TemplateEngine()

    init {
        loadDefaultTemplates()
    }

    /**
     * Load default templates
     */
    private fun loadDefaultTemplates() {
        // Default templates will be loaded from resources
        // For now, we'll create them programmatically
    }

    /**
     * Get template by name
     */
    fun getTemplate(name: String): TemplateInfo? {
        return templates[name]
    }

    /**
     * Save template
     */
    fun saveTemplate(templateInfo: TemplateInfo) {
        templates[templateInfo.name] = templateInfo
    }

    /**
     * Delete template
     */
    fun deleteTemplate(name: String) {
        templates.remove(name)
    }

    /**
     * List all templates
     */
    fun listTemplates(): List<TemplateInfo> {
        return templates.values.toList()
    }

    /**
     * Process template with data
     */
    fun processTemplate(templateName: String, dataModel: Map<String, Any>): String {
        val template = templates[templateName] ?: throw IllegalArgumentException("Template not found: $templateName")
        return templateEngine.processString(template.content, dataModel)
    }

    companion object {
        fun getInstance(project: Project): TemplateManager {
            return project.getService(TemplateManager::class.java)
        }
    }
}
