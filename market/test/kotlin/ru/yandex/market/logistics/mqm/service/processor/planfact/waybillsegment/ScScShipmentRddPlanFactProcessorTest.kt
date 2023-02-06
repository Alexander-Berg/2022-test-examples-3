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
import ru.yandex.market.logistics.mqm.entity.lom.LomOrderCombinatorRoute
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PointType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.ServiceCodeName
import ru.yandex.market.logistics.mqm.service.CombinatorRouteService
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderCombinatorRouteWasUpdatedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.getFirstSegmentByTypeOrNull
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint

@ExtendWith(MockitoExtension::class)
class ScScShipmentRddPlanFactProcessorTest {
    private lateinit var processor: ScScShipmentRddPlanFactProcessor

    private val settingsService = TestableSettingsService()

    private val clock = TestableClock()

    @Mock
    private lateinit var planFactService: PlanFactService

    @Mock
    private lateinit var combinatorRouteService: CombinatorRouteService

    @BeforeEach
    fun setUp() {
        processor = ScScShipmentRddPlanFactProcessor(
            settingsService,
            clock,
            planFactService,
            combinatorRouteService,
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

        mockCombinatorRoute()

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        val planFact = getSavedPlanFact()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe lomOrder.getFirstSegmentByTypeOrNull(SegmentType.SORTING_CENTER)!!.id
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe EXPECTED_TIME
            planFact.producerName shouldBe PRODUCER_NAME
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_TIME
        }
    }

    @Test
    @DisplayName("Проверка создания план-факта, предыдущий сегмент - DROPSHIP")
    fun planFactCreationSuccessPreviousDropship() {
        val previousSegment = createPreviousSegment(partnerType = PartnerType.DROPSHIP)
        val currentSegment = createCurrentSegment()
        val lomOrder = createLomOrder(previousSegment = previousSegment, currentSegment = currentSegment)
        val newCheckpoint = createNewCheckpoint(newSegmentStatus = SegmentStatus.IN, waybillSegment = currentSegment)

        mockCombinatorRoute()

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        val planFact = getSavedPlanFact()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe currentSegment.id
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe EXPECTED_TIME
            planFact.producerName shouldBe PRODUCER_NAME
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_TIME
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

        mockCombinatorRoute()

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
            expectedTime = EXPECTED_TIME.minusSeconds(1),
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        mockCombinatorRoute()

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
        val newCheckpoint = createNewCheckpoint(waybillSegment = currentSegment, newSegmentStatus = SegmentStatus.OUT)
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
            newCheckpointTime = EXPECTED_TIME.plus(Duration.ofSeconds(1)),
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
            newCheckpointTime = EXPECTED_TIME.plus(Duration.ofSeconds(1)),
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
            newCheckpointTime = EXPECTED_TIME.plus(Duration.ofSeconds(1)),
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

    @DisplayName("ПФ не создается, если 2-й сегмент не SORTING_CENTER")
    @ParameterizedTest
    @EnumSource(
        value = SegmentType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["SORTING_CENTER"]
    )
    fun planFactNotCreateIfNextSegmentNotSc(segmentType: SegmentType) {
        val previousSegment = createPreviousSegment()
        val currentSegment = createCurrentSegment()
        val nextSegment = createNextSegment(segmentType = segmentType)
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
    @DisplayName("Проверка обновления план-факта, если изменился route")
    fun planFactCreationSuccessIfRouteChanged() {
        val previousSegment = createPreviousSegment()
        val currentSegment = createCurrentSegment()
        val lomOrder = createLomOrder(previousSegment = previousSegment, currentSegment = currentSegment)
        val existingPlanFact = createPlanFact(waybillSegment = currentSegment)
        val existingPlanFacts = mutableListOf(existingPlanFact)
        addWaybillSegmentCheckpoint(SegmentStatus.OUT, previousSegment)

        mockCombinatorRoute(serviceTime = TEST_COMBINATOR_TIME.plus(RECALCULATION_ADDITIONAL_TIME))

        val context = mockLomOrderCombinatorRouteWasUpdatedContext(lomOrder, existingPlanFacts)

        processor.combinatorRouteWasUpdated(context)

        val planFact = getSavedPlanFact()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe currentSegment.id
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe RECALCULATION_EXPECTED_TIME
            planFact.producerName shouldBe PRODUCER_NAME
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe RECALCULATION_EXPECTED_TIME
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED
        }
    }

    @Test
    @DisplayName("Если существует план-факт с таким-же планом, то новый план-факт не сохраняется, если изменился route")
    fun doNotSaveNewIfExistsSameIfRouteChanged() {
        val previousSegment = createPreviousSegment()
        val currentSegment = createCurrentSegment()
        val lomOrder = createLomOrder(previousSegment = previousSegment, currentSegment = currentSegment)
        val existingPlanFact = createPlanFact(waybillSegment = currentSegment)
        val existingPlanFacts = mutableListOf(existingPlanFact)
        addWaybillSegmentCheckpoint(SegmentStatus.OUT, previousSegment)

        mockCombinatorRoute()

        val context = mockLomOrderCombinatorRouteWasUpdatedContext(lomOrder, existingPlanFacts)

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
    ) = writeWaybillSegmentCheckpoint(waybillSegment, newSegmentStatus, NEW_CHECKPOINT_TIME)

    private fun createPreviousSegment(
        partnerType: PartnerType = PartnerType.FULFILLMENT,
    ): WaybillSegment = WaybillSegment(
        id = 50,
        partnerType = partnerType,
    )

    private fun createCurrentSegment(
        segmentType: SegmentType = SegmentType.SORTING_CENTER,
    ) =
        WaybillSegment(
            id = 51,
            segmentType = segmentType,
            partnerId = TEST_CURRENT_PARTNER_ID,
            combinatorSegmentIds = listOf(1)
        )

    private fun createNextSegment(
        segmentType: SegmentType = SegmentType.SORTING_CENTER,
    ) =
        WaybillSegment(
            id = 52,
            partnerId = TEST_NEXT_PARTNER_ID,
            segmentType = segmentType,
        )

    private fun mockDeliveryService(
        serviceCode: ServiceCodeName = ServiceCodeName.MOVEMENT,
        serviceTime: Instant? = TEST_COMBINATOR_TIME,
    ) = LomOrderCombinatorRoute.DeliveryService(
        code = serviceCode,
        startTime = serviceTime?.let { LomOrderCombinatorRoute.Timestamp(seconds = it.epochSecond) },
    )

    private fun mockCombinatorRoute(
        serviceTime: Instant? = TEST_COMBINATOR_TIME,
    ) {
        val movementService = mockDeliveryService(serviceTime = serviceTime)
        val deliveryRoute = LomOrderCombinatorRoute.DeliveryRoute(
            paths = listOf(
                LomOrderCombinatorRoute.Path(pointFrom = 0, pointTo = 1),
            ),
            points = listOf(
                LomOrderCombinatorRoute.Point(segmentId = 1, segmentType = PointType.WAREHOUSE),
                LomOrderCombinatorRoute.Point(
                    segmentId = 2,
                    segmentType = PointType.MOVEMENT,
                    services = listOf(movementService)
                ),
            )
        )
        val route = LomOrderCombinatorRoute(TEST_ORDER_ID, deliveryRoute)
        whenever(combinatorRouteService.getByLomOrderId(TEST_ORDER_ID)).thenReturn(route)
    }

    private fun createPlanFact(
        expectedTime: Instant = EXPECTED_TIME,
        waybillSegment: WaybillSegment,
    ): PlanFact {
        return PlanFact(
            id = 102,
            entityId = waybillSegment.id,
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            planFactStatus = PlanFactStatus.CREATED,
            expectedStatusDatetime = expectedTime,
            expectedStatus = SegmentStatus.OUT.name,
            producerName = PRODUCER_NAME,
        ).apply { entity = waybillSegment }
    }

    private fun mockLomOrderEventContext(
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
        orderStatus: OrderStatus = OrderStatus.CANCELLED,
        expectedTime: Instant = EXPECTED_TIME,
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
        private val FIXED_TIME = Instant.ofEpochSecond(1635444000)
        private val TEST_COMBINATOR_TIME = FIXED_TIME
        private val ADDITIONAL_TIME = Duration.ofHours(1)
        private const val PRODUCER_NAME = "ScScShipmentRddPlanFactProcessor"
        private const val EXPECTED_STATUS = "OUT"
        private val EXPECTED_TIME = TEST_COMBINATOR_TIME.plus(ADDITIONAL_TIME)
        private const val TEST_NEXT_PARTNER_ID = 2L
        private const val TEST_ORDER_ID = 1L
        private const val TEST_CURRENT_PARTNER_ID = 1L
        private val NEW_CHECKPOINT_TIME = FIXED_TIME
        private val RECALCULATION_ADDITIONAL_TIME = Duration.ofHours(2)
        private val RECALCULATION_EXPECTED_TIME =
            TEST_COMBINATOR_TIME.plus(RECALCULATION_ADDITIONAL_TIME).plus(ADDITIONAL_TIME)
    }
}
