package ru.yandex.market.wms.yt

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.yt.config.DbConfig
import ru.yandex.market.wms.yt.config.YtConfig

@SpringBootTest(classes = [DbConfig::class, YtConfig::class])
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
