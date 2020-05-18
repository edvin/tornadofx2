package tornadofx.tests

import javafx.beans.property.SimpleStringProperty
import org.junit.Assert
import org.junit.Test
import tornadofx.*
import java.time.LocalDate

class JsonTest {

    class AutoPerson : JsonModelAuto {
        val firstNameProperty = SimpleStringProperty()
        var firstName by firstNameProperty

        var lastName by property<String>()
        fun lastNameProperty() = getProperty(AutoPerson::lastName)

        var dob by property<LocalDate>()
        fun dobProperty() = getProperty(AutoPerson::dob)

        var type: Int? = null
        var global = false
    }

    class AutoPerson2 (
        firstName: String? = null,
        lastName: String? = null,
        dob: LocalDate? = null,
        type: Int? = null,
        global: Boolean? = null
    ): JsonModelAuto {

        var firstName by property(firstName)
        fun firstNameProperty() = getProperty(AutoPerson2::firstName)

        var lastName by property(lastName)
        fun lastNameProperty() = getProperty(AutoPerson2::lastName)

        var dob by property(dob)
        fun dobProperty() = getProperty(AutoPerson2::dob)

        var type by property(type)
        fun typeProperty() = getProperty(AutoPerson2::type)

        var global by property(global)
        fun globalProperty() = getProperty(AutoPerson2::global)
    }

    @Test
    fun firstAvailable() {
        val json = loadJsonObject("""{"dob":"1970-06-12","firstName":"John","global":true,"lastName":"Doe","type":42}""")
        val dob = json.date("date_of_birth", "dob")
        Assert.assertEquals(LocalDate.of(1970, 6, 12),dob)
    }
}
