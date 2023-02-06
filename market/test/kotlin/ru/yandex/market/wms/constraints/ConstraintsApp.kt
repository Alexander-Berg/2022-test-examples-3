package ru.yandex.market.wms.constraints

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.constraints.config.ConstraintsIntegrationTest


class ConstraintsAppTest : ConstraintsIntegrationTest() {
    @Test
    fun testHealthCheckEndpoint() {
        mockMvc.perform(MockMvcRequestBuilders.get(HEALTH_CHECK_URL))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    companion object {
        private const val HEALTH_CHECK_URL = "/hc/ping"
    }
}
