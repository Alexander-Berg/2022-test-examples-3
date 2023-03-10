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
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.service.processor.planfact.BaseSelfSufficientPlanFactTest
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.createPvzOrder
import ru.yandex.market.logistics.mqm.utils.getLastDeliverySegment
import ru.yandex.market.logistics.mqm.utils.getMkSegment
import ru.yandex.market.logistics.mqm.utils.getNextSegment
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint

class MkLastMileIntakePlanFactProcessorTest: BaseSelfSufficientPlanFactTest() {

    private val settingsService = TestableSettingsService()

    lateinit var processor: MkLastMileIntakePlanFactProcessor

    @BeforeEach
    fun setUp() {
        processor = MkLastMileIntakePlanFactProcessor(clock, planFactService, settingsService)
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["BERU", "YANDEX_GO"]
    )
    @DisplayName("???????????????? ???????????????? ????????-??????????")
    fun planFactCreationSuccess(platformClient: PlatformClient) {
        val lomOrder = createPvzOrder(platformClient = platformClient)
        val mkSegment = lomOrder.getMkSegment()
        writeWaybillSegmentCheckpoint(
            segment = mkSegment,
            status = SegmentStatus.TRANSIT_PICKUP,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        processNewSegmentStatus(processor, lomOrder)

        val planFact = captureSavedPlanFact()
        verifyPlanFact(planFact, mkSegment)
    }

    @DisplayName("???? ???? ??????????????????, ???????? ?????? MARKET_COURIER ????????????????")
    @ParameterizedTest
    @EnumSource(
        value = PartnerSubtype::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["MARKET_COURIER"]
    )
    fun planFactNotCreateIfNextSegmentNotMk(partnerSubtype: PartnerSubtype) {
        val lomOrder = createPvzOrder()
        val formerMkSegment = lomOrder.getMkSegment().apply { this.partnerSubtype = partnerSubtype }
        writeWaybillSegmentCheckpoint(
            segment = formerMkSegment,
            status = SegmentStatus.TRANSIT_PICKUP,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        processNewSegmentStatus(processor, lomOrder)

        verify(planFactService, never()).save(any())
    }

    @DisplayName("???? ???? ??????????????????, ???????? ???????? ????????")
    @Test
    fun planFactNotCreateIfFactExists() {
        val lomOrder = createPvzOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.getMkSegment(),
            status = SegmentStatus.TRANSIT_PICKUP,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.getMkSegment().getNextSegment(),
            status = SegmentStatus.TRANSIT_PICKUP,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        processNewSegmentStatus(processor, lomOrder)

        verify(planFactService, never()).save(any())
    }


    @DisplayName("???? ???? ??????????????????, ???????? ???????? ???????? ???? ?????????????????????????? ????????????????")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["TRANSIT_TRANSMITTED_TO_RECIPIENT", "OUT", "RETURN_PREPARING_SENDER", "RETURNED", "CANCELLED"]
    )
    fun planFactNotCreateIfNextSegmentsWithCloseStatuses(segmentStatus: SegmentStatus) {
        val lomOrder = createPvzOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.getMkSegment(),
            status = SegmentStatus.TRANSIT_PICKUP,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.getMkSegment().getNextSegment(),
            status = segmentStatus,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        processNewSegmentStatus(processor, lomOrder)

        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("???????? ???????????????????? ????????-???????? ?? ???????????? ????????????, ???? ???? ???????????????????? ?????? OUTDATED")
    fun markOldPlanFactOutdated() {
        val lomOrder = createPvzOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.getMkSegment(),
            status = SegmentStatus.TRANSIT_PICKUP,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        val existingPlanFact = createPlanFact(
            waybillSegment = lomOrder.getMkSegment().getNextSegment(),
            expectedTime = EXPECTED_PLAN_TIME.minus(Duration.ofSeconds(1))
        )
        processNewSegmentStatus(processor, lomOrder, existingPlanFact)

        verify(planFactService).save(any())
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED
    }

    @DisplayName("?????????????????????? ????????-???????? ?? IN_TIME, ???????? ???????? ?????????? ??????????????")
    @Test
    fun setPlanFactInTime() {
        val lomOrder = createPvzOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.getMkSegment().getNextSegment(),
            status = SegmentStatus.TRANSIT_PICKUP,
            checkpointReceivedDatetime = EXPECTED_PLAN_TIME.minus(Duration.ofSeconds(1)),
        )
        val existingPlanFact = createPlanFact(waybillSegment = lomOrder.getMkSegment().getNextSegment())
        processNewSegmentStatus(processor, lomOrder, existingPlanFact)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
    }

    @DisplayName("?????????????????? ????????-???????? ?????? NOT_ACTUAL, ???????? ???????? ??????????????")
    @Test
    fun setPlanFactNotActual() {
        val lomOrder = createPvzOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.getMkSegment().getNextSegment(),
            status = SegmentStatus.TRANSIT_PICKUP,
            checkpointReceivedDatetime = EXPECTED_PLAN_TIME,
        )
        val existingPlanFact = createPlanFact(waybillSegment = lomOrder.getMkSegment().getNextSegment())
        processNewSegmentStatus(processor, lomOrder, existingPlanFact)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
    }


    @DisplayName("?????????????????????? ????????-???????? ?? EXPIRED, ???????? ???????????? ???????? ???? ?????????????????????? ???????????????? ???????????? ???? ??????????")
    @Test
    fun setPlanFactExpiredIfCloseStatusCameOnTime() {
        val lomOrder = createPvzOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.getMkSegment().getNextSegment(),
            status = SegmentStatus.OUT,
            checkpointReceivedDatetime = EXPECTED_PLAN_TIME.minus(Duration.ofSeconds(1)),
        )
        val existingPlanFact = createPlanFact(waybillSegment = lomOrder.getMkSegment().getNextSegment())
        processNewSegmentStatus(processor, lomOrder, existingPlanFact)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
    }

    @DisplayName("?????????????????? ????????-???????? ?????? NOT_ACTUAL, ???????????? ?????????????????????? ???????????? ?????????? ??????????")
    @Test
    fun closePlanFactAsNotActualOnCloseStatusAfterPlan() {
        val lomOrder = createPvzOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.getMkSegment().getNextSegment(),
            status = SegmentStatus.OUT,
            checkpointReceivedDatetime = EXPECTED_PLAN_TIME,
        )
        val existingPlanFact = createPlanFact(waybillSegment = lomOrder.getMkSegment().getNextSegment())
        processNewSegmentStatus(processor, lomOrder,  existingPlanFact)

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
        val lomOrder = createPvzOrder()
        val existingPlanFact = createPlanFact(waybillSegment = lomOrder.getMkSegment().getNextSegment())
        processOrderStatusChanged(processor, lomOrder, orderStatus, existingPlanFact)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
    }

    @DisplayName("???????????????????? ????????-???????? ???? ?????????????? ?????????????????? ???????? ?????????????????? ????????")
    @Test
    fun processPlanFactOnOrderLastMileChanged() {
        val lomOrder = createPvzOrder()
        val mkSegment = lomOrder.getMkSegment()
        writeWaybillSegmentCheckpoint(
            segment = mkSegment,
            status = SegmentStatus.TRANSIT_PICKUP,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        val existingPlanFact = createPlanFact(
            waybillSegment = lomOrder.getLastDeliverySegment(),
            expectedTime = CURRENT_TIME,
        )
        processOrderLastMileChanged(processor, lomOrder, existingPlanFact)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL

        val planFact = captureSavedPlanFact()
        verifyPlanFact(planFact, mkSegment)
    }

    private fun createPlanFact(
        expectedTime: Instant = EXPECTED_PLAN_TIME,
        waybillSegment: WaybillSegment,
    ): PlanFact {
        return PlanFact(
            id = 1,
            entityId = waybillSegment.id,
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            planFactStatus = PlanFactStatus.CREATED,
            expectedStatusDatetime = expectedTime,
            expectedStatus = SegmentStatus.TRANSIT_PICKUP.name,
            producerName = MkLastMileIntakePlanFactProcessor::class.simpleName,
        ).apply { entity = waybillSegment }
    }

    private fun verifyPlanFact(planFact: PlanFact, mkSegment: WaybillSegment) {
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe mkSegment.getNextSegment().id
            planFact.expectedStatus shouldBe SegmentStatus.TRANSIT_PICKUP.name
            planFact.expectedStatusDatetime shouldBe EXPECTED_PLAN_TIME
            planFact.producerName shouldBe MkLastMileIntakePlanFactProcessor::class.simpleName
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_PLAN_TIME
        }
    }

    companion object {
        private val EXPECTED_PLAN_TIME = CURRENT_TIME.plus(Duration.ofHours(1))
    }
}
