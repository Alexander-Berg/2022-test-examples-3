package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.mqm.configuration.properties.TaxiExpressOrdersProperties
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.service.processor.planfact.BaseSelfSufficientPlanFactTest
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint

abstract class ExpressReadyToShipPlanFactProcessorTest: BaseSelfSufficientPlanFactTest() {

    // Проверки создания ПФ, при различных условиях.

    @Test
    @DisplayName("Проверка создания план-факта")
    fun planFactCreationSuccess() {
        val firstSegment = createFirstSegment()
        val secondSegment = createSecondSegment()
        val order = createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment)
        val checkpoint = createNewCheckpoint(waybillSegment = firstSegment)
        val context = mockSegmentStatusAddedContext(order, checkpoint)
        val processor = createProcessor()

        processor.waybillSegmentStatusAdded(context)

        val planFact = getSavedPlanFact(planFactService)
        val expectedPlanFact = createSuccessPlanFact()
        verifyPlanFact(planFact, expectedPlanFact)
    }

    @Test
    @DisplayName("ПФ не создается, если уже есть ожидаемый статус на сегменте")
    fun planFactNotCreateIfSegmentHasStatus() {
        val firstSegment = createFirstSegment()
        val secondSegment = createSecondSegment()
        val order = createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment)
        val checkpoint = createNewCheckpoint(waybillSegment = firstSegment)
        val context = mockSegmentStatusAddedContext(order, checkpoint)
        val processor = createProcessor()

        writeWaybillSegmentCheckpoint(firstSegment, getExpectedStatus(), CURRENT_TIME.minusSeconds(1))

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
    }

    // Проверки при существующих ПФ.

    @DisplayName("Если существует план-факт с таким-же планом, то новый план-факт не сохраняется")
    @Test
    fun doNotSaveNewIfExistsSame() {
        val firstSegment = createFirstSegment()
        val secondSegment = createSecondSegment()
        val order = createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment)
        val checkpoint = createNewCheckpoint(waybillSegment = firstSegment)

        val existingPlanFact = createExistsPlanFact(
            waybillSegment = firstSegment,
            expectedTime = getExpectedTime(),
        )

        val context =
            mockSegmentStatusAddedContext(order, checkpoint, existingPlanFacts = mutableListOf(existingPlanFact))
        val processor = createProcessor()

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.CREATED
    }

    @DisplayName("Если существует план-факт с другим планом, то он помечается как OUTDATED")
    @Test
    fun markExistsPlanFactOutdated() {
        val firstSegment = createFirstSegment()
        val secondSegment = createSecondSegment()
        val order = createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment)
        val checkpoint = createNewCheckpoint(waybillSegment = firstSegment)

        val existingPlanFact = createExistsPlanFact(
            waybillSegment = firstSegment,
            expectedTime = getExpectedTime().minusSeconds(1),
        )

        val context =
            mockSegmentStatusAddedContext(order, checkpoint, existingPlanFacts = mutableListOf(existingPlanFact))
        val processor = createProcessor()

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService).save(any())
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED
    }

    // Проверки при ЧП.

    @DisplayName("Проставлять план-факт в IN_TIME, если факт успел вовремя")
    @Test
    fun setPlanFactInTime() {
        val firstSegment = createFirstSegment()
        val secondSegment = createSecondSegment()
        val order = createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment)
        val checkpoint = createNewCheckpoint(
            waybillSegment = firstSegment,
            newSegmentStatus = getExpectedStatus(),
            newCheckpointTime = CURRENT_TIME.minusSeconds(1),
        )

        val existingPlanFact = createExistsPlanFact(
            waybillSegment = firstSegment,
            expectedTime = CURRENT_TIME,
        )

        val context =
            mockSegmentStatusAddedContext(order, checkpoint, existingPlanFacts = mutableListOf(existingPlanFact))
        val processor = createProcessor()

        processor.waybillSegmentStatusAdded(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
    }

    @DisplayName("Закрывать план-факт как NOT_ACTUAL, если факт опоздал")
    @Test
    fun setPlanFactNotActual() {
        val firstSegment = createFirstSegment()
        val secondSegment = createSecondSegment()
        val order = createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment)
        val checkpoint = createNewCheckpoint(
            waybillSegment = firstSegment,
            newSegmentStatus = getExpectedStatus(),
            newCheckpointTime = CURRENT_TIME.plusSeconds(1),
        )

        val existingPlanFact = createExistsPlanFact(
            waybillSegment = firstSegment,
            expectedTime = CURRENT_TIME,
        )

        val context =
            mockSegmentStatusAddedContext(order, checkpoint, existingPlanFacts = mutableListOf(existingPlanFact))
        val processor = createProcessor()

        processor.waybillSegmentStatusAdded(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
    }

    @DisplayName("Закрывать план-факт как NOT_ACTUAL, если факт пришел со следующим статусом")
    @Test
    fun closePlanFactIfNextSegmentStatus() {
        val firstSegment = createFirstSegment()
        val secondSegment = createSecondSegment()
        val order = createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment)
        val checkpoint = createNewCheckpoint(
            waybillSegment = firstSegment,
            newSegmentStatus = SegmentStatus.OUT,
            newCheckpointTime = CURRENT_TIME.plusSeconds(1),
        )

        val existingPlanFact = createExistsPlanFact(
            waybillSegment = firstSegment,
            expectedTime = CURRENT_TIME,
        )

        val context =
            mockSegmentStatusAddedContext(order, checkpoint, existingPlanFacts = mutableListOf(existingPlanFact))
        val processor = createProcessor()

        processor.waybillSegmentStatusAdded(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
    }

    @Test
    @DisplayName("ПФ не закрывается, если пришел TRANSIT_COURIER_SEARCH (31)")
    fun planFactNotCloseIf31Status() {
        val firstSegment = createFirstSegment(hasExpressTag = true)
        val secondSegment = createSecondSegment()
        val order = createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment)
        val checkpoint = createNewCheckpoint(
            waybillSegment = firstSegment,
            newSegmentStatus = SegmentStatus.TRANSIT_COURIER_SEARCH,
            newCheckpointTime = CURRENT_TIME.minusSeconds(1),
        )

        val existingPlanFact = createExistsPlanFact(
            waybillSegment = firstSegment,
            expectedTime = CURRENT_TIME,
        )

        val context =
            mockSegmentStatusAddedContext(order, checkpoint, existingPlanFacts = mutableListOf(existingPlanFact))
        val processor = createProcessor()

        processor.waybillSegmentStatusAdded(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.CREATED
    }

    // Проверки при обработке заказа.

    @DisplayName("Проставлять план-факт в EXPIRED, если пришел один из закрывающих статусов заказа до плана")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["RETURNED", "DELIVERED", "LOST", "RETURNING"]
    )
    fun setPlanFactExpiredIfCloseStatusCameOnTime(orderStatus: OrderStatus) {
        val context = createLomOrderStatusChangedContext(orderStatus, getExpectedTime())
        val processor = createProcessor()
        val existingPlanFact = context.getPlanFactsFromProcessor(getProducerName()).first()

        processor.lomOrderStatusChanged(context)

        checkPlanFactClosedAs(existingPlanFact, PlanFactStatus.EXPIRED, CURRENT_TIME)
    }

    @DisplayName("Проставлять план-факт в NOT_ACTUAL, если опоздал один из закрывающих статусов заказа")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["RETURNED", "DELIVERED", "LOST", "RETURNING"]
    )
    fun setPlanFactNotActualIfCloseStatusCameLate(orderStatus: OrderStatus) {
        val context = createLomOrderStatusChangedContext(orderStatus, CURRENT_TIME.minusSeconds(1))
        val processor = createProcessor()
        val existingPlanFact = context.getPlanFactsFromProcessor(getProducerName()).first()

        processor.lomOrderStatusChanged(context)

        checkPlanFactClosedAs(existingPlanFact, PlanFactStatus.NOT_ACTUAL, CURRENT_TIME)
    }

    protected abstract fun createProcessor(
        taxiExpressOrdersProperties: TaxiExpressOrdersProperties = TaxiExpressOrdersProperties(),
    ): ExpressReadyToShipPlanFactProcessor

    protected abstract fun createFirstSegment(
        dropshipExpress: Boolean = true,
        hasExpressTag: Boolean = false,
    ): WaybillSegment

    protected abstract fun createSuccessPlanFact(): PlanFact

    protected abstract fun getExpectedStatus(): SegmentStatus

    protected abstract fun createExistsPlanFact(
        waybillSegment: WaybillSegment,
        expectedTime: Instant,
    ): PlanFact

    protected abstract fun getExpectedTime(): Instant

    protected abstract fun getProducerName(): String

    private fun createSecondSegment() = WaybillSegment(
        id = 52L,
        partnerId = 2L,
        waybillSegmentIndex = 2,
        partnerType = PartnerType.DELIVERY,
        segmentType = SegmentType.COURIER,
        callCourierTime = CURRENT_TIME,
    )

    private fun createLomOrderStatusChangedContext(
        orderStatus: OrderStatus,
        expectedTime: Instant,
    ): LomOrderStatusChangedContext {
        val secondSegment = createSecondSegment()
        val lomOrder = createLomOrder(secondSegment = secondSegment)
        val existingPlanFact = createExistsPlanFact(waybillSegment = secondSegment, expectedTime = expectedTime)
        val existingPlanFacts = mutableListOf(existingPlanFact)
        return LomOrderStatusChangedContext(lomOrder, orderStatus, existingPlanFacts)
    }

    // Вспомогательные методы.

    private fun verifyPlanFact(
        actualPlanFact: PlanFact,
        expectedPlanFact: PlanFact,
    ) {
        assertSoftly {
            actualPlanFact.entityType shouldBe expectedPlanFact.entityType
            actualPlanFact.entityId shouldBe expectedPlanFact.entityId
            actualPlanFact.expectedStatus shouldBe expectedPlanFact.expectedStatus
            actualPlanFact.expectedStatusDatetime shouldBe expectedPlanFact.expectedStatusDatetime
            actualPlanFact.producerName shouldBe expectedPlanFact.producerName
            actualPlanFact.planFactStatus shouldBe expectedPlanFact.planFactStatus
            actualPlanFact.processingStatus shouldBe expectedPlanFact.processingStatus
            actualPlanFact.scheduleTime shouldBe expectedPlanFact.scheduleTime
        }
    }

    private fun createLomOrder(
        firstSegment: WaybillSegment = createFirstSegment(),
        secondSegment: WaybillSegment = createSecondSegment(),
    ) = joinInOrder(listOf(firstSegment, secondSegment)).apply { id = 1 }

    private fun createNewCheckpoint(
        newSegmentStatus: SegmentStatus = SegmentStatus.PENDING,
        newCheckpointTime: Instant = CURRENT_TIME,
        waybillSegment: WaybillSegment,
    ): WaybillSegmentStatusHistory {
        val newCheckpoint = WaybillSegmentStatusHistory(status = newSegmentStatus, date = newCheckpointTime)
        waybillSegment.apply {
            waybillSegmentStatusHistory.add(newCheckpoint)
            newCheckpoint.waybillSegment = this
        }
        return newCheckpoint
    }

    private fun getSavedPlanFact(planFactService: PlanFactService): PlanFact {
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        verify(planFactService).save(planFactsCaptor.capture())
        return planFactsCaptor.firstValue.single()
    }

    private fun checkPlanFactClosedAs(
        planFact: PlanFact,
        planFactStatus: PlanFactStatus,
        scheduleTime: Instant,
    ) {
        assertSoftly {
            planFact.planFactStatus shouldBe planFactStatus
            planFact.factStatusDatetime shouldBe null
            planFact.scheduleTime shouldBe scheduleTime
        }
    }
}
