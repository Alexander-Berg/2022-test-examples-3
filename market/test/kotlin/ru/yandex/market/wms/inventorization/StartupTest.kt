package ru.yandex.market.wms.inventorization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.IntegrationTest

class StartupTest : IntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val INVENTORY_HC_URL = "/hc/ping"

    @Test
    fun testHealthCheckEndpoint() {
        mockMvc.perform(get(INVENTORY_HC_URL))
                .andExpect(status().isOk)
    }
}
