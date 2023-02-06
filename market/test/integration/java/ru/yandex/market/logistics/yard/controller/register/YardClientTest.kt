package ru.yandex.market.logistics.yard.controller.register

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.util.FileContentUtils.getFileContent

class YardClientTest : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup("classpath:fixtures/controller/client-register/setup.xml")
    @ExpectedDatabase("classpath:fixtures/controller/client-register/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun register() {
        mockMvc.perform(MockMvcRequestBuilders.post("/client/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("classpath:fixtures/controller/client-register/1/request.json")))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(
                getFileContent("classpath:fixtures/controller/client-register/1/result.json")))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/client-register/setup.xml")
    @ExpectedDatabase("classpath:fixtures/controller/client-register/2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun registerWithPlannedDate() {
        mockMvc.perform(MockMvcRequestBuilders.post("/client/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("classpath:fixtures/controller/client-register/2/request.json")))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(
                getFileContent("classpath:fixtures/controller/client-register/2/result.json")))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/client-register/3/setup.xml")
    @ExpectedDatabase("classpath:fixtures/controller/client-register/3/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun registerIdempotence() {
        mockMvc.perform(MockMvcRequestBuilders.post("/client/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("classpath:fixtures/controller/client-register/3/request.json")))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(
                getFileContent("classpath:fixtures/controller/client-register/3/result.json")))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/client-register/setup.xml")
    @ExpectedDatabase("classpath:fixtures/controller/client-register/4/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun registerWithPlannedSlot() {
        mockMvc.perform(MockMvcRequestBuilders.post("/client/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("classpath:fixtures/controller/client-register/4/request.json")))
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(MockMvcResultMatchers.content().json(
                getFileContent("classpath:fixtures/controller/client-register/4/result.json")))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/client-register/setup.xml")
    @ExpectedDatabase("classpath:fixtures/controller/client-register/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun registerWithInvalidPlannedSlot() {
        mockMvc.perform(MockMvcRequestBuilders.post("/client/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("classpath:fixtures/controller/client-register/5/request.json")))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.content()
                .string("{\"message\":\"Slot left border should not be greater than slot right border\"}"))
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/client-register/1/after.xml")
    fun getUserInfo() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/client")
                .param("clientId", "clientExternalId")
                .param("serviceId", "1")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    getFileContent("classpath:fixtures/controller/client-register/1/result.json")
                )
            )
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/client-register/6/before.xml")
    fun getUserInfoByTicket() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/client/ticket-code")
                .param("ticketCode", "1234")
                .param("siteId", "100")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    getFileContent("classpath:fixtures/controller/client-register/6/result.json")
                )
            )
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/client-register/6/before.xml")
    fun getUserInfoByTicketUuid() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/client/uuid/ticket-code")
                .param("ticketCode", "1234")
                .param("siteUuid", "f1a82833-bb39-46df-b9cc-a2ec2d257a39")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    getFileContent("classpath:fixtures/controller/client-register/6/result.json")
                )
            )
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/client-register/6/before.xml")
    fun getUserInfoByTicketNotFound() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/client/ticket-code")
                .param("ticketCode", "1234")
                .param("siteId", "1")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    getFileContent("classpath:fixtures/controller/client-register/6/result_error.json")
                )
            )
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/client-register/suggest-license-plates/before.xml")
    fun testSuggestLicensePlates() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/client/suggest-license-plates")
                .param("phone", "+79123456789")
                .param("licensePlatePattern", "А")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    getFileContent("classpath:fixtures/controller/client-register/suggest-license-plates/response.json")
                )
            )
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/client-register/suggest-license-plates/before.xml")
    fun testSuggestLicensePlatesNotFound() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/client/suggest-license-plates")
                .param("phone", "+79878546217")
                .param("licensePlatePattern", "B")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    "[]"
                )
            )
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/client-register/suggest-license-plates/before.xml")
    fun testSuggestLicensePlatesBadRequest() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/client/suggest-license-plates")
                .param("phone", "+79878546217")
                .param("licensePlatePattern", "\t")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/controller/client-register/cancel/before.xml")
    @ExpectedDatabase("classpath:fixtures/controller/client-register/cancel/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun testCancel() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/client/cancel")
                .param("clientIds", "12,13")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
    }

    @Test
    fun testCancelBadRequests() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/client/cancel")
                .param("clientIds", "")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

}
