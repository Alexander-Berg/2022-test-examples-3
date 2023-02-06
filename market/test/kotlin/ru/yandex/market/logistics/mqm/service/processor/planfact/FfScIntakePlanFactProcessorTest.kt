package ru.yandex.market.logistics.mqm.service.processor.planfact

import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.logistics.mqm.entity.lom.LomOrderCombinatorRoute
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PointType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.ServiceCodeName
import ru.yandex.market.logistics.mqm.entity.management.PartnerRelationParams
import ru.yandex.market.logistics.mqm.service.CombinatorRouteService
import ru.yandex.market.logistics.mqm.service.management.PartnerRelationParamsService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint

@ExtendWith(MockitoExtension::class)
class FfScIntakePlanFactProcessorTest {

    @Mock
    private lateinit var combinatorRouteService: CombinatorRouteService

    @Mock
    private lateinit var partnerRelationParamsService: PartnerRelationParamsService

    private lateinit var processor: FfScIntakePlanFactProcessor

    @BeforeEach
    fun startUp() {
        processor = FfScIntakePlanFactProcessor(combinatorRouteService, partnerRelationParamsService)
    }

    @Test
    @DisplayName("Проверка применимости процессора")
    fun isEligible() {
        val waybillSegment = prepareTestData()
        processor.isEligible(waybillSegment) shouldBe true
    }

    @Test
    @DisplayName("Процессор не применим, если нет предыдущего сегмента")
    fun isNotEligibleIfNoPreviousSegment() {
        val waybillSegment = prepareTestData(hasPreviousSegment = false)
        processor.isEligible(waybillSegment) shouldBe false
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["FULFILLMENT"]
    )
    @DisplayName("Процессор не применим, если тип партнера предыдущего сегмента не FULFILLMENT")
    fun isNotEligibleIfPreviousIsNotFulfillment(previousPartnerType: PartnerType) {
        val waybillSegment = prepareTestData(previousPartnerType = previousPartnerType)
        processor.isEligible(waybillSegment) shouldBe false
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["SORTING_CENTER"]
    )
    @DisplayName("Процессор не применим, если тип партнера сегмента не SORTING_CENTER")
    fun isNotEligibleIfCurrentIsNotSortingCenter(partnerType: PartnerType) {
        val waybillSegment = prepareTestData(currentPartnerType = partnerType)
        processor.isEligible(waybillSegment) shouldBe false
    }

    @Test
    @DisplayName("Процессор не применим, если нет нужного чекпоинта в истории")
    fun isNotEligibleIfNoCheckpointInHistory() {
        val waybillSegment = prepareTestData(isPreviousSegmentHas130 = false)
        processor.isEligible(waybillSegment) shouldBe false
    }

    @Test
    @DisplayName("Расчет ожидаемого времени")
    fun calculateExpectedDatetime() {
        setUpCombinatorRouteService()
        setUpPartnerRelationParamsService()
        val waybillSegment = prepareTestData()
        processor.calculateExpectedDatetime(waybillSegment) shouldBe COMBINATOR_SC_INBOUND_TIME.plus(INTAKE_DELTA)
    }

    @Test
    @DisplayName("Расчет ожидаемого времени, если ЧП пришел после отгрузки")
    fun calculateExpectedDatetimeIfCheckpointTimeAfter() {
        setUpCombinatorRouteService()
        setUpPartnerRelationParamsService()
        val waybillSegment = prepareTestData(checkpointTime = CHECKPOINT_AFTER_TIME)
        processor.calculateExpectedDatetime(waybillSegment) shouldBe CHECKPOINT_AFTER_TIME.plus(INTAKE_DELTA)
    }

    @Test
    @DisplayName("Бросает исключение, если нет времени начала на предыдущем сервисе")
    fun throwIfNoStartTimeInPreviousService() {
        setUpCombinatorRouteService(fromStartTime = null)
        val waybillSegment = prepareTestData()
        assertThrows<NullPointerException> {
            processor.calculateExpectedDatetime(waybillSegment)
        }
    }

    @Test
    @DisplayName("Бросает исключение, если нет времени начала у текущего сервиса")
    fun throwIfNoStartTimeInService() {
        setUpCombinatorRouteService(toStartTime = null)
        val waybillSegment = prepareTestData()
        assertThrows<NullPointerException> {
            processor.calculateExpectedDatetime(waybillSegment)
        }
    }

    @Test
    @DisplayName("Бросает исключение, если нет длительности у текущего сервиса")
    fun throwIfNoDurationInService() {
        setUpCombinatorRouteService(toStartDuration = null)
        val waybillSegment = prepareTestData()
        assertThrows<NullPointerException> {
            processor.calculateExpectedDatetime(waybillSegment)
        }
    }

    @Test
    @DisplayName("Бросает исключение, если нет длительности у текущего сервиса с типом MOVEMENT")
    fun throwIfNoMovementDurationInService() {
        setUpCombinatorRouteService(toMovementDuration = null)
        val waybillSegment = prepareTestData()
        assertThrows<NullPointerException> {
            processor.calculateExpectedDatetime(waybillSegment)
        }
    }

    @Test
    @DisplayName("Расчет ожидаемого времени, если есть дельта у сервиса MOVEMENT")
    fun calculateExpectedDatetimeIfDurationDelta() {
        setUpCombinatorRouteService(toMovementDurationDelta = INTAKE_MOVEMENT_DELTA)
        val waybillSegment = prepareTestData()
        processor.calculateExpectedDatetime(waybillSegment) shouldBe COMBINATOR_SC_INBOUND_TIME.plus(
            INTAKE_MOVEMENT_DELTA
        )
    }

    private fun prepareTestData(
        hasPreviousSegment: Boolean = true,
        isPreviousSegmentHas130: Boolean = true,
        previousPartnerType: PartnerType = PartnerType.FULFILLMENT,
        currentPartnerType: PartnerType = PartnerType.SORTING_CENTER,
        checkpointTime: Instant = CHECKPOINT_TIME,
    ): WaybillSegment {
        val previousSegment = WaybillSegment(
            partnerId = PARTNER_FROM_ID,
            partnerType = previousPartnerType
        )
        val currentSegment = WaybillSegment(
            externalId = "123",
            partnerId = PARTNER_TO_ID,
            partnerType = currentPartnerType
        )
        if (isPreviousSegmentHas130) {
            writeWaybillSegmentCheckpoint(previousSegment, SegmentStatus.OUT, checkpointTime)
        }
        val segments = if (hasPreviousSegment) listOf(previousSegment, currentSegment) else listOf(currentSegment)
        joinInOrder(segments).apply { id = ORDER_ID }
        return currentSegment
    }

    private fun setUpCombinatorRouteService(
        fromStartTime: Instant? = COMBINATOR_FF_TIME,
        toStartTime: Instant? = COMBINATOR_SC_INBOUND_TIME,
        toStartDuration: Long? = 0L,
        toMovementDuration: Long? = 0L,
        toMovementDurationDelta: Duration? = null,
    ) {
        val combinatorRoute = createCombinatorRoute(
            fromStartTime,
            toStartTime,
            toStartDuration,
            toMovementDuration,
            toMovementDurationDelta
        )
        whenever(combinatorRouteService.getByLomOrderId(ORDER_ID))
            .thenReturn(combinatorRoute)
    }

    private fun createCombinatorRoute(
        fromStartTime: Instant?,
        toStartTime: Instant?,
        toStartDuration: Long?,
        toMovementDuration: Long?,
        toMovementDurationDelta: Duration?,
    ): LomOrderCombinatorRoute {
        val warehousePoint = LomOrderCombinatorRoute.Point(
            segmentId = 1,
            segmentType = PointType.WAREHOUSE,
            ids = LomOrderCombinatorRoute.PointIds(partnerId = PARTNER_FROM_ID),
            services = listOf(
                LomOrderCombinatorRoute.DeliveryService(
                    code = ServiceCodeName.SHIPMENT,
                    startTime = LomOrderCombinatorRoute.Timestamp(
                        seconds = fromStartTime?.let { fromStartTime.epochSecond },
                    ),
                )
            )
        )
        val movementPoint = LomOrderCombinatorRoute.Point(
            segmentId = 2,
            segmentType = PointType.MOVEMENT,
            ids = LomOrderCombinatorRoute.PointIds(partnerId = PARTNER_TO_ID),
            services = listOf(
                LomOrderCombinatorRoute.DeliveryService(
                    code = ServiceCodeName.INBOUND,
                    startTime = LomOrderCombinatorRoute.Timestamp(seconds = toStartTime?.let { toStartTime.epochSecond }),
                    duration = LomOrderCombinatorRoute.Timestamp(seconds = toStartDuration),
                ),
                LomOrderCombinatorRoute.DeliveryService(
                    code = ServiceCodeName.MOVEMENT,
                    duration = LomOrderCombinatorRoute.Timestamp(seconds = toMovementDuration),
                    durationDelta = toMovementDurationDelta?.toMinutes()?.toInt(),
                )
            )
        )
        return LomOrderCombinatorRoute(
            route = LomOrderCombinatorRoute.DeliveryRoute(
                paths = listOf(LomOrderCombinatorRoute.Path(pointFrom = 0, pointTo = 1)),
                points = listOf(warehousePoint, movementPoint),
            )
        )
    }

    private fun setUpPartnerRelationParamsService() {
        whenever(partnerRelationParamsService.findPartnerRelationParams(PARTNER_FROM_ID, PARTNER_TO_ID))
            .thenReturn(PartnerRelationParams(intakeDelta = INTAKE_DELTA))
    }

    companion object {
        private val COMBINATOR_SC_INBOUND_TIME = Instant.parse("2021-01-02T15:00:00.00Z")
        private val COMBINATOR_FF_TIME = Instant.parse("2021-01-02T05:00:00.00Z")
        private val CHECKPOINT_TIME = Instant.parse("2021-01-01T00:00:00.00Z")
        private val CHECKPOINT_AFTER_TIME = Instant.parse("2021-01-02T05:10:00.00Z")
        private val INTAKE_DELTA = Duration.ofHours(1)
        private val INTAKE_MOVEMENT_DELTA = Duration.ofHours(2)
        private const val PARTNER_FROM_ID = 1L
        private const val PARTNER_TO_ID = 2L
        private const val ORDER_ID = 3L
    }
}
