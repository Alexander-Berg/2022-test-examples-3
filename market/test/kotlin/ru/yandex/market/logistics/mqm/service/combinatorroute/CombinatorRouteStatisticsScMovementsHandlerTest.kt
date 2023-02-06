package ru.yandex.market.logistics.mqm.service.combinatorroute

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.logistics.mqm.converter.SolomonMetricsConverter
import ru.yandex.market.logistics.mqm.entity.lom.LomOrderCombinatorRoute
import ru.yandex.market.logistics.mqm.entity.lom.enums.PointType
import ru.yandex.market.logistics.mqm.entity.lom.enums.ServiceCodeName
import ru.yandex.market.logistics.mqm.utils.tskvGetByKey
import ru.yandex.market.logistics.mqm.utils.tskvGetExtra
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor
import java.time.Duration
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class CombinatorRouteStatisticsScMovementsHandlerTest {
    @Mock
    private lateinit var solomonMetricsConverter: SolomonMetricsConverter

    @RegisterExtension
    @JvmField
    final val tskvLogCaptor = BackLogCaptor()

    @Test
    @DisplayName("Удачная обработка")
    fun calculateSuccess() {
        val handler = createCombinatorRouteStatisticsHandler()

        whenever(solomonMetricsConverter.prepareLabelValue(any())).thenAnswer { it.getArgument(0) }

        handler.handle(LOM_ORDER_ID, createLomOrderCombinatorRoute())

        val first = tskvLogCaptor.results[0]

        tskvGetByKey(first, "code") shouldBe Pair("code", "COMBINATOR_ROUTE_STATISTICS")
        tskvGetExtra(first) shouldContainAll listOf(
            Pair("orderId", LOM_ORDER_ID.toString()),
            Pair("fromPartnerId", MOVEMENT_PARTNER_1_ID.toString()),
            Pair("toPartnerId", WAREHOUSE_PARTNER_1_ID.toString()),
            Pair("difference", "7200000"),
            Pair("fromPartnerName", MOVEMENT_PARTNER_1_ID.toString()),
            Pair("toPartnerName", WAREHOUSE_PARTNER_1_ID.toString()),
        )

        val second = tskvLogCaptor.results[1]

        tskvGetByKey(second, "code") shouldBe Pair("code", "COMBINATOR_ROUTE_STATISTICS")
        tskvGetExtra(second) shouldContainAll listOf(
            Pair("orderId", LOM_ORDER_ID.toString()),
            Pair("fromPartnerId", MOVEMENT_PARTNER_2_ID.toString()),
            Pair("toPartnerId", WAREHOUSE_PARTNER_2_ID.toString()),
            Pair("difference", "3600000"),
            Pair("fromPartnerName", MOVEMENT_PARTNER_2_ID.toString()),
            Pair("toPartnerName", WAREHOUSE_PARTNER_2_ID.toString()),
        )
    }

    private fun createCombinatorRouteStatisticsHandler() =
        CombinatorRouteStatisticsScMovementsHandler(solomonMetricsConverter)

    private fun createLomOrderCombinatorRoute(
        lomOrderId: Long = LOM_ORDER_ID,
    ): LomOrderCombinatorRoute {
        return LomOrderCombinatorRoute(
            lomOrderId = lomOrderId,
            route = LomOrderCombinatorRoute.DeliveryRoute(
                paths = simplePaths(),
                points = simplePoints(),
            )
        )
    }

    private fun simplePaths() =
        listOf(
            LomOrderCombinatorRoute.Path(0, 1),
            LomOrderCombinatorRoute.Path(1, 2),
            LomOrderCombinatorRoute.Path(2, 3),
        )

    private fun simplePoints() = listOf(
        LomOrderCombinatorRoute.Point(
            segmentType = PointType.MOVEMENT,
            services = listOf(
                createDeliveryService(
                    ServiceCodeName.SHIPMENT,
                    MOVEMENT_SHIPMENT_1_START_TIME,
                    MOVEMENT_SHIPMENT_1_DURATION,
                ),
            ),
            ids = LomOrderCombinatorRoute.PointIds(
                partnerId = MOVEMENT_PARTNER_1_ID,
            ),
            partnerName = MOVEMENT_PARTNER_1_ID.toString(),
        ),
        LomOrderCombinatorRoute.Point(
            segmentType = PointType.WAREHOUSE,
            services = listOf(
                createDeliveryService(
                    ServiceCodeName.INBOUND,
                    WAREHOUSE_INBOUND_1_START_TIME,
                    WAREHOUSE_INBOUND_1_DURATION,
                )
            ),
            ids = LomOrderCombinatorRoute.PointIds(
                partnerId = WAREHOUSE_PARTNER_1_ID,
            ),
            partnerName = WAREHOUSE_PARTNER_1_ID.toString(),
        ),
        LomOrderCombinatorRoute.Point(
            segmentType = PointType.MOVEMENT,
            services = listOf(
                createDeliveryService(
                    ServiceCodeName.MOVEMENT,
                    MOVEMENT_MOVEMENT_1_START_TIME,
                    MOVEMENT_MOVEMENT_1_DURATION,
                ),
            ),
            ids = LomOrderCombinatorRoute.PointIds(
                partnerId = MOVEMENT_PARTNER_2_ID,
            ),
            partnerName = MOVEMENT_PARTNER_2_ID.toString(),
        ),
        LomOrderCombinatorRoute.Point(
            segmentType = PointType.WAREHOUSE,
            services = listOf(
                createDeliveryService(
                    ServiceCodeName.INBOUND,
                    WAREHOUSE_INBOUND_2_START_TIME,
                    WAREHOUSE_INBOUND_2_DURATION,
                )
            ),
            ids = LomOrderCombinatorRoute.PointIds(
                partnerId = WAREHOUSE_PARTNER_2_ID,
            ),
            partnerName = WAREHOUSE_PARTNER_2_ID.toString(),
        ),
    )

    private fun createDeliveryService(
        code: ServiceCodeName,
        startTime: Instant,
        durationHours: Long,
    ) =
        LomOrderCombinatorRoute.DeliveryService(
            code = code,
            startTime = LomOrderCombinatorRoute.Timestamp(
                seconds = startTime.epochSecond,
                nanos = startTime.nano,
            ),
            duration = duration(Duration.ofHours(durationHours).toSeconds()),
        )

    private fun duration(seconds: Long) = LomOrderCombinatorRoute.Timestamp(seconds = seconds, nanos = 0)

    companion object {
        private const val LOM_ORDER_ID = 1001L
        private const val MOVEMENT_PARTNER_1_ID = 1L
        private val MOVEMENT_SHIPMENT_1_START_TIME = Instant.parse("2021-11-18T20:00:00.00Z")
        private const val MOVEMENT_SHIPMENT_1_DURATION = 2L
        private const val WAREHOUSE_PARTNER_1_ID = 2L
        private val WAREHOUSE_INBOUND_1_START_TIME = Instant.parse("2021-11-18T20:00:00.00Z")
        private const val WAREHOUSE_INBOUND_1_DURATION = 1L
        private const val MOVEMENT_PARTNER_2_ID = 3L
        private val MOVEMENT_MOVEMENT_1_START_TIME = Instant.parse("2021-11-18T20:00:00.00Z")
        private const val MOVEMENT_MOVEMENT_1_DURATION = 1L
        private const val WAREHOUSE_PARTNER_2_ID = 4L
        private val WAREHOUSE_INBOUND_2_START_TIME = Instant.parse("2021-11-18T20:00:00.00Z")
        private const val WAREHOUSE_INBOUND_2_DURATION = 0L
    }
}
