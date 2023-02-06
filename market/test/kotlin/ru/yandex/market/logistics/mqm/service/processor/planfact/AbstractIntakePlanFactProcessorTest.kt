package ru.yandex.market.logistics.mqm.service.processor.planfact

import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.PartnerService
import ru.yandex.market.logistics.mqm.service.logging.LogService
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.at
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint

@ExtendWith(MockitoExtension::class)
abstract class AbstractIntakePlanFactProcessorTest: AbstractTest() {

    @Mock
    protected lateinit var partnerService: PartnerService

    @Mock
    protected lateinit var logService: LogService

    protected val settingsService = TestableSettingsService()

    @Test
    @DisplayName("Проверка неприменимости процессора в случае, если нет 130чп на предыдущем сегменте")
    fun isNonEligible() {
        val waybillSegment = createSegments(previousHasStatus = false)
        Assertions.assertThat(getProcessor().isEligible(waybillSegment)).isFalse
    }

    @Test
    @DisplayName("Расчет ожидаемого времени до shipment-date")
    fun calculateDeadlineBeforeShipment() {
        val waybillSegment = createSegments(
            preparePartnerService = true,
            checkPointDateTime = CHECKPOINT_DATETIME,
            plannedLocalDate = PLANNED_SHIPMENT_DATETIME,
            times = listOf(
                PLANNED_SHIPMENT_DATETIME to PartnerService.TimeWindow(
                    startTime = null,
                    endTime = LocalTime.NOON
                )
            ),
        )
        val expectedDeadline = ZonedDateTime
            .of(PLANNED_SHIPMENT_DATETIME.atStartOfDay(), DateTimeUtils.MOSCOW_ZONE)
            .plusHours(SCHEDULE_HOURS + DELTA_HOURS)
            .toInstant()
        getProcessor().calculateExpectedDatetime(waybillSegment) shouldBe expectedDeadline
    }

    @Test
    @DisplayName("Расчет ожидаемого времени до delivery")
    fun calculateDeadlineBeforeDelivery() {
        val waybillSegment = createSegments(
            preparePartnerService = true,
            checkPointDateTime = CHECKPOINT_BEFORE_DATETIME,
            plannedLocalDate = PLANNED_SHIPMENT_BEFORE_DATETIME,
            packagingDuration = PACKAGING_DURATION_BEFORE,
            times = listOf(
                PLANNED_SHIPMENT_BEFORE_DATETIME to PartnerService.TimeWindow(
                    startTime = null,
                    endTime = LocalTime.NOON
                )
            ),
        )
        val expectedDeadline = ZonedDateTime
            .of(CHECKPOINT_BEFORE_DATE.atStartOfDay(), DateTimeUtils.MOSCOW_ZONE)
            .plusHours(DELTA_HOURS)
            .toInstant()
        getProcessor().calculateExpectedDatetime(waybillSegment) shouldBe expectedDeadline
    }

    @Test
    @DisplayName("Расчет ожидаемого времени после delivery")
    fun calculateDeadlineAfterDelivery() {
        val waybillSegment = createSegments(
            preparePartnerService = true,
            checkPointDateTime = CHECKPOINT_AFTER_DATETIME,
            plannedLocalDate = PLANNED_SHIPMENT_AFTER_DATETIME,
            times = listOf(
                PLANNED_SHIPMENT_AFTER_DATETIME to PartnerService.TimeWindow(
                    startTime = null,
                    endTime = LocalTime.NOON
                ),
                CHECKPOINT_AFTER_DATE to PartnerService.TimeWindow(startTime = null, endTime = LocalTime.NOON),
            ),
        )
        val expectedDeadline = ZonedDateTime
            .of(CHECKPOINT_AFTER_DATE.atStartOfDay(), DateTimeUtils.MOSCOW_ZONE)
            .plusDays(1)
            .plusHours(SCHEDULE_HOURS + DELTA_HOURS)
            .toInstant()
        getProcessor().calculateExpectedDatetime(waybillSegment) shouldBe expectedDeadline
    }

    protected fun createWaybillSegment(
        type: SegmentType,
        partnerType: PartnerType,
        index: Int,
        partnerId: Long
    ) = WaybillSegment(
        externalId = "externalId",
        segmentType = type,
        partnerType = partnerType,
        waybillSegmentIndex = index,
        partnerId = partnerId,
    )

    protected abstract fun createCurrentSegment(): WaybillSegment

    protected abstract fun createPreviousSegment(): WaybillSegment

    protected abstract fun getProcessor(): AbstractIntakePlanFactProcessor

    protected fun createSegments(
        previousHasStatus: Boolean = true,
        preparePartnerService: Boolean = false,
        checkPointDateTime: Instant = CHECKPOINT_DATETIME,
        plannedLocalDate: LocalDate = PLANNED_SHIPMENT_DATETIME,
        packagingDuration: Duration = Duration.ZERO,
        platformClient: PlatformClient = PlatformClient.BERU,
        times: List<Pair<LocalDate, PartnerService.TimeWindow>> = listOf(),
    ): WaybillSegment {
        val current = createCurrentSegment()
        val previous = createPreviousSegment()
        previous.apply {
            shipment.date = plannedLocalDate
        }
        joinInOrder(listOf(previous, current)).apply { platformClientId = platformClient.id }
        if (previousHasStatus) {
            writeWaybillSegmentCheckpoint(previous, SegmentStatus.OUT, checkPointDateTime)
        }
        if (preparePartnerService) {
            preparePartnerService(previous, packagingDuration, times)
        }
        return current
    }

    private fun preparePartnerService(
        previous: WaybillSegment,
        packagingDuration: Duration,
        times: List<Pair<LocalDate, PartnerService.TimeWindow>>,
    ) {
        whenever(partnerService.findCutoffTime(previous)).thenReturn(LocalTime.MIDNIGHT)
        whenever(partnerService.findPackagingDuration(previous)).thenReturn(packagingDuration)
        whenever(partnerService.findIntakeDeadline(previous)).thenReturn(PREVIOUS_DEADLINE)
        mockSchedule(previous, times)
    }

    private fun mockSchedule(
        previous: WaybillSegment,
        times: List<Pair<LocalDate, PartnerService.TimeWindow>>,
    ) {
        times.forEach { pair ->
            whenever(
                partnerService.findScheduleWindowTime(
                    previous,
                    pair.first
                )
            )
                .thenReturn(pair.second)
        }
    }

    companion object {
        private const val DELTA_HOURS = 2L

        private val PREVIOUS_DEADLINE = Duration.ofHours(DELTA_HOURS)

        private const val SCHEDULE_HOURS = 12L

        private val DEFAULT_TIME: LocalTime = LocalTime.of(0, 0)

        private val CHECKPOINT_DATETIME = LocalDate.of(2021, 10, 11).at(DEFAULT_TIME)
        private val PLANNED_SHIPMENT_DATETIME = LocalDate.of(2021, 10, 12)

        private val CHECKPOINT_BEFORE_DATE = LocalDate.of(2021, 10, 11)
        private val CHECKPOINT_BEFORE_DATETIME = CHECKPOINT_BEFORE_DATE.at(DEFAULT_TIME)
        private val PLANNED_SHIPMENT_BEFORE_DATETIME = LocalDate.of(2021, 10, 7)
        private val PACKAGING_DURATION_BEFORE = Duration.ofDays(5)

        private val CHECKPOINT_AFTER_DATE = LocalDate.of(2021, 10, 11)
        private val CHECKPOINT_AFTER_DATETIME = CHECKPOINT_AFTER_DATE.at(DEFAULT_TIME)
        private val PLANNED_SHIPMENT_AFTER_DATETIME = LocalDate.of(2021, 10, 7)
    }
}
