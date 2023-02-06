package ru.yandex.market.logistics.mqm.admin.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.admin.MqmPlugin
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

class AdminLomOrderCombinatorRoutesControllerTest: AbstractContextualTest() {

    @Test
    @DatabaseSetup("/admin/controller/routes_services_lom_order_search/before/routes_services_lom_order_search.xml")
    fun routesSearchServicesByLomOrder() {
        val url = "/admin/lom-order-combinator-routes/${MqmPlugin.SLUG_LOM_ORDER_COMBINATOR_ROUTES_LOM_ORDERS}/search"
        val requestBuilder = get(url)
            .param("orderId", "1")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/routes_services_lom_order_search/response/routes_services_lom_order_search.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/routes_search/before/routes_search.xml")
    fun routesSearchByOrderIds() {
        val requestBuilder = get("/admin/lom-order-combinator-routes/search")
            .param("orderId", "1")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/routes_search/response/routes_search.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/routes_search_get_route/before/routes_search_get_route.xml")
    fun lomOrderCombinatorRouteGetById() {
        val requestBuilder = get("/admin/lom-order-combinator-routes/1")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/routes_search_get_route/response/routes_search_get_route.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/routes_search_get_route/before/routes_search_get_route.xml")
    fun lomOrderCombinatorRouteGetByIdButNotFound() {
        val requestBuilder = get("/admin/lom-order-combinator-routes/10")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }
}
