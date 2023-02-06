package ru.yandex.market.mbi.orderservice.api.controller.summary

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.common.util.currency.Currency
import ru.yandex.market.checkout.checkouter.order.Color
import ru.yandex.market.checkout.checkouter.order.VatType
import ru.yandex.market.mbi.orderservice.api.FunctionalTest
import ru.yandex.market.mbi.orderservice.api.assertBody
import ru.yandex.market.mbi.orderservice.api.persistence.dao.yt.summary.WeeklyCompensatedOrdersDao
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderStatus
import ru.yandex.market.mbi.orderservice.common.model.yt.CheckouterOrderIdIndex
import ru.yandex.market.mbi.orderservice.common.model.yt.ItemStatuses
import ru.yandex.market.mbi.orderservice.common.model.yt.LongWrapper
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineKey
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.CheckouterOrderIdIndexRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderLineEntityRepository

class WeeklyCompensatedOrdersControllerTest : FunctionalTest() {

    @Autowired
    lateinit var weeklyCompensatedOrdersDao: WeeklyCompensatedOrdersDao

    @Autowired
    lateinit var orderIdxRepository: CheckouterOrderIdIndexRepository

    @Autowired
    lateinit var orderEntityRepository: OrderEntityRepository

    @Autowired
    lateinit var orderLineEntityRepository: OrderLineEntityRepository

    @BeforeAll
    fun init() {
        orderIdxRepository.insertRow(CheckouterOrderIdIndex(
            orderId = LongWrapper(1L),
            orderKeys = listOf(OrderKey(partnerId = 11133L, orderId = 1L))
        ))
        orderEntityRepository.insertRow(
            OrderEntity(
                key = OrderKey(
                    partnerId = 11133L,
                    orderId = 1L
                ),
                lineIds = listOf(1L),
                currency = Currency.RUR.name,
                buyerCurrency = Currency.RUR.name,
                status = MerchantOrderStatus.RETURNED,
                isFake = false,
                isFulfillment = false,
                color = Color.BLUE
            )
        )
        orderLineEntityRepository.insertRow(
            OrderLineEntity(
                key = OrderLineKey(
                    partnerId = 11133L,
                    orderId = 1L,
                    orderLineId = 1L
                ),
                price = 13200L,
                subsidy = 100L,
                countInDelivery = 1,
                vatRate = VatType.VAT_20,
                initialCount = 1,
                itemStatuses = ItemStatuses(orderLineId = 1, mapOf()),
                shopSku = "shopsku1",
                warehouseId = 1
            )
        )
    }

    @Test
    fun `test get weekly automatically compensated orders`() {
        whenever(weeklyCompensatedOrdersDao.getWeeklyCompensatedOrders(any(), eq(true)))
            .doReturn(listOf(1L))
        val request = HttpGet(getUri("partners/summary/compensated-orders", emptyMap()))
        val response = HttpClientBuilder.create().build().execute(request)
        Assertions.assertThat(response.statusLine.statusCode).isEqualTo(200)
        assertBody(
            response,
            // language=json
            """
                [
                 {
                   "partnerId": 11133,
                   "orders": [
                      {
                        "orderId": 1,
                        "orderAmount":  13300
                      }
                   ]
                 }
                ]
            """.trimIndent()
        )
    }

    @Test
    fun `test get empty weekly automatically compensated orders`() {
        whenever(weeklyCompensatedOrdersDao.getWeeklyCompensatedOrders(any(), eq(true)))
            .doReturn(listOf())
        val request = HttpGet(getUri("partners/summary/compensated-orders", emptyMap()))
        val response = HttpClientBuilder.create().build().execute(request)
        Assertions.assertThat(response.statusLine.statusCode).isEqualTo(200)
        assertBody(
            response,
            // language=json
            """
                []
            """.trimIndent()
        )
    }

    @Test
    fun `test get weekly compensated orders by claim`() {
        whenever(weeklyCompensatedOrdersDao.getWeeklyCompensatedOrders(any(), eq(false)))
            .doReturn(listOf(1L))
        val request = HttpGet(getUri("partners/summary/compensated-orders-by-claim", emptyMap()))
        val response = HttpClientBuilder.create().build().execute(request)
        Assertions.assertThat(response.statusLine.statusCode).isEqualTo(200)
        assertBody(
            response,
            // language=json
            """
                [
                 {
                   "partnerId": 11133,
                   "orders": [
                      {
                        "orderId": 1,
                        "orderAmount":  13300
                      }
                   ]
                 }
                ]
            """.trimIndent()
        )
    }
    @Test
    fun `test get empty weekly compensated orders by claim`() {
        whenever(weeklyCompensatedOrdersDao.getWeeklyCompensatedOrders(any(), eq(false)))
            .doReturn(listOf())
        val request = HttpGet(getUri("partners/summary/compensated-orders-by-claim", emptyMap()))
        val response = HttpClientBuilder.create().build().execute(request)
        Assertions.assertThat(response.statusLine.statusCode).isEqualTo(200)
        assertBody(
            response,
            // language=json
            """
                []
            """.trimIndent()
        )
    }
}
