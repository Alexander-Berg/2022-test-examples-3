package ru.yandex.market.logistics.mqm.controller

import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.utils.queue.extractFileContent

class MonitoringEventControllerTest : AbstractContextualTest() {

    @Test
    @ExpectedDatabase(
        value = "/controller/monitoringevent/after/push_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun pushEventTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/monitoring-event/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/monitoringevent/request/push_event.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/monitoringevent/after/push_event_complex_payload.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun pushEventWithComplexPayloadTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/monitoring-event/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/monitoringevent/request/push_event_complex_payload.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun pushEventWithoutTypeTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/monitoring-event/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/monitoringevent/request/push_event_without_type.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun pushEventWithoutPayloadTypeTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/monitoring-event/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/monitoringevent/request/push_event_without_payload_type.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun pushEventWrongEventTypeTest() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/monitoring-event/push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/monitoringevent/request/push_event_wrong_type.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }
}
