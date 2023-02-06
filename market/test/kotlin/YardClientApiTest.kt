package ru.yandex.market.logistics.yard.client

import org.apache.commons.io.IOUtils
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import ru.yandex.market.logistics.yard.client.config.YardClientApiTestConfig
import ru.yandex.market.logistics.yard.client.dto.configurator.dto.Capacity
import ru.yandex.market.logistics.yard.client.dto.configurator.dto.Edge
import ru.yandex.market.logistics.yard.client.dto.configurator.dto.Graph
import ru.yandex.market.logistics.yard.client.dto.configurator.dto.PriorityFunction
import ru.yandex.market.logistics.yard.client.dto.configurator.dto.PriorityFunctionParam
import ru.yandex.market.logistics.yard.client.dto.configurator.dto.Restriction
import ru.yandex.market.logistics.yard.client.dto.configurator.dto.RestrictionParam
import ru.yandex.market.logistics.yard.client.dto.configurator.dto.State
import ru.yandex.market.logistics.yard.client.dto.configurator.types.PriorityFunctionParamType
import ru.yandex.market.logistics.yard.client.dto.configurator.types.PriorityFunctionType
import ru.yandex.market.logistics.yard.client.dto.configurator.types.RestrictionParamType
import ru.yandex.market.logistics.yard.client.dto.configurator.types.RestrictionType
import ru.yandex.market.logistics.yard.client.dto.event.read.PushYardClientEventsDto
import ru.yandex.market.logistics.yard.client.dto.event.read.YardClientEventDto
import ru.yandex.market.logistics.yard.client.dto.registration.RegisterYardClientDto
import ru.yandex.market.logistics.yard.client.dto.service.Param
import ru.yandex.market.logistics.yard.client.dto.service.ServiceParamDto
import ru.yandex.market.logistics.yard.client.dto.service.ServiceParamListDto
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [YardClientApiTestConfig::class])
class YardClientApiTest(
    @Value("\${fulfillment-yard.api.host}")
    private val host: String,
    @Autowired
    private val yardClient: YardClientApi,
    @Autowired
    private val mockRestServiceServer: MockRestServiceServer
) {

    private var assertions: SoftAssertions = SoftAssertions()

    @BeforeEach
    fun before() {
        assertions = SoftAssertions()
    }

    @AfterEach
    fun after() {
        assertions.assertAll()
    }

    @Test
    fun registerClient() {

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/client/register"))
            .andExpect(
                MockRestRequestMatchers.content().json(
                    IOUtils.toString(
                        ClassLoader.getSystemResourceAsStream("fixtures/register-client-request.json"),
                        StandardCharsets.UTF_8
                    )
                )
            )
            .andRespond(
                MockRestResponseCreators.withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        IOUtils.toString(
                            ClassLoader.getSystemResourceAsStream("fixtures/register-client-response.json"),
                            StandardCharsets.UTF_8
                        )
                    )
            )

        val response = yardClient.registerClient(
            RegisterYardClientDto(
                "client1",
                10,
                "clientName",
                "+79111111111",
                ZonedDateTime.of(2021, 5, 17, 10, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                ZonedDateTime.of(2021, 5, 17, 11, 0, 0, 0, ZoneId.of("Europe/Moscow"))
            )
        )

        assertions.assertThat(response.id).isEqualTo(1)
        assertions.assertThat(response.externalClientId).isEqualTo("client1")
        assertions.assertThat(response.serviceId).isEqualTo(10)
        assertions.assertThat(response.name).isEqualTo("clientName")
        assertions.assertThat(response.phone).isEqualTo("+79111111111")
        assertions.assertThat(response.stateId).isEqualTo(5)
        assertions.assertThat(response.stateName).isEqualTo("ALLOCATED")
        assertions.assertThat(response.arrivalPlannedDate).isEqualTo(
            ZonedDateTime.of(2021, 5, 17, 10, 0, 0, 0, ZoneId.of("Europe/Moscow"))
        )
    }

    @Test
    fun pushEvent() {

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/event/push"))
            .andExpect(
                MockRestRequestMatchers.content().json(
                    IOUtils.toString(
                        ClassLoader.getSystemResourceAsStream("fixtures/push-event-request.json"),
                        StandardCharsets.UTF_8
                    )
                )
            )
            .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK))

        yardClient.pushEvent(
            PushYardClientEventsDto(
                10, listOf(
                    YardClientEventDto(
                        "client1",
                        "SOME_EVENT",
                        LocalDateTime.of(2021, 5, 17, 10, 0)
                    )
                )
            )
        )
    }

    @Test
    fun updateServicesParams() {

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/services/params"))
            .andExpect(
                MockRestRequestMatchers.content().json(
                    IOUtils.toString(
                        ClassLoader.getSystemResourceAsStream("fixtures/update-services-params.json"),
                        StandardCharsets.UTF_8
                    )
                )
            )
            .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK))

        yardClient.updateServicesParams(
            ServiceParamListDto(
                listOf(
                    ServiceParamDto(
                        1,
                        20,
                        params = listOf(Param("SITE_ID", "value11")),
                        priorityFunctionParams = listOf(Param("SKIP_N_CLIENTS", "10"))
                    ),
                    ServiceParamDto(2, 30, params = listOf(Param("EVENT_TYPE", "value55")))
                )
            )
        )
    }

    @Test
    fun createGraph() {

        mockRestServiceServer.expect(MockRestRequestMatchers.requestTo("$host/graph/create"))
            .andExpect(
                MockRestRequestMatchers.content().json(
                    IOUtils.toString(
                        ClassLoader.getSystemResourceAsStream("fixtures/create-graph-params.json"),
                        StandardCharsets.UTF_8
                    )
                )
            )
            .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK))

        yardClient.createGraph(
            Graph(
                1234,
                "test",
                "REGISTERED",
                listOf(
                    State(
                        "REGISTERED",
                        PriorityFunction(
                            PriorityFunctionType.ARRIVAL_TIME,
                            listOf(PriorityFunctionParam(PriorityFunctionParamType.SKIP_N_CLIENTS, "15"))
                        ),
                        false
                    ),
                    State(
                        "ALLOCATED",
                        PriorityFunction(
                            PriorityFunctionType.ARRIVAL_TIME,
                            listOf(PriorityFunctionParam(PriorityFunctionParamType.SKIP_N_CLIENTS, "15"))
                        ),
                        true
                    )
                ),
                listOf(
                    Edge(
                        "REGISTERED",
                        "ALLOCATED",
                        listOf(
                            Restriction(
                                RestrictionType.CONJUNCTION,
                                listOf(
                                    RestrictionParam(RestrictionParamType.ELEMENTS_IN_ALMOST_EMPTY_QUEUE, "value")
                                )
                            )
                        ),
                        listOf(),
                        1
                    )
                ),
                listOf(
                    Capacity("SC_CAPACITY", listOf("WAITING"), 0, listOf())
                )
            )
        )
    }
}
