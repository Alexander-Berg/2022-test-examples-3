package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
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
import org.mockito.Mock
import org.mockito.Mockito.lenient
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.LomOrderCombinatorRoute
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderTag
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PointType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.ServiceCodeName
import ru.yandex.market.logistics.mqm.service.CombinatorRouteService
import ru.yandex.market.logistics.mqm.service.processor.planfact.BaseSelfSufficientPlanFactTest
import ru.yandex.market.logistics.mqm.utils.TEST_ORDER_ID
import ru.yandex.market.logistics.mqm.utils.createFFSegment
import ru.yandex.market.logistics.mqm.utils.createMkOrder
import ru.yandex.market.logistics.mqm.utils.createMkSegment
import ru.yandex.market.logistics.mqm.utils.createScMkSegment
import ru.yandex.market.logistics.mqm.utils.createScSegment
import ru.yandex.market.logistics.mqm.utils.getMkSegment
import ru.yandex.market.logistics.mqm.utils.getPreviousSegment
import ru.yandex.market.logistics.mqm.utils.overrideOrderWaybill
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint

open class ScScMkIntakeManualRddPlanFactProcessorTest: BaseSelfSufficientPlanFactTest() {

    private lateinit var processor: ScScMkIntakeManualRddPlanFactProcessor

    @Mock
    private lateinit var combinatorRouteService: CombinatorRouteService

    @BeforeEach
    fun setUp() {
        lenient().`when`(combinatorRouteService.getByLomOrderId(eq(TEST_ORDER_ID)))
            .thenReturn(createRoute())

        processor = ScScMkIntakeManualRddPlanFactProcessor(
            clock,
            planFactService,
            combinatorRouteService
        )
    }

    @Test
    @DisplayName("Проверка создания план-факта")
    fun planFactCreationSuccess() {
        val lomOrder = createOrder()
        val scMkSegment = lomOrder.getMkSegment().getPreviousSegment()
        writeWaybillSegmentCheckpoint(
            segment = scMkSegment.getPreviousSegment(),
            status = SegmentStatus.OUT,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        processNewSegmentStatus(processor, lomOrder)

        val planFact = captureSavedPlanFact()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe scMkSegment.id
            planFact.expectedStatus shouldBe SegmentStatus.IN.name
            planFact.expectedStatusDatetime shouldBe EXPECTED_PLAN_TIME
            planFact.producerName shouldBe ScScMkIntakeManualRddPlanFactProcessor::class.simpleName
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_SCHEDULE_TIME
        }
    }

    @Test
    @DisplayName("Проверка создания план-факта при отгрузке с ФФ до СЦ МК")
    fun planFactCreationFfSuccess() {
        val lomOrder = createOrder()
        overrideOrderWaybill(lomOrder, listOf(createFFSegment(), createScMkSegment(), createMkSegment()))
        val scMkSegment = lomOrder.getMkSegment().getPreviousSegment()
        writeWaybillSegmentCheckpoint(
            segment = scMkSegment.getPreviousSegment(),
            status = SegmentStatus.OUT,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        processNewSegmentStatus(processor, lomOrder)

        val planFact = captureSavedPlanFact()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe scMkSegment.id
            planFact.expectedStatus shouldBe SegmentStatus.IN.name
            planFact.expectedStatusDatetime shouldBe EXPECTED_PLAN_TIME
            planFact.producerName shouldBe ScScMkIntakeManualRddPlanFactProcessor::class.simpleName
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_SCHEDULE_TIME
        }
    }


    @Test
    @DisplayName("Проверка обновления план-факта, если изменился route")
    fun planFactUpdatesIfRouteChanged() {
        val lomOrder = createOrder()
        val scMkSegment = lomOrder.getMkSegment().getPreviousSegment()
        writeWaybillSegmentCheckpoint(
            segment = scMkSegment.getPreviousSegment(),
            status = SegmentStatus.OUT,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        val existingPlanFact = createPlanFact(
            waybillSegment = scMkSegment,
            expectedTime = EXPECTED_PLAN_TIME.minus(Duration.ofSeconds(1))
        )

        val context = mockLomOrderCombinatorRouteWasUpdatedContext(lomOrder, mutableListOf(existingPlanFact))

        processor.combinatorRouteWasUpdated(context)

        val planFact = captureSavedPlanFact()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe scMkSegment.id
            planFact.expectedStatus shouldBe SegmentStatus.IN.name
            planFact.expectedStatusDatetime shouldBe EXPECTED_PLAN_TIME
            planFact.producerName shouldBe ScScMkIntakeManualRddPlanFactProcessor::class.simpleName
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_SCHEDULE_TIME
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED
        }
    }

    @DisplayName("ПФ не создается, если перед сц мк не SORTING_CENTER")
    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["SORTING_CENTER"]
    )
    fun planFactNotCreateIfSegmentNotSc(partnerType: PartnerType) {
        val segmentBeforeScMk = createScSegment().apply { this.partnerType = partnerType }
        val lomOrder = createMkOrder(secondSegment = segmentBeforeScMk)
        writeWaybillSegmentCheckpoint(
            segment = segmentBeforeScMk,
            status = SegmentStatus.OUT,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        processNewSegmentStatus(processor, lomOrder)
        val savedPlanFact = captureSavedPlanFact()
        savedPlanFact.producerName shouldBe ScScMkIntakeManualRddPlanFactProcessor::class.simpleName
    }

    @DisplayName("ПФ не создается, если нет MARKET_COURIER сегмента")
    @ParameterizedTest
    @EnumSource(
        value = PartnerSubtype::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["MARKET_COURIER"]
    )
    fun planFactNotCreateIfNextSegmentNotMk(partnerSubtype: PartnerSubtype) {
        val lastSegment = createMkSegment().apply { this.partnerSubtype = partnerSubtype }
        val lomOrder = createMkOrder(fourthSegment = lastSegment)
        val scMkSegment = lomOrder.waybill.last { it.partnerSubtype === PartnerSubtype.MARKET_COURIER_SORTING_CENTER }
        writeWaybillSegmentCheckpoint(
            segment = scMkSegment.getPreviousSegment(),
            status = SegmentStatus.OUT,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        processNewSegmentStatus(processor, lomOrder)

        verify(planFactService, never()).save(any())
    }


    @Test
    @DisplayName("ПФ не создается, если есть факт")
    fun planFactNotCreateIfFactExists() {
        val lomOrder = createOrder()
        val scMkSegment = lomOrder.getMkSegment().getPreviousSegment()
        writeWaybillSegmentCheckpoint(
            segment = scMkSegment,
            status = SegmentStatus.IN,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        val eventCheckpoint = writeWaybillSegmentCheckpoint(
            segment = scMkSegment.getPreviousSegment(),
            status = SegmentStatus.OUT,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        processNewSegmentStatus(processor, lomOrder, checkpoint = eventCheckpoint)

        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("ПФ не создается, если есть один из закрывающихся статусов")
    fun planFactNotCreateIfNextSegmentsWithCloseStatuses() {
        val lomOrder = createOrder()
        val scMkSegment = lomOrder.getMkSegment().getPreviousSegment()
        writeWaybillSegmentCheckpoint(
            segment = scMkSegment,
            status = SegmentStatus.OUT,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        val eventCheckpoint = writeWaybillSegmentCheckpoint(
            segment = scMkSegment.getPreviousSegment(),
            status = SegmentStatus.OUT,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        processNewSegmentStatus(processor, lomOrder, checkpoint = eventCheckpoint)

        verify(planFactService, never()).save(any())
    }

    @DisplayName("ПФ не создается, если план в прошлом")
    @Test
    fun planFactNotCreateIfPlanInPast() {
        clock.setFixed(EXPECTED_PLAN_TIME.plusSeconds(1), DateTimeUtils.MOSCOW_ZONE)
        val lomOrder = createOrder()
        val scMkSegment = lomOrder.getMkSegment().getPreviousSegment()
        writeWaybillSegmentCheckpoint(
            segment = scMkSegment.getPreviousSegment(),
            status = SegmentStatus.OUT,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        processNewSegmentStatus(processor, lomOrder)

        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("Если существует план-факт с другим планом, то он помечается как OUTDATED")
    fun markOldPlanFactOutdated() {
        val lomOrder = createOrder()
        val scMkSegment = lomOrder.getMkSegment().getPreviousSegment()
        writeWaybillSegmentCheckpoint(
            segment = scMkSegment.getPreviousSegment(),
            status = SegmentStatus.OUT,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        val existingPlanFact = createPlanFact(
            waybillSegment = scMkSegment,
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
        val scMkSegment = lomOrder.getMkSegment().getPreviousSegment()
        writeWaybillSegmentCheckpoint(
            segment = scMkSegment,
            status = SegmentStatus.IN,
            checkpointReceivedDatetime = EXPECTED_PLAN_TIME.minus(Duration.ofSeconds(1)),
        )
        val existingPlanFact = createPlanFact(waybillSegment = scMkSegment)
        processNewSegmentStatus(processor, lomOrder, existingPlanFact)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
    }

    @DisplayName("Закрывать план-факт как NOT_ACTUAL, если факт опоздал")
    @Test
    fun setPlanFactNotActual() {
        val lomOrder = createOrder()
        val scMkSegment = lomOrder.getMkSegment().getPreviousSegment()
        writeWaybillSegmentCheckpoint(
            segment = scMkSegment,
            status = SegmentStatus.IN,
            checkpointReceivedDatetime = EXPECTED_PLAN_TIME.plus(Duration.ofSeconds(1)),
        )
        val existingPlanFact = createPlanFact(waybillSegment = scMkSegment)
        processNewSegmentStatus(processor, lomOrder, existingPlanFact)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
    }


    @DisplayName("Проставлять план-факт в EXPIRED, если пришел один из закрывающих статусов заказа до плана")
    @Test
    fun setPlanFactExpiredIfCloseStatusCameOnTime() {
        val lomOrder = createOrder()
        val scMkSegment = lomOrder.getMkSegment().getPreviousSegment()
        writeWaybillSegmentCheckpoint(
            segment = scMkSegment,
            status = SegmentStatus.OUT,
            checkpointReceivedDatetime = EXPECTED_PLAN_TIME.minus(Duration.ofSeconds(1)),
        )
        val existingPlanFact = createPlanFact(waybillSegment = scMkSegment)
        processNewSegmentStatus(processor, lomOrder, existingPlanFact)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
    }

    @DisplayName("Закрывать план-факт как NOT_ACTUAL, пришел закрывающий статус после плана")
    @Test
    fun closePlanFactAsNotActualOnCloseStatusAfterPlan() {
        val lomOrder = createOrder()
        val scMkSegment = lomOrder.getMkSegment().getPreviousSegment()
        writeWaybillSegmentCheckpoint(
            segment = scMkSegment,
            status = SegmentStatus.OUT,
            checkpointReceivedDatetime = EXPECTED_PLAN_TIME.plus(Duration.ofSeconds(1)),
        )
        val existingPlanFact = createPlanFact(waybillSegment = scMkSegment)
        processNewSegmentStatus(processor, lomOrder, existingPlanFact)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
    }

    private fun createOrder(hasTag: Boolean = true): LomOrder {
        val order = createMkOrder()
        if (hasTag) {
            order.orderTags = setOf(OrderTag.DELAYED_RDD_NOTIFICATION)
        }
        return order
    }

    private fun createPlanFact(
        expectedTime: Instant = EXPECTED_PLAN_TIME,
        waybillSegment: WaybillSegment,
    ): PlanFact {
        return PlanFact(
            id = 102,
            entityId = waybillSegment.id,
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            planFactStatus = PlanFactStatus.CREATED,
            expectedStatusDatetime = expectedTime,
            expectedStatus = SegmentStatus.IN.name,
            producerName = ScScMkIntakeManualRddPlanFactProcessor::class.simpleName,
        ).apply { entity = waybillSegment }
    }

    private fun createRoute(): LomOrderCombinatorRoute? {
        val shipmentService = LomOrderCombinatorRoute.DeliveryService(
            code = ServiceCodeName.SHIPMENT,
            tzOffset = 18000,
            startTime = LomOrderCombinatorRoute.Timestamp(1647691200),
            workingSchedule = listOf(
                LomOrderCombinatorRoute.ScheduleDay(
                    daysOfWeek = listOf(6),
                    timeWindows = listOf(
                        LomOrderCombinatorRoute.TimeWindow(0, 43200) //0-12 часов
                    )
                )
            )
        )
        return LomOrderCombinatorRoute(
            route = LomOrderCombinatorRoute.DeliveryRoute(
                points = listOf(
                    LomOrderCombinatorRoute.Point(
                        segmentId = 1041,
                        segmentType = PointType.MOVEMENT,
                        services = listOf(shipmentService)
                    )
                )
            )
        )
    }

    companion object {
        val EXPECTED_SCHEDULE_TIME: Instant = Instant.parse("2022-03-19T03:31:00.00Z")
        val EXPECTED_PLAN_TIME: Instant = Instant.parse("2022-03-19T07:00:00.00Z")
    }
}
