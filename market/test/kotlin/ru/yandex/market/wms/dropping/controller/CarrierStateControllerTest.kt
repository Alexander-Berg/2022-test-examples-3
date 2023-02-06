package ru.yandex.market.wms.dropping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.dropping.HttpAssert

class CarrierStateControllerTest : IntegrationTest() {
    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup("/controller/carrier-state/happy-path/db.xml")
    fun receiveCarrierStateHappyPath() {
        httpAssert.assertApiCallOk(
            MockMvcRequestBuilders.post("/carrier-state/get"),
            "controller/carrier-state/happy-path/request.json",
            "controller/carrier-state/happy-path/response.json",
        )
    }
}
