package com.xiaomi.imybatis.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

/**
 * Project-level service for imybatis plugin
 */
@Service(Service.Level.PROJECT)
class ProjectService(project: Project) {

    init {
        thisLogger().info("imybatis ProjectService initialized for project: ${project.name}")
    }

    companion object {
        fun getInstance(project: Project): ProjectService {
            return project.getService(ProjectService::class.java)
        }
    }
}
