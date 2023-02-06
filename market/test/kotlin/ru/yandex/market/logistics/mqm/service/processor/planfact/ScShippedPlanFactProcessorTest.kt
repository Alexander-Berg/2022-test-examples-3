package ru.yandex.market.logistics.mqm.service.processor.planfact

import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.embedded.WaybillShipment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.PartnerService
import ru.yandex.market.logistics.mqm.service.logging.LogService
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.getNextSegment
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint
import ru.yandex.market.logistics.mqm.utils.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import java.time.Duration

@ExtendWith(MockitoExtension::class)
class ScShippedPlanFactProcessorTest {

    @Mock
    private lateinit var partnerService: PartnerService

    @Mock
    private lateinit var logService: LogService

    private val settingsService = TestableSettingsService()

    private lateinit var processor: ScShippedPlanFactProcessor


    @BeforeEach
    fun setUp() {
        processor = ScShippedPlanFactProcessor(
            partnerService = partnerService,
            logService = logService,
            settingService = settingsService,
        )
    }

    @Test
    fun isEligible() {
        val shipmentDate = LocalDate.of(2021, 4, 2)
        val checkpoint110Time = Instant.parse("2021-04-01T10:00:00.00Z")
        val scSegment = createOrderAndGetFirstSegment(shipmentDate, checkpoint110Time)
        assertSoftly { processor.isEligible(scSegment) shouldBe true }
    }

    @Test
    fun isNotEligibleForScToSc() {
        val shipmentDate = LocalDate.of(2021, 4, 2)
        val checkpoint110Time = Instant.parse("2021-04-01T10:00:01.00Z")
        val scSegment = createOrderAndGetFirstSegment(shipmentDate, checkpoint110Time)
        scSegment.order!!.waybill.add(1, createSortingCenterSegment(shipmentDate, checkpoint110Time))
        assertSoftly { processor.isEligible(scSegment) shouldBe false }
    }

    @Test
    fun isNotEligibleIfWasIntakeOnSd() {
        val shipmentDate = LocalDate.of(2021, 4, 2)
        val checkpoint110Time = Instant.parse("2021-04-01T10:00:00.00Z")
        val scSegment = createOrderAndGetFirstSegment(shipmentDate, checkpoint110Time)
        writeWaybillSegmentCheckpoint( scSegment.getNextSegment(), SegmentStatus.IN, checkpoint110Time)
        assertSoftly { processor.isEligible(scSegment) shouldBe false }
    }

    @Test
    fun calculateExpectedDatetimeIfCheckpointIsBeforeShipmentDateAndIntakeDeadlineIsBefore8() {
        val shipmentDate = LocalDate.of(2021, 4, 2)
        val checkpoint110Time = Instant.parse("2021-04-01T19:14:02.00Z")
        val scSegment = createOrderAndGetFirstSegment(shipmentDate, checkpoint110Time)
        mockSchedule(
            scSegment,
            listOf(shipmentDate to PartnerService.TimeWindow(startTime = null, endTime = LocalTime.of(6, 0)))
        )
        mockDuration(scSegment)

        assertSoftly {
            processor.calculateExpectedDatetime(scSegment) shouldBeEqualComparingTo
                    Instant.parse("2021-04-03T04:00:00Z")
        }
    }

    @Test
    fun calculateExpectedDatetimeIfCheckpointIsOnShipmentDateAndIntakeDeadlineIsBefore8() {
        val shipmentDate = LocalDate.of(2021, 4, 2)
        val checkpoint110Time = Instant.parse("2021-04-02T19:14:03.00Z")
        val scSegment = createOrderAndGetFirstSegment(shipmentDate, checkpoint110Time)
        mockSchedule(
            scSegment,
            listOf(
                shipmentDate to PartnerService.TimeWindow(startTime = null, endTime = LocalTime.of(6, 0)),
                checkpoint110Time.plus(Period.ofDays(1)).toLocalDate() to PartnerService.TimeWindow(
                    startTime = null,
                    endTime = LocalTime.of(15, 0)
                ),
            ),
        )
        mockDuration(scSegment)

        assertSoftly {
            processor.calculateExpectedDatetime(scSegment) shouldBeEqualComparingTo
                    Instant.parse("2021-04-03T13:00:00Z")
        }
    }

    @Test
    fun calculateExpectedDatetimeIfCheckpointIsAfterShipmentDateAndIntakeDeadlineIsBefore8() {
        val shipmentDate = LocalDate.of(2021, 4, 2)
        val checkpoint110Time = Instant.parse("2021-04-08T19:14:03.00Z")
        val scSegment = createOrderAndGetFirstSegment(shipmentDate, checkpoint110Time)
        mockSchedule(
            scSegment,
            listOf(
                shipmentDate to PartnerService.TimeWindow(startTime = null, endTime = LocalTime.of(6, 0)),
                checkpoint110Time.plus(Period.ofDays(1)).toLocalDate() to PartnerService.TimeWindow(
                    startTime = null,
                    endTime = LocalTime.of(6, 0)
                ),
            ),
        )
        mockDuration(scSegment)

        assertSoftly {
            processor.calculateExpectedDatetime(scSegment) shouldBeEqualComparingTo
                    Instant.parse("2021-04-10T04:00:00Z")
        }
    }

    @Test
    fun calculateExpectedDatetimeIfCheckpointIsBeforeShipmentDateAndIntakeDeadlineIsAfter8() {
        val shipmentDate = LocalDate.of(2021, 4, 3)
        val checkpointTime = Instant.parse("2021-04-02T20:21:01.00Z")
        val scSegment = createOrderAndGetFirstSegment(shipmentDate, checkpointTime)
        mockSchedule(
            scSegment,
            listOf(
                shipmentDate to PartnerService.TimeWindow(startTime = null, endTime = LocalTime.of(6, 0)),
                checkpointTime.plus(Period.ofDays(1)).toLocalDate() to PartnerService.TimeWindow(
                    startTime = null,
                    endTime = LocalTime.of(15, 0)
                ),
            ),
        )
        mockDuration(scSegment)

        assertSoftly {
            processor.calculateExpectedDatetime(scSegment) shouldBeEqualComparingTo
                    Instant.parse("2021-04-03T13:00:00Z")
        }
    }

    @Test
    fun calculateExpectedDatetimeIfCheckpointIsBeforeShipmentDateAndIntakeDeadlineIsBefore8AndAnotherDuration() {
        val shipmentDate = LocalDate.of(2021, 4, 2)
        val checkpoint110Time = Instant.parse("2021-04-01T19:14:02.00Z")
        val scSegment = createOrderAndGetFirstSegment(shipmentDate, checkpoint110Time)
        mockSchedule(
            scSegment,
            listOf(
                shipmentDate to PartnerService.TimeWindow(startTime = null, endTime = LocalTime.of(6, 0)),
                checkpoint110Time.plus(Period.ofDays(1)).toLocalDate() to PartnerService.TimeWindow(
                    startTime = null,
                    endTime = LocalTime.of(6, 0)
                ),
            ),
        )
        mockDuration(scSegment, Duration.ofHours(2))

        assertSoftly {
            processor.calculateExpectedDatetime(scSegment) shouldBeEqualComparingTo
                    Instant.parse("2021-04-03T05:00:00Z")
        }
    }

    private fun createOrderAndGetFirstSegment(shipmentDate: LocalDate, checkpointTime: Instant): WaybillSegment {
        val scSegment = createSortingCenterSegment(shipmentDate, checkpointTime)
        val dsSegment = createDeliverySegment()
        joinInOrder(listOf(scSegment, dsSegment))

        return scSegment
    }

    private fun mockDuration(
        partnerFrom: WaybillSegment,
        duration: Duration = Duration.ofHours(1),
    ) {
        whenever(partnerService.getWarehouseHandlingDuration(partnerFrom)).thenReturn(duration)
    }

    private fun mockSchedule(
        partnerFrom: WaybillSegment,
        times: List<Pair<LocalDate, PartnerService.TimeWindow>>,
    ) {
        times.forEach { pair ->
            whenever(
                partnerService.findScheduleWindowTime(
                    partnerFrom,
                    pair.first,
                )
            )
                .thenReturn(pair.second)
        }
    }

    private fun createSortingCenterSegment(shipmentDate: LocalDate, checkpointTime: Instant) = WaybillSegment(
        id = 1L,
        segmentType = SegmentType.SORTING_CENTER,
        partnerType = PartnerType.SORTING_CENTER,
        partnerId = 1L,
        shipment = WaybillShipment(
            date = shipmentDate
        ),
        waybillSegmentIndex = 1,
    ).apply {
        waybillSegmentStatusHistory = mutableSetOf(
            WaybillSegmentStatusHistory(
                status = SegmentStatus.IN,
                date = checkpointTime
            )
        )
    }

    private fun createDeliverySegment() = WaybillSegment(
        id = 2L,
        segmentType = SegmentType.COURIER,
        partnerType = PartnerType.DELIVERY,
        partnerId = 2L,
        waybillSegmentIndex = 2
    )
}
