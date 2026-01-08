package com.xiaomi.imybatis.ui

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import java.io.IOException

/**
 * Helper class for writing generated code to files
 */
class FileWriter(private val project: Project) {

    /**
     * Write Java file
     */
    fun writeJavaFile(
        packageName: String,
        className: String,
        content: String,
        targetDirectory: VirtualFile
    ): VirtualFile? {
        return WriteCommandAction.writeCommandAction(project).compute<VirtualFile?, IOException> {
            try {
                // Create package directory structure
                val packageDirs = packageName.split(".")
                var currentDir = targetDirectory

                for (dirName in packageDirs) {
                    if (dirName.isBlank()) continue
                    var dir = currentDir.findChild(dirName)
                    if (dir == null || !dir.isDirectory) {
                        dir = currentDir.createChildDirectory(this, dirName)
                    }
                    currentDir = dir
                }

                // Create Java file
                val fileName = "$className.java"
                var javaFile = currentDir.findChild(fileName)

                if (javaFile == null) {
                    javaFile = currentDir.createChildData(this, fileName)
                }

                // Write content
                VfsUtil.saveText(javaFile, content)

                // Format code
                val psiFile = PsiManager.getInstance(project).findFile(javaFile)
                if (psiFile is PsiJavaFile) {
                    CodeStyleManager.getInstance(project).reformat(psiFile)
                }

                javaFile
            } catch (e: Exception) {
                throw IOException("Failed to write Java file: ${e.message}", e)
            }
        }
    }

    /**
     * Write XML file
     */
    fun writeXmlFile(
        fileName: String,
        content: String,
        targetDirectory: VirtualFile
    ): VirtualFile? {
        return WriteCommandAction.writeCommandAction(project).compute<VirtualFile?, IOException> {
            try {
                var xmlFile = targetDirectory.findChild(fileName)

                if (xmlFile == null) {
                    xmlFile = targetDirectory.createChildData(this, fileName)
                }

                VfsUtil.saveText(xmlFile, content)
                xmlFile
            } catch (e: Exception) {
                throw IOException("Failed to write XML file: ${e.message}", e)
            }
        }
    }

    /**
     * Find or create source root directory
     */
    fun findOrCreateSourceRoot(module: Module? = null): VirtualFile? {
        return WriteCommandAction.writeCommandAction(project).compute<VirtualFile?, IOException> {
            if (module != null) {
                val sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots(JavaSourceRootType.SOURCE)
                if (sourceRoots.isNotEmpty()) {
                    return@compute sourceRoots[0]
                }
                // If no source root found, try to find src/main/java
                val contentRoots = ModuleRootManager.getInstance(module).contentRoots
                for (root in contentRoots) {
                    val src = root.findChild("src")?.findChild("main")?.findChild("java")
                    if (src != null) return@compute src
                }
            }

            // Fallback to project base dir logic
            val baseDir = project.baseDir ?: return@compute null

            // Try to find existing source directory
            var srcDir = baseDir.findChild("src")
            if (srcDir == null || !srcDir.isDirectory) {
                srcDir = baseDir.createChildDirectory(this, "src")
            }

            var mainDir = srcDir.findChild("main")
            if (mainDir == null || !mainDir.isDirectory) {
                mainDir = srcDir.createChildDirectory(this, "main")
            }

            var javaDir = mainDir.findChild("java")
            if (javaDir == null || !javaDir.isDirectory) {
                javaDir = mainDir.createChildDirectory(this, "java")
            }

            javaDir
        }
    }

    /**
     * Find or create resources directory
     */
    fun findOrCreateResourcesRoot(module: Module? = null): VirtualFile? {
        return WriteCommandAction.writeCommandAction(project).compute<VirtualFile?, IOException> {
            if (module != null) {
                val resourceRoots = ModuleRootManager.getInstance(module).getSourceRoots(JavaResourceRootType.RESOURCE)
                if (resourceRoots.isNotEmpty()) {
                    return@compute resourceRoots[0]
                }
                // If no resource root found, try to find src/main/resources
                val contentRoots = ModuleRootManager.getInstance(module).contentRoots
                for (root in contentRoots) {
                    val res = root.findChild("src")?.findChild("main")?.findChild("resources")
                    if (res != null) return@compute res
                }
            }

            // Fallback to project base dir logic
            val baseDir = project.baseDir ?: return@compute null

            // Try to find existing resources directory
            var srcDir = baseDir.findChild("src")
            if (srcDir == null || !srcDir.isDirectory) {
                srcDir = baseDir.createChildDirectory(this, "src")
            }

            var mainDir = srcDir.findChild("main")
            if (mainDir == null || !mainDir.isDirectory) {
                mainDir = srcDir.createChildDirectory(this, "main")
            }

            var resourcesDir = mainDir.findChild("resources")
            if (resourcesDir == null || !resourcesDir.isDirectory) {
                resourcesDir = mainDir.createChildDirectory(this, "resources")
            }

            resourcesDir
        }
    }
}
