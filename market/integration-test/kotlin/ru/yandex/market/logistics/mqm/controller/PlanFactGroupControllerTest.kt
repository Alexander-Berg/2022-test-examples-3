package ru.yandex.market.logistics.mqm.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.utils.queue.extractFileContent
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

class PlanFactGroupControllerTest : AbstractContextualTest() {

    @Test
    @DatabaseSetup("/controller/planfactgroup/before/find_group.xml")
    fun findGroup() {
        mockMvc.perform(MockMvcRequestBuilders.get("/plan-fact-group/1"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(IntegrationTestUtils.jsonContent("controller/planfactgroup/response/find_group.json"))
    }

    @Test
    fun findGroupNotFoundError() {
        mockMvc.perform(MockMvcRequestBuilders.get("/plan-fact-group/1"))
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DatabaseSetup("/controller/planfactgroup/before/search_group.xml")
    fun searchGroups() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/plan-fact-group/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/planfactgroup/request/search_group.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(IntegrationTestUtils.jsonContent(
                "controller/planfactgroup/response/search_group.json",
                JSONCompareMode.NON_EXTENSIBLE
            ))
    }

    @Test
    fun searchGroupsFindZero() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/plan-fact-group/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/planfactgroup/request/search_group.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(content().json("[]"))
    }

    @Test
    @DatabaseSetup("/controller/planfactgroup/before/find_plan_facts_by_group.xml")
    fun findPlanFactsByGroup() {
        mockMvc.perform(MockMvcRequestBuilders.get("/plan-fact-group/1/plan-facts"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(IntegrationTestUtils.jsonContent("controller/planfactgroup/response/find_plan_facts_by_group.json"))
    }
}
