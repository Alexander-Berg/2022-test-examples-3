package ru.yandex.market.logistics.calendaring.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.apache.http.entity.ContentType
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.util.getFileContent

class BookingControllerChangeExpiresAtTest : AbstractContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/booking/change-expires-at/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/booking/change-expires-at/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun changeExpiresAtSuccessful() {

        val requestJson: String = getFileContent(
            "fixtures/controller/booking/change-expires-at/request.json")

        mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking/expires-at")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/booking/drop-expires-at/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/booking/drop-expires-at/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun dropExpiresAtSuccessful() {

        val requestJson: String = getFileContent(
            "fixtures/controller/booking/drop-expires-at/request.json")

        mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking/expires-at")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/booking/change-expires-at/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/booking/change-expires-at/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun changeExpiresAtNotFound() {

        val requestJson: String = getFileContent(
            "fixtures/controller/booking/change-expires-at-not-found/request.json")

        mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking/expires-at")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

}
