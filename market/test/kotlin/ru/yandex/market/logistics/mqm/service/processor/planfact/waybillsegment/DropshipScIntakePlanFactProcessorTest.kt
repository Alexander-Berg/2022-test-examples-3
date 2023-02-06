package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.ChangeOrderRequest
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.ChangeOrderRequestReason
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.service.processor.planfact.BaseSelfSufficientPlanFactTest
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.createDropshipSegment
import ru.yandex.market.logistics.mqm.utils.createMkOrder
import ru.yandex.market.logistics.mqm.utils.getNextSegment
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint

internal class DropshipScIntakePlanFactProcessorTest: BaseSelfSufficientPlanFactTest() {
    private val settingsService = TestableSettingsService()

    lateinit var processor: DropshipScIntakePlanFactProcessor

    @BeforeEach
    fun setUp() {
        processor = DropshipScIntakePlanFactProcessor(clock, planFactService, settingsService)
    }

    @Test
    @DisplayName("Проверка создания план-факта")
    fun planFactCreationSuccess() {
        val lomOrder = createOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.waybill.first(),
            status = SegmentStatus.PENDING,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        processNewSegmentStatus(processor, lomOrder)

        val planFact = captureSavedPlanFact()
        verifyPlanFact(planFact, lomOrder)
    }

    @DisplayName("ПФ не создается, если первый сегмент не DROPSHIP")
    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["DROPSHIP"]
    )
    fun planFactNotCreateIfFirstSegmentNotDropship(partnerType: PartnerType) {
        val lomOrder = createOrder()
        lomOrder.waybill.first().partnerType = partnerType
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.waybill.first(),
            status = SegmentStatus.PENDING,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        processNewSegmentStatus(processor, lomOrder)

        verify(planFactService, never()).save(any())
    }

    @DisplayName("ПФ не создается, если второй сегмент не SORTING_CENTER")
    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["SORTING_CENTER"]
    )
    fun planFactNotCreateIfSecondSegmentNotDelivery(partnerType: PartnerType) {
        val lomOrder = createOrder()
        lomOrder.waybill.first().getNextSegment().partnerType = partnerType
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.waybill.first(),
            status = SegmentStatus.PENDING,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        processNewSegmentStatus(processor, lomOrder)

        verify(planFactService, never()).save(any())
    }

    @DisplayName("ПФ не создается, если есть IN или следующие чп на втором сегменте")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["IN", "TRANSIT_TRANSMITTED_TO_RECIPIENT", "OUT", "RETURN_PREPARING_SENDER", "RETURNED", "CANCELLED"]
    )
    fun planFactNotCreateIfSecondSegmentHasIn(segmentStatus: SegmentStatus) {
        val lomOrder = createOrder()
        val pendingCp = writeWaybillSegmentCheckpoint(
            segment = lomOrder.waybill.first(),
            status = SegmentStatus.PENDING,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.waybill.first().getNextSegment(),
            status = segmentStatus,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        processNewSegmentStatus(processor = processor, order = lomOrder, checkpoint = pendingCp)

        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("Если существует план-факт с другим планом, то он помечается как OUTDATED")
    fun markOldPlanFactOutdated() {
        val lomOrder = createOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.waybill.first(),
            status = SegmentStatus.PENDING,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        val existingPlanFact = createPlanFact(
            waybillSegment = lomOrder.waybill.first().getNextSegment(),
            expectedTime = EXPECTED_PLAN_TIME.minus(Duration.ofSeconds(1))
        )
        processNewSegmentStatus(processor, lomOrder, existingPlanFact)

        verify(planFactService).save(any())
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED
    }

    @DisplayName("Проставлять план-факт в IN_TIME, если факт успел вовремя")
    @Test
    fun setPlanFactInTime() {
        val lomOrder = createOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.waybill.first().getNextSegment(),
            status = SegmentStatus.IN,
            checkpointReceivedDatetime = EXPECTED_PLAN_TIME.minus(Duration.ofSeconds(1)),
        )
        val existingPlanFact = createPlanFact(waybillSegment = lomOrder.waybill.first().getNextSegment())
        processNewSegmentStatus(processor, lomOrder, existingPlanFact)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
    }

    @DisplayName("Закрывать план-факт как NOT_ACTUAL, если факт опоздал")
    @Test
    fun setPlanFactNotActual() {
        val lomOrder = createOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.waybill.first().getNextSegment(),
            status = SegmentStatus.IN,
            checkpointReceivedDatetime = EXPECTED_PLAN_TIME,
        )
        val existingPlanFact = createPlanFact(waybillSegment = lomOrder.waybill.first().getNextSegment())
        processNewSegmentStatus(processor, lomOrder, existingPlanFact)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
    }


    @DisplayName("Проставлять план-факт в EXPIRED, если пришел один из закрывающих статусов заказа до плана")
    @Test
    fun setPlanFactExpiredIfCloseStatusCameOnTime() {
        val lomOrder = createOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.waybill.first().getNextSegment(),
            status = SegmentStatus.OUT,
            checkpointReceivedDatetime = EXPECTED_PLAN_TIME.minus(Duration.ofSeconds(1)),
        )
        val existingPlanFact = createPlanFact(waybillSegment = lomOrder.waybill.first().getNextSegment())
        processNewSegmentStatus(processor, lomOrder, existingPlanFact)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
    }

    @DisplayName("Закрывать план-факт как NOT_ACTUAL, пришел закрывающий статус после плана")
    @Test
    fun closePlanFactAsNotActualOnCloseStatusAfterPlan() {
        val lomOrder = createOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.waybill.first().getNextSegment(),
            status = SegmentStatus.OUT,
            checkpointReceivedDatetime = EXPECTED_PLAN_TIME,
        )
        val existingPlanFact = createPlanFact(waybillSegment = lomOrder.waybill.first().getNextSegment())
        processNewSegmentStatus(processor, lomOrder, existingPlanFact)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
    }

    @DisplayName("Закрывать план-факт как NOT_ACTUAL, если статус заказа перешел в финальный")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["RETURNED", "DELIVERED", "LOST", "RETURNING"]
    )
    fun closePlanFactIfFinalOrderStatus(orderStatus: OrderStatus) {
        val lomOrder = createOrder()
        val existingPlanFact = createPlanFact(waybillSegment = lomOrder.waybill.first().getNextSegment())
        processOrderStatusChanged(processor, lomOrder, orderStatus, existingPlanFact)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
    }

    @DisplayName("Пересчитать план факт, если был пересчет маршрута без cor")
    @Test
    fun recalculatePlanFactOnRouteUpdateWithoutCor() {
        val lomOrder = createOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.waybill.first(),
            status = SegmentStatus.PENDING,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        val existingPlanFact = createPlanFact(
            waybillSegment = lomOrder.waybill.first().getNextSegment(),
            expectedTime = CURRENT_TIME,
        )
        processCombinatorRouteWasUpdated(processor, lomOrder, null, existingPlanFact)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED

        val planFact = captureSavedPlanFact()
        verifyPlanFact(planFact, lomOrder)
    }

    @DisplayName("Пересчитать план факт, если был пересчет маршрута не от mqm")
    @Test
    fun recalculatePlanFactOnRouteUpdateNotFromMqm() {
        val lomOrder = createOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.waybill.first(),
            status = SegmentStatus.PENDING,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        val existingPlanFact = createPlanFact(
            waybillSegment = lomOrder.waybill.first().getNextSegment(),
            expectedTime = CURRENT_TIME,
        )
        processCombinatorRouteWasUpdated(processor, lomOrder, createCor(), existingPlanFact)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED

        val planFact = captureSavedPlanFact()
        verifyPlanFact(planFact, lomOrder)
    }

    @DisplayName("Не пересчитывать план факт, если был пересчет маршрута от mqm")
    @ParameterizedTest
    @EnumSource(
        value = ChangeOrderRequestReason::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["DELIVERY_DATE_UPDATED_BY_ROUTE_RECALCULATION", "PRE_DELIVERY_ROUTE_RECALCULATION"]
    )
    fun doNotRecalculatePlanFactOnRouteUpdateFromMqm(reason: ChangeOrderRequestReason) {
        val lomOrder = createOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.waybill.first(),
            status = SegmentStatus.PENDING,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        val existingPlanFact = createPlanFact(
            waybillSegment = lomOrder.waybill.first().getNextSegment(),
            expectedTime = CURRENT_TIME,
        )
        processCombinatorRouteWasUpdated(processor, lomOrder, createCor(reason), existingPlanFact)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.CREATED
        verify(planFactService, never()).save(any())
    }

    private fun createCor(
        reason: ChangeOrderRequestReason = ChangeOrderRequestReason.PROCESSING_DELAYED_BY_PARTNER
    ): ChangeOrderRequest {
        return ChangeOrderRequest(reason = reason)
    }

    private fun verifyPlanFact(
        planFact: PlanFact,
        lomOrder: LomOrder
    ) {
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe lomOrder.waybill.first().getNextSegment().id
            planFact.expectedStatus shouldBe SegmentStatus.IN.name
            planFact.expectedStatusDatetime shouldBe EXPECTED_PLAN_TIME
            planFact.producerName shouldBe DropshipScIntakePlanFactProcessor::class.simpleName
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_PLAN_TIME
        }
    }

    private fun createOrder(): LomOrder {
        return createMkOrder(firstSegment = createDropshipSegment())
    }

    private fun createPlanFact(
        waybillSegment: WaybillSegment,
        expectedTime: Instant = EXPECTED_PLAN_TIME,
    ): PlanFact {
        return PlanFact(
            id = 1,
            entityId = waybillSegment.id,
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            planFactStatus = PlanFactStatus.CREATED,
            expectedStatusDatetime = expectedTime,
            expectedStatus = SegmentStatus.IN.name,
            producerName = DropshipScIntakePlanFactProcessor::class.simpleName,
        ).apply { entity = waybillSegment }
    }

    companion object {
        private val EXPECTED_PLAN_TIME = Instant.parse("2022-03-16T21:00:00.00Z")
    }
}

