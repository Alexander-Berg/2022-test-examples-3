package ru.yandex.market.mbi.orderservice.api.controller.orders

import org.apache.commons.io.IOUtils
import org.apache.http.HttpStatus
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.groups.Tuple.tuple
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.whenever
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.loadResourceAsString
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.api.FunctionalTest
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderSubstatus
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEvent
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderForCompensation
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLogisticsEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderSparseIndex
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEventsRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderLineEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderLogisticsEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderSparseIndexRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrdersForCompensationRepository
import ru.yandex.market.mbi.orderservice.common.util.defaultObjectMapper
import ru.yandex.market.mbi.orderservice.model.OrderField
import ru.yandex.market.mbi.orderservice.model.OrderSourcePlatform
import ru.yandex.market.mbi.orderservice.model.OrderStatus
import ru.yandex.market.mbi.orderservice.model.OrderStatusGroup
import ru.yandex.market.mbi.orderservice.model.OrderSubStatus
import ru.yandex.market.mbi.orderservice.model.PartnerOrderCounterRequest
import ru.yandex.market.mbi.orderservice.model.PartnerOrderListingRequest
import ru.yandex.market.mbi.orderservice.model.PartnerOrderTabsRequest
import ru.yandex.market.mbi.orderservice.model.PartnerOrdersPiFilters
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.util.stream.Stream

@CleanupTables(
    [
        OrderEntity::class, OrderLineEntity::class, OrderEvent::class,
        OrderLogisticsEntity::class, OrderSparseIndex::class
    ]
)
@DbUnitDataSet(before = ["pi/OrdersPIControllerTest.before.csv"])
internal class OrdersPIControllerTest : FunctionalTest() {

    @Autowired
    lateinit var orderEntityRepository: OrderEntityRepository

    @Autowired
    lateinit var orderLineEntityRepository: OrderLineEntityRepository

    @Autowired
    lateinit var orderEventsRepository: OrderEventsRepository

    @Autowired
    lateinit var orderSparseIndexRepository: OrderSparseIndexRepository

    @Autowired
    lateinit var orderLogisticsEntityRepository: OrderLogisticsEntityRepository

    @Autowired
    lateinit var ordersForCompensationRepository: OrdersForCompensationRepository

    @Autowired
    lateinit var clock: Clock

    @BeforeEach
    internal fun setUp() {
        val orders = this::class.loadTestEntities<OrderEntity>("pi/orders.json")
        orderEntityRepository.insertRows(orders)
        val orderLines = this::class.loadTestEntities<OrderLineEntity>("pi/order_lines.json")
        orderLineEntityRepository.insertRows(orderLines)
        val orderEvents = this::class.loadTestEntities<OrderEvent>("pi/order_events.json")
        orderEventsRepository.insertRows(orderEvents)
        val orderLogistics = this::class.loadTestEntities<OrderLogisticsEntity>("pi/order_logistics.json")
        orderLogisticsEntityRepository.insertRows(orderLogistics)
        val orderSparseIndex = this::class.loadTestEntities<OrderSparseIndex>("pi/order_sparse_index.json")
        orderSparseIndexRepository.insertRows(orderSparseIndex)
        val ordersForCompensation = this::class.loadTestEntities<OrderForCompensation>(
            "pi/orders_for_compensation.json"
        )
        ordersForCompensationRepository.batchInsert(ordersForCompensation)
        whenever(clock.instant()).thenReturn(Instant.parse("2021-10-12T10:00:00Z"))
    }

    @Test
    fun enumCompatibilityTest() {
        assertThatCode {
            MerchantOrderSubstatus.values().forEach {
                OrderSubStatus.valueOf(it.name)
            }
        }.doesNotThrowAnyException()
    }

    @Test
    fun getAllPartnerComments() {
        verifyNotesRequest(
            "OrdersPIControllerTest.verifyGetNotes.all.expected.json",
            params = listOf(
                "orderIds" to "81545127",
                "orderIds" to "81545128",
                "orderIds" to "81545129",
            )
        )
    }

    @Test
    fun postExistingPartnerComment() {
        verifyNotesRequest(
            "OrdersPIControllerTest.postExistingNote.expected.json",
            orderId = 81545129,
            params = listOf("_user_id" to "123000"),
            bodyFile = "OrdersPIControllerTest.postRequest.json"
        )
        assertThat(getOrderTableContents()).extracting("key.orderId", "partnerNote", "partnerNoteAuthorUid")
            .contains(
                tuple(81545127L, "Комментарий, который пришел раньше самого заказа", 11002345L),
                tuple(81545128L, "Комментарий, который пришел после самого заказа", 11002345L),
                tuple(81545129L, "New comment", 123000L)
            )
    }

    @Test
    fun postPartnerCommentForNewOrder() {
        verifyNotesRequest(
            "OrdersPIControllerTest.postNewOrderNote.expected.json",
            orderId = 9999999,
            params = listOf("_user_id" to "123000"),
            bodyFile = "OrdersPIControllerTest.postRequest.json"
        )
        assertThat(getOrderTableContents())
            .extracting("key.orderId", "partnerNote", "partnerNoteAuthorUid", "lineIds")
            .contains(
                tuple(81545127L, "Комментарий, который пришел раньше самого заказа", 11002345L, emptyList<Long>()),
                tuple(
                    81545128L,
                    "Комментарий, который пришел после самого заказа",
                    11002345L,
                    listOf(140430845L, 140430846L)
                ),
                tuple(9999999L, "New comment", 123000L, emptyList<Long>())
            )
    }

    @Test
    fun `Get missing order detailed info`() {
        val response = getDetailedOrder(0, 0)
        assertThat(response.statusLine.statusCode).isEqualTo(HttpStatus.SC_NOT_FOUND)
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getDetailedOrder.notFound.json",
            expectedHttpCode = 404
        )
    }

    @Test
    fun `Get order detailed info`() {
        val response = getDetailedOrder(543900, 81545128)
        assertExpectedResponse(response, "pi/response/OrdersPIControllerTest.getDetailedOrder.json")
    }

    @Test
    fun `Get order info estimated deadline`() {
        val response = getDetailedOrder(543900, 81545129)
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getDetailedOrder.estimatedShipmentDeadline.json"
        )
    }

    @Test
    fun `Get order history`() {
        val response = getOrderHistory(543900, 81545128)
        assertExpectedResponse(response, "pi/response/OrdersPIControllerTest.getOrderHistory.json")
    }

    @ParameterizedTest(name = "Orders listing by group {0}")
    @MethodSource("orderListingSource")
    fun `Order listing test`(orderStatusGroup: OrderStatusGroup) {
        val response = getOrdersListing(
            listOf(543900), orderStatusGroup,
            OffsetDateTime.parse("2021-06-11T14:30:30+03:00"),
            OffsetDateTime.parse("2022-06-11T14:30:30+03:00")
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrdersListing.$orderStatusGroup.json"
        )
    }

    @Test()
    fun `Order listing status filter test`() {
        val response = getOrdersListing(
            listOf(543900), OrderStatusGroup.ALL,
            OffsetDateTime.parse("2021-06-11T14:30:30+03:00"),
            OffsetDateTime.parse("2022-06-11T14:30:30+03:00"),
            statuses = listOf(OrderStatus.DELIVERED)
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrdersListing.DELIVERED.json"
        )
    }

    @Test
    fun `Order listing createdAt empty list test`() {
        val response = getOrdersListing(
            listOf(543900), OrderStatusGroup.ALL,
            OffsetDateTime.parse("2020-06-11T14:30:30+03:00"),
            OffsetDateTime.parse("2020-06-12T14:30:30+03:00")
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrdersListing.empty.json"
        )
    }

    @Test
    fun `Order listing empty partnerIds test`() {
        val response = getOrdersListing(
            listOf(), OrderStatusGroup.ALL,
            OffsetDateTime.parse("2021-06-11T14:30:30+03:00"),
            OffsetDateTime.parse("2022-06-11T14:30:30+03:00")
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrdersListing.empty.json"
        )
    }

    @Test
    fun `Order listing shipment date filter test`() {
        val response = getOrdersListing(
            listOf(543900), OrderStatusGroup.PROCESSING,
            OffsetDateTime.parse("2021-06-11T14:30:30+03:00"),
            OffsetDateTime.parse("2022-06-12T14:30:30+03:00"),
            shipmentFrom = OffsetDateTime.parse("2021-10-13T00:00:00+03:00"),
            shipmentTo = OffsetDateTime.parse("2021-10-13T23:59:59+03:00")
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrdersListing.shipmentDateFilter.json"
        )
    }

    @Test
    fun `Order listing createdAt single order test`() {
        val response = getOrdersListing(
            listOf(543900), OrderStatusGroup.DELIVERED,
            OffsetDateTime.parse("2021-10-12T11:00:00Z"),
            OffsetDateTime.parse("2021-10-12T12:00:00Z")
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrdersListing.singleDelivered.json"
        )
    }

    @Test
    fun `Order listing limit test`() {
        val response = getOrdersListing(
            listOf(543900), OrderStatusGroup.ALL,
            OffsetDateTime.parse("2021-06-11T14:30:30+03:00"),
            OffsetDateTime.parse("2022-01-01T14:30:30+03:00"),
            pageSize = 1
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrdersListing.limitTest.json"
        )
    }

    @Test
    fun `Page token test`() {
        val response = getOrdersListing(
            listOf(543900), OrderStatusGroup.ALL,
            OffsetDateTime.parse("2021-06-11T14:30:30+03:00"),
            OffsetDateTime.parse("2022-01-01T14:30:30+03:00"),
            pageSize = 1,
            pageToken = "eyJsYXN0T3JkZXJJZCI6ODE1NDUxMzEsImxhc3RTb3J0aW5nRmllbGRWYWx1ZSI6MTYzNDAzOTA4NDAwMH0="
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrdersListing.limitWithTokenTest.json"
        )
    }

    @Test
    fun `Page token for PROCESSING test`() {
        val response = getOrdersListing(
            listOf(543900), OrderStatusGroup.PROCESSING,
            OffsetDateTime.parse("2021-06-11T14:30:30+03:00"),
            OffsetDateTime.parse("2022-01-01T14:30:30+03:00"),
            pageSize = 1,
            pageToken = "eyJsYXN0T3JkZXJJZCI6ODE1NDUxMjQsImxhc3RTb3J0aW5nRmllbGRWYWx1ZSI6MTYyODY3NjAzMH0="
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrdersListing.processingTokenTest.json"
        )
    }

    @Test
    fun `Test pending fake orders`() {
        val response = getOrdersListing(
            listOf(543900), OrderStatusGroup.PENDING,
            OffsetDateTime.parse("2021-06-11T14:30:30+03:00"),
            OffsetDateTime.parse("2022-06-11T14:30:30+03:00"),
            fake = null
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrdersListing.fakePendingOrders.json"
        )
    }

    @Test
    fun `Custom listing serialization test`() {
        val response = getOrdersListing(
            listOf(543900), OrderStatusGroup.ALL,
            OffsetDateTime.parse("2021-06-11T14:30:30+03:00"),
            OffsetDateTime.parse("2022-06-11T14:30:30+03:00"),
            pageSize = 1,
            pageToken = "eyJsYXN0T3JkZXJJZCI6ODE1NDUxMzEsImxhc3RTb3J0aW5nRmllbGRWYWx1ZSI6MTYzNDAzOTA4NDAwMH0="
        )
        assertThat(response.statusLine.statusCode).isEqualTo(HttpStatus.SC_OK)
        val responseContent = IOUtils.toString(response.entity.content)
        val jsonResponse = JSONObject(responseContent)
        val result = jsonResponse.get("result") as JSONObject
        val orders = result.get("orders") as JSONArray
        val order = orders.getJSONObject(0)
        assertThat(order).matches { !it.has("lines") }
        assertThat(order).matches { !it.has("buyer") }
    }

    @Test
    fun `Order listing custom fields test`() {
        val response = getOrdersListing(
            listOf(543900), OrderStatusGroup.IN_DELIVERY,
            OffsetDateTime.parse("2021-06-11T14:30:30+03:00"),
            OffsetDateTime.parse("2022-06-11T14:30:30+03:00"),
            pageSize = 2,
            fields = OrderField.values().toList()
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrdersListing.customFields.json"
        )
    }

    @Test
    fun `Order listing orderId filter test`() {
        val response = getOrdersListing(
            listOf(543900), OrderStatusGroup.ALL,
            OffsetDateTime.parse("2021-06-11T14:30:30+03:00"),
            OffsetDateTime.parse("2022-06-11T14:30:30+03:00"),
            orderIds = listOf(81545129, 81545128),
            pageSize = 1,
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrdersListing.orderIdFilter.json"
        )
    }

    @Test
    fun `Order listing source platform filter test`() {
        val response = getOrdersListing(
            listOf(543900), OrderStatusGroup.ALL,
            OffsetDateTime.parse("2021-06-11T14:30:30+03:00"),
            OffsetDateTime.parse("2022-06-11T14:30:30+03:00"),
            sourcePlatform = OrderSourcePlatform.OTHER,
            pageSize = 10,
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrdersListing.sourcePlatformFilterPaging.json"
        )
    }

    @Test
    fun `Order listing orderId filter paging test`() {
        val response = getOrdersListing(
            listOf(543900), OrderStatusGroup.ALL,
            OffsetDateTime.parse("2021-06-11T14:30:30+03:00"),
            OffsetDateTime.parse("2022-06-11T14:30:30+03:00"),
            pageToken = "eyJsYXN0T3JkZXJJZCI6ODE1NDUxMjksImxhc3RTb3J0aW5nRmllbGRWYWx1ZSI6MTYzMzk0NDYzMDAwMH0=",
            orderIds = listOf(81545129, 81545128),
            pageSize = 1,
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrdersListing.orderIdFilterPaging.json"
        )
    }

    @Test
    fun `Order counters test`() {
        val response = getOrdersCounters(
            listOf(543900),
            OffsetDateTime.parse("2021-06-11T14:30:30+03:00"),
            OffsetDateTime.parse("2022-06-11T14:30:30+03:00"),
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrdersCounters.json"
        )
    }

    @Test
    fun `Order totals test`() {
        val response = getOrderTotals(
            listOf(543900), OrderStatusGroup.IN_DELIVERY,
            OffsetDateTime.parse("2021-06-11T14:30:30+03:00"),
            OffsetDateTime.parse("2022-06-11T14:30:30+03:00"),
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrdersTotals.IN_DELIVERY.json"
        )
    }

    @Test
    fun `Order totals test with statuses`() {
        val response = getOrderTotals(
            listOf(543900), OrderStatusGroup.ALL,
            OffsetDateTime.parse("2021-06-11T14:30:30+03:00"),
            OffsetDateTime.parse("2022-06-11T14:30:30+03:00"),
            statuses = listOf(OrderStatus.DELIVERY)
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrdersTotals.IN_DELIVERY.json"
        )
    }

    @Test
    fun `Order totals by ids test`() {
        val response = getOrderTotals(
            listOf(543900), OrderStatusGroup.IN_DELIVERY,
            OffsetDateTime.parse("2021-06-11T14:30:30+03:00"),
            OffsetDateTime.parse("2022-06-11T14:30:30+03:00"),
            orderIds = listOf(81545129)
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrdersTotals.filteredByOrderIds.json"
        )
    }

    @Test
    fun `Order tabs EXPRESS`() {
        val response = getOrderTabs(
            listOf(543801)
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrderTabs.EXPRESS.json",
            compareMode = JSONCompareMode.LENIENT
        )
    }

    @Test
    fun `Order tabs FBS`() {
        val response = getOrderTabs(
            listOf(543800)
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrderTabs.FBS.json",
            compareMode = JSONCompareMode.LENIENT
        )
    }

    @Test
    fun `Order tabs ALL`() {
        val response = getOrderTabs(
            listOf(543801, 543800)
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrderTabs.ALL.json",
            compareMode = JSONCompareMode.LENIENT
        )
    }

    @Test
    fun `Order tabs for empty partners list`() {
        val response = getOrderTabs(
            listOf()
        )
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.getOrderTabs.NONE.json",
        )
    }

    @Test
    fun `Orders for compensation test`() {
        val partnerId = -1L
        val request = HttpPost(getUri("/partners/$partnerId/pi/orders/get-compensated-orders", listOf()))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        val requestBody = this::class.loadResourceAsString(
            "pi/request/OrdersPIControllerTest.postCompensatedOrders.json"
        )
        request.entity = StringEntity(requestBody)
        val response = HttpClientBuilder.create().build().execute(request)
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.postCompensatedOrders.expected.json"
        )
    }

    @Test
    fun `Empty orders for compensation test`() {
        val partnerId = -1L
        val request = HttpPost(getUri("/partners/$partnerId/pi/orders/get-compensated-orders", listOf()))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        val requestBody = this::class.loadResourceAsString(
            "pi/request/OrdersPIControllerTest.postEmptyCompensatedOrders.json"
        )
        request.entity = StringEntity(requestBody)
        val response = HttpClientBuilder.create().build().execute(request)
        assertExpectedResponse(
            response,
            "pi/response/OrdersPIControllerTest.postEmptyCompensatedOrders.expected.json"
        )
    }

    private fun verifyNotesRequest(
        expectedFile: String,
        partnerId: Long = 543900,
        expectedHttpStatus: Int = HttpStatus.SC_OK,
        params: List<Pair<String, String>> = emptyList(),
        orderId: Long = 0,
        bodyFile: String? = null
    ) {
        val response = HttpClientBuilder.create().build().execute(
            bodyFile?.let {
                preparePostPartnerNotesRequest(
                    partnerId,
                    orderId,
                    params,
                    this::class.loadResourceAsString("pi/request/$it")
                )
            } ?: prepareGetPartnerNotesRequest(partnerId, params)
        )
        assertThat(response.statusLine.statusCode).isEqualTo(expectedHttpStatus)
        val content = IOUtils.toString(response.entity.content)
        JSONAssert.assertEquals(
            this::class.loadResourceAsString("pi/response/$expectedFile"),
            content,
            JSONCompareMode.STRICT_ORDER
        )
    }

    private fun getDetailedOrder(partnerId: Long, orderId: Long): CloseableHttpResponse {
        val request = HttpGet(getUri("/partners/$partnerId/pi/orders/$orderId", listOf()))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        return HttpClientBuilder.create().build().execute(request)
    }

    private fun getOrdersListing(
        partnerIds: List<Long>,
        orderStatusGroup: OrderStatusGroup,
        fromOffsetDateTime: OffsetDateTime,
        toOffsetDateTime: OffsetDateTime,
        fake: Boolean? = false,
        orderIds: List<Long>? = null,
        sourcePlatform: OrderSourcePlatform? = null,
        pageToken: String? = null,
        pageSize: Int? = null,
        fields: List<OrderField>? = null,
        shipmentFrom: OffsetDateTime? = null,
        shipmentTo: OffsetDateTime? = null,
        statuses: List<OrderStatus>? = null
    ): CloseableHttpResponse {
        val request = HttpPost(getUri("/business/1/pi/orders/get-orders", emptyList()))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")

        val requestBody = defaultObjectMapper.writeValueAsString(
            PartnerOrderListingRequest(
                partnerIds = partnerIds,
                ordersType = orderStatusGroup,
                fake = fake,
                pageToken = pageToken,
                pageSize = pageSize,
                responseFields = fields,
                orderIds = orderIds,
                sourcePlatform = sourcePlatform,
                fromTime = fromOffsetDateTime,
                toTime = toOffsetDateTime,
                shipmentDeadlineFrom = shipmentFrom,
                shipmentDeadlineTo = shipmentTo,
                statuses = statuses
            )
        )
        request.entity = StringEntity(requestBody)
        return HttpClientBuilder.create().build().execute(request)
    }

    private fun getOrdersCounters(
        partnerIds: List<Long>,
        fromOffsetDateTime: OffsetDateTime,
        toOffsetDateTime: OffsetDateTime,
    ): CloseableHttpResponse {
        val request = HttpPost(getUri("/business/1/pi/orders/get-order-counters", emptyList()))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        val requestBody = defaultObjectMapper.writeValueAsString(
            PartnerOrderCounterRequest(
                partnerIds = partnerIds,
                fromTime = fromOffsetDateTime,
                toTime = toOffsetDateTime
            )
        )
        request.entity = StringEntity(requestBody)
        return HttpClientBuilder.create().build().execute(request)
    }

    private fun getOrderHistory(partnerId: Long, orderId: Long): CloseableHttpResponse {
        val request = HttpGet(getUri("/partners/$partnerId/pi/orders/$orderId/history", listOf()))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        return HttpClientBuilder.create().build().execute(request)
    }

    private fun getOrderTotals(
        partnerIds: List<Long>,
        orderStatusGroup: OrderStatusGroup,
        fromOffsetDateTime: OffsetDateTime,
        toOffsetDateTime: OffsetDateTime,
        orderIds: List<Long>? = null,
        statuses: List<OrderStatus>? = null
    ): CloseableHttpResponse {
        val request = HttpPost(getUri("/business/1/pi/orders/get-order-totals", emptyList()))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        val requestBody = defaultObjectMapper.writeValueAsString(
            PartnerOrdersPiFilters(
                partnerIds = partnerIds,
                orderIds = orderIds,
                ordersType = orderStatusGroup,
                fake = false,
                fromTime = fromOffsetDateTime,
                toTime = toOffsetDateTime,
                statuses = statuses
            )
        )
        request.entity = StringEntity(requestBody)
        return HttpClientBuilder.create().build().execute(request)
    }

    private fun getOrderTabs(
        partnerIds: List<Long>
    ): CloseableHttpResponse {
        val request = HttpPost(getUri("/business/1/pi/orders/get-order-tabs", emptyList()))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        val requestBody = defaultObjectMapper.writeValueAsString(
            PartnerOrderTabsRequest(
                partnerIds = partnerIds,
            )
        )
        request.entity = StringEntity(requestBody)
        return HttpClientBuilder.create().build().execute(request)
    }

    private fun prepareGetPartnerNotesRequest(partnerId: Long, params: List<Pair<String, String>>): HttpGet {
        val request = HttpGet(getUri("/partners/$partnerId/pi/orders/notes", params))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        return request
    }

    private fun preparePostPartnerNotesRequest(
        partnerId: Long,
        orderId: Long,
        params: List<Pair<String, String>>,
        body: String
    ): HttpPost {
        val request = HttpPost(getUri("/partners/$partnerId/pi/orders/$orderId/notes", params))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        request.entity = StringEntity(body)
        return request
    }

    private fun getOrderTableContents() = orderEntityRepository.selectAll()

    private fun assertExpectedResponse(
        response: CloseableHttpResponse,
        path: String,
        expectedHttpCode: Int = HttpStatus.SC_OK,
        compareMode: JSONCompareMode = JSONCompareMode.STRICT_ORDER
    ): String {
        val content = IOUtils.toString(response.entity.content)
        assertThat(response.statusLine.statusCode).isEqualTo(expectedHttpCode)
        JSONAssert.assertEquals(
            this::class.loadResourceAsString(path),
            content,
            compareMode
        )
        return content
    }

    companion object {
        @JvmStatic
        fun orderListingSource(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(OrderStatusGroup.PENDING),
                Arguments.of(OrderStatusGroup.PROCESSING),
                Arguments.of(OrderStatusGroup.WAITING_FOR_DELIVERY),
                Arguments.of(OrderStatusGroup.IN_DELIVERY),
                Arguments.of(OrderStatusGroup.DELIVERED),
                Arguments.of(OrderStatusGroup.ALL)
            )
        }
    }
}
