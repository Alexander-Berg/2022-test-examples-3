package ru.yandex.market.mbi.orderservice.api.controller.returns

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.HttpHeaders
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track
import ru.yandex.market.checkout.checkouter.returns.PagedReturns
import ru.yandex.market.checkout.checkouter.returns.Return
import ru.yandex.market.checkout.checkouter.returns.ReturnDecisionType
import ru.yandex.market.checkout.checkouter.returns.ReturnDelivery
import ru.yandex.market.checkout.checkouter.returns.ReturnItem
import ru.yandex.market.checkout.checkouter.returns.ReturnSubreason
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.api.FunctionalTest
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.ReturnLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.checkouter.CheckouterReturnEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.checkouter.CheckouterReturnLineEntity
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.CheckouterReturnLineRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.CheckouterReturnRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderLineEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.ReturnLineRepository
import ru.yandex.market.mbi.orderservice.common.service.external.CheckouterApiService
import ru.yandex.market.mbi.orderservice.common.util.DEFAULT_TIMEZONE_ZONE_ID
import ru.yandex.market.mbi.orderservice.common.util.bd
import ru.yandex.market.mbi.orderservice.common.util.toInstantAtMoscowTime
import ru.yandex.market.mbi.orderservice.model.*
import java.math.BigDecimal
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType as CheckouterReturnReasonType
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus as CheckouterReturnStatus

/**
 * Тесты для [ReturnsController]
 */
@CleanupTables(
    [
        OrderEntity::class,
        OrderLineEntity::class,
        ReturnLineEntity::class,
        CheckouterReturnEntity::class,
        CheckouterReturnLineEntity::class,
    ]
)
class ReturnControllerTest : FunctionalTest() {

    @Autowired
    lateinit var checkouterReturnRepository: CheckouterReturnRepository

    @Autowired
    lateinit var checkouterReturnLineRepository: CheckouterReturnLineRepository

    @Autowired
    lateinit var returnLineRepository: ReturnLineRepository

    @Autowired
    lateinit var orderLineRepository: OrderLineEntityRepository

    @Autowired
    lateinit var orderRepository: OrderEntityRepository

    @Autowired
    lateinit var mockCheckouterApiService: CheckouterApiService

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @BeforeEach
    internal fun setUp() {
        this::class.loadTestEntities<CheckouterReturnEntity>("checkouterReturns.json").let {
            checkouterReturnRepository.insertRows(it)
        }
        this::class.loadTestEntities<CheckouterReturnLineEntity>("checkouterReturnLines.json").let {
            checkouterReturnLineRepository.insertRows(it)
        }
        this::class.loadTestEntities<OrderLineEntity>("orderLines.json").let {
            orderLineRepository.insertRows(it)
        }
        this::class.loadTestEntities<ReturnLineEntity>("returnLines.json").let {
            returnLineRepository.insertRows(it)
        }
        this::class.loadTestEntities<OrderEntity>("orders.json").let {
            orderRepository.insertRows(it)
        }
    }

    @Test
    fun `verify get partner returns without filters`() {
        val partnerId = 111L
        val response = HttpClientBuilder.create().build().execute(prepareListPartnerReturnsRequest(partnerId))
        val actualPagedReturnResponse = objectMapper.readValue(
            response.entity.content,
            PagedReturnResponse::class.java
        )
        val expectedPagedReturnsResponse = PagedReturnsResponse(
            orderReturns = listOf(
                return1(),
                return2()
            ),
            pager = PagerWithToken(
                pageSize = 20
            )
        )

        assertEquals(expectedPagedReturnsResponse, actualPagedReturnResponse.result)
    }

    @Test
    fun `verify no error on empty returns list`() {
        val partnerId = 222L
        val response = HttpClientBuilder.create().build().execute(
            prepareListPartnerReturnsRequest(
                partnerId,
                fromDate = "2018-01-01T00:00:00Z",
                toDate = "2019-03-01T00:00:00Z"
            )
        )
        val actualPagedReturnResponse = objectMapper.readValue(
            response.entity.content,
            PagedReturnResponse::class.java
        )

        assertThat(actualPagedReturnResponse.result?.orderReturns).isEmpty()
    }

    @Test
    fun `verify get partner returns with date filter`() {
        val partnerId = 222L
        val response = HttpClientBuilder.create().build().execute(
            prepareListPartnerReturnsRequest(
                partnerId,
                fromDate = "2020-01-01T00:00:00Z",
                toDate = "2020-03-01T00:00:00Z"
            )
        )
        val actualPagedReturnResponse = objectMapper.readValue(
            response.entity.content,
            PagedReturnResponse::class.java
        )
        val expectedPagedReturnsResponse = PagedReturnsResponse(
            pager = PagerWithToken(pageSize = 20, nextPageToken = null),
            orderReturns = listOf(
                return20(),
                return21()
            )
        )

        assertEquals(expectedPagedReturnsResponse, actualPagedReturnResponse.result)
    }

    @Test
    fun `verify get partner returns with type filter`() {
        val partnerId = 111L
        val response = HttpClientBuilder.create().build().execute(
            prepareListPartnerReturnsRequest(
                partnerId = partnerId,
                type = ReturnTypeDTO.UNREDEEMED
            )
        )
        val actualPagedReturnResponse = objectMapper.readValue(
            response.entity.content,
            PagedReturnResponse::class.java
        )
        val expectedPagedReturnsResponse = PagedReturnsResponse(
            orderReturns = listOf(
                return2()
            ),
            pager = PagerWithToken(
                pageSize = 20
            )
        )
    }

    @Test
    fun `verify get partner returns with date filter on first page`() {
        val partnerId = 222L
        val response = HttpClientBuilder.create().build().execute(
            prepareListPartnerReturnsRequest(
                partnerId,
                fromDate = "2020-01-01T00:00:00Z",
                toDate = "2020-03-01T00:00:00Z",
                pageSize = 1
            )
        )
        val actualPagedReturnResponse = objectMapper.readValue(
            response.entity.content,
            PagedReturnResponse::class.java
        )
        val expectedPagedReturnsResponse = PagedReturnsResponse(
            pager = PagerWithToken(
                pageSize = 1,
                nextPageToken = "eyJsYXN0Q29tcGxleElkIjoyMjU0ODU3ODMwNCwic29ydE9yZGVyIjoiQVNDIn0="
            ),
            orderReturns = listOf(
                return20()
            )
        )

        assertEquals(expectedPagedReturnsResponse, actualPagedReturnResponse.result)
    }

    @Test
    fun `verify get partner returns with date filter on first page with descend sort`() {
        val partnerId = 222L
        val response = HttpClientBuilder.create().build().execute(
            prepareListPartnerReturnsRequest(
                partnerId,
                fromDate = "2020-01-01T00:00:00Z",
                toDate = "2020-03-01T00:00:00Z",
                pageSize = 1,
                sortOrder = SortOrder.DESC
            )
        )
        val actualPagedReturnResponse = objectMapper.readValue(
            response.entity.content,

            PagedReturnResponse::class.java
        )
        val expectedPagedReturnsResponse = PagedReturnsResponse(
            pager = PagerWithToken(
                pageSize = 1,
                nextPageToken = "eyJsYXN0Q29tcGxleElkIjoyMTQ3NDgzNjQ4MCwic29ydE9yZGVyIjoiREVTQyJ9"
            ),
            orderReturns = listOf(
                return21()
            )
        )

        assertEquals(expectedPagedReturnsResponse, actualPagedReturnResponse.result)
    }

    @Test
    fun `verify get partner returns with date filter on second page with descend sort`() {
        val partnerId = 222L
        val response = HttpClientBuilder.create().build().execute(
            prepareListPartnerReturnsRequest(
                partnerId,
                fromDate = "2020-01-01T00:00:00Z",
                toDate = "2020-03-01T00:00:00Z",
                pageSize = 1,
                sortOrder = SortOrder.DESC,
                pageToken = "eyJsYXN0Q29tcGxleElkIjoyMTQ3NDgzNjQ4MCwic29ydE9yZGVyIjoiREVTQyJ9"
            )
        )
        val actualPagedReturnResponse = objectMapper.readValue(
            response.entity.content,
            PagedReturnResponse::class.java
        )
        val expectedPagedReturnsResponse = PagedReturnsResponse(
            pager = PagerWithToken(pageSize = 1),
            orderReturns = listOf(
                return20()
            )
        )

        assertEquals(expectedPagedReturnsResponse, actualPagedReturnResponse.result)
    }

    @Test
    fun `verify get partner returns with date filter on second page`() {
        val partnerId = 222L
        val response = HttpClientBuilder.create().build().execute(
            prepareListPartnerReturnsRequest(
                partnerId,
                fromDate = "2020-01-01T00:00:00Z",
                toDate = "2020-03-01T00:00:00Z",
                pageSize = 1,
                pageToken = "eyJsYXN0Q29tcGxleElkIjoyMjU0ODU3ODMwNCwic29ydE9yZGVyIjoiQVNDIn0="
            )
        )
        val actualPagedReturnResponse = objectMapper.readValue(
            response.entity.content,
            PagedReturnResponse::class.java
        )
        val expectedPagedReturnsResponse = PagedReturnsResponse(
            pager = PagerWithToken(pageSize = 1, nextPageToken = null),
            orderReturns = listOf(
                return21()
            )
        )

        assertEquals(expectedPagedReturnsResponse, actualPagedReturnResponse.result)
    }

    @Test
    fun `verify get partner returns with status filter`() {
        val partnerId = 222L
        val response = HttpClientBuilder.create().build().execute(
            prepareListPartnerReturnsRequest(
                partnerId,
                statuses = listOf(OrderReturnRefundStatus.STARTED_BY_USER, OrderReturnRefundStatus.REFUNDED)
            )
        )
        val actualPagedReturnResponse = objectMapper.readValue(
            response.entity.content,
            PagedReturnResponse::class.java
        )
        val expectedPagedReturnsResponse = PagedReturnsResponse(
            pager = PagerWithToken(pageSize = 20, nextPageToken = null),
            orderReturns = listOf(
                return20(),
                return21()
            )
        )

        assertEquals(expectedPagedReturnsResponse, actualPagedReturnResponse.result)
    }

    @Test
    fun `verify get shop returns without filters`() {
        val partnerId = 333L

        val creationTime = OffsetDateTime.now(DEFAULT_TIMEZONE_ZONE_ID).toInstant()

        val pagedReturns = PagedReturns()
        pagedReturns.items = constructCheckouterReturns(creationTime)
        val checkouterPager = ru.yandex.market.checkout.common.rest.Pager()

        checkouterPager.currentPage = 0
        checkouterPager.total = 2
        checkouterPager.pageSize = 2
        checkouterPager.from = 0
        checkouterPager.to = 2

        pagedReturns.pager = checkouterPager

        whenever(
            mockCheckouterApiService.getReturnsForPartner(
                eq(partnerId), isNull(), isNull(),
                isNull(), isNull(), anyInt(), anyInt()
            )
        ).doReturn(pagedReturns)

        val response = HttpClientBuilder.create().build().execute(prepareGetShopReturnsRequest(partnerId))
        val actualPagedReturnsResponse = objectMapper.readValue(
            response.entity.content,
            PagedOrderReturnResponse::class.java
        )

        val expectedReturns = constructExpectedReturns(creationTime)

        val pager = Pager(
            currentPage = 0,
            totalCount = 2,
            pageSize = 2,
            hasNext = false,
            hasPrev = false
        )

        val expectedPagedReturnsResponse = PagedReturnsByPartnerResponse(
            orderReturns = expectedReturns,
            pager = pager
        )

        assertEquals(expectedPagedReturnsResponse, actualPagedReturnsResponse.result)
    }

    @Test
    fun `verify get shop returns without order available`() {
        val partnerId = 333L

        val creationTime = OffsetDateTime.now(DEFAULT_TIMEZONE_ZONE_ID).toInstant()

        val checkouterReturnItem = createSimpleCheckouterReturnItem(30001L, 3000L, 9999L)
        val checkouterReturn = createSimpleCheckouterReturn(
            3000L, 999L,
            CheckouterReturnStatus.WAITING_FOR_DECISION, creationTime, listOf(checkouterReturnItem)
        )

        val pagedReturns = PagedReturns()
        pagedReturns.items = constructCheckouterReturns(creationTime) + checkouterReturn

        val checkouterPager = ru.yandex.market.checkout.common.rest.Pager()

        checkouterPager.currentPage = 0
        checkouterPager.total = 2
        checkouterPager.pageSize = 2
        checkouterPager.from = 0
        checkouterPager.to = 2

        pagedReturns.pager = checkouterPager

        whenever(
            mockCheckouterApiService.getReturnsForPartner(
                eq(partnerId), isNull(), isNull(),
                isNull(), isNull(), anyInt(), anyInt()
            )
        ).doReturn(pagedReturns)

        val response = HttpClientBuilder.create().build().execute(prepareGetShopReturnsRequest(partnerId))
        val actualPagedReturnsResponse = objectMapper.readValue(
            response.entity.content,
            PagedOrderReturnResponse::class.java
        )

        val expectedReturns = constructExpectedReturns(creationTime)

        val pager = Pager(
            currentPage = 0,
            totalCount = 2,
            pageSize = 2,
            hasNext = false,
            hasPrev = false
        )

        val expectedPagedReturnsResponse = PagedReturnsByPartnerResponse(
            orderReturns = expectedReturns,
            pager = pager
        )

        assertEquals(expectedPagedReturnsResponse, actualPagedReturnsResponse.result)
    }

    @Test
    fun `verify get return by id with filters`() {
        val partnerId = 333L

        val creationTime = LocalDate.of(2021, 10, 30)

        val pagedReturns = PagedReturns()
        pagedReturns.items = constructCheckouterReturns(creationTime.toInstantAtMoscowTime())
        val checkouterPager = ru.yandex.market.checkout.common.rest.Pager()

        checkouterPager.currentPage = 0
        checkouterPager.total = 1
        checkouterPager.pageSize = 1
        checkouterPager.from = 0
        checkouterPager.to = 1

        pagedReturns.pager = checkouterPager

        val searchingOrderIds = listOf(3100L, 3200L)
        val searchingStatuses = listOf(
            ReturnStatus.STARTED_BY_USER,
            ReturnStatus.DECISION_MADE
        )

        val checkouterStatuses = listOf(
            CheckouterReturnStatus.STARTED_BY_USER,
            CheckouterReturnStatus.DECISION_MADE,
            CheckouterReturnStatus.REFUND_IN_PROGRESS,
            CheckouterReturnStatus.REFUNDED,
            CheckouterReturnStatus.REFUNDED_WITH_BONUSES,
            CheckouterReturnStatus.REFUNDED_BY_SHOP,
            CheckouterReturnStatus.FAILED
        )

        val fromDate = LocalDate.of(2021, 10, 1)
        val toDate = LocalDate.of(2021, 10, 30)
        val fromDateTime = OffsetDateTime.of(2021, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC)
        val toDateTime = OffsetDateTime.of(2021, 10, 30, 0, 0, 0, 0, ZoneOffset.UTC)

        whenever(
            mockCheckouterApiService.getReturnsForPartner(
                eq(partnerId),
                eq(searchingOrderIds), eq(checkouterStatuses), eq(fromDate), eq(toDate),
                anyInt(), anyInt()
            )
        ).doReturn(pagedReturns)

        val response = HttpClientBuilder.create().build()
            .execute(
                prepareGetShopReturnsRequestWithFilters(
                    partnerId, searchingOrderIds, searchingStatuses,
                    fromDate, toDate
                )
            )
        val actualPagedReturnsResponse = objectMapper.readValue(
            response.entity.content,
            PagedOrderReturnResponse::class.java
        )

        val expectedReturns = constructExpectedReturns(creationTime.toInstantAtMoscowTime())

        val pager = Pager(
            currentPage = 0,
            totalCount = 1,
            pageSize = 1,
            hasNext = false,
            hasPrev = false
        )

        val expectedPagedReturnsResponse = PagedReturnsByPartnerResponse(
            orderReturns = expectedReturns,
            pager = pager
        )

        assertEquals(expectedPagedReturnsResponse, actualPagedReturnsResponse.result)
    }

    @Test
    fun `verify no returns returned by checkouter`() {
        val partnerId = 333L

        val pagedReturns = PagedReturns()
        pagedReturns.items = emptyList()

        val checkouterPager = ru.yandex.market.checkout.common.rest.Pager()

        checkouterPager.currentPage = 1
        checkouterPager.total = 0
        checkouterPager.pageSize = 10
        checkouterPager.from = 0
        checkouterPager.to = 0

        pagedReturns.pager = checkouterPager

        whenever(
            mockCheckouterApiService.getReturnsForPartner(
                eq(partnerId), isNull(), isNull(),
                isNull(), isNull(), anyInt(), anyInt()
            )
        ).doReturn(pagedReturns)

        val response = HttpClientBuilder.create().build().execute(prepareGetShopReturnsRequest(partnerId))
        val actualPagedReturnsResponse = objectMapper.readValue(
            response.entity.content,
            PagedOrderReturnResponse::class.java
        )

        val pager = Pager(
            currentPage = 1,
            totalCount = 0,
            pageSize = 10,
            hasNext = false,
            hasPrev = false
        )

        val expectedPagedReturnsResponse = PagedReturnsByPartnerResponse(
            orderReturns = emptyList(),
            pager = pager
        )

        assertEquals(expectedPagedReturnsResponse, actualPagedReturnsResponse.result)
    }

    @Test
    fun `verify get return by id`() {
        val returnId = 1000L
        val partnerId = 333L
        val orderId = 3100L

        val creationTime = LocalDateTime.now().toInstantAtMoscowTime()

        val checkouterReturn = constructCheckouterReturns(creationTime)[0]

        whenever(mockCheckouterApiService.getReturn(returnId, orderId)).doReturn(checkouterReturn)

        val response = HttpClientBuilder.create().build().execute(prepareGetReturnRequest(returnId, partnerId, orderId))
        val actualReturn = objectMapper.readValue(
            response.entity.content,
            OrderReturnResponse::class.java
        )

        val expectedReturn = constructExpectedReturns(creationTime)[0]

        assertEquals(expectedReturn, actualReturn.result)
    }

    @Test
    fun `verify get return by id postpaid item`() {
        val returnId = 1000L
        val partnerId = 333L
        val orderId = 3100L

        val creationTime = LocalDateTime.now().toInstantAtMoscowTime()

        val checkouterReturn = constructCheckouterReturns(creationTime)[0]
        checkouterReturn.status = ru.yandex.market.checkout.checkouter.returns.ReturnStatus.REFUNDED

        whenever(mockCheckouterApiService.getReturn(returnId, orderId)).doReturn(checkouterReturn)

        val response = HttpClientBuilder.create().build().execute(prepareGetReturnRequest(returnId, partnerId, orderId))
        val actualReturn = objectMapper.readValue(
            response.entity.content,
            OrderReturnResponse::class.java
        )

        val expectedReturn = constructExpectedReturns(creationTime)[0]
        expectedReturn.refundAmount = BigDecimal(0)
        expectedReturn.returnItems?.get(0)?.paymentType = PaymentType.POSTPAID

        assertEquals(expectedReturn, actualReturn.result)
    }

    @Test
    fun `verify empty not required params`() {
        val returnId = 1000L
        val partnerId = 333L
        val orderId = 3100L

        val creationTime = LocalDateTime.now().toInstantAtMoscowTime()

        val checkouterReturnItem = createSimpleCheckouterReturnItem(10001L, returnId, 3101L)

        val checkouterReturn = createSimpleCheckouterReturn(
            returnId, orderId,
            CheckouterReturnStatus.STARTED_BY_USER, creationTime, listOf(checkouterReturnItem)
        )

        whenever(mockCheckouterApiService.getReturn(returnId, orderId)).doReturn(checkouterReturn)

        val response = HttpClientBuilder.create().build().execute(prepareGetReturnRequest(returnId, partnerId, orderId))
        val actualReturn = objectMapper.readValue(
            response.entity.content,
            OrderReturnResponse::class.java
        )

        val expectedReturnItem = createSimpleReturnItem(10001L, 3101L, BigDecimal(200)).copy(
            title = "offer 1.1",
            shopSku = "3101101",
            marketSku = "3101",
            offerId = "3101101:1.1"
        )

        val expectedReturn = createSimpleReturn(
            returnId, orderId,
            ReturnStatus.STARTED_BY_USER, creationTime, listOf(expectedReturnItem), BigDecimal(200)
        )

        assertEquals(expectedReturn, actualReturn.result)
    }

    @Test
    fun `verify checkouter return status matches decision made`() {
        val returnId = 1000L
        val partnerId = 333L
        val orderId = 3100L

        val creationTime = LocalDateTime.now().toInstantAtMoscowTime()

        val checkouterReturnItem = createSimpleCheckouterReturnItem(10001L, returnId, 3101L)

        val checkouterReturn = createSimpleCheckouterReturn(
            returnId, orderId,
            CheckouterReturnStatus.WAITING_FOR_DECISION, creationTime, listOf(checkouterReturnItem)
        )

        whenever(mockCheckouterApiService.getReturn(returnId, orderId)).doReturn(checkouterReturn)

        val response = HttpClientBuilder.create().build().execute(prepareGetReturnRequest(returnId, partnerId, orderId))
        val actualReturn = objectMapper.readValue(
            response.entity.content,
            OrderReturnResponse::class.java
        )

        val expectedReturnItem = createSimpleReturnItem(10001L, 3101L, BigDecimal(200)).copy(
            title = "offer 1.1",
            shopSku = "3101101",
            marketSku = "3101",
            offerId = "3101101:1.1"
        )

        val expectedReturn = createSimpleReturn(
            returnId, orderId,
            ReturnStatus.WAITING_FOR_DECISION, creationTime, listOf(expectedReturnItem), BigDecimal(200)
        )

        assertEquals(expectedReturn, actualReturn.result)
    }

    @Test
    fun `verify submit decision return status`() {
        val partnerId = 333L
        val returnId = 1000L
        val orderId = 3100L

        val creationTime = LocalDateTime.now().toInstantAtMoscowTime()

        val checkouterReturn = constructCheckouterReturns(creationTime)[0]

        whenever(
            mockCheckouterApiService.updateReturnStatus(
                eq(returnId), eq(partnerId),
                eq(orderId), eq(CheckouterReturnStatus.DECISION_MADE)
            )
        ).doReturn(checkouterReturn)

        val response = HttpClientBuilder.create().build().execute(prepareSubmitDecision(returnId, partnerId, orderId))
        val actualReturn = objectMapper.readValue(
            response.entity.content,
            OrderReturnResponse::class.java
        )

        val expectedReturn = constructExpectedReturns(creationTime)[0]

        assertEquals(expectedReturn, actualReturn.result)
    }

    @Test
    fun `verify update return item decisions`() {
        val partnerId = 333L
        val returnId = 1000L
        val orderId = 3200L

        val creationTime = LocalDateTime.now().toInstantAtMoscowTime()

        val checkouterReturn = constructCheckouterReturns(creationTime)[1]

        val itemDecisions = listOf(
            ReturnDecision(
                returnItemId = 2001,
                decisionType = ReturnItemDecisionType.REPLACE,
                decisionComment = "some comment",
            ),
            ReturnDecision(
                returnItemId = 2002,
                decisionType = ReturnItemDecisionType.DECLINE_REFUND,
            )
        )

        whenever(
            mockCheckouterApiService.updateReturnDecisions(
                eq(returnId), eq(partnerId),
                eq(orderId), anyList()
            )
        ).doReturn(checkouterReturn)

        val response = HttpClientBuilder.create().build()
            .execute(prepareUpdateReturnDecisions(returnId, partnerId, orderId, itemDecisions))
        val actualReturn = objectMapper.readValue(
            response.entity.content,
            OrderReturnResponse::class.java
        )

        val expectedReturn = constructExpectedReturns(creationTime)[1]

        assertEquals(expectedReturn, actualReturn.result)
    }

    @CleanupTables()
    @Nested
    inner class CheckRightsTests {
        @Test
        fun `verify getReturn partner has no rights for order`() {
            val returnId = 1000L
            val partnerId = 333L
            val orderId = 3300L

            val response =
                HttpClientBuilder.create().build().execute(prepareGetReturnRequest(returnId, partnerId, orderId))

            assertEquals(HttpStatus.FORBIDDEN.value(), response.statusLine.statusCode)
            assertTrue(
                response.entity.content.bufferedReader().use { it.readText() }
                    .contains("Return interaction is forbidden")
            )
        }

        @Test
        fun `verify submitDecision partner has no rights for order`() {
            val returnId = 1000L
            val partnerId = 333L
            val orderId = 3300L

            val response =
                HttpClientBuilder.create().build().execute(prepareSubmitDecision(returnId, partnerId, orderId))

            assertEquals(HttpStatus.FORBIDDEN.value(), response.statusLine.statusCode)
            assertTrue(
                response.entity.content.bufferedReader().use { it.readText() }
                    .contains("Return interaction is forbidden")
            )
        }

        @Test
        fun `verify updateDecisions partner has no rights for order`() {
            val returnId = 1000L
            val partnerId = 333L
            val orderId = 3300L

            val itemDecisions = listOf(
                ReturnDecision(
                    returnItemId = 2001,
                    decisionType = ReturnItemDecisionType.REPLACE
                )
            )

            val response = HttpClientBuilder.create().build()
                .execute(prepareUpdateReturnDecisions(returnId, partnerId, orderId, itemDecisions))

            assertEquals(HttpStatus.FORBIDDEN.value(), response.statusLine.statusCode)
            assertTrue(
                response.entity.content.bufferedReader().use { it.readText() }
                    .contains("Return interaction is forbidden")
            )
        }
    }

    /**
     * Возврат с id = 1 из файла checkouterReturns.json
     */
    private fun return1(): ReturnDTO {
        return ReturnDTO(
            returnId = 1,
            orderId = 1,
            partnerOrderId = "111-1",
            logisticPickupPoint = LogisticPickupPointDTO(100),
            createdAt = OffsetDateTime.parse("2021-12-11T14:30:30+03:00"),
            updatedAt = OffsetDateTime.parse("2021-12-11T14:30:30+03:00"),
            orderCreationDate = OffsetDateTime.parse("2020-01-11T14:30:30+03:00"),
            returnStatus = OrderReturnRefundStatus.STARTED_BY_USER,
            logisticStatus = LogisticReturnStatusDTO.IN_TRANSIT,
            refundAmount = BigDecimal(1000),
            returnType = ReturnTypeDTO.RETURN,
            logisticHistory = listOf(),
            applicationUrl = "https://s3.mdst.yandex.net/return-application-1.pdf",
            returnLines = listOf(
                ReturnLineDTO(
                    shopSku = "sku-1",
                    marketSku = 1000,
                    count = 1,
                    boxes = listOf(),
                    refunds = listOf(
                        RefundDTO(
                            count = 1,
                            returnReasonType = ReturnReasonType.BAD_QUALITY,
                            returnSubreasonType = ReturnSubreasonType.USER_DID_NOT_LIKE,
                            returnReason = "Не подошел размер. Большая",
                            decisionType = ReturnItemDecisionType.REFUND_MONEY,
                            refundAmount = BigDecimal(1000),
                            partnerCompensation = BigDecimal(100)
                        )
                    ),
                    logisticItems = listOf()
                )
            )
        )
    }

    /**
     * Возврат с id = 2 из файла checkouterReturns.json
     */
    private fun return2(): ReturnDTO {
        return ReturnDTO(
            returnId = 2,
            orderId = 2,
            partnerOrderId = "111-2",
            logisticPickupPoint = LogisticPickupPointDTO(100),
            createdAt = OffsetDateTime.parse("2021-12-11T14:30:30+03:00"),
            updatedAt = OffsetDateTime.parse("2021-12-11T14:30:30+03:00"),
            orderCreationDate = OffsetDateTime.parse("2020-01-11T14:30:30+03:00"),
            returnStatus = OrderReturnRefundStatus.STARTED_BY_USER,
            logisticStatus = LogisticReturnStatusDTO.IN_TRANSIT,
            refundAmount = BigDecimal(1000),
            returnType = ReturnTypeDTO.UNREDEEMED,
            logisticHistory = listOf(),
            applicationUrl = "https://s3.mdst.yandex.net/return-application-2.pdf",
            returnLines = listOf(
                ReturnLineDTO(
                    shopSku = "sku-2",
                    marketSku = 2000,
                    count = 1,
                    boxes = listOf(),
                    refunds = listOf(
                        RefundDTO(
                            count = 1,
                            returnReasonType = ReturnReasonType.BAD_QUALITY,
                            returnSubreasonType = ReturnSubreasonType.USER_DID_NOT_LIKE,
                            returnReason = "Не подошел размер. Большая",
                            decisionType = ReturnItemDecisionType.REFUND_MONEY,
                            refundAmount = BigDecimal(1000),
                            partnerCompensation = BigDecimal(100)
                        )
                    ),
                    logisticItems = listOf()
                )
            )
        )
    }

    /**
     * Возврат id = 20 из файла checkouterReturns.json
     */
    private fun return20(): ReturnDTO {
        return ReturnDTO(
            returnId = 20,
            orderId = 20,
            partnerOrderId = "222-20",
            createdAt = OffsetDateTime.parse("2020-02-01T14:30:30+03:00"),
            updatedAt = OffsetDateTime.parse("2020-02-02T14:30:30+03:00"),
            orderCreationDate = OffsetDateTime.parse("2020-01-11T14:30:30+03:00"),
            returnStatus = OrderReturnRefundStatus.STARTED_BY_USER,
            logisticStatus = LogisticReturnStatusDTO.READY_FOR_PICKUP,
            refundAmount = BigDecimal(2000),
            returnType = ReturnTypeDTO.RETURN,
            logisticHistory = listOf(),
            logisticPickupPoint = LogisticPickupPointDTO(100),
            applicationUrl = "https://s3.mdst.yandex.net/return-application-20.pdf",
            returnLines = listOf(
                ReturnLineDTO(
                    shopSku = "sku-20",
                    marketSku = 2000,
                    count = 1,
                    boxes = listOf(),
                    refunds = listOf(
                        RefundDTO(
                            count = 1,
                            returnReasonType = ReturnReasonType.BAD_QUALITY,
                            returnSubreasonType = ReturnSubreasonType.USER_DID_NOT_LIKE,
                            returnReason = "Не подошел размер. Большая",
                            decisionType = ReturnItemDecisionType.REFUND_MONEY,
                            partnerCompensation = BigDecimal(100),
                            refundAmount = BigDecimal(1000)
                        )
                    ),
                    logisticItems = listOf()
                ),
                ReturnLineDTO(
                    shopSku = "sku-20",
                    marketSku = 2000,
                    count = 1,
                    boxes = listOf(),
                    refunds = listOf(
                        RefundDTO(
                            count = 1,
                            returnReasonType = ReturnReasonType.BAD_QUALITY,
                            returnSubreasonType = ReturnSubreasonType.USER_CHANGED_MIND,
                            decisionType = ReturnItemDecisionType.REFUND_MONEY,
                            returnReason = "Не подошел размер. Большая",
                            partnerCompensation = BigDecimal(100),
                            refundAmount = BigDecimal(1000)
                        )
                    ),
                    logisticItems = listOf(
                        LogisticItemDTO(
                            stockType = StockTypeDTO.DEFECT,
                            status = MerchantItemStatusDTO.RETURN_RECEIVED_ON_FULFILLMENT,
                            itemInfo = mapOf(),
                            ffRequestStatusCommittedAt = OffsetDateTime.parse("2020-02-01T14:30:30+03:00"),
                            warehouseId = 200
                        )
                    )
                ),
            ),
        )
    }

    /**
     * Возврат id = 21 из файла checkouterReturns.json
     */
    private fun return21(): ReturnDTO {
        return ReturnDTO(
            returnId = 21,
            orderId = 21,
            partnerOrderId = "222-21",
            createdAt = OffsetDateTime.parse("2020-02-03T14:30:30+03:00"),
            updatedAt = OffsetDateTime.parse("2020-02-04T14:30:30+03:00"),
            orderCreationDate = OffsetDateTime.parse("2020-01-11T14:30:30+03:00"),
            returnStatus = OrderReturnRefundStatus.REFUNDED,
            logisticStatus = LogisticReturnStatusDTO.READY_FOR_PICKUP,
            refundAmount = BigDecimal(3000),
            returnType = ReturnTypeDTO.RETURN,
            logisticHistory = listOf(),
            logisticPickupPoint = LogisticPickupPointDTO(100),
            applicationUrl = "https://s3.mdst.yandex.net/return-application-21.pdf",
            returnLines = listOf(
                ReturnLineDTO(
                    shopSku = "sku-21-0",
                    marketSku = 2100,
                    count = 3,
                    boxes = listOf(),
                    refunds = listOf(
                        RefundDTO(
                            count = 1,
                            returnReasonType = ReturnReasonType.BAD_QUALITY,
                            returnSubreasonType = ReturnSubreasonType.USER_DID_NOT_LIKE,
                            returnReason = "Не подошел размер. Большая",
                            decisionType = ReturnItemDecisionType.REFUND_MONEY,
                            partnerCompensation = BigDecimal(100),
                            refundAmount = BigDecimal(1000)
                        ),
                        RefundDTO(
                            count = 2,
                            returnReasonType = ReturnReasonType.WRONG_ITEM,
                            returnSubreasonType = ReturnSubreasonType.WRONG_ITEM,
                            returnReason = "Не подошел размер. Большая",
                            decisionType = ReturnItemDecisionType.REFUND_MONEY_INCLUDING_SHIPMENT,
                            partnerCompensation = BigDecimal(200),
                            refundAmount = BigDecimal(2000)
                        )
                    ),
                    logisticItems = listOf(
                        LogisticItemDTO(
                            stockType = StockTypeDTO.FIT,
                            status = MerchantItemStatusDTO.RETURN_IN_TRANSIT,
                            itemInfo = mapOf("CIS" to "12345"),
                            ffRequestStatusCommittedAt = OffsetDateTime.parse("2020-02-03T14:30:30+03:00"),
                            warehouseId = 200
                        )
                    )
                )
            )
        )
    }

    private fun constructExpectedReturns(creationTime: Instant): List<OrderReturnDTO> {
        val firstOrderReturnItem = createSimpleReturnItem(10001L, 3101L, BigDecimal(20 * 100)).copy(
            returnReasonType = ReturnReasonType.BAD_QUALITY,
            returnSubreason = ReturnSubreasonType.DAMAGED,
            decisionType = ReturnItemDecisionType.REPAIR,
            title = "offer 1.1",
            shopSku = "3101101",
            offerId = "3101101:1.1",
            marketSku = "3101",
            photoUrls = listOf("http://aaa.com"),
            partnerCompensation = (100 * 100).bd,
            count = 10,
            paymentType = PaymentType.POSTPAID
        )

        val firstOrderReturn = createSimpleReturn(
            1000L, 3100L, ReturnStatus.DECISION_MADE,
            creationTime, listOf(firstOrderReturnItem), BigDecimal(20 * 100)
        ).copy(
            partnerCompensation = (100 * 100).bd,
            trackCode = "1234567"
        )

        val secondOrderReturnItemA = createSimpleReturnItem(20001L, 3201L, BigDecimal(100 * 100)).copy(
            returnReasonType = ReturnReasonType.WRONG_ITEM,
            returnSubreason = ReturnSubreasonType.WRONG_ITEM,
            decisionType = ReturnItemDecisionType.REPLACE,
            title = "offer 2.1",
            shopSku = "3201201",
            offerId = "3201201:2.1",
            marketSku = "3201",
            photoUrls = listOf("http://aaa.com", "http://bbb.com"),
            count = 1,
            partnerCompensation = (1000 * 100).bd,
            paymentType = PaymentType.POSTPAID,
        )

        val secondOrderReturnItemB = createSimpleReturnItem(20002L, 3202L, BigDecimal(2000 * 100)).copy(
            returnReasonType = ReturnReasonType.BAD_QUALITY,
            returnSubreason = ReturnSubreasonType.DAMAGED,
            title = "offer 2.2",
            shopSku = "3202202",
            offerId = "3202202:2.2",
            marketSku = "0",
            count = 2,
            partnerCompensation = BigDecimal(0),
            paymentType = PaymentType.POSTPAID
        )

        val secondOrderReturn = createSimpleReturn(
            2000L, 3200L, ReturnStatus.STARTED_BY_USER,
            creationTime, listOf(secondOrderReturnItemA, secondOrderReturnItemB), BigDecimal(2100 * 100)
        ).copy(partnerCompensation = (1000 * 100).bd)

        return listOf(firstOrderReturn, secondOrderReturn)
    }

    private fun constructCheckouterReturns(creationTime: Instant): List<Return> {
        val firstReturnItem = createSimpleCheckouterReturnItem(10001L, 1000L, 3101L)
        firstReturnItem.reasonType = CheckouterReturnReasonType.BAD_QUALITY
        firstReturnItem.subreasonType = ReturnSubreason.DAMAGED
        firstReturnItem.decisionType = ReturnDecisionType.REPAIR
        firstReturnItem.picturesUrls = listOf(URL("http://aaa.com"))
        firstReturnItem.supplierCompensation = BigDecimal.valueOf(100)
        firstReturnItem.count = 10

        val deliveryReturnItem = createSimpleCheckouterReturnItem(null, 1000L, 3101L)
        deliveryReturnItem.isDeliveryService = true

        val firstReturn = createSimpleCheckouterReturn(
            1000L, 3100L,
            CheckouterReturnStatus.DECISION_MADE, creationTime, listOf(firstReturnItem, deliveryReturnItem)
        )

        val track = Track()
        track.trackCode = "1234567"
        val delivery = ReturnDelivery()
        delivery.track = track
        firstReturn.delivery = delivery

        val secondReturnItemA = createSimpleCheckouterReturnItem(20001L, 2000L, 3201L)
        secondReturnItemA.reasonType = CheckouterReturnReasonType.WRONG_ITEM
        secondReturnItemA.subreasonType = ReturnSubreason.WRONG_ITEM
        secondReturnItemA.decisionType = ReturnDecisionType.REPLACE
        secondReturnItemA.picturesUrls = listOf(URL("http://aaa.com"), URL("http://bbb.com"))
        secondReturnItemA.supplierCompensation = BigDecimal.valueOf(1000)
        secondReturnItemA.count = 1

        val secondReturnItemB = createSimpleCheckouterReturnItem(20002L, 2000L, 3202L)
        secondReturnItemB.reasonType = CheckouterReturnReasonType.BAD_QUALITY
        secondReturnItemB.subreasonType = ReturnSubreason.DAMAGED
        secondReturnItemB.count = 2

        val secondReturn = createSimpleCheckouterReturn(
            2000L, 3200L,
            CheckouterReturnStatus.STARTED_BY_USER, creationTime, listOf(secondReturnItemA, secondReturnItemB)
        )

        return listOf(firstReturn, secondReturn)
    }

    private fun createSimpleCheckouterReturnItem(itemId: Long?, returnId: Long, orderItemId: Long): ReturnItem {
        val returnItem = ReturnItem()
        returnItem.id = itemId
        returnItem.returnId = returnId
        returnItem.itemId = orderItemId
        returnItem.count = 1

        return returnItem
    }

    private fun createSimpleCheckouterReturn(
        returnId: Long,
        orderId: Long,
        status: CheckouterReturnStatus,
        creationTime: Instant,
        items: List<ReturnItem>
    ): Return {
        val checkouterReturn = Return()
        checkouterReturn.orderId = orderId
        checkouterReturn.id = returnId
        checkouterReturn.items = items
        checkouterReturn.status = status
        checkouterReturn.createdAt = creationTime
        checkouterReturn.updatedAt = creationTime
        checkouterReturn.applicationUrl = "https://s3.mdst.yandex.net/return-application-${returnId}.pdf"
        return checkouterReturn
    }

    private fun createSimpleReturnItem(
        returnItemId: Long,
        orderItemId: Long,
        refundAmount: BigDecimal
    ): OrderReturnItemDTO {
        return OrderReturnItemDTO(
            orderItemId = orderItemId,
            returnItemId = returnItemId,
            paymentType = PaymentType.POSTPAID,
            refundAmount = refundAmount,
            count = 1,
            partnerCompensation = 0.bd
        )
    }

    private fun createSimpleReturn(
        returnId: Long,
        orderId: Long,
        status: ReturnStatus,
        creationTime: Instant,
        items: List<OrderReturnItemDTO>,
        refundAmount: BigDecimal
    ): OrderReturnDTO {
        return OrderReturnDTO(
            orderId = orderId,
            returnId = returnId,
            returnStatus = status,
            returnItems = items,
            createdAt = OffsetDateTime.ofInstant(creationTime, DEFAULT_TIMEZONE_ZONE_ID),
            updatedAt = OffsetDateTime.ofInstant(creationTime, DEFAULT_TIMEZONE_ZONE_ID),
            refundAmount = refundAmount,
            partnerCompensation = 0.bd,
            applicationUrl = "https://s3.mdst.yandex.net/return-application-${returnId}.pdf"
        )
    }

    private fun prepareListPartnerReturnsRequest(
        partnerId: Long,
        fromDate: String? = null,
        toDate: String? = null,
        pageSize: Long? = 20,
        pageToken: String? = null,
        statuses: List<OrderReturnRefundStatus>? = null,
        sortOrder: SortOrder? = SortOrder.ASC,
        type: ReturnTypeDTO? = null
    ): HttpGet {
        val paramMap = hashMapOf<String, String>()

        paramMap["partnerId"] = partnerId.toString()
        fromDate?.let { paramMap.put("fromDateTime", it) }
        toDate?.let { paramMap.put("toDateTime", it) }
        pageSize?.let { paramMap.put("size", it.toString()) }
        pageToken?.let { paramMap.put("pageToken", it) }
        statuses?.let { paramMap.put("statuses", it.joinToString(",")) }
        sortOrder?.let { paramMap.put("sortOrder", it.toString()) }
        type?.let { paramMap.put("type", it.value) }

        val request = HttpGet(getUri("/returns/list/", paramMap))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        return request
    }

    private fun prepareGetShopReturnsRequest(partnerId: Long): HttpGet {
        val request = HttpGet(getUri("/returns/", mapOf("partnerId" to partnerId.toString())))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        return request
    }

    private fun prepareGetShopReturnsRequestWithFilters(
        partnerId: Long,
        orderIds: List<Long>,
        statuses: List<ReturnStatus>,
        fromDate: LocalDate,
        toDate: LocalDate
    ): HttpGet {
        val parameters = mapOf(
            "partnerId" to partnerId.toString(),
            "orderIds" to orderIds.joinToString(separator = ","),
            "statuses" to statuses.joinToString(separator = ","),
            "fromDate" to fromDate.toString(),
            "toDate" to toDate.toString()
        )

        val request = HttpGet(getUri("/returns/", parameters))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        return request
    }

    private fun prepareGetReturnRequest(returnId: Long, partnerId: Long, orderId: Long): HttpGet {
        val parameters = mapOf(
            "partnerId" to partnerId.toString(),
            "orderId" to orderId.toString()
        )

        val request = HttpGet(getUri("/returns/$returnId", parameters))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        return request
    }

    private fun prepareSubmitDecision(returnId: Long, partnerId: Long, orderId: Long): HttpPost {
        val parameters = mapOf(
            "partnerId" to partnerId.toString(),
            "orderId" to orderId.toString()
        )

        val request = HttpPost(getUri("/returns/$returnId/submit-decision", parameters))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        return request
    }

    private fun prepareUpdateReturnDecisions(
        returnId: Long,
        partnerId: Long,
        orderId: Long,
        itemDecisions: List<ReturnDecision>
    ): HttpPost {
        val parameters = mapOf(
            "partnerId" to partnerId.toString(),
            "orderId" to orderId.toString()
        )

        val request = HttpPost(getUri("/returns/$returnId/returnitems/decisions", parameters))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        request.entity = StringEntity(objectMapper.writeValueAsString(itemDecisions))
        return request
    }
}
