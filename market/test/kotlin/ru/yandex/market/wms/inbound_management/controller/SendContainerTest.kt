package ru.yandex.market.wms.inbound_management.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import org.junit.jupiter.api.TestInstance
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.servlet.client.MockMvcWebTestClient
import org.springframework.web.context.WebApplicationContext
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.inbound_management.config.CoreDBClient
import ru.yandex.market.wms.inbound_management.config.TransportationDBClient
import ru.yandex.market.wms.inbound_management.model.dto.ContainerId
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@ContextConfiguration(classes = [CoreDBClient::class, TransportationDBClient::class])
@DatabaseSetups(
    DatabaseSetup("/db/loc/locations.xml"),
    DatabaseSetup("/db/container/pl.xml"),
)
class SendContainerTest(context: WebApplicationContext) : IntegrationTest() {
    private val client = MockMvcWebTestClient.bindToApplicationContext(context).build()

    @Test
    fun sendSuccessfully() {
        client.post()
            .uri("/priorities/send-container")
            .bodyValue(ContainerId(containerId = "PLT001"))
            .exchange()
            .expectStatus().isOk
            .expectBody<ContainerResponse>().isEqualTo(ContainerResponse(container = "PLT001"))
    }

    @Test
    fun sendNotExistingContainer() {
        client.post()
            .uri("/priorities/send-container")
            .bodyValue(ContainerId(containerId = "INCORRECT_CONTAINER"))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<ContainerResponse>().isEqualTo(
                ContainerResponse(
                    container = "",
                    wmsErrorCode = "NO_AVAILABLE_CONTAINERS",
                )
            )
    }

    @Test
    fun sendFromIncorrectLocation() {
        client.post()
            .uri("/priorities/send-container")
            .bodyValue(ContainerId(containerId = "PLT002"))
            .exchange()
            .expectStatus().isBadRequest
            .expectBody<ContainerResponse>().isEqualTo(
                ContainerResponse(
                    container = "",
                    wmsErrorCode = "WRONG_SOURCE_LOCATION",
                    wmsErrorData = mapOf("location" to "STAGE_A1")
                )
            )
    }
}
