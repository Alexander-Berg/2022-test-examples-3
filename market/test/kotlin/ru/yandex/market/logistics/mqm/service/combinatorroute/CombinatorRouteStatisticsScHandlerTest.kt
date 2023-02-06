package ru.yandex.market.logistics.mqm.service.combinatorroute

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
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
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PointType
import ru.yandex.market.logistics.mqm.entity.lom.enums.ServiceCodeName
import ru.yandex.market.logistics.mqm.repository.LomWaybillSegmentRepository
import ru.yandex.market.logistics.mqm.utils.tskvGetByKey
import ru.yandex.market.logistics.mqm.utils.tskvGetExtra
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor
import java.time.Duration
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class CombinatorRouteStatisticsScHandlerTest {
    @Mock
    private lateinit var lomWaybillSegmentRepository: LomWaybillSegmentRepository

    @Mock
    private lateinit var solomonMetricsConverter: SolomonMetricsConverter

    @RegisterExtension
    @JvmField
    final val tskvLogCaptor = BackLogCaptor()

    @Test
    @DisplayName("Удачная обработка")
    fun calculateSuccess() {
        whenever(lomWaybillSegmentRepository.getAllByOrderId(eq(LOM_ORDER_ID)))
            .thenReturn(
                listOf(
                    WaybillSegment(partnerId = WAREHOUSE_PARTNER_1_ID, partnerName = WAREHOUSE_PARTNER_1_ID.toString()),
                    WaybillSegment(partnerId = MOVEMENT_PARTNER_1_ID, partnerName = MOVEMENT_PARTNER_1_ID.toString()),
                    WaybillSegment(partnerId = WAREHOUSE_PARTNER_2_ID, partnerName = WAREHOUSE_PARTNER_2_ID.toString()),
                )
            )
        whenever(solomonMetricsConverter.prepareLabelValue(any())).thenAnswer { it.getArgument(0) }
        val handler = createCombinatorRouteStatisticsHandler()

        handler.handle(LOM_ORDER_ID, createLomOrderCombinatorRoute())

        val first = tskvLogCaptor.results[0]

        tskvGetByKey(first, "code") shouldBe Pair("code", "COMBINATOR_ROUTE_STATISTICS")
        tskvGetExtra(first) shouldContainAll listOf(
            Pair("orderId", LOM_ORDER_ID.toString()),
            Pair("fromPartnerId", WAREHOUSE_PARTNER_1_ID.toString()),
            Pair("toPartnerId", MOVEMENT_PARTNER_1_ID.toString()),
            Pair("difference", "-86400"),
            Pair("servicesDifference", "14400"),
            Pair("warehouseTotal", "100800"),
            Pair("warehouseInboundId", "null"),
            Pair("movementMovementId", "null"),
            Pair("fromPartnerName", WAREHOUSE_PARTNER_1_ID.toString()),
            Pair("toPartnerName", MOVEMENT_PARTNER_1_ID.toString()),
        )

        val second = tskvLogCaptor.results[1]

        tskvGetByKey(second, "code") shouldBe Pair("code", "COMBINATOR_ROUTE_STATISTICS")
        tskvGetExtra(second) shouldContainAll listOf(
            Pair("orderId", LOM_ORDER_ID.toString()),
            Pair("fromPartnerId", WAREHOUSE_PARTNER_2_ID.toString()),
            Pair("toPartnerId", MOVEMENT_PARTNER_2_ID.toString()),
            Pair("difference", "205200"),
            Pair("servicesDifference", "306000"),
            Pair("warehouseTotal", "100800"),
            Pair("warehouseInboundId", "null"),
            Pair("movementMovementId", "null"),
            Pair("fromPartnerName", WAREHOUSE_PARTNER_2_ID.toString()),
            Pair("toPartnerName", "unknown"),
        )
    }

    private fun createCombinatorRouteStatisticsHandler() =
        CombinatorRouteStatisticsScHandler(
            lomWaybillSegmentRepository,
            solomonMetricsConverter,
        )

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

    private fun simplePoints() =
        listOf(
            LomOrderCombinatorRoute.Point(
                segmentType = PointType.WAREHOUSE,
                partnerType = PartnerType.SORTING_CENTER,
                services = listOf(
                    createDeliveryService(ServiceCodeName.INBOUND, WAREHOUSE_INBOUND, 0L),
                    createDeliveryService(ServiceCodeName.SORT, WAREHOUSE_SORT, 1L),
                    createDeliveryService(ServiceCodeName.SHIPMENT, WAREHOUSE_SHIPMENT, 0L),
                    createDeliveryService(ServiceCodeName.WAIT_20, WAREHOUSE_WAIT, 3L),
                ),
                ids = LomOrderCombinatorRoute.PointIds(
                    partnerId = WAREHOUSE_PARTNER_1_ID,
                ),
            ),
            LomOrderCombinatorRoute.Point(
                segmentType = PointType.MOVEMENT,
                partnerType = PartnerType.SORTING_CENTER,
                services = listOf(
                    createDeliveryService(ServiceCodeName.MOVEMENT, MOVEMENT_1_MOVEMENT, 15L),
                ),
                ids = LomOrderCombinatorRoute.PointIds(
                    partnerId = MOVEMENT_PARTNER_1_ID,
                ),
            ),

            LomOrderCombinatorRoute.Point(
                segmentType = PointType.WAREHOUSE,
                partnerType = PartnerType.SORTING_CENTER,
                services = listOf(
                    createDeliveryService(ServiceCodeName.INBOUND, WAREHOUSE_INBOUND, 0L),
                    createDeliveryService(ServiceCodeName.SORT, WAREHOUSE_SORT, 1L),
                    createDeliveryService(ServiceCodeName.SHIPMENT, WAREHOUSE_SHIPMENT, 0L),
                    createDeliveryService(ServiceCodeName.WAIT_20, WAREHOUSE_WAIT, 3L),
                ),
                ids = LomOrderCombinatorRoute.PointIds(
                    partnerId = WAREHOUSE_PARTNER_2_ID,
                ),
            ),
            LomOrderCombinatorRoute.Point(
                segmentType = PointType.MOVEMENT,
                partnerType = PartnerType.SORTING_CENTER,
                services = listOf(
                    createDeliveryService(ServiceCodeName.MOVEMENT, MOVEMENT_2_MOVEMENT, 15L),
                ),
                ids = LomOrderCombinatorRoute.PointIds(
                    partnerId = MOVEMENT_PARTNER_2_ID,
                ),
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
        private const val WAREHOUSE_PARTNER_1_ID = 201L
        private const val WAREHOUSE_PARTNER_2_ID = 202L
        private const val MOVEMENT_PARTNER_1_ID = 301L
        private const val MOVEMENT_PARTNER_2_ID = 302L
        private val WAREHOUSE_INBOUND = Instant.parse("2021-11-18T20:00:00.00Z")
        private val WAREHOUSE_SORT = Instant.parse("2021-11-18T20:00:00.00Z")
        private val WAREHOUSE_SHIPMENT = Instant.parse("2021-11-18T21:00:00.00Z")
        private val WAREHOUSE_WAIT = Instant.parse("2021-11-18T21:00:00.00Z")
        private val MOVEMENT_1_MOVEMENT = Instant.parse("2021-11-19T00:00:00.00Z")
        private val MOVEMENT_2_MOVEMENT = Instant.parse("2021-11-22T09:00:00.00Z")
    }
}
