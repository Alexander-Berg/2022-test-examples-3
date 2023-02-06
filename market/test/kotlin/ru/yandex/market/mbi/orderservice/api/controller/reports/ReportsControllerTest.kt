package ru.yandex.market.mbi.orderservice.api.controller.reports

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.HttpHeaders
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.api.FunctionalTest
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderSparseIndex
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderLineEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderSparseIndexRepository
import ru.yandex.market.mbi.orderservice.model.CisDto
import ru.yandex.market.mbi.orderservice.model.OrderItemsCisDto
import ru.yandex.market.mbi.orderservice.model.OrderStatus
import ru.yandex.market.mbi.orderservice.model.PagedItemCISResponse
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Тесты для [ReportsController]
 */
@CleanupTables(
    [
        OrderEntity::class,
        OrderLineEntity::class,
        OrderSparseIndex::class,
    ]
)

class ReportsControllerTest : FunctionalTest() {

    @Autowired
    lateinit var orderRepository: OrderEntityRepository

    @Autowired
    lateinit var orderLineRepository: OrderLineEntityRepository

    @Autowired
    lateinit var orderSparseIndexRepository: OrderSparseIndexRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @BeforeEach
    internal fun setUp() {
        this::class.loadTestEntities<OrderEntity>("orders.json").let {
            orderRepository.insertRows(it)
        }
        this::class.loadTestEntities<OrderLineEntity>("orderLines.json").let {
            orderLineRepository.insertRows(it)
        }
        this::class.loadTestEntities<OrderSparseIndex>("sparse_idx.json").let {
            orderSparseIndexRepository.insertRows(it)
        }
    }

    @Test
    fun `verify no error on empty item list`() {
        val response = HttpClientBuilder.create().build().execute(
            prepareListOrderItemCIS(listOf(1),
                "2018-01-01T00:00:00+00:00", "2018-02-01T23:59:59+00:00")
        )

        val actualPagedReturnResponse = objectMapper.readValue(
            response.entity.content,
            PagedItemCISResponse::class.java
        )

        Assertions.assertThat(actualPagedReturnResponse.result?.orderItemsCis).isEmpty()
    }

    @Test
    fun `verify no error on date range query`() {
        val response = HttpClientBuilder.create().build().execute(
            prepareListOrderItemCIS(
                listOf(543900, 543901),
                "2021-12-09T00:00:00+00:00", "2021-12-12T00:00:00+00:00"
            )
        )

        val actualPagedReturnResponse = objectMapper.readValue(
            response.entity.content,
            PagedItemCISResponse::class.java
        )

        Assertions.assertThat(actualPagedReturnResponse.result?.orderItemsCis)
            .isEqualTo(getTestRangeExpected())
    }

    @Test
    fun `verify no error on date range and status query`() {
        val response = HttpClientBuilder.create().build().execute(
            prepareListOrderItemCIS(
                partnerIds = listOf(543900, 543901),
                fromDate = "2021-12-09T00:00:00+00:00",
                toDate = "2021-12-12T00:00:00+00:00",
                statuses = listOf(OrderStatus.DELIVERY)
            )
        )

        val actualPagedReturnResponse = objectMapper.readValue(
            response.entity.content,
            PagedItemCISResponse::class.java
        )

        Assertions.assertThat(actualPagedReturnResponse.result?.orderItemsCis)
            .isEqualTo(getTestRangeExpected(setOf(81545128)))
    }

    @Test
    fun `verify no error on order ids`() {
        val response = HttpClientBuilder.create().build().execute(
            prepareListOrderItemCIS(
                partnerIds = listOf(543900),
                orderIds = listOf(81545128)
            )
        )

        val actualPagedReturnResponse = objectMapper.readValue(
            response.entity.content,
            PagedItemCISResponse::class.java
        )

        Assertions.assertThat(actualPagedReturnResponse.result?.orderItemsCis)
            .isEqualTo(getTestRangeExpected(setOf(81545128)))
    }

    @Test
    fun `verify paging is correct`() {
        var response = HttpClientBuilder.create().build().execute(
            prepareListOrderItemCIS(
                partnerIds = listOf(543900, 543901),
                fromDate = "2021-12-09T00:00:00+00:00",
                toDate = "2021-12-12T00:00:00+00:00",
                size = 1
            )
        )

        var actualPagedReturnResponse = objectMapper.readValue(
            response.entity.content,
            PagedItemCISResponse::class.java
        )

        Assertions.assertThat(actualPagedReturnResponse.result?.orderItemsCis)
            .isEqualTo(getTestRangeExpected(setOf(81545128)))

        Assertions.assertThat(actualPagedReturnResponse.result?.pager?.nextPageToken).isNotNull

        response = HttpClientBuilder.create().build().execute(
            prepareListOrderItemCIS(
                partnerIds = listOf(543900, 543901),
                fromDate = "2021-12-09T00:00:00+00:00",
                toDate = "2021-12-12T00:00:00+00:00",
                size = 1,
                pageToken = actualPagedReturnResponse.result?.pager?.nextPageToken
            )
        )

        actualPagedReturnResponse = objectMapper.readValue(
            response.entity.content,
            PagedItemCISResponse::class.java
        )

        Assertions.assertThat(actualPagedReturnResponse.result?.orderItemsCis)
            .isEqualTo(getTestRangeExpected(setOf(81545127)))

        Assertions.assertThat(actualPagedReturnResponse.result?.pager?.nextPageToken).isNull()
    }

    private fun prepareListOrderItemCIS(
        partnerIds: List<Long>,
        fromDate: String? = null,
        toDate: String? = null,
        orderIds: List<Long>? = null,
        statuses: List<OrderStatus>? = null,
        pageToken: String? = null,
        size: Int? = null
    ): HttpGet {
        val paramMap = hashMapOf<String, String>()
        paramMap["partnerIds"] = partnerIds.joinToString(",")
        fromDate?.let { paramMap.put("dateTimeFrom", it) }
        toDate?.let { paramMap.put("dateTimeTo", it) }
        orderIds?.let { paramMap.put("orderIds", it.joinToString(",")) }
        size?.let { paramMap.put("size", it.toString()) }
        pageToken?.let { paramMap.put("pageToken", it) }
        statuses?.let { paramMap.put("statuses", it.joinToString(",")) }

        val request = HttpGet(getUri("/business/${DEFAULT_BUSINESS_ID}/reports/cis", paramMap))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        return request
    }

    private fun getTestRangeExpected(filterOrderIds: Set<Long>? = null): List<OrderItemsCisDto> {
        return listOf(
            OrderItemsCisDto(
                orderId = 81545128,
                createdAt = LocalDate.parse("2021-12-10"),
                lines = listOf(
                    CisDto(
                        cost = BigDecimal(350000),
                        cis = listOf(
                            "0104603619000087211096798760112",
                            "0104603619000087211096794567186"
                        )
                    )
                )
            ),
            OrderItemsCisDto(
                orderId = 81545127,
                createdAt = LocalDate.parse("2021-12-09"),
                lines = listOf(
                    CisDto(
                        cost = BigDecimal(211000),
                        cis = listOf(
                            "0104603619000087211096790147186",
                            "0104603619000087211096790100112"
                        )
                    )
                )
            )
        ).filter { filterOrderIds?.contains(it.orderId) ?: true }
    }

    private companion object {
        const val DEFAULT_BUSINESS_ID = 1L
    }
}
