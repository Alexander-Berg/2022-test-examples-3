package ru.yandex.market.logistics.mqm.admin.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

class AdminPlanFactsGroupsControllerTest: AbstractContextualTest() {

    @Test
    @DatabaseSetup("/admin/controller/plan_facts_groups_search/before/plan_facts_groups_search.xml")
    fun planFactsGroupsSearchByIds() {
        val requestBuilder = get("/admin/plan-facts-groups/search")
            .param("planFactGroupId", "1001")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/plan_facts_groups_search/response/plan_facts_groups_search_ids.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/plan_facts_groups_get_plan_fact/before/plan_facts_groups_get_plan_fact.xml")
    fun planFactsGroupsGetById() {
        val requestBuilder = get("/admin/plan-facts-groups/1001")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/plan_facts_groups_get_plan_fact/response/plan_facts_groups_get_plan_fact.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/plan_facts_groups_get_plan_fact/before/plan_facts_groups_get_plan_fact.xml")
    fun planFactsGroupsGetByIdButNotFound() {
        val requestBuilder = get("/admin/plan-facts-groups/10")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }
}
