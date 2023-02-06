package ru.yandex.market.logistics.calendaring.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.apache.http.entity.ContentType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.util.FileContentUtils

class BookingControllerGetSlotTest : AbstractContextualTest() {

    @AfterEach
    fun verifyMocks() {
        verifyNoMoreInteractions(ffwfClientApi!!)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/get-slot/slot-exists/before.xml")
    fun getExistingSlotTest() {


        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/booking/1")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val jsonResponseNoFile =
            FileContentUtils.getFileContent("fixtures/controller/booking/get-slot/slot-exists/response.json")
        JSONAssert.assertEquals(jsonResponseNoFile, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    fun getNotExistingSlotTest() {

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/booking/1")
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andReturn()

        assertions().assertThat(result.response.contentAsString).contains("Can't find such booking")
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/get-slot/slot-exists/before.xml")
    fun getExistingSlotByExternalIdentifiersTest() {

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/find-by-external-ids")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content("{\"externalIds\":[\"id123\"],\"source\":\"TEST\"}")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val jsonResponseNoFile =
            FileContentUtils.getFileContent("fixtures/controller/booking/get-slot/slot-exists/response-list.json")
        JSONAssert.assertEquals(jsonResponseNoFile, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/get-slot/slot-exists/before.xml")
    fun getExistingSlotByExternalIdentifiersAndStatusTest() {

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/find-by-external-ids")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content("{\"externalIds\":[\"id123\"],\"source\":\"TEST\",\"status\":\"ACTIVE\"}")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val jsonResponseNoFile = FileContentUtils
            .getFileContent("fixtures/controller/booking/get-slot/slot-exists/response-active-list.json")
        JSONAssert.assertEquals(jsonResponseNoFile, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    fun getNotExistingSlotByExternalIdentifiersTest() {

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/find-by-external-ids")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content("{\"externalIds\":[\"id123\"],\"source\":\"TEST\"}")
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andReturn()

        assertions().assertThat(result.response.contentAsString).contains("Can't find such booking")
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/get-slot/slot-exists/before.xml")
    fun getBookingsByIdsWhenExistsTest() {


        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/find-by-ids")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content("{\"bookingIds\":[1, 2]}")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val jsonResponseNoFile =
            FileContentUtils.getFileContent("fixtures/controller/booking/get-slot/slot-exists/response-list.json")
        JSONAssert.assertEquals(jsonResponseNoFile, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/get-slot/slot-exists/before.xml")
    fun getBookingsByIdsAndStatusWhenExistsTest() {


        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/find-by-ids")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content("{\"bookingIds\":[1, 2], \"status\": \"ACTIVE\"}")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val jsonResponseNoFile = FileContentUtils
            .getFileContent("fixtures/controller/booking/get-slot/slot-exists/response-active-list.json")
        JSONAssert.assertEquals(jsonResponseNoFile, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/get-slot/slot-exists/before.xml")
    fun getBookingsByIdsWhenNotExistsTest() {


        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.post("/booking/find-by-ids")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
                .content("{\"bookingIds\":[5]}")
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andReturn()

        assertions().assertThat(result.response.contentAsString).contains("Can't find such booking")
    }

}
