package ru.yandex.market.logistics.mqm.admin.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

class AdminQualityRulesControllerTest: AbstractContextualTest() {

    @Test
    @DatabaseSetup("/admin/controller/quality_rules_search/before/quality_rules_search.xml")
    fun qualityRulesSearchByIds() {
        val requestBuilder = get("/admin/quality-rules/search")
            .param("qualityRuleId", "101")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/quality_rules_search/response/quality_rules_search_ids.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/quality_rules_get_quality_rule/before/quality_rules_get_quality_rule.xml")
    fun qualityRulesGetById() {
        val requestBuilder = get("/admin/quality-rules/101")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/quality_rules_get_quality_rule/response/quality_rules_get_quality_rule.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/quality_rules_get_quality_rule/before/quality_rules_get_quality_rule.xml")
    fun qualityRulesGetByIdButNotFound() {
        val requestBuilder = get("/admin/quality-rules/10")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }
}
