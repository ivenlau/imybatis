package com.xiaomi.imybatis.ui

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.refactoring.ui.PackageNameReferenceEditorCombo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.xiaomi.imybatis.database.*
import com.xiaomi.imybatis.generator.CodeGenerator
import com.xiaomi.imybatis.generator.GeneratedCode
import com.xiaomi.imybatis.template.TemplateEngine
import org.jetbrains.jps.model.java.JavaSourceRootType
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ItemEvent
import java.sql.Connection
import java.sql.DriverManager
import java.io.IOException
import javax.swing.*
import javax.swing.table.DefaultTableModel

/**
 * Code generation wizard with multiple steps
 */
class CodeGenerationWizard(
    private val project: Project
) : DialogWrapper(project) {

    private var currentStep = 0
    private val steps = listOf("数据源", "选择表", "配置", "预览", "生成")

    // Step 1: Data source
    private val dataSourceUrlField = JTextField()
    private val usernameField = JTextField()
    private val passwordField = JPasswordField()
    private val databaseField = JTextField()
    private val dialectComboBox = JComboBox<String>(arrayOf("MySQL", "PostgreSQL"))

    // Step 2: Table selection
    private val tableListModel = DefaultTableModel(arrayOf("表名"), 0)
    private val tableList = JBTable(tableListModel)

    // Step 3: Configuration
    private val moduleComboBox = ComboBox<Module>()
    private val basePackageField = PackageNameReferenceEditorCombo("com.example", project, "RecentPackage", "基础包名")
    private val entityPackageField = JTextField("entity")
    private val mapperPackageField = JTextField("mapper")
    private val xmlPackageField = JTextField("mapper")
    private val servicePackageField = JTextField("service")
    private val controllerPackageField = JTextField("controller")
    private val generateServiceCheckBox = JCheckBox("生成 Service", true)
    private val generateControllerCheckBox = JCheckBox("生成 Controller", false)
    private val useLombokCheckBox = JCheckBox("使用 Lombok", true)
    private val useMyBatisPlusCheckBox = JCheckBox("使用 MyBatis Plus", true)
    private val generateBatchOperationsCheckBox = JCheckBox("批量操作", false)
    private val generateInsertOnDuplicateUpdateCheckBox = JCheckBox("插入时更新", false)
    private val useLocalDateTimeCheckBox = JCheckBox("使用 LocalDateTime", true)

    // Step 4: Preview
    private val previewTextArea = JTextArea()
    
    // State
    private var connection: Connection? = null
    private var metadataProvider: MetadataProvider? = null
    private var selectedTables = mutableListOf<String>()
    private var generatedCodes = mutableMapOf<String, GeneratedCode>()

    private val templateEngine = TemplateEngine()
    private val loadedDrivers = mutableSetOf<String>()
    private val mainPanel = JBPanel<JBPanel<*>>(BorderLayout())
    private val stepContentPanel = createStepContent()
    init {
        title = "imybatis 代码生成向导"

        // Initialize UI before DialogWrapper setup to avoid self-addition
        setupUI()
        init()
    }

    fun setInitialState(provider: MetadataProvider, tables: List<String> = emptyList()) {
        this.metadataProvider = provider

        // Populate table list
        try {
            val allTables = provider.getTables()
            tableListModel.rowCount = 0
            allTables.forEach { table ->
                tableListModel.addRow(arrayOf(table))
            }

            if (tables.isNotEmpty()) {
                this.selectedTables.clear()
                this.selectedTables.addAll(tables)

                // Select rows in tableList
                val selectionModel = tableList.selectionModel
                selectionModel.clearSelection()
                tables.forEach { table ->
                    val index = (0 until tableListModel.rowCount).find {
                        tableListModel.getValueAt(it, 0) == table
                    }
                    if (index != null) {
                        selectionModel.addSelectionInterval(index, index)
                    }
                }

                // Skip to configuration step
                showStep(2)
            } else {
                // Skip to table selection step
                showStep(1)
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "无法获取表信息: ${e.message}",
                "错误",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun setupUI() {
        mainPanel.removeAll()
        mainPanel.layout = BorderLayout()
        mainPanel.add(createStepIndicator(), BorderLayout.NORTH)
        mainPanel.add(stepContentPanel, BorderLayout.CENTER)
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH)

        showStep(0)
    }

    private fun createStepIndicator(): JPanel {
        val panel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 10, 10))
        steps.forEachIndexed { index, stepName ->
            val label = JBLabel("${index + 1}. $stepName")
            if (index == currentStep) {
                label.font = label.font.deriveFont(java.awt.Font.BOLD)
            }
            panel.add(label)
            if (index < steps.size - 1) {
                panel.add(JBLabel("→"))
            }
        }
        return panel
    }
    
    private fun createStepContent(): JPanel {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.border = JBUI.Borders.empty(10)
        return panel
    }

    private fun ensureDriverLoaded(driverClassName: String) {
        if (loadedDrivers.contains(driverClassName)) {
            return
        }

        try {
            Class.forName(driverClassName)
            loadedDrivers.add(driverClassName)
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException("找不到数据库驱动: $driverClassName", e)
        }
    }

    private fun createButtonPanel(): JPanel {
        val panel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.RIGHT, 10, 10))
        
        val prevButton = JButton("上一步")
        prevButton.addActionListener { previousStep() }
        
        val nextButton = JButton("下一步")
        nextButton.addActionListener { nextStep() }
        
        panel.add(prevButton)
        panel.add(nextButton)
        
        return panel
    }
    
    private fun showStep(step: Int) {
        currentStep = step
        stepContentPanel.removeAll()

        when (step) {
            0 -> stepContentPanel.add(createDataSourceStep(), BorderLayout.CENTER)
            1 -> stepContentPanel.add(createTableSelectionStep(), BorderLayout.CENTER)
            2 -> stepContentPanel.add(createConfigurationStep(), BorderLayout.CENTER)
            3 -> stepContentPanel.add(createPreviewStep(), BorderLayout.CENTER)
            4 -> {
                // Generation step - this will be handled in nextStep()
                stepContentPanel.add(createGenerationStep(), BorderLayout.CENTER)
            }
        }

        stepContentPanel.revalidate()
        stepContentPanel.repaint()
        mainPanel.repaint()

        // Update button states
        updateButtonStates()
    }
    private fun createDataSourceStep(): JPanel {
        val panel = JBPanel<JBPanel<*>>(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = JBUI.insets(5)
            anchor = GridBagConstraints.WEST
        }
        
        gbc.gridx = 0
        gbc.gridy = 0
        panel.add(JBLabel("数据库 URL:"), gbc)
        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        dataSourceUrlField.preferredSize = java.awt.Dimension(300, 25)
        panel.add(dataSourceUrlField, gbc)
        
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(JBLabel("用户名:"), gbc)
        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        usernameField.preferredSize = java.awt.Dimension(300, 25)
        panel.add(usernameField, gbc)
        
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(JBLabel("密码:"), gbc)
        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        passwordField.preferredSize = java.awt.Dimension(300, 25)
        panel.add(passwordField, gbc)
        
        gbc.gridx = 0
        gbc.gridy = 3
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(JBLabel("数据库名:"), gbc)
        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        databaseField.preferredSize = java.awt.Dimension(300, 25)
        panel.add(databaseField, gbc)
        
        gbc.gridx = 0
        gbc.gridy = 4
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(JBLabel("数据库类型:"), gbc)
        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        panel.add(dialectComboBox, gbc)
        
        return panel
    }
    
    private fun createTableSelectionStep(): JPanel {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        
        val label = JBLabel("选择要生成代码的表:")
        panel.add(label, BorderLayout.NORTH)
        
        tableList.selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        val scrollPane = JBScrollPane(tableList)
        panel.add(scrollPane, BorderLayout.CENTER)
        
        // Load tables if connection is available
        loadTables()
        
        return panel
    }
    
    private fun createConfigurationStep(): JPanel {
        val panel = JBPanel<JBPanel<*>>(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = JBUI.insets(5)
            anchor = GridBagConstraints.WEST
        }

        var row = 0

        // Module selection
        gbc.gridx = 0
        gbc.gridy = row++
        panel.add(JBLabel("模块:"), gbc)
        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0

        // Populate modules
        val modules = ModuleManager.getInstance(project).modules
        moduleComboBox.model = DefaultComboBoxModel(modules)
        moduleComboBox.renderer = DefaultListCellRenderer() // Uses toString() which usually returns module name

        // Add listener for module selection
        moduleComboBox.addItemListener { e ->
            if (e.stateChange == ItemEvent.SELECTED) {
                val module = e.item as Module
                val inferredPackage = inferBasePackage(module)
                if (inferredPackage.isNotBlank()) {
                    basePackageField.text = inferredPackage
                }
            }
        }

        // Select first module by default if available
        if (modules.isNotEmpty()) {
            moduleComboBox.selectedIndex = 0
            // Trigger listener manually for initial selection
            val inferredPackage = inferBasePackage(modules[0])
            if (inferredPackage.isNotBlank()) {
                basePackageField.text = inferredPackage
            }
        }

        panel.add(moduleComboBox, gbc)

        gbc.gridx = 0
        gbc.gridy = row++
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(JBLabel("基础包名:"), gbc)
        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        // basePackageField is now PackageNameReferenceEditorCombo which handles its own size
        panel.add(basePackageField, gbc)

        gbc.gridx = 0
        gbc.gridy = row++
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(JBLabel("Entity 包名:"), gbc)
        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        entityPackageField.preferredSize = java.awt.Dimension(300, 25)
        panel.add(entityPackageField, gbc)
        
        gbc.gridx = 0
        gbc.gridy = row++
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(JBLabel("Mapper 包名:"), gbc)
        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        mapperPackageField.preferredSize = java.awt.Dimension(300, 25)
        panel.add(mapperPackageField, gbc)
        
        gbc.gridx = 0
        gbc.gridy = row++
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(JBLabel("XML 包名:"), gbc)
        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        xmlPackageField.preferredSize = java.awt.Dimension(300, 25)
        panel.add(xmlPackageField, gbc)
        
        gbc.gridx = 0
        gbc.gridy = row++
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(JBLabel("Service 包名:"), gbc)
        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        servicePackageField.preferredSize = java.awt.Dimension(300, 25)
        panel.add(servicePackageField, gbc)
        
        gbc.gridx = 0
        gbc.gridy = row++
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(JBLabel("Controller 包名:"), gbc)
        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        controllerPackageField.preferredSize = java.awt.Dimension(300, 25)
        panel.add(controllerPackageField, gbc)
        
        gbc.gridx = 0
        gbc.gridy = row++
        gbc.gridwidth = 1
        gbc.weightx = 0.5
        panel.add(generateServiceCheckBox, gbc)

        gbc.gridx = 1
        panel.add(generateControllerCheckBox, gbc)

        gbc.gridx = 0
        gbc.gridy = row++
        panel.add(useLombokCheckBox, gbc)

        gbc.gridx = 1
        panel.add(useMyBatisPlusCheckBox, gbc)

        gbc.gridx = 0
        gbc.gridy = row++
        panel.add(generateBatchOperationsCheckBox, gbc)

        gbc.gridx = 1
        panel.add(generateInsertOnDuplicateUpdateCheckBox, gbc)

        gbc.gridx = 0
        gbc.gridy = row++
        gbc.gridwidth = 2
        panel.add(useLocalDateTimeCheckBox, gbc)

        return panel
    }

    private fun inferBasePackage(module: Module): String {
        val sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots(JavaSourceRootType.SOURCE)
        if (sourceRoots.isEmpty()) return ""

        var currentDir = sourceRoots[0]
        val packageParts = mutableListOf<String>()

        // Simple heuristic: traverse down until branching or file encountered
        while (true) {
            val children = currentDir.children.filter { it.isDirectory }
            if (children.size == 1) {
                currentDir = children[0]
                packageParts.add(currentDir.name)
            } else {
                // If we have multiple directories, check if we already have a reasonable package structure
                // e.g. com.example
                if (packageParts.isNotEmpty()) {
                    break
                }
                // If we are at root and have multiple dirs (e.g. com, org), we can't infer easily.
                // But usually it starts with com/org, so we might want to pick one?
                // For now, let's stop.
                break
            }

            // Check if current dir has files (excluding hidden ones)
            val hasFiles = currentDir.children.any { !it.isDirectory && !it.name.startsWith(".") }
            if (hasFiles) {
                break
            }
        }

        return packageParts.joinToString(".")
    }

    private fun createPreviewStep(): JPanel {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        
        val label = JBLabel("代码预览:")
        panel.add(label, BorderLayout.NORTH)
        
        previewTextArea.isEditable = false
        previewTextArea.font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12)
        val scrollPane = JBScrollPane(previewTextArea)
        panel.add(scrollPane, BorderLayout.CENTER)
        
        // Generate preview
        generatePreview()
        
        return panel
    }
    
    private fun createGenerationStep(): JPanel {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.add(JBLabel("代码生成完成！"), BorderLayout.CENTER)
        return panel
    }
    
    private fun loadTables() {
        try {
            val dialect = when (dialectComboBox.selectedItem as String) {
                "MySQL" -> MySqlDialect()
                "PostgreSQL" -> PostgreSqlDialect()
                else -> MySqlDialect()
            }

            ensureDriverLoaded(dialect.getDriverClassName())

            val url = dataSourceUrlField.text
            val username = usernameField.text
            val password = String(passwordField.password)
            val database = databaseField.text
            
            if (url.isBlank() || username.isBlank() || database.isBlank()) {
                return
            }
            
            val fullUrl = when (dialect) {
                is MySqlDialect -> "$url$database?useSSL=false&serverTimezone=UTC"
                is PostgreSqlDialect -> "$url$database"
                else -> url
            }
            
            connection = DriverManager.getConnection(fullUrl, username, password)
            metadataProvider = JdbcMetadataProvider(connection!!, dialect)

            val tables = metadataProvider!!.getTables()

            tableListModel.rowCount = 0
            tables.forEach { table ->
                tableListModel.addRow(arrayOf(table))
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "连接数据库失败: ${e.message}",
                "错误",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
    
    private fun generatePreview() {
        if (selectedTables.isEmpty() || metadataProvider == null) {
            previewTextArea.text = "请先完成前面的步骤"
            return
        }

        val preview = StringBuilder()

        // Generate file tree preview
        preview.append("=== 生成文件预览 ===\n\n")
        val filePaths = mutableListOf<String>()

        selectedTables.forEach { tableName ->
            val className = tableNameToClassName(tableName)

            // Entity
            val entityPackage = "${basePackageField.text}.${entityPackageField.text}"
            filePaths.add("src/main/java/${entityPackage.replace('.', '/')}/$className.java")

            // Mapper
            val mapperPackage = "${basePackageField.text}.${mapperPackageField.text}"
            filePaths.add("src/main/java/${mapperPackage.replace('.', '/')}/${className}Mapper.java")

            // XML
            val xmlPackage = "${basePackageField.text}.${xmlPackageField.text}"
            filePaths.add("src/main/resources/${xmlPackage.replace('.', '/')}/${className}Mapper.xml")

            // Service
            if (generateServiceCheckBox.isSelected) {
                val servicePackage = "${basePackageField.text}.${servicePackageField.text}"
                filePaths.add("src/main/java/${servicePackage.replace('.', '/')}/I${className}Service.java")
                filePaths.add("src/main/java/${servicePackage.replace('.', '/')}/impl/${className}ServiceImpl.java")
            }

            // Controller
            if (generateControllerCheckBox.isSelected) {
                val controllerPackage = "${basePackageField.text}.${controllerPackageField.text}"
                filePaths.add("src/main/java/${controllerPackage.replace('.', '/')}/${className}Controller.java")
            }
        }

        val selectedModule = moduleComboBox.selectedItem as? Module
        val rootName = selectedModule?.name ?: project.name
        preview.append(generateTreeStructure(filePaths, rootName))
        preview.append("\n\n")

        val codeGenerator = CodeGenerator(templateEngine, metadataProvider!!)

        selectedTables.forEach { tableName ->
            preview.append("=== $tableName ===\n\n")

            try {
                val tableMetadata = metadataProvider!!.getTableMetadata(tableName)
                if (tableMetadata != null) {
                    val className = tableNameToClassName(tableName)
                    val generatedCode = codeGenerator.generateAll(
                        tableName = tableName,
                        basePackage = basePackageField.text,
                        entityPackage = "${basePackageField.text}.${entityPackageField.text}",
                        mapperPackage = "${basePackageField.text}.${mapperPackageField.text}",
                        xmlPackage = "${basePackageField.text}.${xmlPackageField.text}",
                        servicePackage = "${basePackageField.text}.${servicePackageField.text}",
                        controllerPackage = "${basePackageField.text}.${controllerPackageField.text}",
                        className = className,
                        generateService = generateServiceCheckBox.isSelected,
                        generateController = generateControllerCheckBox.isSelected,
                        useMyBatisPlus = useMyBatisPlusCheckBox.isSelected,
                        useLombok = useLombokCheckBox.isSelected,
                        generateBatchOperations = generateBatchOperationsCheckBox.isSelected,
                        generateInsertOnDuplicateUpdate = generateInsertOnDuplicateUpdateCheckBox.isSelected,
                        useLocalDateTime = useLocalDateTimeCheckBox.isSelected
                    )

                    generatedCodes[tableName] = generatedCode

                    preview.append("Entity:\n${generatedCode.entity}\n\n")
                    preview.append("Mapper:\n${generatedCode.mapper}\n\n")
                    preview.append("XML:\n${generatedCode.xml}\n\n")
                }
            } catch (e: Exception) {
                preview.append("生成失败: ${e.message}\n\n")
                preview.append("堆栈跟踪:\n${e.stackTraceToString()}\n\n")
            }
        }

        previewTextArea.text = preview.toString()
        previewTextArea.caretPosition = 0
    }

    private fun generateTreeStructure(paths: List<String>, rootName: String): String {
        val root = Node(rootName)

        for (path in paths) {
            var current = root
            val parts = path.split("/")
            for (part in parts) {
                var child = current.children.find { it.name == part }
                if (child == null) {
                    child = Node(part)
                    current.children.add(child)
                }
                current = child
            }
        }

        // Compact tree (fold empty packages)
        // We apply compaction to children of root to keep root name distinct
        root.children.forEach { compactTree(it) }

        val sb = StringBuilder()
        printTree(root, "", sb)
        return sb.toString()
    }

    private fun compactTree(node: Node) {
        // Recursively compact children first
        node.children.forEach { compactTree(it) }

        // Compact current node if it has exactly one child and that child is a directory (has children)
        while (node.children.size == 1 && node.children[0].children.isNotEmpty()) {
            val child = node.children[0]
            node.name = "${node.name}.${child.name}"
            node.children.clear()
            node.children.addAll(child.children)
        }
    }

    private fun printTree(node: Node, prefix: String, sb: StringBuilder) {
        // Print root node first (if it's the top level call)
        if (prefix.isEmpty()) {
            sb.append(node.name).append("\n")
        }

        val sortedChildren = node.children.sortedWith(compareBy({ it.children.isEmpty() }, { it.name }))

        for (i in sortedChildren.indices) {
            val child = sortedChildren[i]
            val isLast = i == sortedChildren.size - 1

            sb.append(prefix)
            sb.append(if (isLast) "└── " else "├── ")
            sb.append(child.name)
            sb.append("\n")

            val newPrefix = prefix + (if (isLast) "    " else "│   ")
            printTree(child, newPrefix, sb)
        }
    }

    private class Node(var name: String) {
        val children = mutableListOf<Node>()
    }
    
    private fun tableNameToClassName(tableName: String): String {
        val parts = tableName.split("_")
        return parts.joinToString("") { it.replaceFirstChar { char -> char.uppercaseChar() } }
    }
    
    private fun previousStep() {
        if (currentStep > 0) {
            showStep(currentStep - 1)
        }
    }
    
    private fun nextStep() {
        when (currentStep) {
            0 -> {
                // Validate data source
                if (validateDataSource()) {
                    showStep(1)
                }
            }
            1 -> {
                // Get selected tables
                val selectedRows = tableList.selectedRows
                selectedTables.clear()
                selectedRows.forEach { row ->
                    val tableName = tableListModel.getValueAt(row, 0) as String
                    selectedTables.add(tableName)
                }
                
                if (selectedTables.isEmpty()) {
                    JOptionPane.showMessageDialog(
                        contentPanel,
                        "请至少选择一个表",
                        "提示",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                } else {
                    showStep(2)
                }
            }
            2 -> {
                // Validate configuration
                if (validateConfiguration()) {
                    showStep(3)
                }
            }
            3 -> {
                // Generate code
                generateCode()
                showStep(4)
            }
            4 -> {
                // Close dialog
                close(OK_EXIT_CODE)
            }
        }
    }
    
    private fun validateDataSource(): Boolean {
        if (dataSourceUrlField.text.isBlank()) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "请输入数据库 URL",
                "验证失败",
                JOptionPane.WARNING_MESSAGE
            )
            return false
        }
        
        if (usernameField.text.isBlank()) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "请输入用户名",
                "验证失败",
                JOptionPane.WARNING_MESSAGE
            )
            return false
        }
        
        if (databaseField.text.isBlank()) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "请输入数据库名",
                "验证失败",
                JOptionPane.WARNING_MESSAGE
            )
            return false
        }
        
        return true
    }
    
    private fun validateConfiguration(): Boolean {
        if (basePackageField.text.isBlank()) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "请输入基础包名",
                "验证失败",
                JOptionPane.WARNING_MESSAGE
            )
            return false
        }
        
        return true
    }
    
    private fun generateCode() {
        if (metadataProvider == null || selectedTables.isEmpty()) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "无法生成代码：缺少必要信息",
                "错误",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        val fileWriter = FileWriter(project)
        val codeGenerator = CodeGenerator(templateEngine, metadataProvider!!)

        val selectedModule = moduleComboBox.selectedItem as? Module
        val javaSourceRoot = fileWriter.findOrCreateSourceRoot(selectedModule)
        val resourcesRoot = fileWriter.findOrCreateResourcesRoot(selectedModule)

        if (javaSourceRoot == null || resourcesRoot == null) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "无法找到或创建源代码目录",
                "错误",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        var successCount = 0
        var failCount = 0

        selectedTables.forEach { tableName ->
            try {
                val className = tableNameToClassName(tableName)
                val generatedCode = codeGenerator.generateAll(
                    tableName = tableName,
                    basePackage = basePackageField.text,
                    entityPackage = "${basePackageField.text}.${entityPackageField.text}",
                    mapperPackage = "${basePackageField.text}.${mapperPackageField.text}",
                    xmlPackage = "${basePackageField.text}.${xmlPackageField.text}",
                    servicePackage = "${basePackageField.text}.${servicePackageField.text}",
                    controllerPackage = "${basePackageField.text}.${controllerPackageField.text}",
                    className = className,
                    generateService = generateServiceCheckBox.isSelected,
                    generateController = generateControllerCheckBox.isSelected,
                    useMyBatisPlus = useMyBatisPlusCheckBox.isSelected,
                    useLombok = useLombokCheckBox.isSelected,
                    generateBatchOperations = generateBatchOperationsCheckBox.isSelected,
                    generateInsertOnDuplicateUpdate = generateInsertOnDuplicateUpdateCheckBox.isSelected,
                    useLocalDateTime = useLocalDateTimeCheckBox.isSelected
                )

                // Write Entity
                fileWriter.writeJavaFile(
                    "${basePackageField.text}.${entityPackageField.text}",
                    className,
                    generatedCode.entity,
                    javaSourceRoot
                )

                // Write Mapper
                fileWriter.writeJavaFile(
                    "${basePackageField.text}.${mapperPackageField.text}",
                    "${className}Mapper",
                    generatedCode.mapper,
                    javaSourceRoot
                )

                // Write XML
                val xmlPackagePath = "${basePackageField.text}.${xmlPackageField.text}".replace(".", "/")
                val xmlDir = createPackageDirectory(resourcesRoot, xmlPackagePath)
                fileWriter.writeXmlFile(
                    "${className}Mapper.xml",
                    generatedCode.xml,
                    xmlDir ?: resourcesRoot
                )

                // Write Service if enabled
                if (generateServiceCheckBox.isSelected && generatedCode.service != null) {
                    fileWriter.writeJavaFile(
                        "${basePackageField.text}.${servicePackageField.text}",
                        "I${className}Service",
                        generatedCode.service,
                        javaSourceRoot
                    )

                    if (generatedCode.serviceImpl != null) {
                        val implPackage = "${basePackageField.text}.${servicePackageField.text}.impl"
                        fileWriter.writeJavaFile(
                            implPackage,
                            "${className}ServiceImpl",
                            generatedCode.serviceImpl,
                            javaSourceRoot
                        )
                    }
                }

                // Write Controller if enabled
                if (generateControllerCheckBox.isSelected && generatedCode.controller != null) {
                    fileWriter.writeJavaFile(
                        "${basePackageField.text}.${controllerPackageField.text}",
                        "${className}Controller",
                        generatedCode.controller,
                        javaSourceRoot
                    )
                }

                successCount++
            } catch (e: Exception) {
                failCount++
                e.printStackTrace()
            }
        }

        val message = if (failCount == 0) {
            "成功生成 $successCount 个表的代码！"
        } else {
            "成功生成 $successCount 个表的代码，失败 $failCount 个"
        }

        JOptionPane.showMessageDialog(
            contentPanel,
            message,
            if (failCount == 0) "成功" else "部分成功",
            if (failCount == 0) JOptionPane.INFORMATION_MESSAGE else JOptionPane.WARNING_MESSAGE
        )
    }

    private fun createPackageDirectory(root: VirtualFile, packagePath: String): VirtualFile? {
        return com.intellij.openapi.command.WriteCommandAction.writeCommandAction(project).compute<VirtualFile?, IOException> {
            try {
                var currentDir = root
                val dirs = packagePath.split("/")
                for (dirName in dirs) {
                    if (dirName.isBlank()) continue
                    var dir = currentDir.findChild(dirName)
                    if (dir == null || !dir.isDirectory) {
                        dir = currentDir.createChildDirectory(this, dirName)
                    }
                    currentDir = dir
                }
                currentDir
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private fun updateButtonStates() {
        // Update button states based on current step
        // This can be enhanced to disable/enable buttons as needed
    }
    
    override fun createCenterPanel(): JComponent {
        return mainPanel
    }
    
    override fun doOKAction() {
        if (currentStep == 4) {
            super.doOKAction()
        } else {
            nextStep()
        }
    }
    
    override fun doCancelAction() {
        connection?.close()
        super.doCancelAction()
    }
}
