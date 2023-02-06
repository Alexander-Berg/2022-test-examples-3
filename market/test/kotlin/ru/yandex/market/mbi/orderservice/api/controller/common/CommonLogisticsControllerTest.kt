package ru.yandex.market.mbi.orderservice.api.controller.common

import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.commons.io.IOUtils
import org.apache.http.HttpStatus
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import ru.yandex.market.logistics.logistics4shops.api.InternalOrderApi
import ru.yandex.market.logistics.logistics4shops.api.model.LogisticOrderBox
import ru.yandex.market.logistics.logistics4shops.api.model.LogisticOrderInfo
import ru.yandex.market.logistics.logistics4shops.api.model.LogisticOrderSearchResponse
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.defaultTestMapper
import ru.yandex.market.mbi.helpers.loadResourceAsString
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.api.FunctionalTest
import ru.yandex.market.mbi.orderservice.common.model.dto.combinator.DeliveryOption
import ru.yandex.market.mbi.orderservice.common.model.dto.combinator.DeliveryOptionAddress
import ru.yandex.market.mbi.orderservice.common.model.dto.combinator.DeliveryPaymentMethod
import ru.yandex.market.mbi.orderservice.common.model.dto.combinator.DeliveryTimeInterval
import ru.yandex.market.mbi.orderservice.common.model.dto.stocks.StockItem
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLogisticsEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderShipment
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderLineEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderLogisticsEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderShipmentsRepository
import ru.yandex.market.mbi.orderservice.common.service.external.CombinatorApiService
import ru.yandex.market.mbi.orderservice.common.service.external.GeocoderApiService
import ru.yandex.market.mbi.orderservice.common.service.external.StockStorageApiService
import ru.yandex.market.mbi.orderservice.model.CommonApiErrorResponse
import java.time.LocalDate
import java.time.LocalTime

/**
 * Тесты для [CommonLogisticsController]
 */
@CleanupTables([OrderShipment::class])
class CommonLogisticsControllerTest : FunctionalTest() {

    @Autowired
    lateinit var combinatorApiService: CombinatorApiService

    @Autowired
    lateinit var geocoderApiService: GeocoderApiService

    @Autowired
    lateinit var stockStorageApiService: StockStorageApiService

    @Autowired
    lateinit var shipmentsRepository: OrderShipmentsRepository

    @Autowired
    lateinit var orderRepository: OrderEntityRepository

    @Autowired
    lateinit var orderLineEntityRepository: OrderLineEntityRepository

    @Autowired
    lateinit var logisticsEntityRepository: OrderLogisticsEntityRepository

    @Autowired
    lateinit var internalOrderApi: InternalOrderApi

    @BeforeEach
    fun setUp() {
        val shipments = this::class.loadTestEntities<OrderShipment>("shipments/order_shipments.json")
        shipmentsRepository.insertRows(shipments)
        val orders = this::class.loadTestEntities<OrderEntity>("shipments/orders.json")
        orderRepository.insertRows(orders)
        val lines = this::class.loadTestEntities<OrderLineEntity>("shipments/order_lines.json")
        orderLineEntityRepository.insertRows(lines)
        val logistics = this::class.loadTestEntities<OrderLogisticsEntity>("shipments/order_logistics.json")
        logisticsEntityRepository.insertRows(logistics)
    }

    @DisplayName("Получение заказов в отгрузках")
    @Test
    fun `Test shipment orders`() {
        val shipments = this::class.loadTestEntities<OrderShipment>("shipments/order_shipments.json")
        shipmentsRepository.insertRows(shipments)

        val result = post(
            "/business/123/common/logistics/shipments/get-shipment-orders",
            listOf(),
            this::class.loadResourceAsString(
                "shipments/request/CommonLogisticsControllerTest.getShipmentOrders.request.json"
            )
        )
        assertExpectedResponse(
            result,
            "shipments/response/CommonLogisticsControllerTest.getShipmentOrders.response.json"
        )
    }

    @DisplayName("Получение заказов в отгрузках")
    @Test
    fun `Test shipment orders separate cancelled`() {
        val shipments = this::class.loadTestEntities<OrderShipment>("shipments/order_shipments.json")
        shipmentsRepository.insertRows(shipments)

        val result = post(
            "/business/123/common/logistics/shipments/get-shipment-orders",
            listOf("filterCancelled" to "true"),
            this::class.loadResourceAsString(
                "shipments/request/CommonLogisticsControllerTest.getShipmentOrders.request.json"
            )
        )
        assertExpectedResponse(
            result,
            "shipments/response/CommonLogisticsControllerTest.getShipmentOrders.response.separateCancelled.json"
        )
    }

    @DisplayName("Получение заказов в отгрузках при пустом списке отгрузок")
    @Test
    fun `Test get shipment orders empty identifiers`() {
        val shipments = this::class.loadTestEntities<OrderShipment>("shipments/order_shipments.json")
        shipmentsRepository.insertRows(shipments)
        val result = post(
            "/business/123/common/logistics/shipments/get-shipment-orders",
            listOf(),
            this::class.loadResourceAsString(
                "shipments/request/CommonLogisticsControllerTest.getShipmentOrdersEmpty.request.json"
            )
        )
        assertExpectedResponse(
            result,
            "shipments/response/CommonLogisticsControllerTest.getShipmentOrdersEmpty.response.json"
        )
    }

    @Test
    fun `Test shipment order counts`() {
        val result = post(
            "/business/123/common/logistics/shipments/get-shipment-order-counts",
            listOf(),
            this::class.loadResourceAsString(
                "shipments/request/CommonLogisticsControllerTest.getShipmentOrders.request.json"
            )
        )
        assertExpectedResponse(
            result,
            "shipments/response/CommonLogisticsControllerTest.getShipmentOrderCounts.response.json"
        )
    }

    @Test
    fun `Test validate order labels`() {
        mockOrderBoxes()
        val result = post(
            "/business/123/common/logistics/shipments/validate-order-labels",
            listOf(),
            this::class.loadResourceAsString(
                "shipments/request/CommonLogisticsControllerTest.validateOrderLabels.request.json"
            )
        )
        assertExpectedResponse(
            result,
            "shipments/response/CommonLogisticsControllerTest.validateOrderLabels.response.json"
        )
    }

    @Test
    fun `Test shipment order counts filter cancelled orders`() {
        val result = post(
            "/business/123/common/logistics/shipments/get-shipment-order-counts",
            listOf("filterCancelled" to "true"),
            this::class.loadResourceAsString(
                "shipments/request/CommonLogisticsControllerTest.getShipmentOrders.request.json"
            )
        )
        assertExpectedResponse(
            result,
            "shipments/response/CommonLogisticsControllerTest.getShipmentOrderCounts.filterCancelled.response.json"
        )
    }

    @DisplayName("Получение опций доставки, проверки маппингов")
    @Test
    fun `test delivery options mappings`() {
        whenever(stockStorageApiService.getAvailableAmounts(any()))
            .thenReturn(
                listOf(
                    StockItem(
                        partnerId = 123,
                        shopSku = "string",
                        warehouseId = 445,
                        count = 3
                    )
                )
            )

        whenever(geocoderApiService.getDeliveryAddresses(any()))
            .thenReturn(
                listOf(
                    DeliveryOptionAddress(
                        city = "8686",
                        street = "7879",
                        house = "8668",
                        geoId = 213,
                        latitude = 0.0,
                        longitude = 0.0
                    )
                )
            )
        whenever(combinatorApiService.getCourierOptions(any(), any(), any()))
            .thenReturn(
                listOf(
                    DeliveryOption(
                        deliveryDateFrom = LocalDate.of(1, 2, 3),
                        deliveryDateTo = LocalDate.of(1, 2, 3),
                        deliveryTimeInterval = DeliveryTimeInterval(
                            from = LocalTime.of(1, 2),
                            to = LocalTime.of(3, 4)
                        ),
                        deliveryServiceId = 5,
                        paymentMethods = listOf(DeliveryPaymentMethod.PREPAYMENT),
                        leaveAtTheDoor = false,
                        doNotCall = true,
                        customizers = listOf()
                    )
                )
            )

        val result = post(
            "/partners/123/common/logistics/delivery/get-delivery-options",
            listOf(),
            this::class.loadResourceAsString("delivery/request/CommonLogisticsControllerTest.getDeliveryOptions.request.json")
        )
        assertExpectedResponse(
            result,
            "delivery/response/CommonLogisticsControllerTest.getDeliveryOptions.response.json"
        )
    }

    @Test
    fun `verify that insufficient stocks returns error code 400`() {
        whenever(stockStorageApiService.getAvailableAmounts(any()))
            .thenReturn(
                listOf(
                    StockItem(
                        partnerId = 123,
                        shopSku = "sku1",
                        warehouseId = 677,
                        count = 3
                    ),
                    StockItem(
                        partnerId = 123,
                        shopSku = "sku2",
                        warehouseId = 677,
                        count = 1
                    )
                )
            )

        val result = post(
            "/partners/123/common/logistics/delivery/get-delivery-options",
            listOf(),
            this::class.loadResourceAsString("delivery/request/CommonLogisticsControllerTest.insufficientStocks.request.json")
        )
        val response = defaultTestMapper.readValue<CommonApiErrorResponse>(
            IOUtils.toString(result.entity.content)
        ).errors
        assertThat(response)
            .first()
            .extracting("message")
            .isEqualTo("Not enough stock for sskus")
    }

    private fun mockOrderBoxes() {
        whenever(internalOrderApi.internalSearchOrders(any())).thenReturn(
            LogisticOrderSearchResponse()
                .orders(
                    listOf(
                        LogisticOrderInfo().id("10204").boxes(listOf(LogisticOrderBox().barcode("barcode1"))),
                        LogisticOrderInfo().id("10200").boxes(listOf(LogisticOrderBox().barcode("barcode2")))
                    )
                )
        )
    }

    private fun post(
        path: String,
        params: List<Pair<String, String>>,
        body: String
    ): CloseableHttpResponse {
        val request = HttpPost(getUri(path, params))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        request.entity = StringEntity(body)
        return HttpClientBuilder.create().build().execute(request)
    }

    private fun assertExpectedResponse(response: CloseableHttpResponse, path: String) {
        val content = IOUtils.toString(response.entity.content)
        assertThat(response.statusLine.statusCode).isEqualTo(HttpStatus.SC_OK)
        JSONAssert.assertEquals(
            this::class.loadResourceAsString(path),
            content,
            JSONCompareMode.STRICT_ORDER
        )
    }
}
