package com.github.ipcjs.migratertl

import com.intellij.lang.Language
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.actions.BaseRefactoringAction

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

/** [Document.setText] can't undo. */
fun Document.setTextCanUndo(text: CharSequence, project: Project) {
    WriteCommandAction.runWriteCommandAction(project, Computable {
        this.replaceString(0, this.charsSequence.length, text)
    })
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

        fun fromAttr(seAttr: String) = Rtl("\"$seAttr\"")
    }

    fun toLR(str: String) = str.replace(this.se, this.lr)
    fun toSE(str: String) = str.replace(this.lr, this.se)

    constructor(se: String) : this(se, se2lr(se))
}

abstract class MigrateAction() : BaseRefactoringAction(), RefactoringActionHandler {
    companion object {
        val rtlArray = arrayOf(
                /* constraint */
                Rtl.fromAttr("layout_constraintStart_toEndOf"),
                Rtl.fromAttr("layout_constraintEnd_toEndOf"),
                Rtl.fromAttr("layout_constraintStart_toStartOf"),
                Rtl.fromAttr("layout_constraintEnd_toStartOf"),
                /* android */
                Rtl.fromAttr("paddingStart"),
                Rtl.fromAttr("paddingEnd"),
                Rtl.fromAttr("layout_marginStart"),
                Rtl.fromAttr("layout_marginEnd"),
                Rtl.fromAttr("drawableStart"),
                Rtl.fromAttr("drawableEnd"),
                Rtl.fromAttr("layout_toStartOf"),
                Rtl.fromAttr("layout_toEndOf"),
                Rtl.fromAttr("layout_alignStart"),
                Rtl.fromAttr("layout_alignEnd"),
                Rtl.fromAttr("layout_alignParentStart"),
                Rtl.fromAttr("layout_alignParentEnd")
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
        if (false) {
            // multi undo
            rtlArray.forEach {
                doReplaceDocument(document, it, project)
            }
        } else {
            // once undo
            var text = document.text
            rtlArray.forEach {
                text = doReplaceText(text, it)
            }
            document.setTextCanUndo(text, project)
        }
    }

    abstract fun doReplaceText(text: String, rtl: Rtl): String

    abstract fun doReplaceDocument(document: Document, rtl: Rtl, project: Project)
}

class ToLRMigrateAction : MigrateAction() {
    override fun doReplaceText(text: String, rtl: Rtl): String = rtl.toLR(text)

    override fun doReplaceDocument(document: Document, rtl: Rtl, project: Project) {
        document.replaceString(rtl.se, rtl.lr, project)
    }
}

class ToSEMigrateAction : MigrateAction() {
    override fun doReplaceText(text: String, rtl: Rtl): String = rtl.toSE(text)

    override fun doReplaceDocument(document: Document, rtl: Rtl, project: Project) {
        document.replaceString(rtl.lr, rtl.se, project)
    }
}
