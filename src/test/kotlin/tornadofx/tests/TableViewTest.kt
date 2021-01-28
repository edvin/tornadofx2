package tornadofx.tests

import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.scene.Scene
import javafx.scene.control.TableCell
import javafx.scene.control.TableView
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.assertj.core.api.Assertions.*
import org.junit.Test
import org.testfx.api.FxAssert.*
import org.testfx.api.FxRobot
import org.testfx.api.FxService.serviceContext
import org.testfx.api.FxToolkit
import org.testfx.matcher.base.NodeMatchers.*
import tornadofx.*
import java.nio.file.Paths

class TableViewTest {
    class TestObject(i: Int) {
        val A = SimpleIntegerProperty(5 * i)
        val B = SimpleDoubleProperty(3.14159 * i)
        val C = SimpleStringProperty("Test string $i")
    }

    val TestList = FXCollections.observableArrayList(Array(5, ::TestObject).asList())

    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    @Test
    fun columnTest() {
        FxToolkit.setupFixture {
            val root = StackPane().apply {
                tableview(TestList) {
                    makeIndexColumn()
                    column("A Column", TestObject::A)
                    column("A Column", Boolean::class)
                    column("B Column", Double::class) {
                        value { it.value.B }
                    }
                    column("C Column", TestObject::C)
                }
                setPrefSize(400.0, 160.0)
            }
            primaryStage.scene = Scene(root)
            primaryStage.show()
        }

        val robot = FxRobot()
        robot.robotContext().captureSupport.saveImage(robot.capture(primaryStage.scene.root).image, Paths.get("example-table.png"))
    }

    @Test
    fun `SmartTableCell should reset styleClass for empty cells`() {
        val testValue0 = SimpleIntegerProperty(0)
        val testValue1 = SimpleIntegerProperty(1)
        val list = FXCollections.observableArrayList(testValue0)

        FxToolkit.setupFixture {
            val root = StackPane().apply {
                tableview(list) {
                    column("Column A", Int::class) {
                        value { it.value }
                    }.cellFormat {
                        text = it.toString()
                        if (it == 0) {
                            styleClass.remove("teststyle")
                        } else {
                            styleClass.add("teststyle")
                        }
                    }
                }
                setPrefSize(100.0, 150.0)
            }
            primaryStage.scene = Scene(root)
            primaryStage.show()
        }
        //precheck
        val table = serviceContext().nodeFinder.lookup(".table-view").query<TableView<Any>>()
        verifyThat(table, isNotNull())

        //given
        assertThat(getCellsWithStyle(table, "teststyle")).isEmpty()

        //change value
        FxToolkit.setupFixture {
            list.replaceAll { it?.let{testValue1}  }
        }
        //expect exactly one cell with teststyle
        assertThat(getCellsWithStyle(table, "teststyle")).hasSize(1)

        //change back
        FxToolkit.setupFixture {
            list.replaceAll { it?.let{testValue0}  }
        }
        //expect no cell with teststyle
        assertThat(getCellsWithStyle(table, "teststyle"))
            .`as`("empty cell should not have style 'teststyle' set")
            .isEmpty()

    }

    private fun getCellsWithStyle(table: TableView<Any>?, styleClass: String) =
        getAllCells(table).filter { it.styleClass.contains(styleClass) }.toList()

    private fun getAllCells(table: TableView<Any>?) =
        serviceContext().nodeFinder.from(table).lookup(".table-cell").queryAll<TableCell<Any, Any>>()
}