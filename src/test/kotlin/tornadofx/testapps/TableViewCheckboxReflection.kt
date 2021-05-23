package tornadofx.testapps

import javafx.beans.property.SimpleBooleanProperty
import tornadofx.*

class TableViewCheckboxApp : App(TableViewCheckbox::class)

class Model {
    val booleanProperty = SimpleBooleanProperty()
}

class TableViewCheckbox : View() {
    val items = observableListOf(Model())

    override val root = tableview(items) {
        column("Checkbox", Model::booleanProperty)
            .useCheckbox()
    }
}
