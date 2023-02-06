package ru.yandex.market.wms.constraints.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.whenever
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.constraints.config.ConstraintsIntegrationTest
import ru.yandex.market.wms.core.base.dto.WghValue
import ru.yandex.market.wms.core.base.response.GetSerialNumberCharacteristicsResponse

class CharacteristicsControllerTest : ConstraintsIntegrationTest() {
    @AfterEach
    fun clear() {
        clearInvocations(coreClient)
    }

    @Test
    @DatabaseSetup("/controller/characteristics/serial/before.xml")
    fun `Get characteristics by serial number returns successful response`() {
        setCoreClientMock()
        mockMvc.perform(get("/admin/characteristics/serial").param("serialNumber", "1"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    FileContentUtils.getFileContent(
                        "controller/characteristics/serial/successful/response.json"
                    ),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/characteristics/serial/before.xml")
    fun `Get characteristics by serial number returns successful response when core returns empty cargo types`() {
        setCoreClientMock(cargoTypes = listOf())
        mockMvc.perform(get("/admin/characteristics/serial").param("serialNumber", "1"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    FileContentUtils.getFileContent(
                        "controller/characteristics/serial/cargo-types-empty/response.json"
                    ),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/characteristics/serial/before.xml")
    fun `Get characteristics by serial number returns successful response when core returns not enabled cargo types`() {
        setCoreClientMock(cargoTypes = listOf("10"))
        mockMvc.perform(get("/admin/characteristics/serial").param("serialNumber", "1"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    FileContentUtils.getFileContent(
                        "controller/characteristics/serial/cargo-types-not-enabled/response.json"
                    ),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/characteristics/serial/before.xml")
    fun `Get characteristics by serial number returns successful response when core returns unknown cargo types`() {
        setCoreClientMock(cargoTypes = listOf("999999"))
        mockMvc.perform(get("/admin/characteristics/serial").param("serialNumber", "1"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    FileContentUtils.getFileContent(
                        "controller/characteristics/serial/cargo-types-unknown/response.json"
                    ),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/characteristics/serial/before.xml")
    fun `Get characteristics by serial number returns successful response when core returns null vals in dimensions`() {
        setCoreClientMock(dimensions = WghValue(null, null, null, null, null))
        mockMvc.perform(get("/admin/characteristics/serial").param("serialNumber", "1"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    FileContentUtils.getFileContent(
                        "controller/characteristics/serial/dimensions-null-vals/response.json"
                    ),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/characteristics/serial/before.xml")
    fun `Get characteristics by serial number returns successful response when core returns null dimensions`() {
        setCoreClientMock(dimensions = null)
        mockMvc.perform(get("/admin/characteristics/serial").param("serialNumber", "1"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    FileContentUtils.getFileContent(
                        "controller/characteristics/serial/dimensions-null/response.json"
                    ),
                    true
                )
            )
    }

    private fun setCoreClientMock(
        dimensions: WghValue? = WghValue(width = "10", height = "20", length = "30", volume = "40", weight = "50"),
        cargoTypes: List<String> = listOf("80")
    ) {
        whenever(coreClient.getSerialNumberCharacteristics(any()))
            .thenReturn(
                GetSerialNumberCharacteristicsResponse(
                    serialNumber = "1",
                    name = "test",
                    sku = "ROV0000000018",
                    storerKey = "1000",
                    dimensions = dimensions,
                    cargoTypes = cargoTypes
                )
            )
    }
}
