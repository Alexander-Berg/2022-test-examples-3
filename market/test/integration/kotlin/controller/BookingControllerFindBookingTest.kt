package ru.yandex.market.logistics.calendaring.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.jayway.jsonpath.JsonPath
import org.apache.http.entity.ContentType
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus
import ru.yandex.market.logistics.calendaring.util.FileContentUtils
import ru.yandex.market.logistics.calendaring.util.MockParametersHelper
import java.time.LocalDateTime
import java.util.*

class BookingControllerFindBookingTest : AbstractContextualTest() {

    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/find-booking/by-supplierId/before.xml")
    fun foundSupplierSuccessfully() {
        setupLmsBusinessWarehouses()

        val expectedJson = JsonPath.read<List<Any>>(
            FileContentUtils.getFileContent("fixtures/controller/booking/find-booking/by-supplierId/response.json"),
            "content",
            null
        )

        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/booking/find-by-supplier/")
                .param("supplierId", SUPPLIER_ID_101)
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("content").value(expectedJson))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/find-booking/empty/before.xml")
    fun emptyResult() {
        setupLmsBusinessWarehouses()

        val result = mockMvc!!.perform(
            MockMvcRequestBuilders.get("/booking/find-by-supplier/")
                .param("supplierId", SUPPLIER_ID_NOT_EXISTING)
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isOk)
            .andReturn()

        val expectedJson =
            FileContentUtils.getFileContent("fixtures/controller/booking/find-booking/empty/response.json")

        JSONAssert.assertEquals(expectedJson, result.response.contentAsString, JSONCompareMode.NON_EXTENSIBLE)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/find-booking/warehouse-not-found/before.xml")
    fun warehouseNotFound() {
        setupLmsBusinessWarehouses()

        val expectedJson = JsonPath.read<List<Any>>(
            FileContentUtils.getFileContent("fixtures/controller/booking/find-booking/warehouse-not-found/response.json"),
            "content",
            null
        )

        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/booking/find-by-supplier/")
                .param("supplierId", SUPPLIER_ID_102)
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("content").value(expectedJson))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/find-booking/by-supplier-with-filter/by-warehouse/before.xml")
    fun foundSupplierFilteredByWarehouse() {
        setupLmsBusinessWarehouses()

        val expectedJson = JsonPath.read<List<Any>>(
            FileContentUtils.getFileContent("fixtures/controller/booking/find-booking/by-supplier-with-filter/by-warehouse/response.json"),
            "content",
            null
        )

        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/booking/find-by-supplier/")
                .param("supplierId", SUPPLIER_ID_101)
                .param("warehouseId", "2")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("content").value(expectedJson))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/find-booking/by-supplier-with-filter/by-status-and-from/before.xml")
    fun foundSupplierFilteredByStatusAndFrom() {
        setupLmsBusinessWarehouses()

        val expectedJson = JsonPath.read<List<Any>>(
            FileContentUtils.getFileContent("fixtures/controller/booking/find-booking/by-supplier-with-filter/by-status-and-from/response.json"),
            "content",
            null
        )

        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/booking/find-by-supplier/")
                .param("supplierId", SUPPLIER_ID_101)
                .param("status", BookingStatus.ACTIVE.name)
                .param("from", LocalDateTime.of(2021, 5, 21, 0, 0, 0).toString())
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("content").value(expectedJson))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/find-booking/one-page/before.xml")
    fun onePageTest() {
        setupLmsBusinessWarehouses()

        val expectedJson = JsonPath.read<List<Any>>(
            FileContentUtils.getFileContent("fixtures/controller/booking/find-booking/one-page/response.json"),
            "content",
            null
        )

        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/booking/find-by-supplier/")
                .param("supplierId", SUPPLIER_ID_101)
                .param("page", "0")
                .param("size", "5")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("content").value(expectedJson))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/booking/find-booking/third-page/before.xml")
    fun thirdPageTest() {
        setupLmsBusinessWarehouses()

        val expectedJson = JsonPath.read<List<Any>>(
            FileContentUtils.getFileContent("fixtures/controller/booking/find-booking/third-page/response.json"),
            "content",
            null
        )

        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/booking/find-by-supplier/")
                .param("supplierId", SUPPLIER_ID_101)
                .param("page", "2")
                .param("size", "2")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("content").value(expectedJson))
    }

    @Test
    @DatabaseSetup("classpath:" +
        "fixtures/controller/booking/find-booking/by-supplier-with-filter/by-status-expired/before.xml")
    fun findBookingsBySupplierIdFilterByExpiredStatus() {
        setupLmsBusinessWarehouses()

        val json = "fixtures/controller/booking/find-booking/by-supplier-with-filter/by-status-expired/response.json"

        val expectedJson = JsonPath.read<List<Any>>(
            FileContentUtils.getFileContent(json),
            "content",
            null
        )

        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/booking/find-by-supplier/")
                .param("supplierId", SUPPLIER_ID_101)
                .param("status", BookingStatus.EXPIRED.name)
                .param("page", "0")
                .param("size", "5")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isOk)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(jsonPath("content").value(expectedJson))
    }

    private fun setupLmsBusinessWarehouses() {
        val warehouseIds: List<Long> = listOf(1L, 2L, 3L)
        val addresses: List<String> = listOf("Rostov", "Ekb", "Moscow")
        warehouseIds.zip(addresses) { warehouseId, address ->
            Mockito.`when`(lmsClient?.getBusinessWarehouseForPartner(warehouseId))
                .thenReturn(Optional.of(MockParametersHelper.mockBusinessWarehouseResponse(address)!!))
        }
    }

    companion object {
        const val SUPPLIER_ID_101 = "101"
        const val SUPPLIER_ID_102 = "102"
        const val SUPPLIER_ID_NOT_EXISTING = "105"
    }
}
