package com.xiaomi.imybatis.template

import freemarker.ext.beans.BeansWrapperBuilder
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.Version
import java.io.StringWriter
import java.util.*

/**
 * Template engine using FreeMarker
 */
class TemplateEngine {
    private val freemarkerVersion = Version("2.3.32")
    private val config: Configuration = Configuration(freemarkerVersion)

    init {
        config.setClassForTemplateLoading(this.javaClass, "/templates")
        config.defaultEncoding = "UTF-8"
        config.setLocale(Locale.getDefault())
        config.setNumberFormat("0.######")

        val wrapper = BeansWrapperBuilder(freemarkerVersion).apply {
            exposeFields = true
        }.build()
        config.objectWrapper = wrapper
        config.booleanFormat = "c"
    }

    /**
     * Process template with data model
     */
    fun process(templateName: String, dataModel: Map<String, Any>): String {
        val template: Template = config.getTemplate(templateName)
        val writer = StringWriter()
        template.process(dataModel, writer)
        return writer.toString()
    }

    /**
     * Process template string with data model
     */
    fun processString(templateString: String, dataModel: Map<String, Any>): String {
        val template = Template("inline", templateString, config)
        template.objectWrapper = config.objectWrapper

        val writer = StringWriter()
        template.process(dataModel, writer)
        return writer.toString()
    }
}
