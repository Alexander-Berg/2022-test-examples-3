package ru.yandex.market.logistics.mqm.admin.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

class AdminMonitoringEventProcessorsControllerTest: AbstractContextualTest() {

    @Test
    @DatabaseSetup("/admin/controller/monitoring_event_processors_search/before/monitoring_event_processors_search.xml")
    fun searchByIds() {
        val requestBuilder = MockMvcRequestBuilders.get("/admin/monitoring-event-processors/search")
            .param("monitoringEventProcessorId", "101")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/monitoring_event_processors_search/response/monitoring_event_processors_search_ids.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/monitoring_event_processors_get_processor/before/monitoring_event_processors_get_processor.xml")
    fun getById() {
        val requestBuilder = MockMvcRequestBuilders.get("/admin/monitoring-event-processors/101")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/monitoring_event_processors_get_processor/response/monitoring_event_processors_get_processor.json",
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/admin/controller/monitoring_event_processors_get_processor/before/monitoring_event_processors_get_processor.xml")
    fun getByIdButNotFound() {
        val requestBuilder = MockMvcRequestBuilders.get("/admin/monitoring-event-processors/1")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }
}
