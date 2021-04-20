package tornadofx.testapps

import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.control.cell.ComboBoxTableCell
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.util.Callback
import javafx.util.StringConverter
import javafx.util.converter.DoubleStringConverter
import org.junit.Test
import tornadofx.*

fun main() {
    launch<SmartTableCellEditableTest>()
}

class SmartTableCellEditableTest : App(SmartTableCellView::class){

}

class SmartTableCellView: View() {
    override val root = stackpane {
        tableview<Model>(listOf(
            Model(0.5, 1, 1, true, "d", Sub("first")),
            Model(2.0, 2, 2, false, "f", Sub("second"))
        ).toObservable()) {
            column("textField", Model::first) {
                cellFormat(editableDelegate = SmartTableCell.TextFieldDelegate()) {
                    text = it.toString()
                    style(true){
                        backgroundColor += it.toInt().color()
                    }
                }

            }
            column("combobox", Model::second) {
                cellFormat(editableDelegate = SmartTableCell.ComboBoxDelegate((1..8).toList().asObservable())) {
                    text = it.toString()
                    style(true){
                        backgroundColor += it.color()
                    }
                }
            }
            column("choicebox", Model::third){
                cellFormat(editableDelegate = SmartTableCell.ChoiceBoxDelegate((1..8).toList().asObservable())){
                    text = it.toString()
                    style(true){
                        backgroundColor += it.color()
                    }
                }
            }
            val booleanConverter1 = BooleanConverter1()
            column("checkbox", Model::bool){
                cellFormat(scope = Scope(), editableDelegate = SmartTableCell.CheckBoxDelegate(booleanConverter1)){
                    text = booleanConverter1.toString(it)
                    val color = when(it!!){
                        true -> Color.GREEN
                        false -> Color.RED
                    }
                    style(true){
                        backgroundColor += color
                    }
                }
            }
           column("tfSub", Model::sub){
               cellFormat(editableDelegate = SmartTableCell.TextFieldDelegate( object: StringConverter<Sub>(){
                   override fun toString(`object`: Sub?): String {
                       return `object`?.name ?: ""
                   }

                   override fun fromString(string: String?): Sub {
                       return if (string == null) Sub("") else Sub(string)
                   }

               })){
                   text = it.name
                   style{
                       // if value contains digit symbols
                       if (text.matches(".*\\d+.*".toRegex())) backgroundColor += Color.RED
                   }

               }
           }

            column("fragment", Model::str).cellFragment(fragment = MyCellFragemnt::class, editableDelegate = SmartTableCell.TextFieldDelegate())
            column("comboFr", Model::str).cellFragment(fragment = MyCellFragemnt::class, editableDelegate = SmartTableCell.ComboBoxDelegate(
                listOf("dds", "sdsdf", "dfsf").asObservable()))
            column("choiceFr", Model::str).cellFragment(fragment = MyCellFragemnt::class, editableDelegate = SmartTableCell.ChoiceBoxDelegate(
                listOf("dds", "sdsdf", "dfsf").asObservable()))

        }
    }

    class MyCellFragemnt: TableCellFragment<Model, String>() {
        lateinit var l: Label
        override val root = hbox {
                l = label(""){
                    style {
                        backgroundColor += Color.BLACK
                        textFill = Color.WHITE
                    }
                }
            }

        override fun commitEdit(newValue: String) {
            super.commitEdit(newValue)
            l.text = "Up: ${newValue.toUpperCase()}"
        }
    }

    class MyBoolFragment: TableCellFragment<Model, Boolean?>(){
        lateinit var l: Label
        override val root = hbox {
            l = label("start") {
                style {
                    backgroundColor += Color.BLACK
                    textFill = Color.WHITE
                }
            }
        }

        override fun commitEdit(newValue: Boolean?) {
            super.commitEdit(newValue)
            l.text = newValue.toString().reversed()

        }
    }


}

class Model(var first: Double, var second: Int, var third: Int, var bool: Boolean?, var str: String, var sub: Sub)

class Sub(val name: String)

class BooleanConverter1: StringConverter<Boolean?>(){
    override fun toString(`object`: Boolean?): String {
        return if (`object` == true) "ok" else "no"
    }

    override fun fromString(string: String?): Boolean {
        return string == "ok"
    }
}

fun Int.color(): Color {
    return when (this) {
        1 -> c("#ffdddd")
        2 -> c("#ffaaaa")
        3 -> c("#ff8888")
        4 -> c("#ff6666")
        5 -> c("#ff4444")
        6 -> c("#ff2222")
        7 -> c("#ff1111")
        else -> c("#ff0000")
    }
}
