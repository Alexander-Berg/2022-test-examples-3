package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient
import ru.yandex.market.wms.common.spring.servicebus.model.request.PushCarrierStateRequest
import ru.yandex.market.wms.shipping.HttpAssert

class ShipmentControllerTestNotificationInvocation : IntegrationTest() {
    @MockBean(name="servicebusClient")
    @Autowired
    lateinit var servicebusClient: ServicebusClient

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/happy/db/ship-packed-with-notification.xml")
    fun `ship one packed order with notification about new state`(): Unit = run {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-orders"),
            requestFile = "controller/shipping/ship-orders/request/ship-one-order.json"
        )

        Mockito.verify(servicebusClient, Mockito.times(1))
            .pushCarrierState(PushCarrierStateRequest.builder().carrierCode("CARRIER-01").build())
    }
}
