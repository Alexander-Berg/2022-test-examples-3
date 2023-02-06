package ru.yandex.market.logistics.mqm.service.ok

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
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
import ru.yandex.market.logistics.mqm.configuration.properties.OkProperties
import ru.yandex.market.logistics.mqm.service.ok.apimodel.Actions
import ru.yandex.market.logistics.mqm.service.ok.apimodel.ApprovementResponse
import ru.yandex.market.logistics.mqm.service.ok.apimodel.Stage
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils
import ru.yandex.market.logistics.util.client.HttpTemplate

@DisplayName("Тесты сервиса для OK")
class OkServiceImplTest : AbstractContextualTest() {
    @Autowired
    lateinit var clientRestTemplate: RestTemplate

    @Autowired
    lateinit var httpTemplate: HttpTemplate

    @Autowired
    @Value("\${ok.url}")
    lateinit var okApiUrl: String

    lateinit var mockServer: MockRestServiceServer

    @Autowired
    lateinit var client: OkClient

    lateinit var service: OkService

    @BeforeEach
    fun setUp() {
        mockServer = MockRestServiceServer.createServer(clientRestTemplate)
        service = OkServiceImpl(client, clock, OkProperties(
            enable = true,
            approvers = listOf("test-user1")
        ))
    }

    @Test
    fun startApprovement() {
        val testRequestUrl = arrayOf(okApiUrl)
            .joinToString(separator = "/")
        mockServer.expect(MockRestRequestMatchers.method(HttpMethod.POST))
            .andExpect(MockRestRequestMatchers.requestTo(testRequestUrl))
            .andExpect(IntegrationTestUtils.jsonRequestContent("service/ok/start_approvement_request.json"))
            .andRespond(
                MockRestResponseCreators.withSuccess()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(IntegrationTestUtils.extractFileContent("service/ok/start_approvement_response.json"))
            )

        var response = ApprovementResponse(
            actions = Actions(
                approve = false,
                close = false,
                edit = false,
                reject = false,
                resume = false,
                suspend = false,
            ),
            author = "ivan-zusik",
            stages = listOf(
                Stage(
                    approvedBy = "test-user1",
                    approvementSource = "",
                    approver = "",
                    id = 1,
                    isApproved = false,
                    isWithDeputies = false,
                    needAll = false,
                    needApprovals = 1,
                    position = 0,
                    stages = emptyList(),
                    status = "IN_PROGRESS",
                )
            ),
            uuid = "8af64cbd-fc3a-4b19-8921-9bf5256d7c34",
            status = "IN_PROGRESS",
            resolution = ""
        )

        val createApprovement = service.createApprovement("TEST-267237")
        assertSoftly {
            createApprovement.toString() shouldBe response.uuid
        }
    }
}
