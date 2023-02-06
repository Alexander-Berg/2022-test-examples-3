package ru.yandex.market.logistics.yard.controller.`event-push`

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.util.FileContentUtils

class PushYardClientEventsTest : AbstractSecurityMockedContextualTest() {


    @Test
    @DatabaseSetup("classpath:fixtures/controller/event-push/1/before.xml")
    @ExpectedDatabase("classpath:fixtures/controller/event-push/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun pushEvent() {
        mockMvc.perform(MockMvcRequestBuilders.post("/event/push")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("classpath:fixtures/controller/event-push/1/request.json")))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/event-push/1/before.xml")
    @ExpectedDatabase("classpath:fixtures/controller/event-push/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun pushEventForYardClient() {
        mockMvc.perform(MockMvcRequestBuilders.post("/event/push")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("classpath:fixtures/controller/event-push/1/request_cli.json")))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/event-push/6/before.xml")
    @ExpectedDatabase("classpath:fixtures/controller/event-push/6/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun pushEventWithTicketCode() {
        mockMvc.perform(MockMvcRequestBuilders.post("/event/push")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("classpath:fixtures/controller/event-push/6/request.json")))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/event-push/7/before.xml")
    @ExpectedDatabase("classpath:fixtures/controller/event-push/7/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun pushEventWithUnknownTicketCode() {
        mockMvc.perform(MockMvcRequestBuilders.post("/event/push")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("classpath:fixtures/controller/event-push/7/request.json")))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    fun pushNullServiceId() {
        mockMvc.perform(MockMvcRequestBuilders.post("/event/push")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("classpath:fixtures/controller/event-push/2/request.json")))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(content().json(
                FileContentUtils.getFileContent("classpath:fixtures/controller/event-push/2/result.json")))
    }

    @Test
    fun pushEmptyEvents() {
        mockMvc.perform(MockMvcRequestBuilders.post("/event/push")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("classpath:fixtures/controller/event-push/3/request.json")))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(content().json(
                FileContentUtils.getFileContent("classpath:fixtures/controller/event-push/3/result.json")))
    }

    @Test
    fun pushBadServiceId() {
        mockMvc.perform(MockMvcRequestBuilders.post("/event/push")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("classpath:fixtures/controller/event-push/4/request.json")))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun pushBadExternalId() {
        mockMvc.perform(MockMvcRequestBuilders.post("/event/push")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("classpath:fixtures/controller/event-push/5/request.json")))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }
}
