package net.priimak

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ShowCurrentProjectSwitcherDialog : AnAction("Switch to opened Project") {
    override fun actionPerformed(e: AnActionEvent) {
        CurrentProjectSwitcherDialog().showAndGet()
    }
}