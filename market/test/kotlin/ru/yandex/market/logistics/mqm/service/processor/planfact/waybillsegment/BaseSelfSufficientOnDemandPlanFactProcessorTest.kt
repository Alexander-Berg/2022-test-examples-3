package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mock
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import java.time.Instant

abstract class BaseSelfSufficientOnDemandPlanFactProcessorTest {

    @Mock
    protected lateinit var planFactService: PlanFactService

    // Создание ПФ.

    @DisplayName("Проверка создания план-факта")
    @Test
    fun planFactCreationSuccess() {
        val context = createPlanFactCreationSuccessLomWaybillStatusAddedContext()
        val processor = createProcessor()

        processor.waybillSegmentStatusAdded(context)

        val planFact = getSavedPlanFact(planFactService)
        val expectedPlanFact = createSuccessPlanFact()
        verifyPlanFact(planFact, expectedPlanFact)
    }

    // Проверки при существующих ПФ.

    @DisplayName("Если существует план-факт с таким-же планом, то новый план-факт не сохраняется")
    @Test
    fun doNotSaveNewIfExistsSame() {
        val context = createDoNotSaveNewIfExistsSameLomWaybillStatusAddedContext()
        val processor = createProcessor()
        val existingPlanFact = context.getPlanFactsFromProcessor(getProducerName()).first()

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.CREATED
    }

    @DisplayName("Если существует план-факт с другим планом, то он помечается как OUTDATED")
    @Test
    fun markExistsPlanFactOutdated() {
        val context = createMarkExistsPlanFactOutdatedLomWaybillStatusAddedContext()
        val processor = createProcessor()
        val existingPlanFact = context.getPlanFactsFromProcessor(getProducerName()).first()

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService).save(any())
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED
    }

    // Проверки при ЧП.

    @DisplayName("Проставлять план-факт в IN_TIME, если факт успел вовремя")
    @Test
    fun setPlanFactInTime() {
        val context = createSetPlanFactInTimeLomWaybillStatusAddedContext()
        val processor = createProcessor()
        val existingPlanFact = context.getPlanFactsFromProcessor(getProducerName()).first()

        processor.waybillSegmentStatusAdded(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
    }

    @DisplayName("Закрывать план-факт как NOT_ACTUAL, если факт опоздал")
    @Test
    fun setPlanFactNotActual() {
        val context = createSetPlanFactNotActualLomWaybillStatusAddedContext()
        val processor = createProcessor()
        val existingPlanFact = context.getPlanFactsFromProcessor(getProducerName()).first()

        processor.waybillSegmentStatusAdded(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
    }

    @DisplayName("Закрывать план-факт как NOT_ACTUAL, если факт пришел со следующим статусом")
    @Test
    fun closePlanFactIfNextSegmentStatus() {
        val context = createClosePlanFactIfNextSegmentStatusLomWaybillStatusAddedContext()
        val processor = createProcessor()
        val existingPlanFact = context.getPlanFactsFromProcessor(getProducerName()).first()

        processor.waybillSegmentStatusAdded(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
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

        checkPlanFactClosedAs(existingPlanFact, PlanFactStatus.EXPIRED, getFixedTime())
    }

    @DisplayName("Проставлять план-факт в NOT_ACTUAL, если опоздал один из закрывающих статусов заказа")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["RETURNED", "DELIVERED", "LOST", "RETURNING"]
    )
    fun setPlanFactNotActualIfCloseStatusCameLate(orderStatus: OrderStatus) {
        val context = createLomOrderStatusChangedContext(orderStatus, getFixedTime().minusSeconds(1))
        val processor = createProcessor()
        val existingPlanFact = context.getPlanFactsFromProcessor(getProducerName()).first()

        processor.lomOrderStatusChanged(context)

        checkPlanFactClosedAs(existingPlanFact, PlanFactStatus.NOT_ACTUAL, getFixedTime())
    }

    protected abstract fun createProcessor(): BaseSelfSufficientOnDemandPlanFactProcessor

    protected abstract fun getProducerName(): String

    protected abstract fun getFixedTime(): Instant

    protected abstract fun getExpectedTime(): Instant

    protected abstract fun createSuccessPlanFact(): PlanFact

    protected abstract fun createPlanFactCreationSuccessLomWaybillStatusAddedContext(): LomWaybillStatusAddedContext

    protected abstract fun createDoNotSaveNewIfExistsSameLomWaybillStatusAddedContext(): LomWaybillStatusAddedContext

    protected abstract fun createMarkExistsPlanFactOutdatedLomWaybillStatusAddedContext(): LomWaybillStatusAddedContext

    protected abstract fun createSetPlanFactInTimeLomWaybillStatusAddedContext(): LomWaybillStatusAddedContext

    protected abstract fun createSetPlanFactNotActualLomWaybillStatusAddedContext(): LomWaybillStatusAddedContext

    protected abstract fun createClosePlanFactIfNextSegmentStatusLomWaybillStatusAddedContext(): LomWaybillStatusAddedContext

    protected abstract fun createLomOrderStatusChangedContext(
        orderStatus: OrderStatus,
        expectedTime: Instant,
    ): LomOrderStatusChangedContext

    protected fun createSegmentStatusAddedContext(
        lomOrder: LomOrder,
        newCheckpoint: WaybillSegmentStatusHistory,
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
    ) = LomWaybillStatusAddedContext(newCheckpoint, lomOrder, existingPlanFacts)

    protected fun createNewCheckpoint(
        newSegmentStatus: SegmentStatus,
        newCheckpointTime: Instant,
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
