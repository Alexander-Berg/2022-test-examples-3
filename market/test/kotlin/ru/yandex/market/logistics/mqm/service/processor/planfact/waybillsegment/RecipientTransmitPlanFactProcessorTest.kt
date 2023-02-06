package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
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
import ru.yandex.market.logistics.mqm.entity.lom.embedded.DeliveryInterval
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.utils.joinInOrder

@ExtendWith(MockitoExtension::class)
class RecipientTransmitPlanFactProcessorTest {
    @Mock
    private lateinit var planFactService: PlanFactService

    private val clock = TestableClock()

    lateinit var processor: RecipientTransmitPlanFactProcessor

    @BeforeEach
    fun setUp() {
        processor = RecipientTransmitPlanFactProcessor(clock, planFactService)
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Успешное создание план-факта c дедлайном, когда 48 чекпоинт пришел до ПДД")
    @Test
    fun createPlanFactIfCheckpointIsBeforeDeliveryDate() {
        val context = createWaybillSegmentStatusAddedContext()
        processor.waybillSegmentStatusAdded(context)
        verifyPlanFactCreated()
    }

    @DisplayName("Успешное создание план-факта с дедлайном, когда 48 чекпоинт пришел после ПДД")
    @Test
    fun createPlanFactIfCheckpointIsAfterDeliveryDate() {
        val context = createWaybillSegmentStatusAddedContext(
            checkpointTime = FIXED_TIME.plus(5, ChronoUnit.DAYS)
        )
        processor.waybillSegmentStatusAdded(context)
        verifyPlanFactCreated(Instant.parse("2021-12-26T20:59:59Z"))
    }

    @DisplayName("Не создавать план-факт если пришел не 48ой чекпоинт")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["TRANSIT_TRANSPORTATION_RECIPIENT"]
    )
    fun doNotCreatePlanFactUponWrongCheckpoint(segmentStatus: SegmentStatus) {
        val context = createWaybillSegmentStatusAddedContext(checkpoint = segmentStatus)
        processor.waybillSegmentStatusAdded(context)
        verifyNoMoreInteractions(planFactService)
    }

    @DisplayName("Не создавать план-факт если это Express или OnDemand заказ")
    @ParameterizedTest
    @EnumSource(
        value = WaybillSegmentTag::class,
        names = ["CALL_COURIER", "ON_DEMAND"]
    )
    fun doNotCreatePlanFactIfExpressOrOnDemandOrder(tag: WaybillSegmentTag) {
        val context = createWaybillSegmentStatusAddedContext()
        context.order.waybill[1].waybillSegmentTags = mutableSetOf(tag)
        processor.waybillSegmentStatusAdded(context)
        verifyNoMoreInteractions(planFactService)
    }

    @DisplayName("Не создавать план-факт если сегмент не последней мили")
    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["DELIVERY"]
    )
    fun doNotCreatePlanFactIfNotLastDeliverySegment(partnerType: PartnerType) {
        val context = createWaybillSegmentStatusAddedContext()
        context.order.waybill[1].partnerType = partnerType
        processor.waybillSegmentStatusAdded(context)
        verifyNoMoreInteractions(planFactService)
    }

    @DisplayName("Пометить существующий план-факт как OUTDATED и сохранить новый")
    @Test
    fun markOutdatedAndSaveNewPlanFact() {
        val oldPlanFact = createPlanFact()
        oldPlanFact.expectedStatusDatetime = DEFAULT_EXPECTED_TIME.minusSeconds(10)
        val context = createWaybillSegmentStatusAddedContext(
            planFacts = listOf(oldPlanFact)
        )
        processor.waybillSegmentStatusAdded(context)
        verifyPlanFactCreated()
        assertSoftly {
            oldPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED
        }
    }

    @DisplayName("Пометить план-факт IN_TIME")
    @Test
    fun markInTime() {
        val planFact = createPlanFact()
        val context = createWaybillSegmentStatusAddedContext(
            checkpoint = EXPECTED_STATUS,
            planFacts = listOf(planFact)
        )
        processor.waybillSegmentStatusAdded(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
        }
    }

    @DisplayName("Пометить план-факт NOT_ACTUAL")
    @Test
    fun markNotActual() {
        val planFact = createPlanFact()
        val context = createWaybillSegmentStatusAddedContext(
            checkpoint = EXPECTED_STATUS,
            checkpointTime = DEFAULT_EXPECTED_TIME.plusSeconds(10),
            planFacts = listOf(planFact)
        )
        processor.waybillSegmentStatusAdded(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    @DisplayName("Пометить план-факт EXPIRED по приходу финального статуса заказа до плана")
    @ParameterizedTest
    @EnumSource(
        OrderStatus::class,
        names = ["RETURNED", "LOST", "DELIVERED", "RETURNING"]
    )
    fun markExpiredOnCloseOrderStatus(orderStatus: OrderStatus) {
        val planFact = createPlanFact()
        val context = createLomOrderStatusChangedContext(
            orderStatus = orderStatus,
            planFacts = listOf(planFact)
        )
        processor.lomOrderStatusChanged(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
        }
    }

    @DisplayName("Пометить план-факт NOT_ACTUAL по приходу финального статуса заказа после плана")
    @ParameterizedTest
    @EnumSource(
        OrderStatus::class,
        names = ["RETURNED", "LOST", "DELIVERED", "RETURNING"]
    )
    fun markNotActualOnCloseOrderStatus(orderStatus: OrderStatus) {
        val planFact = createPlanFact()
        planFact.expectedStatusDatetime = FIXED_TIME.minusSeconds(10)
        val context = createLomOrderStatusChangedContext(
            orderStatus = orderStatus,
            planFacts = listOf(planFact)
        )
        processor.lomOrderStatusChanged(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    @DisplayName("Пометить план-факт EXPIRED по приходу следующего чекпоинта на этом сегменте до плана")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        names = ["OUT", "RETURN_ARRIVED", "RETURN_PREPARING_SENDER", "RETURNED"]
    )
    fun markExpiredOnCloseSegmentStatus(segmentStatus: SegmentStatus) {
        val planFact = createPlanFact()
        val context = createWaybillSegmentStatusAddedContext(
            checkpoint = segmentStatus,
            checkpointTime = DEFAULT_EXPECTED_TIME.minusSeconds(10),
            planFacts = listOf(planFact)
        )
        processor.waybillSegmentStatusAdded(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
        }
    }

    @DisplayName("Пометить план-факт NOT_ACTUAL по приходу следующего чекпоинта на этом сегменте после плана")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        names = ["OUT", "RETURN_ARRIVED", "RETURN_PREPARING_SENDER", "RETURNED"]
    )
    fun markNotActualOnCloseSegmentStatus(segmentStatus: SegmentStatus) {
        val planFact = createPlanFact()
        val context = createWaybillSegmentStatusAddedContext(
            checkpoint = segmentStatus,
            checkpointTime = DEFAULT_EXPECTED_TIME.plusSeconds(10),
            planFacts = listOf(planFact)
        )
        processor.waybillSegmentStatusAdded(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    private fun verifyPlanFactCreated(expectedTime: Instant = DEFAULT_EXPECTED_TIME) {
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.waybillSegmentType shouldBe null
            planFact.entityId shouldBe 2L
            planFact.expectedStatus shouldBe EXPECTED_STATUS.name
            planFact.expectedStatusDatetime shouldBe expectedTime
            planFact.producerName shouldBe processor.producerName()
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe expectedTime
        }
    }

    private fun createWaybillSegmentStatusAddedContext(
        checkpoint: SegmentStatus = SegmentStatus.TRANSIT_TRANSPORTATION_RECIPIENT,
        checkpointTime: Instant = FIXED_TIME,
        planFacts: List<PlanFact> = listOf()
    ): LomWaybillStatusAddedContext {
        val newCheckpoint = WaybillSegmentStatusHistory(status = checkpoint, date = checkpointTime)
        val previousSegment = WaybillSegment(id = 1)
        val lastMileSegment = WaybillSegment(
            id = 2,
            partnerId = PARTNER_ID,
            partnerType = PartnerType.DELIVERY,
            segmentType = SegmentType.COURIER
        ).apply {
            waybillSegmentStatusHistory = mutableSetOf(newCheckpoint)
            newCheckpoint.waybillSegment = this
            planFacts.forEach { pf -> pf.entity = this }
        }
        val order = joinInOrder(listOf(previousSegment, lastMileSegment))
            .apply {
                deliveryInterval = DEFAULT_INTERVAL
            }
        return LomWaybillStatusAddedContext(newCheckpoint, order, planFacts)
    }

    private fun createLomOrderStatusChangedContext(
        orderStatus: OrderStatus,
        planFacts: List<PlanFact> = listOf(),
    ) = LomOrderStatusChangedContext(LomOrder(), orderStatus, planFacts)

    private fun createPlanFact() = PlanFact(
        producerName = processor.producerName(),
        expectedStatus = EXPECTED_STATUS.name,
        planFactStatus = PlanFactStatus.CREATED,
        entityType = EntityType.LOM_WAYBILL_SEGMENT,
        entityId = 2L,
        expectedStatusDatetime = DEFAULT_EXPECTED_TIME
    )

    companion object {
        private const val PARTNER_ID = 12345L
        private val FIXED_TIME = Instant.parse("2021-12-21T12:00:00.00Z")
        private val DEFAULT_EXPECTED_TIME = Instant.parse("2021-12-25T20:59:59Z")
        private val DEFAULT_INTERVAL = DeliveryInterval(
            deliveryDateMax = LocalDate.of(2021, 12, 25),
        )
        private val EXPECTED_STATUS = SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT
    }
}
