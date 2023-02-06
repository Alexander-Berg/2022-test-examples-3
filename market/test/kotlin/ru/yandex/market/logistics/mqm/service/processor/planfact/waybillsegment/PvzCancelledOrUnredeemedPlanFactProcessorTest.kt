package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.logistics.mqm.configuration.properties.ReturnSegmentPlanFactProcessorProperties
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.processor.planfact.BaseSelfSufficientPlanFactTest
import ru.yandex.market.logistics.mqm.utils.createPvzOrder
import ru.yandex.market.logistics.mqm.utils.createWaybillSegmentWithCheckpoint
import ru.yandex.market.logistics.mqm.utils.getFirstSegmentByType
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint

open class PvzCancelledOrUnredeemedPlanFactProcessorTest : BaseSelfSufficientPlanFactTest() {

    private lateinit var processor: PvzCancelledOrUnredeemedPlanFactProcessor


    @BeforeEach
    fun setUp() {
        processor = PvzCancelledOrUnredeemedPlanFactProcessor(
            clock,
            planFactService,
            ReturnSegmentPlanFactProcessorProperties()
        )
    }

    @DisplayName("Проверка создания план-факта")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["RETURN_ARRIVED", "TRANSIT_STORAGE_PERIOD_EXPIRED"]
    )
    fun planFactCreationSuccess(openStatus: SegmentStatus) {
        val order = createPvzOrder()
        val history = writeWaybillSegmentCheckpoint(
            order.getFirstSegmentByType(SegmentType.PICKUP),
            status = openStatus,
            checkpointReceivedDatetime = FIXED_TIME
        )
        val context = mockSegmentStatusAddedContext(order, history)
        processor.waybillSegmentStatusAdded(context)
        val planFact = captureSavedPlanFact()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe context.checkpoint.waybillSegment!!.id
            planFact.expectedStatus shouldBe SegmentStatus.OUT.name
            planFact.expectedStatusDatetime shouldBe EXPECTED_FACT_TIME
            planFact.producerName shouldBe PvzCancelledOrUnredeemedPlanFactProcessor::class.simpleName
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_FACT_TIME
        }
    }

    @DisplayName("ПФ не создается, если сегмент не ПВЗ")
    @ParameterizedTest
    @EnumSource(
        value = SegmentType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["PICKUP"]
    )
    fun planFactNotCreateIfSegmentNotPvz(segmentType: SegmentType) {
        val segment = createWaybillSegmentWithCheckpoint(segmentType, SegmentStatus.RETURN_ARRIVED)
        val order = joinInOrder(listOf(segment))
        val context = mockSegmentStatusAddedContext(order, segment.waybillSegmentStatusHistory.last())
        processor.waybillSegmentStatusAdded(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("ПФ не создается, если есть закрывающие статусы")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["TRANSIT_STORAGE_PERIOD_EXTENDED", "OUT"]
    )
    fun planFactNotCreateIfContainsCloseStatuses(closeStatus: SegmentStatus) {
        val order = createPvzOrder()
        writeWaybillSegmentCheckpoint(
            order.getFirstSegmentByType(SegmentType.PICKUP),
            status = closeStatus,
            checkpointReceivedDatetime = FIXED_TIME
        )
        val history = writeWaybillSegmentCheckpoint(
            order.getFirstSegmentByType(SegmentType.PICKUP),
            status = SegmentStatus.TRANSIT_STORAGE_PERIOD_EXPIRED,
            checkpointReceivedDatetime = FIXED_TIME
        )
        val context = mockSegmentStatusAddedContext(order, history)
        processor.waybillSegmentStatusAdded(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Проставлять план-факт в IN_TIME, если факт успел вовремя")
    @Test
    fun setPlanFactInTime() {
        val order = createPvzOrder()
        val history = writeWaybillSegmentCheckpoint(
            order.getFirstSegmentByType(SegmentType.PICKUP),
            status = SegmentStatus.RETURNED,
            checkpointReceivedDatetime = EXPECTED_FACT_TIME.minusSeconds(1),
        )
        val existingPlanFact = createPlanFact(waybillSegment = order.getFirstSegmentByType(SegmentType.PICKUP))
        val context = mockSegmentStatusAddedContext(order, history, mutableListOf(existingPlanFact))
        processor.waybillSegmentStatusAdded(context)
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
    }

    @DisplayName("ПФ не создается, если есть факт")
    @Test
    fun planFactNotCreateIfFactExists() {
        val order = createPvzOrder()
        writeWaybillSegmentCheckpoint(
            order.getFirstSegmentByType(SegmentType.PICKUP),
            status = SegmentStatus.RETURN_ARRIVED,
            checkpointReceivedDatetime = EXPECTED_FACT_TIME,
        )
        val history = writeWaybillSegmentCheckpoint(
            order.getFirstSegmentByType(SegmentType.PICKUP),
            status = SegmentStatus.TRANSIT_STORAGE_PERIOD_EXPIRED,
            checkpointReceivedDatetime = EXPECTED_FACT_TIME,
        )
        val context = mockSegmentStatusAddedContext(order, history)
        processor.waybillSegmentStatusAdded(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Закрывать план-факт как NOT_ACTUAL, если факт опоздал")
    @Test
    fun setPlanFactNotActual() {
        val order = createPvzOrder()
        val history = writeWaybillSegmentCheckpoint(
            order.getFirstSegmentByType(SegmentType.PICKUP),
            status = SegmentStatus.RETURNED,
            checkpointReceivedDatetime = EXPECTED_FACT_TIME,
        )
        val existingPlanFact = createPlanFact(waybillSegment = order.getFirstSegmentByType(SegmentType.PICKUP))
        val context = mockSegmentStatusAddedContext(order, history, mutableListOf(existingPlanFact))
        processor.waybillSegmentStatusAdded(context)
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
    }

    @DisplayName("Проставлять план-факт в EXPIRED, если пришел один из закрывающих статусов заказа до плана")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["TRANSIT_STORAGE_PERIOD_EXTENDED", "OUT"]
    )
    fun setPlanFactExpiredIfCloseStatusCameOnTime(closeStatus: SegmentStatus) {
        val order = createPvzOrder()
        val history = writeWaybillSegmentCheckpoint(
            order.getFirstSegmentByType(SegmentType.PICKUP),
            status = closeStatus,
            checkpointReceivedDatetime = EXPECTED_FACT_TIME.minusSeconds(1),
        )
        val existingPlanFact = createPlanFact(waybillSegment = order.getFirstSegmentByType(SegmentType.PICKUP))
        val context = mockSegmentStatusAddedContext(order, history, mutableListOf(existingPlanFact))
        processor.waybillSegmentStatusAdded(context)
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
    }

    @DisplayName("Закрывать план-факт как NOT_ACTUAL, пришел закрывающий статус после плана")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["TRANSIT_STORAGE_PERIOD_EXTENDED", "OUT", "TRANSIT_TRANSMITTED_TO_RECIPIENT"]
    )
    fun closePlanFactAsNotActualOnCloseStatusAfterPlan(closeStatus: SegmentStatus) {
        val order = createPvzOrder()
        val history = writeWaybillSegmentCheckpoint(
            order.getFirstSegmentByType(SegmentType.PICKUP),
            status = closeStatus,
            checkpointReceivedDatetime = EXPECTED_FACT_TIME,
        )
        val existingPlanFact = createPlanFact(waybillSegment = order.getFirstSegmentByType(SegmentType.PICKUP))
        val context = mockSegmentStatusAddedContext(order, history, mutableListOf(existingPlanFact))
        processor.waybillSegmentStatusAdded(context)
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
    }

    private fun createPlanFact(
        expectedTime: Instant = EXPECTED_FACT_TIME,
        waybillSegment: WaybillSegment,
    ): PlanFact {
        return PlanFact(
            id = 102,
            entityId = waybillSegment.id,
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            planFactStatus = PlanFactStatus.CREATED,
            expectedStatusDatetime = expectedTime,
            expectedStatus = SegmentStatus.OUT.name,
            producerName = PvzCancelledOrUnredeemedPlanFactProcessor::class.simpleName,
        ).apply { entity = waybillSegment }
    }

    companion object {
        val FIXED_TIME: Instant = Instant.parse("2021-12-21T12:00:00.00Z")
        val EXPECTED_FACT_TIME: Instant = Instant.parse("2022-03-19T09:00:00Z")
    }
}
