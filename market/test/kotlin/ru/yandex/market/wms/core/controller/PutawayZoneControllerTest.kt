package ru.yandex.market.wms.core.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent

class PutawayZoneControllerTest : IntegrationTest() {
    @Test
    @DatabaseSetup("/controller/putaway-zone/search/data.xml")
    fun `searchPutawayZones without parameters`() {
        testSearchPutawayZones("without-parameters", emptyMap())
    }

    @Test
    @DatabaseSetup("/controller/putaway-zone/search/data.xml")
    fun `searchPutawayZones limited`() {
        testSearchPutawayZones("limited", mapOf("limit" to "2"))
    }

    @Test
    @DatabaseSetup("/controller/putaway-zone/search/data.xml")
    fun `searchPutawayZones with query param`() {
        testSearchPutawayZones("query-param", mapOf("query" to "MEZ"))
    }

    @Test
    @DatabaseSetup("/controller/putaway-zone/search/data.xml")
    fun `searchPutawayZones with query param and location type`() {
        testSearchPutawayZones("location-type-param", mapOf("query" to "CONS", "locationType" to "PICK"))
    }

    @Test
    @DatabaseSetup("/controller/putaway-zone/search/data.xml")
    fun `searchPutawayZones with query param and location types`() {
        testSearchPutawayZones("location-type-location-types",
            mapOf("query" to "CONS", "locationTypes" to "PICK,OTHER"))
    }

    @Test
    @DatabaseSetup("/controller/putaway-zone/search/data.xml")
    fun `searchPutawayZones with query param and location types is empty`() {
        testSearchPutawayZones("location-type-location-types-empty",
            mapOf("locationTypes" to ""))
    }

    private fun testSearchPutawayZones(testCase: String, params: Map<String, String>) {
        val request = get("/putaway-zone/search")
        params.forEach { (key, value) -> request.param(key, value) }

        mockMvc.perform(request)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(content().json(getFileContent("controller/putaway-zone/search/$testCase/response.json")))
    }
}
