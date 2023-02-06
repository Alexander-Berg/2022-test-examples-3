package ru.yandex.market.logistics.mqm.admin.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

class AdminEventHandlerRulesControllerTest: AbstractContextualTest() {

    @Test
    @DatabaseSetup("/admin/controller/event_handler_rules_search/before/event_handler_rules_search.xml")
    fun eventHandlerRulesSearchByIds() {
        val requestBuilder = MockMvcRequestBuilders.get("/admin/event-handler-rules/search")
            .param("eventHandlerRuleId", "101")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/event_handler_rules_search/response/event_handler_rules_search_ids.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/event_handler_rules_search_get_rules/before/event_handler_rules_search_get_rules.xml")
    fun eventHandlerRulesGetById() {
        val requestBuilder = MockMvcRequestBuilders.get("/admin/event-handler-rules/101")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/event_handler_rules_search_get_rules/response/event_handler_rules_search_get_rules.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/event_handler_rules_search_get_rules/before/event_handler_rules_search_get_rules.xml")
    fun eventHandlerRulesGetByIdButNotFound() {
        val requestBuilder = MockMvcRequestBuilders.get("/admin/event-handler-rules/10")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }
}
