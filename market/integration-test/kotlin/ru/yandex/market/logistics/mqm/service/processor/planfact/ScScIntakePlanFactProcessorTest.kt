package ru.yandex.market.logistics.mqm.service.processor.planfact

import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.entity.lom.LomOrderCombinatorRoute
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.PointType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.ServiceCodeName
import ru.yandex.market.logistics.mqm.entity.management.PartnerRelationParams
import ru.yandex.market.logistics.mqm.service.CombinatorRouteService
import ru.yandex.market.logistics.mqm.service.management.PartnerRelationParamsService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint
import java.time.Duration
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class ScScIntakePlanFactProcessorTest : AbstractTest() {

    @Mock
    private lateinit var combinatorRouteService: CombinatorRouteService

    @Mock
    private lateinit var partnerRelationParamsService: PartnerRelationParamsService

    private lateinit var processor: ScScIntakePlanFactProcessor

    @BeforeEach
    fun setUp() {
        processor = ScScIntakePlanFactProcessor(
            combinatorRouteService,
            partnerRelationParamsService,
        )
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["BERU", "YANDEX_GO"]
    )
    @DisplayName("Проверка, что процессор можно применить")
    fun isEligible(platformClient: PlatformClient) {
        processor.isEligible(mockSegment(platformClient = platformClient)) shouldBe true
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["BERU", "YANDEX_GO"]
    )
    @DisplayName("Проверка неприменимости процессора для platformClient")
    fun isNonEligiblePlatformClient(platformClient: PlatformClient) {
        processor.isEligible(mockSegment(platformClient = platformClient)) shouldBe false
    }

    @ParameterizedTest
    @EnumSource(
        value = SegmentType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["SORTING_CENTER"],
    )
    @DisplayName("Проверка, что процессор нельзя применить, если предыдущий сегмент не SORTING_CENTER")
    fun isEligibleInternalReturnFalseIfPreviousNotSortingCenter(unsupportedSegmentType: SegmentType) {
        val segment = mockSegment(
            previousSegmentType = unsupportedSegmentType
        )
        processor.isEligible(segment) shouldBe false
    }

    @ParameterizedTest
    @EnumSource(
        value = SegmentType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["SORTING_CENTER"],
    )
    @DisplayName("Проверка, что процессор нельзя применить, если текущий сегмент не SORTING_CENTER")
    fun isEligibleInternalReturnFalseIfCurrentNotSortingCenter(unsupportedSegmentType: SegmentType) {
        val segment = mockSegment(
            segmentType = unsupportedSegmentType,
        )
        processor.isEligible(segment) shouldBe false
    }

    @Test
    @DisplayName("Проверка, что процессор нельзя применить, если нет чекпоинта")
    fun isEligibleInternalReturnFalseIfNoCheckpoint() {
        val segment = mockSegment(
            checkpoint = SegmentStatus.IN,
        )
        processor.isEligible(segment) shouldBe false
    }

    @Test
    @DisplayName("Проверка, что процессор нельзя применить, если нет предыдущий сегмент дропофф")
    fun isEligibleInternalReturnFalseIfPreviousIsDropoff() {
        val segment = mockSegment(
            isDropoff = true,
        )
        processor.isEligible(segment) shouldBe false
    }

    @Test
    @DisplayName("Проверка, что дедлайн не вычисляется, если для предыдущего партнёра нет SHIPMENT сервиса")
    fun calculateExpectedDatetimeThrowsAnExceptionIfNoShipment() {
        mockPreviousShipmentService(exist = false)
        val segment = mockSegment(
            mockPartnerRelation = false,
        )
        assertThrows<NoSuchElementException> {
            processor.calculateExpectedDatetime(segment)
        }
    }

    @Test
    @DisplayName("Проверка, что дедлайн не вычисляется, если для предыдущего партнёра нет времени SHIPMENT сервиса")
    fun calculateExpectedDatetimeThrowsAnExceptionIfNoShipmentTime() {
        mockPreviousShipmentService(serviceTime = null)
        mockService(
            pointType = PointType.WAREHOUSE,
            serviceCode = ServiceCodeName.SHIPMENT,
            partnerId = TEST_PARTNER_FROM_ID,
        )
        val segment = mockSegment(
            mockPartnerRelation = false,
        )
        assertThrows<IllegalStateException> {
            processor.calculateExpectedDatetime(segment)
        }
    }

    @Test
    @DisplayName("Проверка, что дедлайн не вычисляется, если для предыдущего партнёра нет MOVEMENT сервиса")
    fun calculateExpectedDatetimeThrowsAnExceptionIfNoMovement() {
        mockPreviousShipmentService()
        mockCurrentMovementService(exist = false)
        val segment = mockSegment(
            mockPartnerRelation = false,
        )
        assertThrows<NoSuchElementException> {
            processor.calculateExpectedDatetime(segment)
        }
    }

    @Test
    @DisplayName("Проверка, что дедлайн не вычисляется, если для предыдущего партнёра нет длительности MOVEMENT сервиса")
    fun calculateExpectedDatetimeThrowsAnExceptionIfNoMovementDuration() {
        mockPreviousShipmentService()
        mockCurrentMovementService(duration = null)
        val segment = mockSegment(
            mockPartnerRelation = false,
        )
        assertThrows<IllegalStateException> {
            processor.calculateExpectedDatetime(segment)
        }
    }

    @Test
    @DisplayName("Проверка, что дедлайн не вычисляется, если для текущего партнёра нет INBOUND сервиса")
    fun calculateExpectedDatetimeThrowsAnExceptionIfNoInbound() {
        mockPreviousShipmentService()
        mockCurrentMovementService()
        mockCurrentInboundService(exist = false)
        val segment = mockSegment(
            mockPartnerRelation = false,
        )
        assertThrows<NoSuchElementException> {
            processor.calculateExpectedDatetime(segment)
        }
    }

    @Test
    @DisplayName("Проверка, что дедлайн не вычисляется, если для текущего партнёра нет времени INBOUND сервиса")
    fun calculateExpectedDatetimeThrowsAnExceptionIfNoInboundTime() {
        mockPreviousShipmentService()
        mockCurrentMovementService()
        mockCurrentInboundService(serviceTime = null)
        val segment = mockSegment(
            mockPartnerRelation = false,
        )
        assertThrows<IllegalStateException> {
            processor.calculateExpectedDatetime(segment)
        }
    }

    @Test
    @DisplayName("Проверка, что дедлайн не вычисляется, если для текущего партнёра нет длительности INBOUND сервиса")
    fun calculateExpectedDatetimeThrowsAnExceptionIfNoDuration() {
        mockPreviousShipmentService()
        mockCurrentMovementService()
        mockCurrentInboundService(duration = null)
        val segment = mockSegment(
            mockPartnerRelation = false,
        )
        assertThrows<IllegalStateException> {
            processor.calculateExpectedDatetime(segment)
        }
    }

    @Test
    @DisplayName("Проверка расчета дедлайна, если чекпоинт пришел до планируемой отгрузки")
    fun calculateExpectedDatetimeIfCheckpointInTime() {
        mockPreviousShipmentService()
        mockCurrentMovementService()
        mockCurrentInboundService()
        val segment = mockSegment(
            mockPartnerRelation = true,
        )
        processor.calculateExpectedDatetime(segment) shouldBe TEST_CURRENT_INBOUNT_TIME
            .plus(TEST_CURRENT_INBOUNT_DURATION)
            .plus(TEST_DELTA)
    }

    @Test
    @DisplayName("Проверка расчета дедлайна, если чекпоинт пришел после планируемой отгрузки")
    fun calculateExpectedDatetimeIfCheckpointAfterShipment() {
        mockPreviousShipmentService()
        mockCurrentMovementService()
        mockCurrentInboundService()
        val checkpointTime = Instant.parse("2021-12-03T08:00:00.00Z")
        val segment = mockSegment(
            mockPartnerRelation = true,
            checkpointTime = checkpointTime
        )
        processor.calculateExpectedDatetime(segment) shouldBe checkpointTime
            .plus(TEST_PREVIOUS_MOVEMENT_DURATION)
            .plus(TEST_DELTA)
    }

    @Test
    @DisplayName("Проверка расчета дедлайна, если чекпоинт пришел до планируемой отгрузки и есть дельта")
    fun calculateExpectedDatetimeIfCheckpointInTimeAndDelta() {
        mockPreviousShipmentService()
        mockCurrentMovementService(durationDelta = TEST_CURRENT_INBOUNT_DELTA_DURATION)
        mockCurrentInboundService()
        val segment = mockSegment(
            mockPartnerRelation = false,
        )
        processor.calculateExpectedDatetime(segment) shouldBe TEST_CURRENT_INBOUNT_TIME
            .plus(TEST_CURRENT_INBOUNT_DURATION)
            .plus(TEST_CURRENT_INBOUNT_DELTA_DURATION)
    }

    private fun mockPreviousShipmentService(
        serviceTime: Instant? = TEST_PREVIOUS_SHIPMENT_TIME,
        exist: Boolean = true,
    ) = mockService(
        pointType = PointType.WAREHOUSE,
        serviceCode = ServiceCodeName.SHIPMENT,
        partnerId = TEST_PARTNER_FROM_ID,
        serviceTime = serviceTime,
        exist = exist,
    )

    private fun mockCurrentMovementService(
        duration: Duration? = TEST_PREVIOUS_MOVEMENT_DURATION,
        exist: Boolean = true,
        durationDelta: Duration? = null,
    ) = mockService(
        pointType = PointType.MOVEMENT,
        serviceCode = ServiceCodeName.MOVEMENT,
        partnerId = TEST_PARTNER_ID,
        duration = duration,
        exist = exist,
        durationDelta = durationDelta,
    )

    private fun mockCurrentInboundService(
        serviceTime: Instant? = TEST_CURRENT_INBOUNT_TIME,
        duration: Duration? = TEST_CURRENT_INBOUNT_DURATION,
        exist: Boolean = true,
    ) = mockService(
        pointType = PointType.WAREHOUSE,
        serviceCode = ServiceCodeName.INBOUND,
        partnerId = TEST_PARTNER_ID,
        serviceTime = serviceTime,
        duration = duration,
        exist = exist,
    )

    private fun mockSegment(
        segmentType: SegmentType = SegmentType.SORTING_CENTER,
        previousSegmentType: SegmentType = SegmentType.SORTING_CENTER,
        isDropoff: Boolean = false,
        checkpoint: SegmentStatus = SegmentStatus.OUT,
        checkpointTime: Instant = TEST_CHECKPOINT_TIME,
        mockPartnerRelation: Boolean = false,
        platformClient: PlatformClient = PlatformClient.BERU,
    ): WaybillSegment {
        if (mockPartnerRelation) {
            whenever(partnerRelationParamsService.findPartnerRelationParams(TEST_ORDER_ID, TEST_PARTNER_ID))
                .thenReturn(PartnerRelationParams(intakeDelta = TEST_DELTA))
        }

        val previousSegment = WaybillSegment(segmentType = previousSegmentType)
            .apply { partnerId = TEST_PARTNER_FROM_ID }
        writeWaybillSegmentCheckpoint(previousSegment, checkpoint, checkpointTime)
        val segment = WaybillSegment(segmentType = segmentType)
            .apply { partnerId = TEST_PARTNER_ID }
        joinInOrder(listOf(previousSegment, segment))
            .apply { id = TEST_ORDER_ID; platformClientId = platformClient.id }

        if (isDropoff) {
            previousSegment.partnerType = PartnerType.DELIVERY
            previousSegment.segmentType = SegmentType.SORTING_CENTER
        }

        return segment
    }

    private fun mockService(
        pointType: PointType,
        serviceCode: ServiceCodeName,
        partnerId: Long,
        exist: Boolean = true,
        serviceTime: Instant? = null,
        duration: Duration? = null,
        durationDelta: Duration? = null,
    ) = if (exist) {
        whenever(combinatorRouteService.findService(TEST_ORDER_ID, partnerId, pointType, serviceCode))
            .thenReturn(
                LomOrderCombinatorRoute.DeliveryService(
                    code = serviceCode,
                    startTime = serviceTime?.let { LomOrderCombinatorRoute.Timestamp(seconds = it.epochSecond) },
                    duration = duration?.let { LomOrderCombinatorRoute.Timestamp(seconds = it.seconds) },
                    durationDelta = durationDelta?.toMinutes()?.toInt(),
                )
            )
    } else {
        whenever(combinatorRouteService.findService(TEST_ORDER_ID, partnerId, pointType, serviceCode))
            .thenThrow(NoSuchElementException("not_found"))
    }

    companion object {
        val TEST_PREVIOUS_SHIPMENT_TIME = Instant.parse("2021-12-01T09:00:00.00Z")
        val TEST_PREVIOUS_MOVEMENT_DURATION = Duration.ofHours(10)
        val TEST_CURRENT_INBOUNT_TIME = Instant.parse("2021-12-02T09:00:00.00Z")
        val TEST_CURRENT_INBOUNT_DURATION = Duration.ofHours(1)
        val TEST_CURRENT_INBOUNT_DELTA_DURATION = Duration.ofHours(2)
        val TEST_CHECKPOINT_TIME = Instant.parse("2021-12-01T08:00:00.00Z")

        const val TEST_ORDER_ID = 1L
        const val TEST_PARTNER_FROM_ID = 1L
        const val TEST_PARTNER_ID = 2L
        val TEST_DELTA = Duration.ofMinutes(10)
    }
}
