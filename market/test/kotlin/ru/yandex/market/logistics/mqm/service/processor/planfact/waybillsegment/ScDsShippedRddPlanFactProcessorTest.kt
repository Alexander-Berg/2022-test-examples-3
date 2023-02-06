package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.embedded.WaybillShipment
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderCombinatorRouteWasUpdatedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.service.PartnerService
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint

@ExtendWith(MockitoExtension::class)
class ScDsShippedRddPlanFactProcessorTest {
    private lateinit var processor: ScDsShippedRddPlanFactProcessor

    private val settingsService = TestableSettingsService()

    private val clock = TestableClock()

    @Mock
    private lateinit var planFactService: PlanFactService

    @Mock
    private lateinit var partnerService: PartnerService

    @BeforeEach
    fun setUp() {
        processor = ScDsShippedRddPlanFactProcessor(
            settingsService,
            clock,
            planFactService,
            partnerService,
        )
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE)
    }

    // Создание ПФ.

    @Test
    @DisplayName("Проверка создания план-факта, предыдущий сегмент - FF")
    fun planFactCreationSuccessPreviousFf() {
        val previousSegment = createPreviousSegment()
        val lomOrder = createLomOrder(previousSegment = previousSegment)
        val newCheckpoint = createNewCheckpoint(waybillSegment = previousSegment)

        mockSchedule(
            lomOrder.waybill[1],
            listOf(
                lomOrder.waybill[1].shipment.date!! to PartnerService.TimeWindow(
                    startTime = null,
                    endTime = LocalTime.of(6, 0)
                )
            ),
        )
        mockDuration(partnerFrom = lomOrder.waybill[1])

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        val planFact = getSavedPlanFact()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe lomOrder.waybill[1].id
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe EXPECTED_DATETIME_BEFORE_8
            planFact.producerName shouldBe PRODUCER_NAME
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_DATETIME_BEFORE_8
        }
    }

    @Test
    @DisplayName("Проверка создания план-факта, предыдущий сегмент - DROPSHIP")
    fun planFactCreationSuccessPreviousDropship() {
        val previousSegment = createPreviousSegment(partnerType = PartnerType.DROPSHIP)
        val currentSegment = createCurrentSegment()
        val lomOrder = createLomOrder(previousSegment = previousSegment, currentSegment = currentSegment)
        val newCheckpoint = createNewCheckpoint(newSegmentStatus = SegmentStatus.IN, waybillSegment = currentSegment)

        mockSchedule(
            currentSegment,
            listOf(
                currentSegment.shipment.date!! to PartnerService.TimeWindow(
                    startTime = null,
                    endTime = LocalTime.of(6, 0)
                )
            ),
        )
        mockDuration(partnerFrom = currentSegment)

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        val planFact = getSavedPlanFact()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe currentSegment.id
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe EXPECTED_DATETIME_BEFORE_8
            planFact.producerName shouldBe PRODUCER_NAME
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_DATETIME_BEFORE_8
        }
    }

    @Test
    @DisplayName("Проверка создания план-факта если время плана после ПДО")
    fun planFactCreationSuccessPlanAfterPdo() {
        val shipmentOnSaturday = SHIPMENT_DATE.plusDays(1)
        val currentSegment = createCurrentSegment(shipmentDate = shipmentOnSaturday)
        val previousSegment = createPreviousSegment()
        val lomOrder = createLomOrder(previousSegment = previousSegment, currentSegment = currentSegment)
        val newCheckpoint = createNewCheckpoint(waybillSegment = previousSegment)

        mockSchedule(
            lomOrder.waybill[1],
            listOf(
                lomOrder.waybill[1].shipment.date!! to PartnerService.TimeWindow(
                    startTime = null,
                    endTime = LocalTime.of(15, 0)
                )
            ),
        )
        mockDuration(partnerFrom = lomOrder.waybill[1])

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        val planFact = getSavedPlanFact()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe currentSegment.id
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe Instant.parse("2021-04-03T13:00:00Z")
            planFact.producerName shouldBe PRODUCER_NAME
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe Instant.parse("2021-04-03T13:00:00Z")
        }
    }

    // Проверки обработки при существующих ПФ.

    @Test
    @DisplayName("Если существует план-факт с таким-же планом, то новый план-факт не сохраняется")
    fun doNotSaveNewIfExistsSame() {
        val previousSegment = createPreviousSegment()
        val currentSegment = createCurrentSegment()
        val lomOrder = createLomOrder(previousSegment = previousSegment, currentSegment = currentSegment)
        val newCheckpoint = createNewCheckpoint(waybillSegment = previousSegment)
        val existingPlanFact = createPlanFact(waybillSegment = currentSegment)
        val existingPlanFacts = mutableListOf(existingPlanFact)

        mockSchedule(
            currentSegment,
            listOf(
                currentSegment.shipment.date!! to PartnerService.TimeWindow(
                    startTime = null,
                    endTime = LocalTime.of(6, 0)
                )
            ),
        )
        mockDuration(currentSegment)

        val context = mockSegmentStatusAddedContext(
            lomOrder = lomOrder,
            newCheckpoint = newCheckpoint,
            existingPlanFacts = existingPlanFacts,
        )

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.CREATED
    }

    @Test
    @DisplayName("Если существует план-факт с другим планом, то он помечается как OUTDATED")
    fun markOldPlanFactOutdated() {
        val previousSegment = createPreviousSegment()
        val currentSegment = createCurrentSegment()
        val lomOrder = createLomOrder(previousSegment = previousSegment, currentSegment = currentSegment)
        val newCheckpoint = createNewCheckpoint(waybillSegment = previousSegment)
        val existingPlanFact = createPlanFact(
            waybillSegment = currentSegment,
            expectedTime = EXPECTED_DATETIME_BEFORE_8.minusSeconds(1),
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        mockSchedule(
            currentSegment,
            listOf(
                currentSegment.shipment.date!! to PartnerService.TimeWindow(
                    startTime = null,
                    endTime = LocalTime.of(6, 0)
                )
            ),
        )
        mockDuration(currentSegment)

        val context = mockSegmentStatusAddedContext(
            lomOrder = lomOrder,
            newCheckpoint = newCheckpoint,
            existingPlanFacts = existingPlanFacts,
        )

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService).save(any())
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED
    }

    // Проверки при ЧП.

    @DisplayName("Проставлять план-факт в IN_TIME, если факт успел вовремя")
    @Test
    fun setPlanFactInTime() {
        val currentSegment = createCurrentSegment()
        val lomOrder = createLomOrder(currentSegment = currentSegment)
        val newCheckpoint = createNewCheckpoint(
            waybillSegment = currentSegment,
            newSegmentStatus = SegmentStatus.OUT,
            newCheckpointTime = CHECKPOINT_110_BEFORE_8_TIME,
        )
        val existingPlanFact = createPlanFact(waybillSegment = currentSegment)
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockSegmentStatusAddedContext(
            lomOrder = lomOrder,
            newCheckpoint = newCheckpoint,
            existingPlanFacts = existingPlanFacts,
        )

        processor.waybillSegmentStatusAdded(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
    }

    @DisplayName("Закрывать план-факт как NOT_ACTUAL, если факт опоздал")
    @Test
    fun setPlanFactNotActual() {
        val currentSegment = createCurrentSegment()
        val lomOrder = createLomOrder(currentSegment = currentSegment)
        val newCheckpoint = createNewCheckpoint(
            waybillSegment = currentSegment,
            newSegmentStatus = SegmentStatus.OUT,
            newCheckpointTime = EXPECTED_DATETIME_BEFORE_8.plus(Duration.ofSeconds(1)),
        )
        val existingPlanFact = createPlanFact(waybillSegment = currentSegment)
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockSegmentStatusAddedContext(
            lomOrder = lomOrder,
            newCheckpoint = newCheckpoint,
            existingPlanFacts = existingPlanFacts,
        )

        processor.waybillSegmentStatusAdded(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
    }

    @DisplayName("Закрывать план-факт как NOT_ACTUAL, если на следующий сегмент пришел закрывающий статус после плана")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["IN", "OUT"]
    )
    fun closePlanFactAsNotActualOnCloseNextSegmentStatusAfterPlan(segmentStatus: SegmentStatus) {
        val currentSegment = createCurrentSegment()
        val nextSegment = createNextSegment()
        val lomOrder = createLomOrder(currentSegment = currentSegment, nextSegment = nextSegment)
        val newCheckpoint = createNewCheckpoint(
            waybillSegment = nextSegment,
            newSegmentStatus = segmentStatus,
            newCheckpointTime = EXPECTED_DATETIME_BEFORE_8.plus(Duration.ofSeconds(1)),
        )
        val existingPlanFact = createPlanFact(
            waybillSegment = currentSegment,
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockSegmentStatusAddedContext(
            lomOrder = lomOrder,
            newCheckpoint = newCheckpoint,
            existingPlanFacts = existingPlanFacts,
        )

        processor.waybillSegmentStatusAdded(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
    }

    @DisplayName("Не закрывать план-факт, если есть статусы не для закрытия на следующих сегментах")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["STARTED", "TRACK_RECEIVED", "PENDING", "INFO_RECEIVED"]
    )
    fun notClosePlanFactAsExpiredOnCloseNextSegmentsCp(segmentStatus: SegmentStatus) {
        val currentSegment = createCurrentSegment()
        val nextSegment = createNextSegment()
        val lomOrder = createLomOrder(currentSegment = currentSegment, nextSegment = nextSegment)
        val newCheckpoint = createNewCheckpoint(
            waybillSegment = nextSegment,
            newSegmentStatus = segmentStatus,
            newCheckpointTime = EXPECTED_DATETIME_BEFORE_8.plus(Duration.ofSeconds(1)),
        )
        val existingPlanFact = createPlanFact(
            waybillSegment = currentSegment,
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockSegmentStatusAddedContext(
            lomOrder = lomOrder,
            newCheckpoint = newCheckpoint,
            existingPlanFacts = existingPlanFacts,
        )

        processor.waybillSegmentStatusAdded(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.CREATED
    }

    // Проверки создания ПФ, при различных условиях.

    @DisplayName("ПФ не создается, если 1-й сегмент не SORTING_CENTER")
    @ParameterizedTest
    @EnumSource(
        value = SegmentType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["SORTING_CENTER"]
    )
    fun planFactNotCreateIfSegmentNotSc(segmentType: SegmentType) {
        val previousSegment = createPreviousSegment()
        val currentSegment = createCurrentSegment(segmentType = segmentType)
        val lomOrder = createLomOrder(previousSegment = previousSegment, currentSegment = currentSegment)
        val newCheckpoint = createNewCheckpoint(waybillSegment = previousSegment)

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
    }

    @DisplayName("ПФ не создается, если 2-й сегмент не DELIVERY")
    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["DELIVERY"]
    )
    fun planFactNotCreateIfNextSegmentNotSc(partnerType: PartnerType) {
        val previousSegment = createPreviousSegment()
        val currentSegment = createCurrentSegment()
        val nextSegment = createNextSegment(partnerType = partnerType)
        val lomOrder = createLomOrder(
            previousSegment = previousSegment,
            currentSegment = currentSegment,
            nextSegment = nextSegment
        )
        val newCheckpoint = createNewCheckpoint(waybillSegment = previousSegment)

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
    }

    @DisplayName("ПФ не создается, если 2-й сегмент MARKET_COURIER")
    @Test
    fun planFactNotCreateIfNextSegmentMarketCourier() {
        val previousSegment = createPreviousSegment()
        val currentSegment = createCurrentSegment()
        val nextSegment = createNextSegment(partnerSubtype = PartnerSubtype.MARKET_COURIER)
        val lomOrder = createLomOrder(
            previousSegment = previousSegment,
            currentSegment = currentSegment,
            nextSegment = nextSegment
        )
        val newCheckpoint = createNewCheckpoint(waybillSegment = previousSegment)

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("ПФ не создается, если Dropoff")
    fun planFactNotCreateIfDropoff() {
        val previousSegment = createPreviousSegment()
        val currentSegment = createDropoffSegment()
        val nextSegment = createNextSegment()
        val lomOrder = createLomOrder(
            previousSegment = previousSegment,
            currentSegment = currentSegment,
            nextSegment = nextSegment
        )
        val newCheckpoint = createNewCheckpoint(waybillSegment = previousSegment)

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("ПФ не создается, если приходит не ожидаемый статус")
    fun planFactNotCreateIfNotExpectedCheckpoint() {
        val currentSegment = createCurrentSegment()
        val nextSegment = createNextSegment()
        val lomOrder = createLomOrder(currentSegment = currentSegment, nextSegment = nextSegment)
        val newCheckpoint = createNewCheckpoint(
            waybillSegment = currentSegment,
            newSegmentStatus = SegmentStatus.PENDING,
        )

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("ПФ не создается, если на сегментах после сегмента для ПФ есть один из закрывающихся статусов")
    fun planFactNotCreateIfNextSegmentsWithCloseStatuses() {
        val previousSegment = createPreviousSegment()
        val currentSegment = createCurrentSegment()
        val nextSegment = createNextSegment()
        val lomOrder = createLomOrder(
            previousSegment = previousSegment,
            currentSegment = currentSegment,
            nextSegment = nextSegment
        )
        val newCheckpoint = createNewCheckpoint(waybillSegment = previousSegment)
        addWaybillSegmentCheckpoint(waybillSegment = nextSegment)

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("ПФ не создается, если есть факт")
    fun planFactNotCreateIfFactExists() {
        val previousSegment = createPreviousSegment()
        val currentSegment = createCurrentSegment()
        val lomOrder = createLomOrder(previousSegment = previousSegment, currentSegment = currentSegment)
        val newCheckpoint = createNewCheckpoint(waybillSegment = previousSegment)
        addWaybillSegmentCheckpoint(waybillSegment = currentSegment, newSegmentStatus = SegmentStatus.OUT)

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("Проверка создания план-факта при получении duration из route")
    fun planFactCreationSuccessWithAfter8AndDurationFromRoute() {
        val previousSegment = createPreviousSegment()
        val lomOrder = createLomOrder(previousSegment = previousSegment)
        val newCheckpoint = createNewCheckpoint(waybillSegment = previousSegment)

        mockSchedule(
            lomOrder.waybill[1],
            listOf(
                lomOrder.waybill[1].shipment.date!! to PartnerService.TimeWindow(
                    startTime = null,
                    endTime = LocalTime.of(6, 0)
                )
            ),
        )
        mockDuration(
            partnerFrom = lomOrder.waybill[1],
            duration = Duration.ofHours(2),
        )

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        val planFact = getSavedPlanFact()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe lomOrder.waybill[1].id
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe EXPECTED_DATETIME_BEFORE_8.plus(Duration.ofHours(1))
            planFact.producerName shouldBe PRODUCER_NAME
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_DATETIME_BEFORE_8.plus(Duration.ofHours(1))
        }
    }

    // Проверки при обработки заказа.

    @DisplayName("Проставлять план-факт в EXPIRED, если пришел один из закрывающих статусов заказа до плана")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["RETURNED", "DELIVERED", "LOST", "RETURNING"]
    )
    fun setPlanFactExpiredIfCloseStatusCameOnTime(triggerStatus: OrderStatus) {
        val context = mockLomOrderEventContext(
            orderStatus = triggerStatus,
            expectedTime = EXPECTED_DATETIME_BEFORE_8,
        )
        processor.lomOrderStatusChanged(context)
        val existingPlanFact = context.getPlanFactsFromProcessor(PRODUCER_NAME).single()
        checkPlanFactClosedAs(existingPlanFact, PlanFactStatus.EXPIRED)
    }

    @DisplayName("Проставлять план-факт в NOT_ACTUAL, если опоздал один из закрывающих статусов заказа")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["RETURNED", "DELIVERED", "LOST", "RETURNING"]
    )
    fun setPlanFactNotActualIfCloseStatusCameLate(triggerStatus: OrderStatus) {
        val context = mockLomOrderEventContext(
            orderStatus = triggerStatus,
            expectedTime = FIXED_TIME.minusSeconds(1),
        )
        processor.lomOrderStatusChanged(context)
        val existingPlanFact = context.getPlanFactsFromProcessor(PRODUCER_NAME).single()
        checkPlanFactClosedAs(existingPlanFact, PlanFactStatus.NOT_ACTUAL)
    }

    // Создание ПФ при изменении route.

    @Test
    @DisplayName("Обновление план-факта, если изменился route и waybillSegment.shipment.date")
    fun planFactCreationSuccessIfRouteChangedWithLogisticDate() {
        val currentSegment = createCurrentSegment(shipmentDate = SHIPMENT_DATE.plusDays(6))
        val lomOrder = createLomOrder(currentSegment = currentSegment)
        val existingPlanFact = createPlanFact(waybillSegment = currentSegment)
        val existingPlanFacts = mutableListOf(existingPlanFact)
        addWaybillSegmentCheckpoint(SegmentStatus.OUT, waybillSegment = lomOrder.waybill[0])

        mockSchedule(
            currentSegment,
            listOf(
                currentSegment.shipment.date!! to PartnerService.TimeWindow(
                    startTime = null,
                    endTime = LocalTime.of(6, 0)
                )
            ),
        )
        mockDuration(currentSegment)

        val context = mockLomOrderCombinatorRouteWasUpdatedContext(
            lomOrder = lomOrder,
            existingPlanFacts = existingPlanFacts
        )

        processor.combinatorRouteWasUpdated(context)

        val planFact = getSavedPlanFact()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe currentSegment.id
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe Instant.parse("2021-04-09T04:00:00Z")
            planFact.producerName shouldBe PRODUCER_NAME
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe Instant.parse("2021-04-09T04:00:00Z")
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED
        }
    }

    @Test
    @DisplayName("Если существует план-факт с таким-же планом, то новый план-факт не сохраняется, если изменился route")
    fun doNotSaveNewIfExistsSameIfRouteChanged() {
        val currentSegment = createCurrentSegment()
        val lomOrder = createLomOrder(currentSegment = currentSegment)
        val existingPlanFact = createPlanFact(waybillSegment = currentSegment)
        val existingPlanFacts = mutableListOf(existingPlanFact)
        addWaybillSegmentCheckpoint(SegmentStatus.OUT, waybillSegment = lomOrder.waybill[0])

        mockSchedule(
            currentSegment,
            listOf(
                currentSegment.shipment.date!! to PartnerService.TimeWindow(
                    startTime = null,
                    endTime = LocalTime.of(6, 0)
                )
            ),
        )
        mockDuration(currentSegment)

        val context = mockLomOrderCombinatorRouteWasUpdatedContext(
            lomOrder = lomOrder,
            existingPlanFacts = existingPlanFacts
        )

        processor.combinatorRouteWasUpdated(context)

        verify(planFactService, never()).save(any())
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.CREATED
    }

    // Вспомогательные методы.

    private fun getSavedPlanFact(): PlanFact {
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        verify(planFactService).save(planFactsCaptor.capture())
        return planFactsCaptor.firstValue.single()
    }

    private fun mockSegmentStatusAddedContext(
        lomOrder: LomOrder = createLomOrder(),
        newCheckpoint: WaybillSegmentStatusHistory,
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
    ) = LomWaybillStatusAddedContext(newCheckpoint, lomOrder, existingPlanFacts)

    private fun createLomOrder(
        previousSegment: WaybillSegment = createPreviousSegment(),
        currentSegment: WaybillSegment = createCurrentSegment(),
        nextSegment: WaybillSegment = createNextSegment(),
    ) = joinInOrder(listOf(previousSegment, currentSegment, nextSegment)).apply { id = TEST_ORDER_ID }

    private fun createNewCheckpoint(
        newSegmentStatus: SegmentStatus = SegmentStatus.OUT,
        newCheckpointTime: Instant = FIXED_TIME,
        waybillSegment: WaybillSegment,
    ): WaybillSegmentStatusHistory {
        val newCheckpoint = WaybillSegmentStatusHistory(status = newSegmentStatus, date = newCheckpointTime)
        waybillSegment.apply {
            waybillSegmentStatusHistory.add(newCheckpoint)
            newCheckpoint.waybillSegment = this
        }
        return newCheckpoint
    }

    private fun addWaybillSegmentCheckpoint(
        newSegmentStatus: SegmentStatus = SegmentStatus.IN,
        waybillSegment: WaybillSegment,
        checkpointReceivedDatetime: Instant = CHECKPOINT_110_BEFORE_8_TIME,
    ) = writeWaybillSegmentCheckpoint(waybillSegment, newSegmentStatus, checkpointReceivedDatetime)

    private fun createPreviousSegment(
        partnerType: PartnerType = PartnerType.FULFILLMENT,
    ): WaybillSegment = WaybillSegment(
        id = 50,
        partnerType = partnerType,
    )

    private fun createCurrentSegment(
        segmentType: SegmentType = SegmentType.SORTING_CENTER,
        shipmentDate: LocalDate = SHIPMENT_DATE,
    ) =
        WaybillSegment(
            id = 51,
            segmentType = segmentType,
            partnerId = TEST_CURRENT_PARTNER_ID,
            shipment = WaybillShipment(
                date = shipmentDate
            ),
        )

    private fun createDropoffSegment() =
        createCurrentSegment().apply { partnerType = PartnerType.DELIVERY }

    private fun createNextSegment(
        partnerType: PartnerType = PartnerType.DELIVERY,
        partnerSubtype: PartnerSubtype? = null
    ) =
        WaybillSegment(
            id = 52,
            partnerId = TEST_NEXT_PARTNER_ID,
            partnerType = partnerType,
            partnerSubtype = partnerSubtype
        )

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

    private fun createPlanFact(
        expectedTime: Instant = EXPECTED_DATETIME_BEFORE_8,
        waybillSegment: WaybillSegment,
    ): PlanFact {
        return PlanFact(
            id = 102,
            entityId = waybillSegment.id,
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            planFactStatus = PlanFactStatus.CREATED,
            expectedStatusDatetime = expectedTime,
            expectedStatus = EXPECTED_STATUS,
            producerName = PRODUCER_NAME,
        ).apply { entity = waybillSegment }
    }

    private fun mockLomOrderEventContext(
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
        orderStatus: OrderStatus = OrderStatus.CANCELLED,
        expectedTime: Instant = EXPECTED_DATETIME_BEFORE_8,
    ): LomOrderStatusChangedContext {
        val currentSegment = createCurrentSegment()
        val lomOrder = createLomOrder(currentSegment = currentSegment)
        val existingPlanFact = createPlanFact(
            waybillSegment = currentSegment,
            expectedTime = expectedTime,
        )
        existingPlanFacts.add(existingPlanFact)
        return LomOrderStatusChangedContext(lomOrder, orderStatus, existingPlanFacts)
    }

    private fun checkPlanFactClosedAs(planFact: PlanFact, planFactStatus: PlanFactStatus) {
        assertSoftly {
            planFact.planFactStatus shouldBe planFactStatus
            planFact.factStatusDatetime shouldBe null
            planFact.scheduleTime shouldBe FIXED_TIME
        }
    }

    private fun mockLomOrderCombinatorRouteWasUpdatedContext(
        lomOrder: LomOrder = createLomOrder(),
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
    ) = LomOrderCombinatorRouteWasUpdatedContext(lomOrder, null, existingPlanFacts)

    companion object {
        private val SHIPMENT_DATE: LocalDate = LocalDate.of(2021, 4, 2)
        private val FIXED_TIME = Instant.parse("2021-04-01T10:00:00.00Z")
        private val CHECKPOINT_110_BEFORE_8_TIME = Instant.parse("2021-04-01T19:14:00.00Z")
        private val EXPECTED_DATETIME_BEFORE_8 = Instant.parse("2021-04-03T04:00:00Z")
        private const val TEST_ORDER_ID = 1L
        private const val TEST_CURRENT_PARTNER_ID = 1L
        private const val TEST_NEXT_PARTNER_ID = 2L
        private const val PRODUCER_NAME = "ScDsShippedRddPlanFactProcessor"
        private const val EXPECTED_STATUS = "OUT"
    }
}
