package com.xiaomi.imybatis.core.inspection

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.xiaomi.imybatis.core.index.IndexManager
import com.xiaomi.imybatis.ImybatisBundle

/**
 * Inspection for MyBatis XML files
 * Checks for ID consistency, type matching, and other issues
 */
class MyBatisInspection : LocalInspectionTool() {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean
    ): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitFile(file: PsiFile) {
                if (file !is XmlFile) return

                val rootTag = file.rootTag ?: return
                if (rootTag.name != "mapper") return

                val namespace = rootTag.getAttributeValue("namespace") ?: return
                val project = file.project
                val indexManager = IndexManager.getInstance(project)

                // Check statement IDs match Mapper methods
                checkStatementIds(rootTag, namespace, indexManager, holder)

                // Check resultMap references
                checkResultMapReferences(rootTag, namespace, indexManager, holder)

                // Check type matching
                checkTypeMatching(rootTag, namespace, indexManager, holder)
            }
        }
    }

    /**
     * Check if statement IDs match Mapper interface methods
     */
    private fun checkStatementIds(
        rootTag: XmlTag,
        namespace: String,
        indexManager: IndexManager,
        holder: ProblemsHolder
    ) {
        val mapperInfo = indexManager.findMapper(namespace) ?: return
        val methodNames = mapperInfo.methods.map { it.name }.toSet()

        val statementTypes = listOf("select", "insert", "update", "delete")
        for (statementType in statementTypes) {
            val statements = rootTag.findSubTags(statementType)
            for (statement in statements) {
                val id = statement.getAttributeValue("id") ?: continue
                if (id !in methodNames) {
                    holder.registerProblem(
                        statement.getAttribute("id") ?: statement,
                        ImybatisBundle.message("error.statement.id.not.found", id),
                        ProblemHighlightType.WARNING
                    )
                }
            }
        }
    }

    /**
     * Check if resultMap references exist
     */
    private fun checkResultMapReferences(
        rootTag: XmlTag,
        namespace: String,
        indexManager: IndexManager,
        holder: ProblemsHolder
    ) {
        val xmlMapper = indexManager.findXmlMapper(namespace) ?: return
        val resultMapIds = xmlMapper.resultMaps.toSet()

        val statementTypes = listOf("select", "insert", "update", "delete")
        for (statementType in statementTypes) {
            val statements = rootTag.findSubTags(statementType)
            for (statement in statements) {
                val resultMap = statement.getAttributeValue("resultMap") ?: continue
                if (resultMap !in resultMapIds) {
                    holder.registerProblem(
                        statement.getAttribute("resultMap") ?: statement,
                        ImybatisBundle.message("error.resultmap.not.found", resultMap),
                        ProblemHighlightType.ERROR
                    )
                }
            }
        }
    }

    /**
     * Check type matching between Mapper methods and XML statements
     */
    private fun checkTypeMatching(
        rootTag: XmlTag,
        namespace: String,
        indexManager: IndexManager,
        holder: ProblemsHolder
    ) {
        val mapperInfo = indexManager.findMapper(namespace) ?: return
        val xmlMapper = indexManager.findXmlMapper(namespace) ?: return

        for (statement in xmlMapper.statements) {
            val method = mapperInfo.methods.find { it.name == statement.id } ?: continue

            // Check parameterType
            if (statement.parameterType != null && method.parameters.isNotEmpty()) {
                val expectedType = method.parameters[0].first
                if (!typesMatch(statement.parameterType, expectedType)) {
                    holder.registerProblem(
                        statement.xmlTag.getAttribute("parameterType") ?: statement.xmlTag,
                        ImybatisBundle.message("error.parameter.type.mismatch", statement.parameterType, expectedType),
                        ProblemHighlightType.WARNING
                    )
                }
            }

            // Check resultType/resultMap
            if (statement.resultType != null) {
                if (!typesMatch(statement.resultType, method.returnType)) {
                    holder.registerProblem(
                        statement.xmlTag.getAttribute("resultType") ?: statement.xmlTag,
                        ImybatisBundle.message("error.result.type.mismatch", statement.resultType, method.returnType),
                        ProblemHighlightType.WARNING
                    )
                }
            }
        }
    }

    /**
     * Check if two types match (simple comparison)
     */
    private fun typesMatch(type1: String, type2: String): Boolean {
        // Normalize types
        val normalized1 = normalizeType(type1)
        val normalized2 = normalizeType(type2)

        return normalized1 == normalized2 || normalized1.endsWith(normalized2) || normalized2.endsWith(normalized1)
    }

    /**
     * Normalize type name for comparison
     */
    private fun normalizeType(type: String): String {
        return type.replace("java.lang.", "").trim()
    }
}
