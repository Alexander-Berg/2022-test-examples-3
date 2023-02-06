package ru.yandex.market.wms.constraints.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent
import ru.yandex.market.wms.constraints.config.ConstraintsIntegrationTest

class CargoTypeControllerTest : ConstraintsIntegrationTest() {

    @Test
    @DatabaseSetup("/controller/cargotype/before.xml")
    fun `Get cargo types returns all cargo types when enabled is empty`() {
        val requestBuilder = MockMvcRequestBuilders.get("/cargotype")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andExpect(
                content().json(getFileContent("controller/cargotype/enabled-empty/response.json"), true)
            )
    }

    @Test
    @DatabaseSetup("/controller/cargotype/before.xml")
    fun `Get cargo types returns only enabled cargo types when enabled is 1`() {
        val requestBuilder = MockMvcRequestBuilders.get("/cargotype")
            .contentType(MediaType.APPLICATION_JSON)
            .param("enabled", "1")

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andExpect(
                content().json(getFileContent("controller/cargotype/enabled-true/response.json"), true)
            )
    }

    @Test
    @DatabaseSetup("/controller/cargotype/before.xml")
    fun `Get cargo types returns all cargo types when enabled is 0`() {
        val requestBuilder = MockMvcRequestBuilders.get("/cargotype")
            .contentType(MediaType.APPLICATION_JSON)
            .param("enabled", "0")

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andExpect(
                content().json(getFileContent("controller/cargotype/enabled-false/response.json"), true)
            )
    }
}
