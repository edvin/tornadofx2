package tornadofx

import javafx.beans.InvalidationListener
import javafx.beans.Observable
import javafx.beans.property.*
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.skin.ComboBoxListViewSkin
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.util.Callback
import javafx.util.StringConverter
import javafx.util.converter.*
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import kotlin.reflect.KClass

abstract class TableCellFragment<S, T> : RowItemFragment<S, T>() {
    val cellProperty: ObjectProperty<TableCell<S, T>?> = SimpleObjectProperty()
    var cell by cellProperty

    val editingProperty = SimpleBooleanProperty(false)
    val editing by editingProperty

    open fun startEdit() {
    }

    open fun commitEdit(newValue: T) {
    }

    open fun cancelEdit() {
    }

    open fun onEdit(op: () -> Unit) {
        editingProperty.onChange { if (it) op() }
    }
}

@Suppress("UNCHECKED_CAST")
open class SmartTableCell<S, T>(val scope: Scope = FX.defaultScope, val owningColumn: TableColumn<S, T>) : TableCell<S, T>() {
    private val editSupport: (TableCell<S, T>.(EditEventType, T?) -> Unit)? get() = owningColumn.properties["tornadofx.editSupport"] as (TableCell<S, T>.(EditEventType, T?) -> Unit)?
    private val cellFormat: (TableCell<S, T>.(T) -> Unit)? get() = owningColumn.properties["tornadofx.cellFormat"] as (TableCell<S, T>.(T) -> Unit)?
    private val cellCache: TableColumnCellCache<T>? get() = owningColumn.properties["tornadofx.cellCache"] as TableColumnCellCache<T>?
    private var cellFragment: TableCellFragment<S, T>? = null
    private var fresh = true
    private var freshStyleClass: Collection<String>? = null
    private var editableDelegate: EditableDelegate<T>? = null

    init {
        owningColumn.properties["tornadofx.cellFormatCapable"] = true
        owningColumn.properties["tornadofx.cellCacheCapable"] = true
        owningColumn.properties["tornadofx.editCapable"] = true
        indexProperty().onChange {
            if (it == -1) clearCellFragment()
        }
    }

    constructor(scope: Scope = FX.defaultScope, owningColumn: TableColumn<S, T>, editableDelegate: EditableDelegate<T>): this(scope, owningColumn){
        this.editableDelegate = editableDelegate
        if (editableDelegate.type == CheckBox::class){
            graphic = cache {
                alignment = Pos.CENTER
                checkbox {
                    selectedProperty().bindBidirectional(itemProperty() as ObjectProperty<Boolean>)
                    setOnAction {
                        tableView.edit(index, tableColumn)
                        commitEdit(!this.isSelected as T)
                    }
                }
            }
        }
    }

    override fun startEdit() {
        super.startEdit()
        editSupport?.invoke(this, EditEventType.StartEdit, null)
        cellFragment?.startEdit()
        if (editableDelegate == null || editableDelegate!!.type == CheckBox::class) return
        text = null
        val editableNode = when(editableDelegate!!.type){
            TextField::class -> TextField().apply {
                text = this@SmartTableCell.text
                setOnAction {
                    commitEdit(editableDelegate!!.converter!!.fromString(text))
                    it.consume()
                }
                setOnKeyReleased {
                    if (it.code == KeyCode.ESCAPE){
                        cancelEdit()
                        it.consume()
                    }
                }
                selectAll()
                requestFocus()
            }
            ComboBox::class -> ComboBox((editableDelegate as ComboBoxDelegate<T>).items).apply {
                maxWidth = Double.MAX_VALUE
                addEventFilter(KeyEvent.KEY_RELEASED) { e: KeyEvent ->
                    if (e.code == KeyCode.ENTER) {
                        commitEdit(value)
                    } else if (e.code == KeyCode.ESCAPE) {
                        cancelEdit()
                    }
                }
                if (skin != null && skin is ComboBoxListViewSkin<*>){
                    (skin as ComboBoxListViewSkin<*>).popupContent.addEventFilter(MouseEvent.MOUSE_RELEASED){ _ ->
                        commitEdit(value)
                    }
                }else{
                    skinProperty().addListener(object : InvalidationListener {
                        override fun invalidated(it: Observable) {
                            (skin as ComboBoxListViewSkin<*>).popupContent.addEventFilter(MouseEvent.MOUSE_RELEASED){ _ ->
                                commitEdit(value)
                            }
                            skinProperty().removeListener(this)
                        }
                    })
                }
               editor.focusedProperty().addListener { _ ->
                   if (!isFocused) {
                       commitEdit(value as T)
                   }
               }
                if (this.items.contains(this@SmartTableCell.item))
                    selectionModel.select(items.indexOf(this@SmartTableCell.item))
                else selectionModel.select(0)
            }
            ChoiceBox::class -> ChoiceBox((editableDelegate as ChoiceBoxDelegate<T>).items).apply {
                showingProperty().onChange {
                    if (!it) commitEdit(selectionModel.selectedItem)
                }
                if (this.items.contains(this@SmartTableCell.item))
                    selectionModel.select(items.indexOf(this@SmartTableCell.item))
                else selectionModel.select(0)
            }
            else -> throw RuntimeException("Control ${editableDelegate!!.type} not supported")
        }
        graphic = editableNode
    }

    override fun commitEdit(newValue: T) {
        super.commitEdit(newValue)
        editSupport?.invoke(this, EditEventType.CommitEdit, newValue)
        cellFragment?.commitEdit(newValue)
        if (editableDelegate == null) return
        if (cellFragment != null){
            graphic = cellFragment!!.root
            text = null
        }
        else {
            if (editableDelegate!!.type != CheckBox::class) graphic = null
            textProperty().value = editableDelegate!!.converter!!.toString(newValue)
        }
    }

    override fun cancelEdit() {
        super.cancelEdit()
        if (editableDelegate == null) return
        editSupport?.invoke(this, EditEventType.CancelEdit, null)
        cellFragment?.cancelEdit()
        graphic = cellFragment?.root
        text = editableDelegate!!.converter!!.toString(item)
    }

    override fun updateItem(item: T, empty: Boolean) {
        super.updateItem(item, empty)
        if (item == null || empty) {
            cleanUp()
            clearCellFragment()
        } else {
            FX.ignoreParentBuilder = FX.IgnoreParentBuilder.Once
            try {
                cellCache?.apply { graphic = getOrCreateNode(item) }
            } finally {
                FX.ignoreParentBuilder = FX.IgnoreParentBuilder.No
            }
            if (fresh) {
                val cellFragmentType = owningColumn.properties["tornadofx.cellFragment"] as KClass<TableCellFragment<S, T>>?
                cellFragment = if (cellFragmentType != null) find(cellFragmentType, scope) else null
                freshStyleClass = listOf(*styleClass.toTypedArray())
                fresh = false
            }
            cellFragment?.apply {
                editingProperty.cleanBind(editingProperty())
                itemProperty.value = item
                rowItemProperty.value = tableView.items[index]
                cellProperty.value = this@SmartTableCell
                graphic = root
                this@SmartTableCell.commitEdit(item)
            }
            cellFormat?.invoke(this, item)
        }
    }

    private fun cleanUp() {
        textProperty().unbind()
        graphicProperty().unbind()
        text = null
        graphic = null
        style = null
        //restore default style classes
        freshStyleClass?.let { styleClass.retainAll(it) }
    }

    private fun clearCellFragment() {
        cellFragment?.apply {
            cellProperty.value = null
            itemProperty.value = null
            editingProperty.unbind()
            editingProperty.value = false
        }
    }

    /**
     * Gives opportunity use [cellFormat] with editing.
     * It receive class of Node for editing, optional converter, and items for switchable controls
     */
    abstract class EditableDelegate<T>(val type: KClass<out Control>, var converter: StringConverter<T>?)

    /**
     * Gives opportunity use [cellFormat] and [cellFragment] with editing.
     * It receive class of Node for editing, optional converter, and items for switchable controls
     */
    abstract class SwitchableRenderingDelegate<T>(type: KClass<out Control>, converter: StringConverter<T>? = null): EditableDelegate<T>(type, converter)

    class TextFieldDelegate<T>(converter: StringConverter<T>? = null): SwitchableRenderingDelegate<T>(TextField::class, converter)
    class ComboBoxDelegate<T>(val items: ObservableList<T>? = null, converter: StringConverter<T>? = null):  SwitchableRenderingDelegate<T>(ComboBox::class, converter)
    class ChoiceBoxDelegate<T>(val items: ObservableList<T>, converter: StringConverter<T>? = null): SwitchableRenderingDelegate<T>(ChoiceBox::class, converter)
    class CheckBoxDelegate<T>(converter: StringConverter<T>? = null): EditableDelegate<T>(CheckBox::class, converter)

}

fun <S, T> TableColumn<S, T>.cellFormat(scope: Scope = FX.defaultScope, formatter: TableCell<S, T>.(T) -> Unit) {
    properties["tornadofx.cellFormat"] = formatter
    if (properties["tornadofx.cellFormatCapable"] != true)
        cellFactory = Callback { SmartTableCell<S, T>(scope, it) }
    isEditable = false
}

@Suppress("UNCHECKED_CAST")
inline fun <S, reified T> TableColumn<S, T>.cellFormat(
    scope: Scope = FX.defaultScope,
    editableDelegate: SmartTableCell.EditableDelegate<T>,
    noinline formatter: TableCell<S, T>.(T) -> Unit) {

    properties["tornadofx.cellFormat"] = formatter
    tableView.isEditable = true
    isEditable = true
    with(editableDelegate){
        if (converter == null) converter = if (T::class == String::class) DefaultStringConverter() as StringConverter<T> else
            defaultConverter()
    }
    if (properties["tornadofx.cellFormatCapable"] != true)
        cellFactory = Callback { SmartTableCell<S, T>(scope, it, editableDelegate) }
}

fun <S, T, F: TableCellFragment<S, T>> TableColumn<S, T>.cellFragment(scope: Scope = FX.defaultScope, fragment: KClass<F>) {
    properties["tornadofx.cellFragment"] = fragment
    if (properties["tornadofx.cellFormatCapable"] != true)
        cellFactory = Callback { SmartTableCell<S, T>(scope, it) }
}

@Suppress("UNCHECKED_CAST")
inline fun <S, reified T, F: TableCellFragment<S, T>> TableColumn<S, T>.cellFragment(
    scope: Scope = FX.defaultScope,
    fragment: KClass<F>,
    editableDelegate: SmartTableCell.SwitchableRenderingDelegate<T>,
    noinline formatter: TableCell<S, T>.(T) -> Unit = {}) {

    properties["tornadofx.cellFragment"] = fragment
    properties["tornadofx.cellFormat"] = formatter
    tableView.isEditable = true
    isEditable = true
    with(editableDelegate){
       if (converter == null) converter = if (T::class == String::class) DefaultStringConverter() as StringConverter<T> else
          defaultConverter()
    }
    if (properties["tornadofx.cellFormatCapable"] != true)
        cellFactory = Callback { SmartTableCell<S, T>(scope, it, editableDelegate) }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> defaultConverter() = when (T::class) {
    Int::class -> IntegerStringConverter()
    Long::class -> LongStringConverter()
    Double::class -> DoubleStringConverter()
    Float::class -> FloatStringConverter()
    Date::class -> DateStringConverter()
    BigDecimal::class -> BigDecimalStringConverter()
    BigInteger::class -> BigIntegerStringConverter()
    Number::class -> NumberStringConverter()
    LocalDate::class -> LocalDateStringConverter()
    LocalTime::class -> LocalTimeStringConverter()
    LocalDateTime::class -> LocalDateTimeStringConverter()
    Boolean::class -> BooleanStringConverter()
    else -> throw RuntimeException("Converter is not implemented for specified class type: ${T::class}," +
            " please define the converter manually")
} as StringConverter<T>?


/**
 * Calculate a unique Node per item and set this Node as the graphic of the TableCell.
 *
 * To support this feature, a custom cellFactory is automatically installed, unless an already
 * compatible cellFactory is found. The cellFactories installed via #cellFormat already knows
 * how to retrieve cached values.
 */
fun <S, T> TableColumn<S, T>.cellCache(scope: Scope  = FX.defaultScope, cachedGraphicProvider: (T) -> Node) {
    properties["tornadofx.cellCache"] = TableColumnCellCache(cachedGraphicProvider)
    // Install a cache capable cellFactory it none is present. The default cellFormat factory will do.
    if (properties["tornadofx.cellCacheCapable"] != true)
        cellFactory = Callback { SmartTableCell<S, T>(scope, it) }
}


fun <T, S> TableColumn<T, S?>.converter(converter: StringConverter<in S>): TableColumn<T, S?> = apply {
    cellFormat(FX.defaultScope) { text = converter.toString(it) }
}

fun <T> TableView<T>.multiSelect(enable: Boolean = true) {
    selectionModel.selectionMode = if (enable) SelectionMode.MULTIPLE else SelectionMode.SINGLE
}