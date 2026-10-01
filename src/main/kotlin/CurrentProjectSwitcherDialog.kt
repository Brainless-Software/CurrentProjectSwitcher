package net.priimak

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import io.ktor.util.*
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

class SubstringColorRenderer(val substring: () -> String) : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component? {
        val c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        if (value != null) {
            val strValue = value.toString()
            val strValueToMatch = strValue.lowercase()
            val startAt = strValueToMatch.lowercase().indexOf(substring())
            if (startAt < 0)
                return c
            else {
                val endAt = startAt + substring().length
                setText(
                    "<html>" +
                            strValue.substring(0, startAt).escapeHTML() +
                            "<span style='background-color: yellow; color: black;'>" +
                            strValue.substring(startAt, endAt).escapeHTML() +
                            "</span>" +
                            strValue.substring(endAt).escapeHTML() +
                            "</html>"
                )
            }
        }
        return c
    }
}

class SearchField(val dialog: CurrentProjectSwitcherDialog, val table: ProjectsTable, val model: DefaultTableModel) :
    JTextField() {
    init {
        val documentListener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                changedUpdate(e)
            }

            override fun removeUpdate(e: DocumentEvent?) {
                changedUpdate(e)
            }

            override fun changedUpdate(e: DocumentEvent?) {
                try {
                    val txt = text.lowercase()
                    val columns = arrayOf("Project Name")
                    val data =
                        ProjectManager.getInstance().openProjects
                            .filter { it.name.lowercase().indexOf(txt) > -1 }
                            .sortedBy { it.name.lowercase() }
                            .map { project -> arrayOf(project.name) }.toTypedArray()
                    model.setDataVector(data, columns)
                    table.setRowSelectionInterval(0, 0) // select first row
                } catch (ex: Exception) {
                    // ignore
                }
            }
        }
        document.addDocumentListener(documentListener)

        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent?) {
                when (event?.keyCode) {
                    KeyEvent.VK_ENTER -> {
                        dialog.selectProject()
                    }

                    KeyEvent.VK_DOWN, KeyEvent.VK_KP_DOWN -> {
                        try {
                            table.setRowSelectionInterval(table.selectedRow + 1, table.selectedRow + 1)
                        } catch (_: IllegalArgumentException) {
                            // suppress out of range error
                        }
                    }

                    KeyEvent.VK_UP, KeyEvent.VK_KP_UP -> {
                        try {
                            table.setRowSelectionInterval(table.selectedRow - 1, table.selectedRow - 1)
                        } catch (_: IllegalArgumentException) {
                            // suppress out of range error
                        }
                    }
                }
            }
        })
    }

    override fun getMinimumSize(): Dimension? {
        val dimension = super.getMinimumSize()
        dimension.width = getFontMetrics(font).stringWidth("Project Name") * 4
        return dimension
    }

    override fun getPreferredSize(): Dimension? {
        return minimumSize
    }
}

class ProjectsTable(dialog: CurrentProjectSwitcherDialog, model: TableModel) : JBTable(model) {
    init {
        rowSelectionAllowed = true
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                if (e?.clickCount == 2) {
                    val row = rowAtPoint(e.point)
                    if (row != -1) {
                        dialog.selectProject()
                    }
                }
            }
        })
        addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent?) {
                dialog.searchField.grabFocus()
            }
        })
    }

    override fun isCellEditable(row: Int, column: Int): Boolean {
        return false
    }

    override fun getDefaultRenderer(columnClass: Class<*>?): TableCellRenderer? {
        return super.getDefaultRenderer(columnClass)
    }
}

class CurrentProjectSwitcherDialog : DialogWrapper(false) {
    val mainPanel = JPanel(BorderLayout())
    val columns = arrayOf("Project Name")
    val data = ProjectManager.getInstance().openProjects.sortedBy { it.name.lowercase() }
        .map { project -> arrayOf(project.name) }.toTypedArray()
    val model = DefaultTableModel(data, columns)
    val table = ProjectsTable(this, model)
    val searchField = SearchField(this, table, model)

    init {
        super.init()
        setTitle("Switch To Opened Project")

        val columnClass = Any::class.java
        table.setDefaultRenderer(columnClass, SubstringColorRenderer({ searchField.text }))
        table.setTableHeader(null)
        table.setRowSelectionInterval(0, 0) // select first row
        mainPanel.add(searchField, BorderLayout.NORTH)

        val scrollPane = JBScrollPane(table)

        val minRows = 15
        val totalMinHeight = table.getRowHeight() * minRows + table.getIntercellSpacing().height * (minRows + 1)
        scrollPane.setPreferredSize(Dimension(scrollPane.getPreferredSize().width, totalMinHeight))

        mainPanel.add(scrollPane, BorderLayout.CENTER)
        searchField.requestFocus()
        searchField.grabFocus()
    }

    override fun createActions(): Array<out Action?> {
        return arrayOf()
    }

    fun selectProject() {
        try {
            val selectedRow = table.selectedRow
            val targetProjectName = table.getValueAt(if (selectedRow == -1) 0 else selectedRow, 0)
            val project: Project? =
                ProjectManager.getInstance().openProjects.find { project -> project.name == targetProjectName }
            if (project != null) {
                val frame = WindowManager.getInstance().getFrame(project)
                close(0)
                frame?.toFront()
                frame?.requestFocus()
            }
        } catch (_: Exception) {
            // suppress all errors
        }
    }

    override fun createCenterPanel(): JComponent {
        return mainPanel
    }
}