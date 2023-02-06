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

class BookingControllerPutExternalIdTest: AbstractContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/put-external-id/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/put-external-id/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun putExternalIdSuccessful() {

        val requestJson: String = getFileContent(
            "fixtures/controller/booking/put-external-id/request.json")

        mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking/external-id")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/put-external-id/after.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/put-external-id/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun putExternalIdSuccessfulWhenAlreadyUpdated() {

        val requestJson: String = getFileContent(
            "fixtures/controller/booking/put-external-id/request.json")

        mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking/external-id")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/put-external-id/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/put-external-id/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun putExternalIdFailedDueToIncorrectBookingId() {

        val requestJson: String = getFileContent(
            "fixtures/controller/booking/put-external-id/request-incorrect-booking-id.json")

        mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking/external-id")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/put-external-id/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/put-external-id/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun putExternalIdFailedDueToIncorrectOldId() {

        val requestJson: String = getFileContent(
            "fixtures/controller/booking/put-external-id/request-incorrect-old-id.json")

        mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking/external-id")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/put-external-id/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/put-external-id/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun putExternalIdFailedDueToIncorrectSource() {

        val requestJson: String = getFileContent(
            "fixtures/controller/booking/put-external-id/request-incorrect-source.json")

        mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking/external-id")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/controller/booking/put-external-id/before-inactive.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/put-external-id/before-inactive.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun putExternalIdFailedDueToInactiveStatus() {

        val requestJson: String = getFileContent(
            "fixtures/controller/booking/put-external-id/request.json")

        mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking/external-id")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }
}
