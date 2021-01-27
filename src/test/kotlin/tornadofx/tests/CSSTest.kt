package tornadofx.tests

import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Test
import org.testfx.api.FxAssert
import org.testfx.api.FxService
import org.testfx.api.FxToolkit
import org.testfx.matcher.base.NodeMatchers
import tornadofx.*

class CSSTest {
    val primaryStage: Stage = FxToolkit.registerPrimaryStage()

    class Styles: Stylesheet() {
        companion object {
            val tcss by cssclass()
        }
    }

    @Test
    fun `addClass should add only unique classes`() {
        val testValue0 = SimpleIntegerProperty(0)
        val list = FXCollections.observableArrayList(testValue0)

        FxToolkit.setupFixture {
            val root = StackPane().apply {
                label("test label")
            }
            primaryStage.scene = Scene(root)
            primaryStage.show()
        }
        //precheck
        val label = FxService.serviceContext().nodeFinder.lookup(".label").query<Label>()
        FxAssert.verifyThat(label, NodeMatchers.isNotNull())

        //given
        label.addClass("t1")
        label.addClass(Styles.tcss)
        assertThat(label.styleClass)
            .containsOnlyOnce("t1")
        assertThat(label.styleClass)
            .containsOnlyOnce(Styles.tcss.name)

        //when
        label.addClass("t1")
        label.addClass("t2", "t3","t2","t3")
        label.addClass(Styles.tcss, Styles.tcss)

        SoftAssertions().apply {
            assertThat(label.styleClass)
                .`as`("single param call: t1")
                .containsOnlyOnce("t1")
            assertThat(label.styleClass)
                .`as`("vararg string call: t2, t3")
                .containsOnlyOnce("t2")
            assertThat(label.styleClass)
                .`as`("vararg string call: t2, t3")
                .containsOnlyOnce("t3")
            assertThat(label.styleClass)
                .`as`("Styles#cssclass call: tcss")
                .containsOnlyOnce(Styles.tcss.name)
        }.assertAll()

    }

}