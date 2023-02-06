package ru.yandex.market.logistics.mqm.service.event.processors

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.configuration.properties.ExpressAutomaticReturnProcessorProperties
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(MockitoExtension::class)
class ExpressAutomaticReturnProcessorTest : AbstractTest() {

    @Mock
    private lateinit var planFactService: PlanFactService

    lateinit var processor: ExpressAutomaticReturnProcessor

    private val clock = TestableClock()

    @BeforeEach
    fun setup() {
        processor = ExpressAutomaticReturnProcessor(
            mockProperties(),
            planFactService,
            clock
        )
    }

    @Test
    @DisplayName("Проверка создания план-факта")
    fun createPlanFact() {
        val testCheckpoint = mockCheckpoint(
                OrderDeliveryCheckpointStatus.RETURN_PREPARING,
                emptyList()
        )
        val context = createCheckpointContext(testCheckpoint)
        val planFactCaptor = argumentCaptor<List<PlanFact>>()

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService).save(planFactCaptor.capture())
        val planFact = planFactCaptor.firstValue.single()

        assertSoftly {
            planFact.entityId shouldBe TEST_TAXI_SEGMENT_ID
            planFact.expectedStatus shouldBe SegmentStatus.UNKNOWN.name
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.expectedStatusDatetime shouldBe EXPECTED_STATUS_DATETIME
            planFact.scheduleTime shouldBe EXPECTED_STATUS_DATETIME

            verifyNoMoreInteractions(planFactService)
        }
    }

    @Test
    @DisplayName("Проверка закрытия план-факта")
    fun closePlanFact() {
        clock.setFixed(Instant.parse("2022-02-14T15:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
        val planFact = mockPlanFact()
        val context = createOrderStatusContext(orderStatus = OrderStatus.RETURNED, planFacts = listOf(planFact))

        processor.lomOrderStatusChanged(context)

        assertSoftly {
            planFact.entityId shouldBe TEST_TAXI_SEGMENT_ID
            planFact.expectedStatus shouldBe SegmentStatus.UNKNOWN.name
            planFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
            planFact.processingStatus shouldBe ProcessingStatus.PROCESSED
            planFact.expectedStatusDatetime shouldBe EXPECTED_STATUS_DATETIME
            planFact.scheduleTime shouldBe null

            verifyNoMoreInteractions(planFactService)
        }
    }

    @Test
    @DisplayName("Проверка закрытия план-факта после даты наступления факта")
    fun closePlanFactAfterFactDate() {
        clock.setFixed(Instant.parse("2022-02-14T19:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
        val planFact = mockPlanFact()
        val context = createOrderStatusContext(orderStatus = OrderStatus.LOST, planFacts = listOf(planFact))

        processor.lomOrderStatusChanged(context)

        assertSoftly {
            planFact.entityId shouldBe TEST_TAXI_SEGMENT_ID
            planFact.expectedStatus shouldBe SegmentStatus.UNKNOWN.name
            planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.expectedStatusDatetime shouldBe EXPECTED_STATUS_DATETIME
            planFact.scheduleTime shouldBe clock.instant()

            verifyNoMoreInteractions(planFactService)
        }
    }

    @Test
    @DisplayName("Проверка, что процессор ничего не делает, если он выключен")
    fun processDoNothingIfDisabled() {
        val disabledProcessor = ExpressAutomaticReturnProcessor(
            mockProperties(enabled = false),
            planFactService,
            clock
        )
        val testCheckpoint = mockCheckpoint()
        val context = createCheckpointContext(testCheckpoint)

        disabledProcessor.waybillSegmentStatusAdded(context)

        verifyNoMoreInteractions(planFactService)
    }

    @Test
    @DisplayName("Проверка, что процессор не применяется, если не Express")
    fun processDoNothingIfNotExpress() {
        val testCheckpoint = mockCheckpoint(isExpress = false)
        val context = createCheckpointContext(testCheckpoint)
        processor.waybillSegmentStatusAdded(context)
        verifyNoMoreInteractions(planFactService)
    }

    @Test
    @DisplayName("Проверка, что процессор не применяется, если обрабатывается неправильные чекпоинт")
    fun processDoNothingIfWrongCheckpoint() {
        val testCheckpoint = mockCheckpoint(eventStatus = OrderDeliveryCheckpointStatus.CANCELED)
        val context = createCheckpointContext(testCheckpoint)
        processor.waybillSegmentStatusAdded(context)
        verifyNoMoreInteractions(planFactService)
    }

    private fun mockPlanFact(): PlanFact {
        return PlanFact(
                id = 1,
                entityId = TEST_TAXI_SEGMENT_ID,
                expectedStatus = SegmentStatus.UNKNOWN.name,
                producerName = ExpressAutomaticReturnProcessor::class.simpleName,
                planFactStatus = PlanFactStatus.CREATED,
                expectedStatusDatetime = EXPECTED_STATUS_DATETIME,
        )
    }

    private fun mockCheckpoint(
        eventStatus: OrderDeliveryCheckpointStatus = OrderDeliveryCheckpointStatus.RETURN_ARRIVED_DELIVERY,
        history: List<WaybillSegmentStatusHistory> = listOf(
            TEST_CHECKPOINT_60,
            TEST_CHECKPOINT_70,
            TEST_CHECKPOINT_80,
            TEST_CHECKPOINT_WITHOUT_STATUS,
        ),
        isExpress: Boolean = true,
        orderStatus: OrderStatus = OrderStatus.RETURNING,
        checkpointDate: Instant = TEST_CHECKPOINT_60_TIME,
    ): WaybillSegmentStatusHistory {
        val dropshipSegment = WaybillSegment(partnerName = TEST_PARTNER_NAME)
        val taxiSegment = WaybillSegment(
            id = TEST_TAXI_SEGMENT_ID,
            segmentType = SegmentType.COURIER,
            waybillSegmentTags = if (isExpress) mutableSetOf(WaybillSegmentTag.CALL_COURIER) else mutableSetOf()
        ).apply { waybillSegmentStatusHistory.addAll(history) }
        joinInOrder(listOf(dropshipSegment, taxiSegment)).apply {
            id = TEST_ORDER_ID
            barcode = TEST_BARCODE
            status = orderStatus
        }

        return WaybillSegmentStatusHistory(
            trackerStatus = eventStatus.name,
            date = checkpointDate
        ).apply { waybillSegment = taxiSegment }
    }

    private fun mockProperties(
        enabled: Boolean = true,
        intervalFrom: Duration = Duration.ofDays(1),
        intervalTo: Duration = Duration.ofDays(1),
    ) = ExpressAutomaticReturnProcessorProperties(
        enabled = enabled,
        queueName = TEST_QUEUE_NAME,
        component = TEST_COMPONENT,
        intervalFrom = intervalFrom,
        intervalTo = intervalTo,
    )

    private fun createCheckpointContext(
            testCheckpoint: WaybillSegmentStatusHistory,
            planFacts: List<PlanFact> = listOf()
    ) = LomWaybillStatusAddedContext(
            checkpoint = testCheckpoint,
            order = testCheckpoint.waybillSegment!!.order!!,
            orderPlanFacts = planFacts
    )

    private fun createOrderStatusContext(
            order: LomOrder = LomOrder(),
            orderStatus: OrderStatus = OrderStatus.RETURNING,
            planFacts: List<PlanFact> = listOf()
    ) = LomOrderStatusChangedContext(
            order = order.apply { status = orderStatus },
            orderStatus = orderStatus,
            orderPlanFacts = planFacts
    )

    companion object {
        private const val TEST_BARCODE = "test_order_id"
        private const val TEST_ORDER_ID = 1L
        private const val TEST_QUEUE_NAME = "TEST_QUEUE_NAME"
        private const val TEST_COMPONENT = 123L
        private const val TEST_PARTNER_NAME = "TEST_PARTNER_NAME"
        private const val TEST_TAXI_SEGMENT_ID = 123L
        private val TEST_CHECKPOINT_60_TIME = Instant.parse("2022-02-14T12:00:01.00Z")
        private val EXPECTED_STATUS_DATETIME = TEST_CHECKPOINT_60_TIME.plus(6L,  ChronoUnit.HOURS)
        private val TEST_TIME_BETWEEN_CHECKPOINTS = Duration.ofDays(1)
        private val TEST_CHECKPOINT_60 = WaybillSegmentStatusHistory(
            trackerStatus = OrderDeliveryCheckpointStatus.RETURN_PREPARING.name,
            date = TEST_CHECKPOINT_60_TIME,
        )
        private val TEST_CHECKPOINT_70 = WaybillSegmentStatusHistory(
            trackerStatus = OrderDeliveryCheckpointStatus.RETURN_ARRIVED_DELIVERY.name,
            date = TEST_CHECKPOINT_60_TIME.plus(TEST_TIME_BETWEEN_CHECKPOINTS),
        )
        private val TEST_CHECKPOINT_80 = WaybillSegmentStatusHistory(
            trackerStatus = OrderDeliveryCheckpointStatus.RETURN_TRANSMITTED_FULFILMENT.name,
            date = TEST_CHECKPOINT_60_TIME.plus(TEST_TIME_BETWEEN_CHECKPOINTS),
        )
        private val TEST_CHECKPOINT_WITHOUT_STATUS = WaybillSegmentStatusHistory(
            status = SegmentStatus.TRACK_RECEIVED,
            date = TEST_CHECKPOINT_60_TIME.plus(TEST_TIME_BETWEEN_CHECKPOINTS),
        )
    }
}
