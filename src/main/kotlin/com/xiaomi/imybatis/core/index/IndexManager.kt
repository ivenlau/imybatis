package com.xiaomi.imybatis.core.index

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.util.indexing.FileBasedIndex
import com.xiaomi.imybatis.core.index.model.*

/**
 * Manager for MyBatis indexes
 * Provides high-level API for querying indexed data
 */
class IndexManager(private val project: Project) {

    /**
     * Find Mapper interface by qualified name
     */
    fun findMapper(qualifiedName: String): MapperInfo? {
        val keys = FileBasedIndex.getInstance().getAllKeys(MapperIndex.NAME, project)
        if (!keys.contains(qualifiedName)) return null

        val files = FileBasedIndex.getInstance()
            .getContainingFiles(MapperIndex.NAME, qualifiedName, GlobalSearchScope.projectScope(project))

        for (file in files) {
            val psiFile = PsiManager.getInstance(project).findFile(file) as? PsiJavaFile ?: continue
            val classes = PsiTreeUtil.getChildrenOfType(psiFile, PsiClass::class.java) ?: continue
            
            for (psiClass in classes) {
                if (psiClass.qualifiedName == qualifiedName) {
                    return extractMapperInfo(psiClass)
                }
            }
        }

        return null
    }

    /**
     * Find Mapper XML by namespace
     */
    fun findXmlMapper(namespace: String): XmlMapperInfo? {
        val keys = FileBasedIndex.getInstance().getAllKeys(XmlIndex.NAME, project)
        if (!keys.contains(namespace)) return null

        val files = FileBasedIndex.getInstance()
            .getContainingFiles(XmlIndex.NAME, namespace, GlobalSearchScope.projectScope(project))

        for (file in files) {
            val psiFile = PsiManager.getInstance(project).findFile(file) as? XmlFile ?: continue
            return extractXmlMapperInfo(psiFile)
        }

        return null
    }

    /**
     * Find Entity class by qualified name
     */
    fun findEntity(qualifiedName: String): EntityInfo? {
        val keys = FileBasedIndex.getInstance().getAllKeys(EntityIndex.NAME, project)
        if (!keys.contains(qualifiedName)) return null

        val files = FileBasedIndex.getInstance()
            .getContainingFiles(EntityIndex.NAME, qualifiedName, GlobalSearchScope.projectScope(project))

        for (file in files) {
            val psiFile = PsiManager.getInstance(project).findFile(file) as? PsiJavaFile ?: continue
            val classes = PsiTreeUtil.getChildrenOfType(psiFile, PsiClass::class.java) ?: continue
            
            for (psiClass in classes) {
                if (psiClass.qualifiedName == qualifiedName) {
                    return extractEntityInfo(psiClass)
                }
            }
        }

        return null
    }

    /**
     * Find all Mapper interfaces in the project
     */
    fun findAllMappers(): List<MapperInfo> {
        val result = mutableListOf<MapperInfo>()
        val keys = FileBasedIndex.getInstance().getAllKeys(MapperIndex.NAME, project)

        for (key in keys) {
            val mapperInfo = findMapper(key) ?: continue
            result.add(mapperInfo)
        }

        return result
    }

    /**
     * Find all Mapper XML files in the project
     */
    fun findAllXmlMappers(): List<XmlMapperInfo> {
        val result = mutableListOf<XmlMapperInfo>()
        val keys = FileBasedIndex.getInstance().getAllKeys(XmlIndex.NAME, project)

        for (key in keys) {
            val xmlMapperInfo = findXmlMapper(key) ?: continue
            result.add(xmlMapperInfo)
        }

        return result
    }

    /**
     * Find all Entity classes in the project
     */
    fun findAllEntities(): List<EntityInfo> {
        val result = mutableListOf<EntityInfo>()
        val keys = FileBasedIndex.getInstance().getAllKeys(EntityIndex.NAME, project)

        for (key in keys) {
            val entityInfo = findEntity(key) ?: continue
            result.add(entityInfo)
        }

        return result
    }

    /**
     * Find XML statement by mapper namespace and statement id
     */
    fun findXmlStatement(namespace: String, statementId: String): XmlStatementInfo? {
        val xmlMapper = findXmlMapper(namespace) ?: return null
        return xmlMapper.statements.find { it.id == statementId }
    }

    /**
     * Find Mapper method by namespace and method name
     */
    fun findMapperMethod(namespace: String, methodName: String): MapperMethodInfo? {
        val mapperInfo = findMapper(namespace) ?: return null
        return mapperInfo.methods.find { it.name == methodName }
    }

    companion object {
        fun getInstance(project: Project): IndexManager {
            return project.getService(IndexManager::class.java) ?: IndexManager(project)
        }
    }
}
