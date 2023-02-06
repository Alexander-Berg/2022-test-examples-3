package ru.yandex.market.logistics.ow.mqm

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.client.RestTemplate
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.ow.OwClient
import ru.yandex.market.logistics.mqm.ow.OwClientImpl
import ru.yandex.market.logistics.mqm.ow.OwClientImpl.Companion.WITHOUT_TICKET
import ru.yandex.market.logistics.mqm.ow.dto.CreateCallTicketRequest
import ru.yandex.market.logistics.mqm.ow.dto.CreateCallTicketResponse
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils
import ru.yandex.market.logistics.util.client.HttpTemplate

class OwClientImplTest : AbstractContextualTest() {
    @Autowired
    lateinit var clientRestTemplate: RestTemplate

    @Autowired
    lateinit var httpTemplate: HttpTemplate

    @Autowired
    @Value("\${ow.url}")
    lateinit var owApiUrl: String

    lateinit var mockServer: MockRestServiceServer

    @Autowired
    lateinit var client: OwClient

    @BeforeEach
    fun setUp() {
        mockServer = MockRestServiceServer.createServer(clientRestTemplate)
    }

    @Test
    fun createCallTicket() {
        val testRequestUrl = arrayOf(owApiUrl, *OwClientImpl.CREATE_CALL_TICKET_PATH)
            .joinToString(separator = "/")
        mockServer.expect(MockRestRequestMatchers.method(HttpMethod.POST))
            .andExpect(MockRestRequestMatchers.requestTo(testRequestUrl))
            .andExpect(IntegrationTestUtils.jsonRequestContent("client/ow/create_call_ticket_request.json"))
            .andRespond(
                MockRestResponseCreators.withSuccess()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(IntegrationTestUtils.extractFileContent("client/ow/create_call_ticket_response.json"))
            )

        val testTicket = CreateCallTicketRequest(
            "test_order_id",
            "test_ticket_title",
            "test_ticket_description",
            "test@mail.com",
            "+79681234567"
        )

        assertSoftly {
            client.createCallTicket(testTicket) shouldBe CreateCallTicketResponse("$owApiUrl/entity/ticket@187166624")
        }
    }

    @Test
    fun createCallTicketIfCallDisabled() {
        val testTicket = CreateCallTicketRequest(
            "test_order_id",
            "test_ticket_title",
            "test_ticket_description",
            "test@mail.com",
            "+79681234567"
        )
        val clientWithDisabledCall = OwClientImpl(
            httpTemplate,
            false,
            "http://test"
        );

        assertSoftly {
            clientWithDisabledCall.createCallTicket(testTicket) shouldBe CreateCallTicketResponse(WITHOUT_TICKET)
        }
    }
}
