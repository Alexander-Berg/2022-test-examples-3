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
import ru.yandex.market.logistics.mqm.utils.createDropoffSegment
import ru.yandex.market.logistics.mqm.utils.createDropshipSegment
import ru.yandex.market.logistics.mqm.utils.createMkOrder
import ru.yandex.market.logistics.mqm.utils.getNextSegment
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint

class DropshipDropoffIntakePlanFactProcessorTest: BaseSelfSufficientPlanFactTest() {
    private val settingsService = TestableSettingsService()

    lateinit var processor: DropshipDropoffIntakePlanFactProcessor

    @BeforeEach
    fun setUp() {
        processor = DropshipDropoffIntakePlanFactProcessor(clock, planFactService, settingsService)
    }

    @Test
    @DisplayName("???????????????? ???????????????? ????????-??????????")
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

    @DisplayName("???? ???? ??????????????????, ???????? ???????????? ?????????????? ???? DROPSHIP")
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

    @DisplayName("???? ???? ??????????????????, ???????? ???????????? ?????????????? ???? DROPOFF")
    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["DELIVERY"]
    )
    fun planFactNotCreateIfSecondSegmentNotDropoff(partnerType: PartnerType) {
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

    @DisplayName("???? ???? ??????????????????, ???????? ???????? IN ?????? ?????????????????? ???? ???? DROPOFF ????????????????")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["IN", "TRANSIT_TRANSMITTED_TO_RECIPIENT", "OUT", "RETURN_PREPARING_SENDER", "RETURNED", "CANCELLED"]
    )
    fun planFactNotCreateIfDropoffHasIn(segmentStatus: SegmentStatus) {
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
    @DisplayName("???????? ???????????????????? ????????-???????? ?? ???????????? ????????????, ???? ???? ???????????????????? ?????? OUTDATED")
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

    @DisplayName("?????????????????????? ????????-???????? ?? IN_TIME, ???????? ???????? ?????????? ??????????????")
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

    @DisplayName("?????????????????? ????????-???????? ?????? NOT_ACTUAL, ???????? ???????? ??????????????")
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


    @DisplayName("?????????????????????? ????????-???????? ?? EXPIRED, ???????? ???????????? ???????? ???? ?????????????????????? ???????????????? ???????????? ???? ??????????")
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

    @DisplayName("?????????????????? ????????-???????? ?????? NOT_ACTUAL, ???????????? ?????????????????????? ???????????? ?????????? ??????????")
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

    @DisplayName("?????????????????? ????????-???????? ?????? NOT_ACTUAL, ???????? ???????????? ???????????? ?????????????? ?? ??????????????????")
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

    @DisplayName("?????????????????????? ???????? ????????, ???????? ?????? ???????????????? ???????????????? ?????? cor")
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

    @DisplayName("?????????????????????? ???????? ????????, ???????? ?????? ???????????????? ???????????????? ???? ???? mqm")
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

    @DisplayName("???? ?????????????????????????? ???????? ????????, ???????? ?????? ???????????????? ???????????????? ???? mqm")
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
            planFact.producerName shouldBe DropshipDropoffIntakePlanFactProcessor::class.simpleName
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_PLAN_TIME
        }
    }

    private fun createOrder(): LomOrder {
        return createMkOrder(firstSegment = createDropshipSegment(), secondSegment = createDropoffSegment())
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
            producerName = DropshipDropoffIntakePlanFactProcessor::class.simpleName,
        ).apply { entity = waybillSegment }
    }

    companion object {
        private val EXPECTED_PLAN_TIME = Instant.parse("2022-03-16T12:00:00.00Z")
    }
}
