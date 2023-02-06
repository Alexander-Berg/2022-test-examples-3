package ru.yandex.market.logistics.mqm.admin.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

class AdminPlanFactsAnalyticsControllerTest: AbstractContextualTest() {

    @Test
    @DatabaseSetup("/admin/controller/plan_facts_analytics_search/before/plan_facts_analytics_search.xml")
    fun planFactsSearchByIds() {
        val requestBuilder = get("/admin/plan-facts-analytics/search")
            .param("planFactId", "1")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/plan_facts_analytics_search/response/plan_facts_analytics_search_ids.json",
                    false
                )
            )
    }
}
