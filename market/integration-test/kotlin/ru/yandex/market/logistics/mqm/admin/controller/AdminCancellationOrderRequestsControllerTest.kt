package ru.yandex.market.logistics.mqm.admin.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.admin.MqmPlugin
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

class AdminCancellationOrderRequestsControllerTest: AbstractContextualTest() {

    @Test
    @DatabaseSetup("/admin/controller/cancellation_order_requests_search/before/cancellation_order_requests_search.xml")
    fun searchCancellationOrderRequestsByLomOrder() {
        val url = "/admin/lom-cancellation-order-requests/${MqmPlugin.SLUG_CANCELLATION_ORDER_REQUESTS_LOM_ORDERS}/search"
        val requestBuilder = MockMvcRequestBuilders.get(url)
            .param("orderId", "3")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/cancellation_order_requests_search/response/cancellation_order_requests_search.json",
                    false
                )
            )
    }
}
