package ru.yandex.market.logistics.mqm.service.processor.planfact

import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito
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
import ru.yandex.market.logistics.mqm.service.CombinatorRouteService
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.at
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.toLocalDate
import ru.yandex.market.logistics.mqm.utils.toLocalTime
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint
import java.time.Duration
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class ScScShipmentPlanFactProcessorTest : AbstractTest() {

    private val combinatorRouteService = Mockito.mock(CombinatorRouteService::class.java)

    private val settingsService = TestableSettingsService()

    private val processor = ScScShipmentPlanFactProcessor(
        combinatorRouteService = combinatorRouteService,
        settingService = settingsService,
    )

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

    @ParameterizedTest
    @EnumSource(
        value = SegmentType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["SORTING_CENTER"],
    )
    @DisplayName("Проверка, что процессор нельзя применить, если текущий сегмент не SORTING_CENTER")
    fun isEligibleInternalReturnFalseIfCurrentNotSortingCenter(unsupportedSegmentType: SegmentType) {
        val segment = mockSegment(
            currentSegmentType = unsupportedSegmentType
        )
        processor.isEligible(segment) shouldBe false
    }

    @ParameterizedTest
    @EnumSource(
        value = SegmentType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["SORTING_CENTER"],
    )
    @DisplayName("Проверка, что процессор нельзя применить, если следующий сегмент не SORTING_CENTER")
    fun isEligibleInternalReturnFalseIfNextNotSortingCenter(unsupportedSegmentType: SegmentType) {
        val segment = mockSegment(
            nextSegmentType = unsupportedSegmentType,
        )
        processor.isEligible(segment) shouldBe false
    }

    @Test
    @DisplayName("Проверка, что процессор нельзя применить, если нет чекпоинта")
    fun isEligibleInternalReturnFalseIfNoCheckpoint() {
        val segment = mockSegment(
            checkpoint = SegmentStatus.OUT,
        )
        processor.isEligible(segment) shouldBe false
    }

    @Test
    @DisplayName("Проверка, что процессор нельзя применить, если текущий сегмент дропофф")
    fun isEligibleInternalReturnFalseIfPreviousIsDropoff() {
        val segment = mockSegment(
            isDropoff = true,
        )
        processor.isEligible(segment) shouldBe false
    }

    @ParameterizedTest
    @EnumSource(
        value = PointType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["MOVEMENT"],
    )
    @DisplayName("Проверка, что дедлайн не вычисляется, если для партнёра нет MOVEMENT сервиса")
    fun calculateExpectedDatetimeThrowsAnExceptionIfNoWarehouse(unsupportedType: PointType) {
        val segment = mockSegment(
            points = listOf(mockCombinatorPoint(pointType = unsupportedType)),
        )
        assertThrows<NoSuchElementException> {
            processor.calculateExpectedDatetime(segment)
        }
    }

    @ParameterizedTest
    @EnumSource(
        value = ServiceCodeName::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["MOVEMENT"],
    )
    @DisplayName("Проверка, что дедлайн не вычисляется, если в маршруте комбинатора нет нужных сервисов")
    fun calculateExpectedDatetimeThrowsAnExceptionIfNoServiceInCombinatorRoute(unsupportedServiceCode: ServiceCodeName) {
        val segment = mockSegment(
            points = listOf(mockCombinatorPoint(serviceCode = unsupportedServiceCode)),
        )
        assertThrows<NoSuchElementException> {
            processor.calculateExpectedDatetime(segment)
        }
    }

    @Test
    @DisplayName("Проверка, что дедлайн не вычисляется, если в маршруте больше одного сервиса для партнёра")
    fun isEligibleInternalReturnFalseIfMoreThanOneServiceInCombinatorRoute() {
        val segment = mockSegment(
            points = listOf(mockCombinatorPoint(), mockCombinatorPoint()),
        )
        assertThrows<NoSuchElementException> {
            processor.calculateExpectedDatetime(segment)
        }
    }

    @Test
    @DisplayName("Проверка, что дедлайн не вычисляется, если время сервиса не заполнено")
    fun calculateExpectedDatetimeThrowsAnExceptionIfNoServiceInCombinatorRo() {
        val segment = mockSegment(
            mockCombinatorRoute = true,
            points = listOf(mockCombinatorPoint(serviceTime = null)),
        )
        assertThrows<IllegalStateException> {
            processor.calculateExpectedDatetime(segment)
        }
    }

    @Test
    @DisplayName("Проверка расчета дедлайна")
    fun calculateExpectedDatetime() {
        val segment = mockSegment(
            mockCombinatorRoute = true,
        )
        processor.calculateExpectedDatetime(segment) shouldBe
                TEST_COMBINATOR_TIME.plus(MOVEMENT_ADDITIONAL_TIME)
    }

    @Test
    @DisplayName("Проверка расчета дедлайна, если чекпоинт после времени перемещения и время чекпоинта меньше времени перемещения")
    fun calculateExpectedDatetimeCheckpointAfterMovementTimeAndCheckpointTimeBeforeMovementTime() {
        val segment = mockSegment(
            mockCombinatorRoute = true,
            checkpointTime = CHECKPOINT_AFTER_TIME,
        )
        val movementAndAdditionalDateTime = TEST_COMBINATOR_TIME.plus(MOVEMENT_ADDITIONAL_TIME)
        val movementAndAdditionalTime = movementAndAdditionalDateTime.toLocalTime()
        val deadline =
            CHECKPOINT_AFTER_TIME.toLocalDate().at(movementAndAdditionalTime).plus(AFTER_CHECKPOINT_ADDITIONAL_TIME)
        processor.calculateExpectedDatetime(segment) shouldBe deadline
    }

    @Test
    @DisplayName("Проверка расчета дедлайна, если чекпоинт после времени перемещения и время чекпоинта больше времени перемещения")
    fun calculateExpectedDatetimeCheckpointAfterMovementTimeAndCheckpointTimeAfterMovementTime() {
        val segment = mockSegment(
            mockCombinatorRoute = true,
            checkpointTime = CHECKPOINT_AFTER_MOVEMENT_TIME,
        )
        val movementAndAdditionalDateTime = TEST_COMBINATOR_TIME.plus(MOVEMENT_ADDITIONAL_TIME)
        val movementAndAdditionalTime = movementAndAdditionalDateTime.toLocalTime()
        val deadline =
            CHECKPOINT_AFTER_MOVEMENT_TIME.toLocalDate().at(movementAndAdditionalTime)
        processor.calculateExpectedDatetime(segment) shouldBe deadline
    }

    private fun mockSegment(
        currentSegmentType: SegmentType = SegmentType.SORTING_CENTER,
        nextSegmentType: SegmentType = SegmentType.SORTING_CENTER,
        isDropoff: Boolean = false,
        checkpoint: SegmentStatus = SegmentStatus.IN,
        points: List<LomOrderCombinatorRoute.Point> = listOf(mockCombinatorPoint()),
        mockCombinatorRoute: Boolean = false,
        checkpointTime: Instant = CHECKPOINT_TIME,
        platformClient: PlatformClient = PlatformClient.BERU,
    ): WaybillSegment {
        if (mockCombinatorRoute) {
            whenever(combinatorRouteService.findPartnerPoints(TEST_ORDER_ID, TEST_NEXT_PARTNER_ID))
                .thenReturn(points)
        }

        val currentSegment = WaybillSegment(segmentType = currentSegmentType)
            .apply { partnerId = TEST_CURRENT_PARTNER_ID }
        writeWaybillSegmentCheckpoint(currentSegment, checkpoint, checkpointTime)
        val nextSegment = WaybillSegment(segmentType = nextSegmentType)
            .apply { partnerId = TEST_NEXT_PARTNER_ID }
        joinInOrder(listOf(currentSegment, nextSegment))
            .apply {
                id = TEST_ORDER_ID
                platformClientId = platformClient.id
            }

        if (isDropoff) {
            currentSegment.partnerType = PartnerType.DELIVERY
            currentSegment.segmentType = SegmentType.SORTING_CENTER
        }

        return currentSegment
    }

    private fun mockCombinatorPoint(
        pointType: PointType = PointType.MOVEMENT,
        serviceCode: ServiceCodeName = ServiceCodeName.MOVEMENT,
        serviceTime: Instant? = TEST_COMBINATOR_TIME,
    ): LomOrderCombinatorRoute.Point {

        val deliveryService = LomOrderCombinatorRoute.DeliveryService(
            code = serviceCode,
            startTime = serviceTime?.let { LomOrderCombinatorRoute.Timestamp(seconds = it.epochSecond) },
        )
        return LomOrderCombinatorRoute.Point(
            segmentType = pointType,
            ids = LomOrderCombinatorRoute.PointIds(partnerId = TEST_NEXT_PARTNER_ID),
            services = listOf(deliveryService)
        )
    }

    companion object {
        const val TEST_ORDER_ID = 1L
        const val TEST_CURRENT_PARTNER_ID = 1L
        const val TEST_NEXT_PARTNER_ID = 2L
        private val TEST_COMBINATOR_TIME = Instant.parse("2021-10-28T18:00:00Z")
        private val MOVEMENT_ADDITIONAL_TIME = Duration.ofHours(1)
        private val CHECKPOINT_TIME = Instant.parse("2021-10-28T09:00:00.00Z")
        private val CHECKPOINT_AFTER_TIME = Instant.parse("2021-10-28T20:00:00.00Z")
        private val CHECKPOINT_AFTER_MOVEMENT_TIME = Instant.parse("2021-10-28T21:00:00.00Z")
        private val AFTER_CHECKPOINT_ADDITIONAL_TIME: Duration = Duration.ofDays(1)
    }
}
