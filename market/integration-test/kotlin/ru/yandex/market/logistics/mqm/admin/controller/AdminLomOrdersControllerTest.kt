package ru.yandex.market.logistics.mqm.admin.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.admin.MqmPlugin
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

class AdminLomOrdersControllerTest: AbstractContextualTest() {
    @Test
    @DatabaseSetup("/admin/controller/lom_orders_search/before/lom_orders_search.xml")
    fun planFactsSearchByIds() {
        val requestBuilder = get("/admin/lom-orders/search")
            .param("orderId", "1")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/lom_orders_search/response/lom_orders_search_ids.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/lom_orders_get_lom_order/before/lom_orders_get_lom_order.xml")
    fun lomOrdersGetById() {
        val requestBuilder = get("/admin/lom-orders/1")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/lom_orders_get_lom_order/response/lom_orders_get_lom_order.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/lom_orders_get_lom_order/before/lom_orders_get_lom_order.xml")
    fun lomOrdersGetByIdButNotFound() {
        val requestBuilder = get("/admin/lom-orders/10")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DatabaseSetup("/admin/controller/lom_orders_waybill/before/lom_orders_waybill.xml")
    fun lomOrdersWaybill() {
        val url = "/admin/lom-orders/${MqmPlugin.SLUG_LOM_ORDERS_WAYBILL}"
        val requestBuilder = get(url)
            .param("orderId", "1")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/lom_orders_waybill/response/lom_orders_waybill.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/lom_orders_waybill/before/lom_orders_waybill.xml")
    fun lomOrdersWaybillButNotFound() {
        val url = "/admin/lom-orders/${MqmPlugin.SLUG_LOM_ORDERS_WAYBILL}"
        val requestBuilder = get(url)
            .param("orderId", "10")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DatabaseSetup("/admin/controller/lom_orders_statuses/before/lom_orders_statuses.xml")
    fun lomOrdersStatuses() {
        val url = "/admin/lom-orders/${MqmPlugin.SLUG_LOM_ORDERS_STATUSES}"
        val requestBuilder = get(url)
            .param("orderId", "1")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/lom_orders_statuses/response/lom_orders_statuses.json",
                    false
                )
            )
    }
}
