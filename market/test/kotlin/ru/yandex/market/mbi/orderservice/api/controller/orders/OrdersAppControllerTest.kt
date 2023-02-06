package ru.yandex.market.mbi.orderservice.api.controller.orders

import org.apache.commons.io.IOUtils
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
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
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEditRequestEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLogisticsEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderSparseIndex
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEditRequestRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderLineEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderLogisticsEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderSparseIndexRepository
import ru.yandex.market.mbi.orderservice.model.OrderAppSortField
import java.time.Clock
import java.time.Instant

/**
 * Тесты для [OrdersAppController]
 */
@CleanupTables(
    [
        OrderEntity::class,
        OrderLineEntity::class,
        OrderLogisticsEntity::class,
        OrderSparseIndex::class,
        OrderEditRequestEntity::class,
    ]
)
class OrdersAppControllerTest : FunctionalTest() {

    @Autowired
    lateinit var orderEntityRepository: OrderEntityRepository

    @Autowired
    lateinit var orderLineEntityRepository: OrderLineEntityRepository

    @Autowired
    lateinit var orderLogisticsEntityRepository: OrderLogisticsEntityRepository

    @Autowired
    lateinit var orderSparseIndexRepository: OrderSparseIndexRepository

    @Autowired
    lateinit var orderEditRequestRepository: OrderEditRequestRepository

    @Autowired
    lateinit var clock: Clock

    @BeforeEach
    internal fun setUp() {
        val orders = this::class.loadTestEntities<OrderEntity>("app/orders.json")
        val ordersLines = this::class.loadTestEntities<OrderLineEntity>("app/ordersLines.json")
        val orderLogistics = this::class.loadTestEntities<OrderLogisticsEntity>("app/orderLogistics.json")
        val sparseIndexRows = this::class.loadTestEntities<OrderSparseIndex>("app/sparse_idx.json")
        val orderEditRequests = this::class.loadTestEntities<OrderEditRequestEntity>("app/ordersEditRequests.json")
        orderEntityRepository.insertRows(orders)
        orderLineEntityRepository.insertRows(ordersLines)
        orderLogisticsEntityRepository.insertRows(orderLogistics)
        orderSparseIndexRepository.insertRows(sparseIndexRows)
        orderEditRequestRepository.insertRows(orderEditRequests)

        whenever(clock.instant()).thenReturn(Instant.parse("2021-12-12T10:00:00Z"))
    }

    @DisplayName("Проверка получения заказа")
    @Test
    fun `verify get order`() {
        verifyGetOrder(81545127, "OrdersAppControllerTest.verifyGetOrder.json")
    }

    @DisplayName("Проверка получения заказа, где shipmentDeadline = 0, дедлайн должен быть примерным")
    @Test
    fun `get order when shipmentDeadline = 0`() {
        verifyGetOrder(
            partnerId = 544522,
            orderId = 81545135,
            expectedFile = "OrdersAppControllerTest.verifyGetOrderWithZeroShipmentDeadline.json"
        )
    }

    @DisplayName("Попытка получения не существующего заказа -> NOT_FOUND")
    @Test
    fun `verify get non existing order`() {
        verifyGetOrder(22, expectedHttpStatus = HttpStatus.SC_NOT_FOUND)
    }

    @DisplayName("Проверка получения заказов с пагинацией")
    @Test
    fun `verify get orders with pagination`() {
        val params = mapOf(
            "deliveryFeatures" to "EXPRESS_DELIVERY",
            "page" to "2",
            "size" to "1"
        )
        verifyGetOrders(params, "OrdersAppControllerTest.verifyGetOrders.withPage.json")
    }

    @DisplayName("Проверка получения заказов с пагинацией (ожидается, что hasNextPage = true)")
    @Test
    fun `check hasNextPage`() {
        val params = mapOf(
            "deliveryFeatures" to "EXPRESS_DELIVERY",
            "page" to "1",
            "size" to "1"
        )
        verifyGetOrders(params, "OrdersAppControllerTest.verifyGetOrders.hasNextPage.json")
    }

    @DisplayName("Проверка получения экспресс заказов")
    @Test
    fun `verify get express orders only`() {
        verifyGetOrders(
            mapOf("deliveryFeatures" to "EXPRESS_DELIVERY"),
            "OrdersAppControllerTest.verifyGetOrders.express.json"
        )
    }

    @DisplayName("Проверка получения заказов с hasCancellationRequest = true")
    @Test
    fun `verify get orders with hasCancellationRequest`() {
        verifyGetOrders(
            mapOf("hasCancellationRequest" to "true"),
            "OrdersAppControllerTest.filter.hasCancellationRequest.json"
        )
    }

    @DisplayName("Проверка получения всех заказов за 30 дней")
    @Test
    fun `verify get last orders`() {
        verifyGetOrders(null, "OrdersAppControllerTest.verifyGetOrders.all.json")
    }

    @DisplayName("Проверка работоспособности параметра fake")
    @Test
    fun `check request with fake param`() {
        verifyGetOrders(
            mapOf("fake" to "true"),
            "OrdersAppControllerTest.params.fake.json"
        )
    }

    @DisplayName("Проверка получения заказов за определенный день")
    @Test
    fun `verify filter orders by creation date`() {
        val params = mapOf(
            "createdFrom" to "2021-12-11T00:00:00+00:00",
            "createdTo" to "2021-12-11T23:59:59+00:00",
        )
        verifyGetOrders(params, "OrdersAppControllerTest.verifyGetOrders.sameDay.json")
    }

    @DisplayName("Проверка получения заказов в определенных статусах")
    @Test
    fun `verify filter orders by statuses`() {
        val params = mapOf(
            "statuses" to "PROCESSING,DELIVERY",
            "excludeStatuses" to "DELIVERY",
        )
        verifyGetOrders(params, "OrdersAppControllerTest.verifyGetOrders.byStatuses.json")
    }

    @DisplayName("Проверка получения заказов в определенных сабстатусах")
    @Test
    fun `verify filter orders by substatuses`() {
        val params = mapOf(
            "statuses" to "PROCESSING,DELIVERY",
            "subStatuses" to "STARTED,DELIVERY_SERVICE_RECEIVED",
            "excludeSubStatuses" to "STARTED",
        )
        verifyGetOrders(params, "OrdersAppControllerTest.verifyGetOrders.bySubstatuses.json")
    }

    @DisplayName("Проверка сортировки по")
    @ParameterizedTest
    @EnumSource(value = OrderAppSortField::class, names = [
        "SHIPMENT_DEADLINE_DESC",
        "ORDER_CREATED_ASC",
        "ORDER_CREATED_DESC",
        "DELIVERY_DATE_ASC",
        "DELIVERY_DATE_DESC",
        "ORDER_UPDATED_ASC",
        "ORDER_UPDATED_DESC",
        "ORDER_DELIVERED_ASC",
        "ORDER_DELIVERED_DESC",
    ])
    fun `verify get sorted orders`(field: OrderAppSortField) {
        val params = mapOf(
            "sort" to field.name,
        )
        verifyGetOrders(params, "OrdersAppControllerTest.sort.${field.name.lowercase()}.json")
    }

    @DisplayName("Проверка сортировки по SHIPMENT_DEADLINE ASC + пагинация")
    @Test
    fun `verify get sorted orders with pagination`() {
        val params = mapOf(
            "sort" to "SHIPMENT_DEADLINE_ASC",
            "page" to "2",
            "size" to "1"
        )
        verifyGetOrders(params, "OrdersAppControllerTest.verifyGetOrders.orderDescPagination.json")
    }

    @DisplayName("Проверка получения заказов по id")
    @Test
    fun `verify get orders by id`() {
        val params = mapOf(
            "orderId" to "81545128"
        )
        verifyGetOrders(params, "OrdersAppControllerTest.verifyGetOrders.byId.json")
    }

    @DisplayName("Проверка фильтрации по shipmentDeadline")
    @ParameterizedTest(name = "[$INDEX_PLACEHOLDER] {3}")
    @CsvSource(value = [
        ",2021-12-08T21:46:40+00:00,empty,to задан до shipmentDeadline заказов -> пустой список",
        "2021-12-20T11:31:40+00:00,,empty,from задан после shipmentDeadline заказов -> пустой список",
        "2021-12-11T15:30:00+00:00,2021-12-14T15:00:00+00:00,81545128_81545129,2 заказа с EXACT и APPROXIMATE дедлайнами",
    ])
    fun `verify get orders filtered by shipment deadline`(
        from: String?,
        to: String?,
        file: String,
        description: String,
    ) {
        val params = listOfNotNull(
            from?.let { Pair("shipmentDeadlineFrom", it) },
            to?.let { Pair("shipmentDeadlineTo", it) }
        ).toMap()
        verifyGetOrders(params, "OrdersAppControllerTest.filter.shipmentDeadline.$file.json")
    }

    @DisplayName("Проверка фильтрации по deliveryDate")
    @ParameterizedTest(name = "[$INDEX_PLACEHOLDER] {3}")
    @CsvSource(value = [
        ",2021-12-08T21:46:40+00:00,empty,to задан до deliveryDate заказов -> пустой список",
        "2021-12-20T11:31:40+00:00,,empty,from задан после deliveryDate заказов -> пустой список",
        "2021-12-15T00:00:00+00:00,2021-12-19T00:00:00+00:00,81545130_81545128,-> 2 заказа",
    ])
    fun `verify get orders filtered by deliveryDate`(
        from: String?,
        to: String?,
        file: String,
        description: String,
    ) {
        val params = listOfNotNull(
            from?.let { Pair("deliveryDateFrom", it) },
            to?.let { Pair("deliveryDateTo", it) }
        ).toMap()
        verifyGetOrders(params, "OrdersAppControllerTest.filter.deliveryDate.$file.json")
    }

    @DbUnitDataSet(before = ["app/verifyCountOrders.before.csv"])
    @Test
    fun `verify get order count by status`() {
        val params = mapOf(
            "createdFrom" to "2021-12-11T00:00:00+00:00",
        )
        verifyCountOrdersByStatus(params, "OrdersAppControllerTest.verifyCountOrdersByStatus.basic.json")
    }

    private fun verifyGetOrder(
        orderId: Long,
        expectedFile: String? = null,
        partnerId: Long = 543900,
        expectedHttpStatus: Int = HttpStatus.SC_OK
    ) {
        val request = HttpGet(getUri("/partners/$partnerId/app/orders/$orderId", emptyMap()))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        val response = HttpClientBuilder.create().build().execute(request)
        assertThat(response.statusLine.statusCode).isEqualTo(expectedHttpStatus)
        if (expectedFile != null) {
            JSONAssert.assertEquals(
                this::class.loadResourceAsString("app/response/$expectedFile"),
                IOUtils.toString(response.entity.content),
                JSONCompareMode.STRICT_ORDER
            )
        }
    }

    private fun verifyGetOrders(
        params: Map<String, String>?,
        expectedFile: String,
        partnerId: Long = 543900,
        expectedHttpStatus: Int = HttpStatus.SC_OK
    ) {
        val response = HttpClientBuilder.create().build().execute(prepareGetShopOrdersRequest(partnerId, params))
        assertThat(response.statusLine.statusCode).isEqualTo(expectedHttpStatus)
        JSONAssert.assertEquals(
            this::class.loadResourceAsString("app/response/$expectedFile"),
            IOUtils.toString(response.entity.content),
            JSONCompareMode.STRICT_ORDER
        )
    }

    private fun prepareGetShopOrdersRequest(partnerId: Long, params: Map<String, String>?): HttpGet {
        val request = HttpGet(getUri("/partners/$partnerId/app/orders", params ?: emptyMap()))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        return request
    }

    private fun verifyCountOrdersByStatus(
        params: Map<String, String>?,
        expectedFile: String,
        partnerId: Long = 543900,
        expectedHttpStatus: Int = HttpStatus.SC_OK
    ) {
        val response = HttpClientBuilder.create().build().execute(prepareGetShopOrdersCountRequest(partnerId, params))
        assertThat(response.statusLine.statusCode).isEqualTo(expectedHttpStatus)
        val body = IOUtils.toString(response.entity.content)
        JSONAssert.assertEquals(
            this::class.loadResourceAsString("app/response/$expectedFile"),
            body,
            JSONCompareMode.STRICT_ORDER
        )
    }

    private fun prepareGetShopOrdersCountRequest(partnerId: Long, params: Map<String, String>?): HttpGet {
        val request = HttpGet(getUri("/partners/$partnerId/app/orders/count", params ?: emptyMap()))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        return request
    }
}
