package ru.yandex.market.mbi.orderservice.api.controller.promo

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.HttpHeaders
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.api.FunctionalTest
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.PromoIndex
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderLineEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.PromoIndexRepository
import ru.yandex.market.mbi.orderservice.model.OrderItemCountsByPromoResponse
import ru.yandex.market.mbi.orderservice.model.OrderItemCountsByPromoResponseAllOfCounts

/**
 * Тесты для [OrdersPromoController]
 */
class OrdersPromoControllerTest : FunctionalTest() {

    @Autowired
    lateinit var orderEntityRepository: OrderEntityRepository

    @Autowired
    lateinit var orderLineRepository: OrderLineEntityRepository

    @Autowired
    lateinit var promoIndexRepository: PromoIndexRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @BeforeEach
    internal fun setUp() {
        this::class.loadTestEntities<OrderEntity>("orders.json").let {
            orderEntityRepository.insertRows(it)
        }
        this::class.loadTestEntities<OrderLineEntity>("order_lines.json").let {
            orderLineRepository.insertRows(it)
        }
        this::class.loadTestEntities<PromoIndex>("promo_idx.json").let {
            promoIndexRepository.insertRows(it)
        }
    }

    @Test
    fun `verify get counts empty`() {
        val partnerId = 1L
        val response = HttpClientBuilder.create().build().execute(
            getOrderCountByPromoRequest(
                partnerId,
                listOf("CODE_3")
            )
        )
        val actualPagedReturnResponse = objectMapper.readValue(
            response.entity.content,
            OrderItemCountsByPromoResponse::class.java
        )

        assertThat(actualPagedReturnResponse).satisfies {
            assertThat(it.counts).isEmpty()
        }
    }

    @Test
    fun `verify get counts without filters`() {
        val partnerId = 1L
        val response = HttpClientBuilder.create().build().execute(getOrderCountByPromoRequest(partnerId, listOf()))
        val actualPagedReturnResponse = objectMapper.readValue(
            response.entity.content,
            OrderItemCountsByPromoResponse::class.java
        )

        assertThat(actualPagedReturnResponse).satisfies {
            assertThat(it.counts.size).isEqualTo(2)
            assertThat(it.counts).containsAll(
                listOf(
                    OrderItemCountsByPromoResponseAllOfCounts(promo = "CODE_1", 8),
                    OrderItemCountsByPromoResponseAllOfCounts(promo = "CODE_2", 4)
                )
            )
        }
    }

    @Test
    fun `verify get counts with filters`() {
        val partnerId = 1L
        val response = HttpClientBuilder.create().build().execute(
            getOrderCountByPromoRequest(
                partnerId,
                listOf("CODE_1")
            )
        )
        val actualPagedReturnResponse = objectMapper.readValue(
            response.entity.content,
            OrderItemCountsByPromoResponse::class.java
        )

        assertThat(actualPagedReturnResponse).satisfies {
            assertThat(it.counts.size).isEqualTo(1)
            assertThat(it.counts).containsAll(
                listOf(
                    OrderItemCountsByPromoResponseAllOfCounts(promo = "CODE_1", 8)
                )
            )
        }
    }

    private fun getOrderCountByPromoRequest(partnerId: Long, promoIds: List<String>?): HttpUriRequest {
        val parameters = promoIds?.map { "promoIds" to it }?.toList() ?: listOf()

        val request = HttpGet(getUri("/partners/$partnerId/promo/order/items/count", parameters))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        return request
    }
}
