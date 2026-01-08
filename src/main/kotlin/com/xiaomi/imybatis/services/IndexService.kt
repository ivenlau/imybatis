package com.xiaomi.imybatis.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

/**
 * Service for managing MyBatis indexes
 */
@Service(Service.Level.PROJECT)
class IndexService(project: Project) {

    init {
        thisLogger().info("imybatis IndexService initialized for project: ${project.name}")
    }

    /**
     * Initialize indexes for the project
     */
    fun initializeIndexes() {
        // Index initialization will be implemented in index module
        thisLogger().info("Initializing MyBatis indexes...")
    }

    companion object {
        fun getInstance(project: Project): IndexService {
            return project.getService(IndexService::class.java)
        }
    }
}
