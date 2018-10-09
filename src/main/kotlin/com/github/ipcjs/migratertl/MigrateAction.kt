package com.github.ipcjs.migratertl

import com.intellij.lang.Language
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.actions.BaseRefactoringAction
import org.apache.xmlbeans.XmlLanguage

fun Document.replaceString(oldString: String, newString: String, project: Project) {
    var startIndex = 0
    val content = this.charsSequence
    while (startIndex < content.length) {
        val index = content.indexOf(oldString, startIndex)
        if (index == -1) {
            break
        }
        WriteCommandAction.runWriteCommandAction(project, Computable {
            this.replaceString(index, index + oldString.length, newString)
        })
        startIndex = index + newString.length
    }
}


class Rtl(
        val se: String,
        val lr: String
) {
    companion object {
        private val base = arrayOf(
                Rtl("Start", "Left"),
                Rtl("start", "left"),
                Rtl("End", "Right"),
                Rtl("end", "right")
        )

        private fun se2lr(se: String): String {
            var result = se
            for (rtl in base) {
                result = rtl.toLR(result)
            }
            return result
        }
    }

    fun toLR(str: String) = str.replace(this.se, this.lr)
    fun toSE(str: String) = str.replace(this.lr, this.se)

    constructor(se: String) : this(se, se2lr(se))
}

abstract class MigrateAction() : BaseRefactoringAction(), RefactoringActionHandler {
    companion object {
        val rtlArray = arrayOf(
                /* constraint */
                Rtl("layout_constraintStart_toEndOf"),
                Rtl("layout_constraintEnd_toEndOf"),
                Rtl("layout_constraintStart_toStartOf"),
                Rtl("layout_constraintEnd_toStartOf"),
                /* android */
                Rtl("paddingStart"),
                Rtl("paddingEnd"),
                Rtl("layout_marginStart"),
                Rtl("layout_marginEnd"),
                Rtl("drawableStart"),
                Rtl("drawableEnd"),
                Rtl("layout_toStartOf"),
                Rtl("layout_toEndOf"),
                Rtl("layout_alignStart"),
                Rtl("layout_alignEnd"),
                Rtl("layout_alignParentStart"),
                Rtl("layout_alignParentEnd")
        )
    }

    override fun isEnabledOnElements(elements: Array<out PsiElement>): Boolean = true

    override fun isAvailableForLanguage(language: Language): Boolean = language.isKindOf(XMLLanguage.INSTANCE)

    override fun getHandler(dataContext: DataContext): RefactoringActionHandler? = this

    override fun isAvailableInEditorOnly(): Boolean = true

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        // pass
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile?, dataContext: DataContext?) {
        val document = editor.document
        rtlArray.forEach {
            replace(project, document, it)
        }
    }

    abstract fun replace(project: Project, document: Document, rtl: Rtl)
}

class ToLRMigrateAction : MigrateAction() {
    override fun replace(project: Project, document: Document, rtl: Rtl) {
        document.replaceString(rtl.se, rtl.lr, project)
    }
}

class ToSEMigrateAction : MigrateAction() {
    override fun replace(project: Project, document: Document, rtl: Rtl) {
        document.replaceString(rtl.lr, rtl.se, project)
    }
}
