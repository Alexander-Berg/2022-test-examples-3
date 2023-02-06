package ru.yandex.market.logistics.mqm.admin.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

class AdminPlanFactProcessorSettingsControllerTest: AbstractContextualTest() {

    @Test
    @DatabaseSetup("/admin/controller/plan_fact_processor_settings_search/before/plan_fact_processor_settings_search.xml")
    fun settingsSearchByIds() {
        val requestBuilder = get("/admin/plan-fact-processor-settings/search")
            .param("planFactProcessorSettingId", "101")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/plan_fact_processor_settings_search/response/plan_fact_processor_settings_search_ids.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/plan_fact_processor_settings_get_settings/before/plan_fact_processor_settings_get_settings.xml")
    fun settingsGetById() {
        val requestBuilder = get("/admin/plan-fact-processor-settings/101")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/plan_fact_processor_settings_get_settings/response/plan_fact_processor_settings_get_settings.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/plan_fact_processor_settings_get_settings/before/plan_fact_processor_settings_get_settings.xml")
    fun settingsGetByIdButNotFound() {
        val requestBuilder = get("/admin/plan-fact-processor-settings/10")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }
}
