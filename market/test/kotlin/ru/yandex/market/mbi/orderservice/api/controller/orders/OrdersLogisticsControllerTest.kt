package ru.yandex.market.mbi.orderservice.api.controller.orders

import org.apache.commons.io.IOUtils
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import ru.yandex.market.mbi.helpers.loadResourceAsString
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.api.FunctionalTest
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderLineEntityRepository

/**
 * Тесты для [OrdersLogisticsController]
 */
class OrdersLogisticsControllerTest : FunctionalTest() {

    @Autowired
    lateinit var orderEntityRepository: OrderEntityRepository

    @Autowired
    lateinit var orderLineEntityRepository: OrderLineEntityRepository

    @BeforeEach
    internal fun setUp() {
        val orders = this::class.loadTestEntities<OrderEntity>("logistics/orders.json")
        val ordersLines = this::class.loadTestEntities<OrderLineEntity>("logistics/orderLines.json")
        orderEntityRepository.insertRows(orders)
        orderLineEntityRepository.insertRows(ordersLines)
    }

    @DisplayName("Проверка получения заказа")
    @Test
    fun `test get order`() {
        verifyGetOrder(543900, 81545128, "OrdersLogisticsControllerTest.getOrder.response.json")
    }

    @DisplayName("Заказ не найден, в ответе должно вернуться ORDER_NOT_FOUND")
    @Test
    fun `test order not found exception`() {
        verifyGetOrder(111, 222, "OrdersLogisticsControllerTest.notFound.response.json", HttpStatus.SC_NOT_FOUND)
    }

    private fun verifyGetOrder(
        partnerId: Long, orderId: Long, expectedFile: String,
        expectedHttpStatus: Int = HttpStatus.SC_OK
    ) {
        val response = HttpClientBuilder.create().build().execute(prepareGetShopOrdersRequest(partnerId, orderId))
        assertThat(response.statusLine.statusCode).isEqualTo(expectedHttpStatus)
        JSONAssert.assertEquals(
            this::class.loadResourceAsString("logistics/response/$expectedFile"),
            IOUtils.toString(response.entity.content),
            JSONCompareMode.STRICT_ORDER
        )
    }

    private fun prepareGetShopOrdersRequest(partnerId: Long, orderId: Long): HttpGet {
        val request = HttpGet(getUri("/partners/$partnerId/logistics/orders/$orderId", emptyMap()))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        return request
    }
}
