package tornadofx.tests

import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.testfx.api.FxAssert
import org.testfx.api.FxService
import org.testfx.api.FxToolkit
import org.testfx.matcher.base.NodeMatchers
import tornadofx.*

class BorderPaneTest {
    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    @Test
    fun borderPaneBasicTest() {
        FxToolkit.setupFixture {
            val root = StackPane().apply {
                borderpane {
                    styleClass.add("my-border=pane")
                    // Direct access
                    top = label("Top")
                    // Builder target
                    center {
                        hbox {
                            label("Center") {
                            }
                        }
                    }
                    bottom {
                        label("Bottom")
                    }
                }
                setPrefSize(150.0, 150.0)
            }
            primaryStage.scene = Scene(root)
            primaryStage.show()
        }
        //precheck
        val borderpane = FxService.serviceContext().nodeFinder.lookup(".my-border=pane").query<BorderPane>()
        FxAssert.verifyThat(borderpane, NodeMatchers.isNotNull())

        //expect no cell with teststyle
        Assertions.assertThat(borderpane.top)
            .`as`("top should be a Label")
            .isInstanceOf(Label::class.java)
        Assertions.assertThat(borderpane.center)
            .`as`("center should be a HBox")
            .isInstanceOf(HBox::class.java)
        Assertions.assertThat((borderpane.center as HBox).children)
            .`as`("center should contain only 1 child")
            .hasSize(1)
    }

}