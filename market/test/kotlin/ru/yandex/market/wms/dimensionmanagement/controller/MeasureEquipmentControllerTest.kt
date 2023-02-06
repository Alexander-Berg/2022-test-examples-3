package ru.yandex.market.wms.dimensionmanagement.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils
import ru.yandex.market.wms.dimensionmanagement.configuration.DimensionManagementIntegrationTest
import java.nio.charset.StandardCharsets
import com.google.gson.Gson
import ru.yandex.market.wms.dimensionmanagement.core.response.EquipmentTypesResponse
import ru.yandex.market.wms.servicebus.core.measurement.enums.MeasureEquipmentType

class MeasureEquipmentControllerTest :  DimensionManagementIntegrationTest() {

    @Test
    @DatabaseSetup("/controller/measure-equipment-controller/initial-state.xml")
    @ExpectedDatabase(value = "/controller/measure-equipment-controller/initial-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getPropertiesTest() {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/api/v1/equipment/4ece0221ea224f268bfc5ed04ff61729/properties")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent(
                "controller/measure-equipment-controller/get-properties/response.json"), false))
    }

    @Test
    @DatabaseSetup("/controller/measure-equipment-controller/initial-state.xml")
    @ExpectedDatabase(value = "/controller/measure-equipment-controller/initial-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getPropertiesTestNotFoundException() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/equipment/3ece0221ea224f268bfc5ed04ff61729/properties")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @DatabaseSetup("/controller/measure-equipment-controller/initial-state.xml")
    @ExpectedDatabase(value = "/controller/measure-equipment-controller/create-properties/final-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun createPropertiesTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/equipment/properties")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent(
                    "controller/measure-equipment-controller/create-properties/request.json"
                ))
        )
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.content().json("{\"version\": 1}"))
    }

    @Test
    @DatabaseSetup("/controller/measure-equipment-controller/initial-state.xml")
    @ExpectedDatabase(value = "/controller/measure-equipment-controller/update-properties/final-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun updatePropertiesTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/equipment/properties")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent(
                    "controller/measure-equipment-controller/update-properties/request.json"
                ))
        )
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.content().json("{\"version\": 2}"))
    }

    @Test
    @DatabaseSetup("/controller/measure-equipment-controller/update-properties/concurrent-update.xml")
    @ExpectedDatabase(value = "/controller/measure-equipment-controller/update-properties/concurrent-update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun updatePropertiesWhenThereWasConcurrentUpdateTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/api/v1/equipment/properties")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent(
                    "controller/measure-equipment-controller/update-properties/request.json"
                ))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonMatcher("controller/measure-equipment-controller/update-properties/concurrent-update-response.json"))
    }

    @Test
    @DatabaseSetup("/controller/measure-equipment-controller/initial-state.xml")
    @ExpectedDatabase(value = "/controller/measure-equipment-controller/delete-properties/final-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun deletePropertiesTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/equipment/4ece0221ea224f268bfc5ed04ff61729/properties"))
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/measure-equipment-controller/list-equipments/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measure-equipment-controller/list-equipments/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listEquipmentsReturnsFirstPageSortByAddDate() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/equipment")
                .param("limit", "2")
                .param("sort", "addDate")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonMatcher("controller/measure-equipment-controller/list-equipments/first-page.json"))
    }

    @Test
    @DatabaseSetup("/controller/measure-equipment-controller/list-equipments/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measure-equipment-controller/list-equipments/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listEquipmentsReturnsNextPageSortByAddDate() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/equipment")
                .param("limit", "2")
                .param("offset", "2")
                .param("sort", "addDate")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonMatcher("controller/measure-equipment-controller/list-equipments/next-page.json"))
    }

    @Test
    @DatabaseSetup("/controller/measure-equipment-controller/list-equipments/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measure-equipment-controller/list-equipments/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listEquipmentsReturnsNothingWhenOffsetEqualsToNumberOfEquipments() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/equipment")
                .param("offset", "4")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonMatcher("controller/measure-equipment-controller/list-equipments/no-equipment.json"))
    }

    @Test
    @DatabaseSetup("/controller/measure-equipment-controller/list-equipments/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measure-equipment-controller/list-equipments/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listEquipmentsFilterByPort() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/equipment")
                .param("offset", "0")
                .param("filter", "port==1024")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonMatcher("controller/measure-equipment-controller/list-equipments/filter-by-port.json"))
    }

    @Test
    @DatabaseSetup("/controller/measure-equipment-controller/list-equipments/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measure-equipment-controller/list-equipments/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listEquipmentsFilterByLoginNoResults() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/equipment")
                .param("offset", "0")
                .param("filter", "login==no_login")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonMatcher("controller/measure-equipment-controller/list-equipments/no-equipment-2.json"))
    }

    @Test
    @DatabaseSetup("/controller/measure-equipment-controller/initial-state.xml")
    @ExpectedDatabase(value = "/controller/measure-equipment-controller/toggle-equipment/final-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun toggleMeasureEquipmentTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.patch("/api/v1/equipment/4ece0221ea224f268bfc5ed04ff61729/toggle")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun getEquipmentTypesTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/equipment/types")
        )
            .andExpect(status().isOk)
            .andExpect(MockMvcResultMatchers.content().json(Gson().toJson(
                EquipmentTypesResponse(MeasureEquipmentType.values().map { it.name }.toList())))
            )
    }

    private fun jsonMatcher(expectedJsonFileName: String): ResultMatcher {
        return ResultMatcher { result: MvcResult ->
            val content = result.response.getContentAsString(StandardCharsets.UTF_8)
            JsonAssertUtils.assertFileEquals(expectedJsonFileName, content, JSONCompareMode.LENIENT)
        }
    }
}
