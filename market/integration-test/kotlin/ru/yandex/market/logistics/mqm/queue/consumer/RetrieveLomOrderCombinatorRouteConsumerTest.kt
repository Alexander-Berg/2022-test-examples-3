package ru.yandex.market.logistics.mqm.queue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.lom.client.LomClient
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.queue.dto.LomOrderIdBarcodeDto
import java.math.BigDecimal
import java.util.Optional

class RetrieveLomOrderCombinatorRouteConsumerTest : AbstractContextualTest() {
    @Autowired
    lateinit var consumer: RetrieveLomOrderCombinatorRouteConsumer

    @Autowired
    lateinit var lomClient: LomClient

    @Test
    @DatabaseSetup("/queue/consumer/before/retrieve_lom_order_combinator_route_consumer/saved_route.xml")
    @ExpectedDatabase(
        value = "/queue/consumer/after/retrieve_lom_order_combinator_route_consumer/saved_route.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testConsumer() {
        whenever(lomClient.getRouteByOrderBarcode(eq("orderId")))
            .thenReturn(Optional.of(buildCombinatorRoute()))
        consumer.processPayload(LomOrderIdBarcodeDto(123L, "orderId"))
    }

    fun buildCombinatorRoute() = CombinatorRoute().setRoute(
        CombinatorRoute.DeliveryRoute()
            .setCost(BigDecimal.ONE)
            .setDateFrom(CombinatorRoute.Date().setDay(1).setMonth(1).setYear(2021))
            .setTariffId(12L)
            .setPoints(mutableListOf(
                CombinatorRoute.Point()
                    .setIds(
                        CombinatorRoute.PointIds()
                            .setRegionId(2)
                            .setPartnerId(125)
                    )
            ))
    )
}
