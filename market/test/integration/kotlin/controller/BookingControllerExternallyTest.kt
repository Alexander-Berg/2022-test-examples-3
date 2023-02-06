package ru.yandex.market.logistics.calendaring.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.apache.http.entity.ContentType
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.util.getFileContent


class BookingControllerExternallyTest() :
    AbstractContextualTest() {

    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/put-externally/active/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun putBookingExternallyActive() {
        val requestJson: String =
            getFileContent("fixtures/controller/booking/put-externally/active/request.json")

        mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking/externally")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isOk)

        verifyNoMoreInteractions(ffwfClientApi!!)

    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/put-externally/connected-bookings/before.xml")
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/put-externally/connected-bookings/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun putBookingExternallyConnectedBookings() {
        val requestJson: String =
            getFileContent("fixtures/controller/booking/put-externally/connected-bookings/request.json")

        mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking/externally")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isOk)

        verifyNoMoreInteractions(ffwfClientApi!!)

    }

    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/put-externally/active-without-supplier-id/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun putBookingExternallyActiveWithoutSupplierId() {
        val requestJson: String =
            getFileContent("fixtures/controller/booking/put-externally/active-without-supplier-id/request.json")

        mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking/externally")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isOk)

        verifyNoMoreInteractions(ffwfClientApi!!)

    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/put-externally/active-exists/before.xml")
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/put-externally/active-exists/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun putBookingExternallyActiveExists() {
        val requestJson: String =
            getFileContent("fixtures/controller/booking/put-externally/active-exists/request.json")

        mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking/externally")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isOk)

        verifyNoMoreInteractions(ffwfClientApi!!)

    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/put-externally/cancelled/before.xml")
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/put-externally/cancelled/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun putBookingExternallyCancelled() {
        val requestJson: String =
            getFileContent("fixtures/controller/booking/put-externally/cancelled/request.json")

        mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking/externally")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isOk)

        verifyNoMoreInteractions(ffwfClientApi!!)

    }

    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/booking/put-externally/cancelled-not-exists/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT,
    )
    fun putBookingExternallyCancelledNotExists() {
        val requestJson: String =
            getFileContent("fixtures/controller/booking/put-externally/cancelled-not-exists/request.json")

        mockMvc!!.perform(
            MockMvcRequestBuilders.put("/booking/externally")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content(requestJson)
        )
            .andExpect(status().isOk)

        verifyNoMoreInteractions(ffwfClientApi!!)

    }
}
