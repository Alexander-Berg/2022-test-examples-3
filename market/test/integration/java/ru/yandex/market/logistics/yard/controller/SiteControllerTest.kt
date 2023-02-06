package ru.yandex.market.logistics.yard.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.util.FileContentUtils

class SiteControllerTest : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup("classpath:fixtures/controller/site-queue/before.xml")
    fun testGetTicketsLineBoard() {
        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/site-queue/response.json"
        )
        mockMvc.perform(
            MockMvcRequestBuilders.get("/site/100/queue")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.content().json(expected))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/site-queue/before_multi.xml")
    fun testGetTicketsLineBoardMultiSite() {
        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/site-queue/response_multi.json"
        )
        mockMvc.perform(
            MockMvcRequestBuilders.get("/site/queue/100,101")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.content().json(expected))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/site-queue/before.xml")
    fun testGetTicketsLineBoardConcreteStates() {
        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/site-queue/response_concrete.json"
        )
        mockMvc.perform(
            MockMvcRequestBuilders.get("/site/100/queue")
                .param("states", "RECALC")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.content().json(expected))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/site-queue/before.xml")
    fun testGetTicketsLineBoardConcreteStatesNotFound() {
        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/site-queue/response_empty.json"
        )
        mockMvc.perform(
            MockMvcRequestBuilders.get("/site/100/queue")
                .param("states", "UNKNOWN")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.content().json(expected))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/site-queue/before_multi.xml")
    fun testGetSiteTickets() {
        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/site-queue/response_site_tickets.json"
        )
        mockMvc.perform(
            MockMvcRequestBuilders.get("/site/100,101/ticket")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.content().json(expected))
    }

    @Test
    fun getAllowedRequestTypesOnEmptyDb() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/site/100/allowed-request-types")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.content().json("{\"requestTypes\":[]}"))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/site-queue/before_allowed_request_types.xml")
    fun getAllowedRequestTypes() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/site/1130/allowed-request-types")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(
                MockMvcResultMatchers.content()
                    .json("{\"requestTypes\":[\"LOADING\",\"SHIPMENT\",\"SIGNING_DOCUMENTS\"]}")
            )
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/site-queue/before.xml")
    fun getSite() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/site/100")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(
                MockMvcResultMatchers.content()
                    .json("{\"id\":100,\"name\":\"capacity\",\"value\":1,\"capacityUnits\":[],\"registrationUrl\":null,\"serviceType\":\"SORTING_CENTER\",\"params\":[{\"key\":\"first\",\"value\":\"first\"}]}")
            )
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/site-queue/get-by-pass/before.xml")
    @ExpectedDatabase(
        value = "classpath:fixtures/controller/site-queue/get-by-pass/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testGetTicketByPassNumber() {
        val siteUUID = "0076e38f-8052-4686-af0b-e3425cc58414"
        mockMvc.perform(
            MockMvcRequestBuilders.post("/site/uuid/$siteUUID/get-ticket-by-passnumber")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"passNumber\":\"121213\"}")

        )
            .andExpect(
                MockMvcResultMatchers.content()
                    .json("{\"ticketNumber\":\"2000\"}")
            )
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/site-queue/site-uuid-queue/before.xml")
    fun testGetTicketsUUIDLineBoardMultiSite() {
        val expected = FileContentUtils.getFileContent(
            "classpath:fixtures/controller/site-queue/site-uuid-queue/response.json"
        )
        mockMvc.perform(
            MockMvcRequestBuilders.get("/site/uuid/queue/0076e38f-8052-4686-af0b-e3425cc58414," +
                "0076e38f-8052-4686-af0b-e3425cc58415")
                .param("states", "WAITING")
        )
            .andExpect(MockMvcResultMatchers.content().json(expected))
    }

}
