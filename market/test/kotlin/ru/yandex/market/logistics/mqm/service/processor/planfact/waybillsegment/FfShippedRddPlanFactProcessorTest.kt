package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant
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
import ru.yandex.market.logistics.mqm.entity.lom.PartnerSettings
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.embedded.WaybillShipment
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderCombinatorRouteWasUpdatedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint

@ExtendWith(MockitoExtension::class)
class FfShippedRddPlanFactProcessorTest {
    private lateinit var processor: FfShippedRddPlanFactProcessor

    private val settingsService = TestableSettingsService()

    private val clock = TestableClock()

    @Mock
    private lateinit var planFactService: PlanFactService

    @BeforeEach
    fun setUp() {
        processor = FfShippedRddPlanFactProcessor(settingsService, clock, planFactService)
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE)
    }

    // Создание ПФ.

    @Test
    @DisplayName("Проверка создания план-факта")
    fun planFactCreationSuccess() {
        val context = mockLomOrderEventContext()

        processor.lomOrderStatusChanged(context)

        val planFact = getSavedPlanFact()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe 51L
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe EXPECTED_TIME
            planFact.producerName shouldBe PRODUCER_NAME
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_TIME
        }
    }

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        names = [
            "ENQUEUED", "PROCESSING"
        ],
        mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("План-факт не создается, если статус заказа не [ENQUEUED, PROCESSING]")
    fun planFactNotCreationIfNotOrderStatus(orderStatus: OrderStatus) {
        val lomOrder = createLomOrder(orderStatus = orderStatus)
        val context = mockLomOrderEventContext(lomOrder = lomOrder)

        processor.lomOrderStatusChanged(context)

        verify(planFactService, never()).save(any())
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        names = [
            "FULFILLMENT",
        ],
        mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("План-факт не создается, если тип партнера сегмента не FULFILLMENT")
    fun planFactNotCreationIfNotPartnerType(partnerType: PartnerType) {
        val lomOrder = createLomOrder(currentSegment = createCurrentWaybillSegment(partnerType = partnerType))
        val context = mockLomOrderEventContext(lomOrder = lomOrder)

        processor.lomOrderStatusChanged(context)

        verify(planFactService, never()).save(any())
    }

    @DisplayName("План-факт не создается, если сегмент - это Экспресс")
    @Test
    fun planFactNotCreationIfExpress() {
        val lomOrder = createLomOrder(currentSegment = createCurrentWaybillSegment(isFromExpress = true))
        val context = mockLomOrderEventContext(lomOrder = lomOrder)

        processor.lomOrderStatusChanged(context)

        verify(planFactService, never()).save(any())
    }

    @DisplayName("План-факт не создается, если сегмент - это OnDemand")
    @Test
    fun planFactNotCreationIfOnDemand() {
        val currentSegment = createCurrentWaybillSegment(
            partnerType = PartnerType.DELIVERY,
            isFromOnDemand = true,
        )
        val lomOrder = createLomOrder(currentSegment = currentSegment)
        val context = mockLomOrderEventContext(lomOrder = lomOrder)

        processor.lomOrderStatusChanged(context)

        verify(planFactService, never()).save(any())
    }

    @DisplayName("План-факт не создается, если есть признак о возвратности")
    @Test
    fun planFactNotCreationIfReturn() {
        val lomOrder = createLomOrder(isReturn = true)
        val context = mockLomOrderEventContext(lomOrder = lomOrder)

        processor.lomOrderStatusChanged(context)

        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("ПФ не создается, если есть один из закрывающихся статусов на сегментах")
    fun planFactNotCreateIfSegmentsWithClosesStatuses() {
        val currentSegment = createCurrentWaybillSegment()
        val lomOrder = createLomOrder(currentSegment = currentSegment)
        addWaybillSegmentCheckpoint(waybillSegment = currentSegment)
        val context = mockLomOrderEventContext(lomOrder = lomOrder)

        processor.lomOrderStatusChanged(context)

        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("Если существует план-факт с таким-же планом, то новый план-факт не сохраняется")
    fun doNotSaveNewIfExistsSame() {
        val currentSegment = createCurrentWaybillSegment()
        val lomOrder = createLomOrder(currentSegment = currentSegment)
        val existingPlanFact = createPlanFact(waybillSegment = currentSegment)
        val existingPlanFacts = mutableListOf(existingPlanFact)
        val context = mockLomOrderEventContext(lomOrder = lomOrder, existingPlanFacts = existingPlanFacts)

        processor.lomOrderStatusChanged(context)

        verify(planFactService, never()).save(any())
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.CREATED
    }

    @Test
    @DisplayName("Если существует план-факт с другим планом, то он помечается как OUTDATED")
    fun markOldPlanFactOutdated() {
        val currentSegment = createCurrentWaybillSegment()
        val lomOrder = createLomOrder(currentSegment = currentSegment)
        val existingPlanFact = createPlanFact(
            waybillSegment = currentSegment,
            expectedTime = EXPECTED_TIME.minusSeconds(1),
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)
        val context = mockLomOrderEventContext(lomOrder = lomOrder, existingPlanFacts = existingPlanFacts)

        processor.lomOrderStatusChanged(context)

        verify(planFactService).save(any())
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED
    }

    // Закрытие ПФ при закрывающих статусах заказа.

    @DisplayName("Проставлять план-факт в EXPIRED, если пришел один из закрывающих статусов заказа до плана")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["CANCELLED", "RETURNING", "RETURNED", "LOST", "DELIVERED"]
    )
    fun setPlanFactExpiredIfCloseStatusCameOnTime(orderStatus: OrderStatus) {
        val currentSegment = createCurrentWaybillSegment()
        val lomOrder = createLomOrder(orderStatus = orderStatus, currentSegment = currentSegment)
        val existingPlanFact = createPlanFact(
            waybillSegment = currentSegment,
            expectedTime = EXPECTED_TIME,
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)
        val context = mockLomOrderEventContext(lomOrder = lomOrder, existingPlanFacts = existingPlanFacts)

        processor.lomOrderStatusChanged(context)

        verify(planFactService, never()).save(any())

        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.scheduleTime shouldBe FIXED_TIME
        }
    }

    @DisplayName("Проставлять план-факт в NOT_ACTUAL, если опоздал один из закрывающих статусов заказа")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["CANCELLED", "RETURNING", "RETURNED", "LOST", "DELIVERED"]
    )
    fun setPlanFactNotActualIfCloseStatusCameLate(orderStatus: OrderStatus) {
        val currentSegment = createCurrentWaybillSegment()
        val lomOrder = createLomOrder(orderStatus = orderStatus, currentSegment = currentSegment)
        val existingPlanFact = createPlanFact(
            waybillSegment = currentSegment,
            expectedTime = FIXED_TIME.minusSeconds(1),
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)
        val context = mockLomOrderEventContext(lomOrder = lomOrder, existingPlanFacts = existingPlanFacts)

        processor.lomOrderStatusChanged(context)

        verify(planFactService, never()).save(any())

        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.scheduleTime shouldBe FIXED_TIME
        }
    }

    // Закрытие ПФ.

    @DisplayName("Проставлять план-факт в IN_TIME, если факт успел вовремя")
    @Test
    fun setPlanFactInTime() {
        val currentSegment = createCurrentWaybillSegment()
        val lomOrder = createLomOrder(currentSegment = currentSegment)
        val existingPlanFact = createPlanFact(
            waybillSegment = currentSegment,
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)
        val newCheckpoint = createNewCheckpoint(waybillSegment = currentSegment)
        val context = mockSegmentStatusAddedContext(
            lomOrder = lomOrder,
            existingPlanFacts = existingPlanFacts,
            newCheckpoint = newCheckpoint,
        )

        processor.waybillSegmentStatusAdded(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
    }

    @DisplayName("Закрывать план-факт как NOT_ACTUAL, если факт опоздал")
    @Test
    fun setPlanFactNotActual() {
        val currentSegment = createCurrentWaybillSegment()
        val lomOrder = createLomOrder(currentSegment = currentSegment)
        val existingPlanFact = createPlanFact(
            waybillSegment = currentSegment,
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)
        val newCheckpoint = createNewCheckpoint(
            waybillSegment = currentSegment,
            newCheckpointTime = EXPECTED_TIME.plus(Duration.ofSeconds(1)),
        )
        val context = mockSegmentStatusAddedContext(
            lomOrder = lomOrder,
            existingPlanFacts = existingPlanFacts,
            newCheckpoint = newCheckpoint,
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
        val currentSegment = createCurrentWaybillSegment()
        val nextSegment = createNextSegment()
        val lomOrder = createLomOrder(currentSegment = currentSegment, nextSegment = nextSegment)
        val existingPlanFact = createPlanFact(
            waybillSegment = currentSegment,
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)
        val newCheckpoint = createNewCheckpoint(
            waybillSegment = nextSegment,
            newCheckpointTime = EXPECTED_TIME.plus(Duration.ofSeconds(1)),
            newSegmentStatus = segmentStatus,
        )
        val context = mockSegmentStatusAddedContext(
            lomOrder = lomOrder,
            existingPlanFacts = existingPlanFacts,
            newCheckpoint = newCheckpoint,
        )

        processor.waybillSegmentStatusAdded(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
    }

    // Создание ПФ при изменении route.

    @Test
    @DisplayName("Проверка создания план-факта, если изменился route")
    fun planFactCreationSuccessIfRouteChanged() {
        val currentSegment = createCurrentWaybillSegment()
        val lomOrder = createLomOrder(currentSegment = currentSegment)
        val context = mockLomOrderCombinatorRouteWasUpdatedContext(lomOrder)

        processor.combinatorRouteWasUpdated(context)

        val planFact = getSavedPlanFact()

        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe 51L
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe EXPECTED_TIME
            planFact.producerName shouldBe PRODUCER_NAME
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_TIME
        }
    }

    @Test
    @DisplayName("Если существует план-факт с таким-же планом, то новый план-факт не сохраняется, если изменился route")
    fun doNotSaveNewIfExistsSameIfRouteChanged() {
        val currentSegment = createCurrentWaybillSegment()
        val lomOrder = createLomOrder(currentSegment = currentSegment)
        val existingPlanFact = createPlanFact(waybillSegment = currentSegment)
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockLomOrderCombinatorRouteWasUpdatedContext(lomOrder, existingPlanFacts)

        processor.combinatorRouteWasUpdated(context)

        verify(planFactService, never()).save(any())
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.CREATED
    }

    @Test
    @DisplayName("Если существует план-факт с другим планом, то он помечается как OUTDATED, если изменился route")
    fun markOldPlanFactOutdatedIfRouteChanged() {
        val currentSegment = createCurrentWaybillSegment(shipmentDateTime = RECALCULATED_SERVICE_SCHEDULE_END_TIME)
        val lomOrder = createLomOrder(currentSegment = currentSegment)
        val existingPlanFact = createPlanFact(waybillSegment = currentSegment)
        val existingPlanFacts = mutableListOf(existingPlanFact)
        val context = mockLomOrderCombinatorRouteWasUpdatedContext(lomOrder, existingPlanFacts)

        processor.combinatorRouteWasUpdated(context)

        verify(planFactService).save(any())
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED
    }

    // Вспомогательные методы.

    private fun getSavedPlanFact(): PlanFact {
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        verify(planFactService).save(planFactsCaptor.capture())
        return planFactsCaptor.firstValue.single()
    }

    private fun mockLomOrderEventContext(
        lomOrder: LomOrder = createLomOrder(),
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
    ) = LomOrderStatusChangedContext(lomOrder, lomOrder.status!!, existingPlanFacts)

    private fun mockSegmentStatusAddedContext(
        lomOrder: LomOrder = createLomOrder(),
        newCheckpoint: WaybillSegmentStatusHistory,
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
    ) = LomWaybillStatusAddedContext(newCheckpoint, lomOrder, existingPlanFacts)

    private fun createLomOrder(
        nextSegment: WaybillSegment = createNextSegment(),
        currentSegment: WaybillSegment = createCurrentWaybillSegment(),
        isReturn: Boolean = false,
        orderStatus: OrderStatus = OrderStatus.ENQUEUED,
    ): LomOrder {
        val lomOrder = if (isReturn) {
            joinInOrder(listOf(nextSegment, currentSegment))
        } else {
            joinInOrder(listOf(currentSegment, nextSegment))
        }
        lomOrder.apply { status = orderStatus }
        return lomOrder
    }

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
        newSegmentStatus: SegmentStatus = SegmentStatus.OUT,
        waybillSegment: WaybillSegment,
    ) = writeWaybillSegmentCheckpoint(waybillSegment, newSegmentStatus, CURRENT_CHECKPOINT_TIME)

    private fun createCurrentWaybillSegment(
        partnerType: PartnerType = PartnerType.FULFILLMENT,
        isFromExpress: Boolean = false,
        isFromOnDemand: Boolean = false,
        shipmentDateTime: Instant? = DEFAULT_PLANNED_SHIPMENT_DATETIME,
    ): WaybillSegment {
        val segment = WaybillSegment(
            id = 51,
            partnerId = 1,
            partnerType = partnerType,
            segmentType = SegmentType.FULFILLMENT,
            partnerSettings = PartnerSettings(dropshipExpress = isFromExpress),
            shipment = WaybillShipment(dateTime = shipmentDateTime)
        )
        if (isFromOnDemand) {
            segment.apply {
                waybillSegmentTags = mutableSetOf(WaybillSegmentTag.ON_DEMAND)
            }
        }
        return segment
    }

    private fun createNextSegment(): WaybillSegment =
        WaybillSegment(
            id = 52,
            partnerId = 2,
            partnerType = PartnerType.DELIVERY
        )

    private fun createPlanFact(
        expectedTime: Instant = EXPECTED_TIME,
        waybillSegment: WaybillSegment,
    ): PlanFact {
        val planFact = PlanFact(
            entityId = waybillSegment.id,
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            planFactStatus = PlanFactStatus.CREATED,
            expectedStatusDatetime = expectedTime,
            expectedStatus = EXPECTED_STATUS,
            producerName = PRODUCER_NAME,
        )
        planFact.apply { entity = waybillSegment }
        return planFact
    }

    private fun mockLomOrderCombinatorRouteWasUpdatedContext(
        lomOrder: LomOrder = createLomOrder(),
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
    ) = LomOrderCombinatorRouteWasUpdatedContext(lomOrder, null, existingPlanFacts)

    companion object {
        private val FIXED_TIME = Instant.parse("2021-10-01T02:00:00.00Z")
        private val DEFAULT_PLANNED_SHIPMENT_DATETIME = Instant.parse("2021-10-01T10:00:00.00Z")
        private val DEFAULT_TIMEOUT = Duration.ofHours(5)
        private val EXPECTED_TIME = DEFAULT_PLANNED_SHIPMENT_DATETIME.plus(DEFAULT_TIMEOUT)
        private const val PRODUCER_NAME = "FfShippedRddPlanFactProcessor"
        private const val EXPECTED_STATUS = "OUT"
        private val CURRENT_CHECKPOINT_TIME = Instant.parse("2021-10-01T02:00:00.00Z")
        private val RECALCULATED_SERVICE_SCHEDULE_END_TIME = Instant.parse("2021-10-01T16:00:00.00Z")
    }
}
