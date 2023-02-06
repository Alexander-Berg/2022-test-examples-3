package ru.yandex.market.wms.inbound_management.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.arrayContainingInAnyOrder
import org.junit.jupiter.api.TestInstance
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.reactive.server.body
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.servlet.client.MockMvcWebTestClient
import org.springframework.web.context.WebApplicationContext
import reactor.core.publisher.Mono
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.dao.entity.SkuId
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.inbound_management.config.CoreDBClient
import ru.yandex.market.wms.inbound_management.controller.dto.ReceiptPriorityResponse
import ru.yandex.market.wms.inbound_management.controller.dto.SkuToOos
import ru.yandex.market.wms.inbound_management.controller.dto.SkuToOosUpsertResponse
import ru.yandex.market.wms.inbound_management.entity.Container
import ru.yandex.market.wms.inbound_management.entity.ReceiptPriorityContainers
import ru.yandex.market.wms.inbound_management.model.dto.ReceiptPriorityOrder
import java.math.BigDecimal
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@ContextConfiguration(classes = [CoreDBClient::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PriorityControllerTest(context: WebApplicationContext) : IntegrationTest() {
    private val webTestClient = MockMvcWebTestClient.bindToApplicationContext(context).build()

    @Test
    fun getEmptyPriorityList() {
        webTestClient.get()
            .uri("/priorities")
            .exchange()
            .expectStatus().isOk
            .expectBody<ReceiptPriorityResponse>()
            .isEqualTo(
                ReceiptPriorityResponse(
                    limit = 20,
                    offset = 0,
                    total = 0,
                    content = listOf()
                )
            )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/5-rows.xml"),
        DatabaseSetup("/db/loc/locations.xml"),
        DatabaseSetup("/db/receiptDetail/only-PL.xml"),
    )
    fun getPriorityList() {
        val response = webTestClient.get()
            .uri("/priorities")
            .exchange()
            .expectStatus().isOk
            .expectBody<ReceiptPriorityResponse>()
            .returnResult()
            .responseBody
        assertNotNull(response)
        assertEquals(20, response.limit)
        assertEquals(0, response.offset)
        assertEquals(4, response.total)
        assertContentEquals(
            listOf(
                ReceiptPriorityContainers(
                    receiptKey = "0000000004",
                    priorityCoeff = BigDecimal.valueOf(42.0001),
                    containers = listOf(Container(id = "PLT007", location = "BUF_B3", zone = "IN_BUF_B")),
                ),
                ReceiptPriorityContainers(
                    receiptKey = "0000000001",
                    priorityCoeff = BigDecimal.valueOf(8.3547),
                    containers = listOf(Container(id = "PLT001", location = "BUF_A1", zone = "IN_BUF_A")),
                ),
                ReceiptPriorityContainers(
                    receiptKey = "0000000002",
                    priorityCoeff = BigDecimal.valueOf(1.0022),
                    containers = listOf(
                        Container(id = "PLT002", location = "BUF_C1", zone = "IN_BUF_C"),
                        Container(id = "PLT003", location = "BUF_B1", zone = "IN_BUF_B"),
                    ),
                ),
                ReceiptPriorityContainers(
                    receiptKey = "0000000003",
                    priorityCoeff = BigDecimal.valueOf(0.0314),
                    containers = listOf(
                        Container(id = "PLT004", location = "BUF_A1", zone = "IN_BUF_A"),
                        Container(id = "PLT005", location = "BUF_B2", zone = "IN_BUF_B"),
                        Container(id = "PLT006", location = "BUF_C2", zone = "IN_BUF_C"),
                    ),
                ),
            ),
            response.content
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/5-rows.xml"),
        DatabaseSetup("/db/loc/locations.xml"),
        DatabaseSetup("/db/receiptDetail/only-PL.xml"),
    )
    fun getReceiptPriorityPosition() {
        val response = webTestClient.get()
            .uri("/priorities/receipt/0000000001")
            .exchange()
            .expectStatus().isOk
            .expectBody<PositionOfReceiptByCoeffResponse>()
            .returnResult()
            .responseBody
        webTestClient.get()
            .uri("/priorities/receipt/0")
            .exchange()
            .expectStatus().isNotFound
        assertNotNull(response)
        assertEquals(
            PositionOfReceiptByCoeffResponse(
                receiptPriorityOrder = Optional.of(
                    ReceiptPriorityOrder(
                        receiptKey = "0000000001",
                        priorityCoeff = BigDecimal.valueOf(83547, 4),
                        order = 1,
                    )
                ),
            ),
            response
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/5-rows.xml"),
        DatabaseSetup("/db/loc/locations.xml"),
        DatabaseSetup("/db/receiptDetail/only-PL.xml"),
    )
    fun getFilteredPriorityList() {
        val response = webTestClient.get()
            .uri("/priorities?filter=(receiptKey=='0000000003')")
            .exchange()
            .expectStatus().isOk
            .expectBody<ReceiptPriorityResponse>()
            .returnResult()
            .responseBody
        assertNotNull(response)
        assertEquals(20, response.limit)
        assertEquals(0, response.offset)
        assertEquals(1, response.total)
        assertContentEquals(
            listOf(
                ReceiptPriorityContainers(
                    receiptKey = "0000000003",
                    priorityCoeff = BigDecimal.valueOf(0.0314),
                    containers = listOf(
                        Container(id = "PLT004", location = "BUF_A1", zone = "IN_BUF_A"),
                        Container(id = "PLT005", location = "BUF_B2", zone = "IN_BUF_B"),
                        Container(id = "PLT006", location = "BUF_C2", zone = "IN_BUF_C"),
                    ),
                ),
            ),
            response.content
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/5-rows.xml"),
        DatabaseSetup("/db/loc/locations.xml"),
        DatabaseSetup("/db/receiptDetail/only-PL.xml"),
    )
    fun getSortedLimitPriorityList() {
        val response = webTestClient.get()
            .uri("/priorities?sort=receiptKey&order=ASC&limit=2&offset=1")
            .exchange()
            .expectStatus().isOk
            .expectBody<ReceiptPriorityResponse>()
            .returnResult()
            .responseBody
        assertNotNull(response)
        assertEquals(2, response.limit)
        assertEquals(1, response.offset)
        assertEquals(4, response.total)
        assertContentEquals(
            listOf(
                ReceiptPriorityContainers(
                    receiptKey = "0000000002",
                    priorityCoeff = BigDecimal.valueOf(1.0022),
                    containers = listOf(
                        Container(id = "PLT002", location = "BUF_C1", zone = "IN_BUF_C"),
                        Container(id = "PLT003", location = "BUF_B1", zone = "IN_BUF_B"),
                    ),
                ),
                ReceiptPriorityContainers(
                    receiptKey = "0000000003",
                    priorityCoeff = BigDecimal.valueOf(0.0314),
                    containers = listOf(
                        Container(id = "PLT004", location = "BUF_A1", zone = "IN_BUF_A"),
                        Container(id = "PLT005", location = "BUF_B2", zone = "IN_BUF_B"),
                        Container(id = "PLT006", location = "BUF_C2", zone = "IN_BUF_C"),
                    ),
                ),
            ),
            response.content
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/5-rows.xml"),
        DatabaseSetup("/db/loc/locations.xml"),
        DatabaseSetup("/db/receiptDetail/only-PL.xml"),
    )
    fun getFilteredByZonePriorityList() {
        val response = webTestClient.get()
            .uri("/priorities?filter=(zone=='IN_BUF_B')&sort=priorityCoeff&order=ASC")
            .exchange()
            .expectStatus().isOk
            .expectBody<ReceiptPriorityResponse>()
            .returnResult()
            .responseBody
        assertNotNull(response)
        assertEquals(20, response.limit)
        assertEquals(0, response.offset)
        assertEquals(3, response.total)
        assertContentEquals(
            listOf(
                ReceiptPriorityContainers(
                    receiptKey = "0000000003",
                    priorityCoeff = BigDecimal.valueOf(0.0314),
                    containers = listOf(Container(id = "PLT005", location = "BUF_B2", zone = "IN_BUF_B")),
                ),
                ReceiptPriorityContainers(
                    receiptKey = "0000000002",
                    priorityCoeff = BigDecimal.valueOf(1.0022),
                    containers = listOf(Container(id = "PLT003", location = "BUF_B1", zone = "IN_BUF_B")),
                ),
                ReceiptPriorityContainers(
                    receiptKey = "0000000004",
                    priorityCoeff = BigDecimal.valueOf(42.0001),
                    containers = listOf(Container(id = "PLT007", location = "BUF_B3", zone = "IN_BUF_B")),
                ),
            ),
            response.content
        )
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/receipts-to-priorities/5-rows.xml"),
        DatabaseSetup("/db/loc/locations.xml"),
        DatabaseSetup("/db/receiptDetail/only-PL.xml"),
    )
    fun getMultiFilteredPriorityList() {
        val response = webTestClient.get()
            .uri("/priorities?filter=((priorityCoeff>=42,receiptKey=='0000000003');zone=in=('IN_BUF_B','IN_BUF_C'))")
            .exchange()
            .expectStatus().isOk
            .expectBody<ReceiptPriorityResponse>()
            .returnResult()
            .responseBody
        assertNotNull(response)
        assertEquals(20, response.limit)
        assertEquals(0, response.offset)
        assertEquals(2, response.total)
        assertContentEquals(
            listOf(
                ReceiptPriorityContainers(
                    receiptKey = "0000000004",
                    priorityCoeff = BigDecimal.valueOf(42.0001),
                    containers = listOf(Container(id = "PLT007", location = "BUF_B3", zone = "IN_BUF_B")),
                ),
                ReceiptPriorityContainers(
                    receiptKey = "0000000003",
                    priorityCoeff = BigDecimal.valueOf(0.0314),
                    containers = listOf(
                        Container(id = "PLT005", location = "BUF_B2", zone = "IN_BUF_B"),
                        Container(id = "PLT006", location = "BUF_C2", zone = "IN_BUF_C"),
                    ),
                ),
            ),
            response.content
        )
    }

    @Test
    @DatabaseSetup("/db/sku-to-oos/controller/before.xml")
    @ExpectedDatabase(value = "/db/sku-to-oos/controller/after-upsert.xml", assertionMode = NON_STRICT_UNORDERED)
    fun upsertSkuToOosMetrics() {
        val content = FileContentUtils.getFileContent("./db/sku-to-oos/controller/upsert-request.json")
        val body: List<SkuToOos> = jacksonObjectMapper().readValue(content)
        val response = webTestClient.put()
            .uri("/metrics/upsert")
            .contentType(MediaType.APPLICATION_JSON)
            .body<List<SkuToOos>>(Mono.just(body))
            .exchange()
            .expectStatus().isOk
            .expectBody<SkuToOosUpsertResponse>()
            .returnResult()
            .responseBody
        assertNotNull(response)

        assertThat(
            response.processed.toTypedArray(),
            arrayContainingInAnyOrder(
                SkuId("942665", "ROV0000000000000000359"),
                SkuId("546763", "ROV0000000000000000360"),
                SkuId("546019", "ROV0000000000000000361")
            )
        )
    }
}
