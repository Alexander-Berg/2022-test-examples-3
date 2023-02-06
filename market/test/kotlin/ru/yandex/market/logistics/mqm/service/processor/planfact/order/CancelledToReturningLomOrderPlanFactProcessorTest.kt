package ru.yandex.market.logistics.mqm.service.processor.planfact.order

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.FirstCteIntakeService
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.SecondCteIntakeService
import ru.yandex.market.logistics.mqm.service.event.cte.FirstCteIntakeContext
import ru.yandex.market.logistics.mqm.service.event.cte.SecondCteIntakeContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.service.returns.LrmReturnService
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Clock
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class CancelledToReturningLomOrderPlanFactProcessorTest {

    @Mock
    private lateinit var planFactService: PlanFactService

    @Mock
    private lateinit var clock: Clock

    @Mock
    private lateinit var firstCteIntakeService: FirstCteIntakeService

    @Mock
    private lateinit var secondCteIntakeService: SecondCteIntakeService

    private var lrmReturnService: LrmReturnService = mock()

    lateinit var processor: CancelledToReturningLomOrderPlanFactProcessor

    private val settingService = TestableSettingsService()

    @BeforeEach
    fun setUp() {
        processor = CancelledToReturningLomOrderPlanFactProcessor(
            planFactService = planFactService,
            clock = clock,
            settingService = settingService,
            firstCteIntakeService = firstCteIntakeService,
            secondCteIntakeService = secondCteIntakeService,
            lrmReturnService
        )
    }

    @DisplayName("Проверка создания план-факта")
    @Test
    fun planFactCreationSuccess() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockLomOrderEventContext()
        processor.lomOrderStatusChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_ORDER
            planFact.entityId shouldBe 1L
            planFact.expectedStatus shouldBe "RETURNING"
            planFact.expectedStatusDatetime shouldBe Instant.parse("2021-12-14T09:00:00.00Z")
            planFact.producerName shouldBe "CancelledToReturningLomOrderPlanFactProcessor"
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe Instant.parse("2021-12-14T09:00:00.00Z")
        }
    }

    @DisplayName("Проверка создания план-факта, если не найден CANCELLED чекпоинт")
    @Test
    fun planFactCreationWithoutCancelledCheckpoint() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockLomOrderEventContext(hasCanceledCheckpoint = false)
        processor.lomOrderStatusChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_ORDER
            planFact.entityId shouldBe 1L
            planFact.expectedStatus shouldBe "RETURNING"
            planFact.expectedStatusDatetime shouldBe Instant.parse("2021-12-15T09:00:00.00Z")
            planFact.producerName shouldBe "CancelledToReturningLomOrderPlanFactProcessor"
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe Instant.parse("2021-12-15T09:00:00.00Z")
        }
    }

    @DisplayName("Не создавать план-факт, если статус заказа не CANCELLED")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["CANCELLED"]
    )
    fun doNotCreatePlanFactIfOrderNotCanceled(orderStatus: OrderStatus) {
        val context = mockLomOrderEventContext(orderStatus = orderStatus)
        processor.lomOrderStatusChanged(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Не создавать план-факт, если нет чекпоинта IN")
    @Test
    fun doNotCreatePlanFactIfOrderNotInLogistics() {
        val context = mockLomOrderEventContext(hasInCheckpoint = false)
        processor.lomOrderStatusChanged(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Не создавать план-факт, если пришел не статус CANCELLED")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["CANCELLED"]
    )
    fun doNotCreatePlanFactIfTriggerStatusNotCanceled(triggerStatus: OrderStatus) {
        val context = mockLomOrderEventContext(triggerStatus = triggerStatus)
        processor.lomOrderStatusChanged(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Не создавать план-факт, если заказ не был отгружен с фф")
    @Test
    fun doNotCreatePlanFactIfOrderNotShippedFromFf() {
        val context = mockLomOrderEventContext(wasShipped = false)
        processor.lomOrderStatusChanged(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Не создавать план-факт, если есть активный план-факт")
    @Test
    fun doNotCreatePlanFactIfExistsActive() {
        val existingPlanFact = PlanFact(
            producerName = CancelledToReturningLomOrderPlanFactProcessor::class.simpleName
        ).markCreated(DEFAULT_TIME)
        val context = mockLomOrderEventContext(planFacts = listOf(existingPlanFact))
        processor.lomOrderStatusChanged(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Не создавать планфакт если он есть в ЛРМ")
    @Test
    fun createPlanFactIfExistingActivePlanFactNotFromThisProducer() {
        whenever(lrmReturnService.findByExternalOrderId(any())).thenReturn(mock())
        val existingPlanFact = PlanFact(
            producerName = "TestProducer"
        ).markCreated(DEFAULT_TIME)
        val context = mockLomOrderEventContext(planFacts = listOf(existingPlanFact))
        processor.lomOrderStatusChanged(context)
        verify(planFactService, times(0)).save(any())
    }


    @DisplayName("Создавать план-факт, если есть активный план-факт не от этого продьюсера")
    @Test
    fun notCreatePlanFactIfExistingLrm() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = "TestProducer"
        ).markCreated(DEFAULT_TIME)
        val context = mockLomOrderEventContext(planFacts = listOf(existingPlanFact))
        processor.lomOrderStatusChanged(context)
        verify(planFactService).save(any())
    }

    @DisplayName("Не создавать план-факт, если есть приемка на первичном ЦТЭ")
    @Test
    fun doNotCreatePlanFactIfExistsFirstCteIntake() {
        val context = mockLomOrderEventContext()
        whenever(firstCteIntakeService.getFirstCteIntakeTime(BARCODE)).thenReturn(DEFAULT_TIME)
        processor.lomOrderStatusChanged(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Не создавать план-факт, если есть вторичная приемка на ЦТЭ")
    @Test
    fun doNotCreatePlanFactIfExistsSecondCteIntake() {
        val context = mockLomOrderEventContext()
        whenever(secondCteIntakeService.getSecondCteIntakeTime(BARCODE)).thenReturn(DEFAULT_TIME)
        processor.lomOrderStatusChanged(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Проставлять план-факт в NotActual если факт опоздал")
    @Test
    fun setPlanFactNotActualIfFactCameLate() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = CancelledToReturningLomOrderPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-14T15:59:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockLomOrderEventContext(
            planFacts = listOf(existingPlanFact),
            triggerStatus = OrderStatus.RETURNING,
            hasCanceledCheckpoint = true,
            hasReturnArrivedCheckpoint = true
        )
        processor.lomOrderStatusChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
            existingPlanFact.factStatusDatetime shouldBe RETURN_ARRIVED_TIME
            existingPlanFact.scheduleTime shouldBe DEFAULT_TIME
        }
    }

    @DisplayName("Проставлять план-факт в InTime если факт успел вовремя")
    @Test
    fun setPlanFactInTime() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = CancelledToReturningLomOrderPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-14T16:01:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockLomOrderEventContext(
            planFacts = listOf(existingPlanFact),
            triggerStatus = OrderStatus.RETURNING,
            hasCanceledCheckpoint = true,
            hasReturnArrivedCheckpoint = true
        )
        processor.lomOrderStatusChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
            existingPlanFact.factStatusDatetime shouldBe RETURN_ARRIVED_TIME
        }
    }

    @DisplayName("Проставлять план-факт в NotActual если опоздал один из закрывающих статусов заказа")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["RETURNED", "DELIVERED", "LOST", "FINISHED"]
    )
    fun setPlanFactNotActualIfCloseStatusCameLate(triggerStatus: OrderStatus) {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = CancelledToReturningLomOrderPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-13T15:59:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockLomOrderEventContext(
            planFacts = listOf(existingPlanFact),
            triggerStatus = triggerStatus,
            hasCanceledCheckpoint = true,
            hasReturnArrivedCheckpoint = true
        )
        processor.lomOrderStatusChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.scheduleTime shouldBe DEFAULT_TIME
        }
    }

    @DisplayName("Проставлять план-факт в Expired если пришел один из закрывающих статусов заказа до плана")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["RETURNED", "DELIVERED", "LOST", "FINISHED"]
    )
    fun setPlanFactExpiredIfCloseStatusCameOnTime(triggerStatus: OrderStatus) {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = CancelledToReturningLomOrderPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-14T16:01:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockLomOrderEventContext(
            planFacts = listOf(existingPlanFact),
            triggerStatus = triggerStatus,
            hasCanceledCheckpoint = true,
            hasReturnArrivedCheckpoint = true
        )
        processor.lomOrderStatusChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.scheduleTime shouldBe DEFAULT_TIME
        }
    }

    @DisplayName("Проставлять план-факт в NotActual если опоздал факт первичной приемки на ЦТЭ")
    @Test
    fun setPlanFactNotActualIfLateFirstCteIntake() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = CancelledToReturningLomOrderPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-13T15:59:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockFirstCteEventContext(planFacts = listOf(existingPlanFact))
        processor.intakeInFirstCte(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.scheduleTime shouldBe DEFAULT_TIME
        }
    }

    @DisplayName("Проставлять план-факт в Expired если факт первичной приемки на ЦТЭ произошел до плана ")
    @Test
    fun setPlanFactExpiredIfFirstCteIntakeOnTime() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = CancelledToReturningLomOrderPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-13T16:01:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockFirstCteEventContext(planFacts = listOf(existingPlanFact))
        processor.intakeInFirstCte(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.scheduleTime shouldBe DEFAULT_TIME
        }
    }

    @DisplayName("Проставлять план-факт в NotActual если опоздал факт вторичной приемки на ЦТЭ")
    @Test
    fun setPlanFactNotActualIfLateSecondCteIntake() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = CancelledToReturningLomOrderPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-13T15:59:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockSecondCteEventContext(planFacts = listOf(existingPlanFact))
        processor.intakeInSecondCte(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.scheduleTime shouldBe DEFAULT_TIME
        }
    }

    @DisplayName("Проставлять план-факт в Expired если факт вторичной приемки на ЦТЭ произошел до плана ")
    @Test
    fun setPlanFactExpiredIfSecondCteIntakeOnTime() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = CancelledToReturningLomOrderPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-13T16:01:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockSecondCteEventContext(planFacts = listOf(existingPlanFact))
        processor.intakeInSecondCte(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.scheduleTime shouldBe DEFAULT_TIME
        }
    }

    private fun mockLomOrderEventContext(
        planFacts: List<PlanFact> = listOf(),
        orderStatus: OrderStatus = OrderStatus.CANCELLED,
        triggerStatus: OrderStatus = OrderStatus.CANCELLED,
        hasCanceledCheckpoint: Boolean = true,
        hasInCheckpoint: Boolean = true,
        hasReturnArrivedCheckpoint: Boolean = false,
        wasShipped: Boolean = true,
    ): LomOrderStatusChangedContext {

        val canceledStatus = WaybillSegmentStatusHistory(status = SegmentStatus.CANCELLED, date = CANCELED_TIME)
        val inStatus = WaybillSegmentStatusHistory(status = SegmentStatus.IN)
        val returnArrivedStatus = WaybillSegmentStatusHistory(
            status = SegmentStatus.RETURN_ARRIVED,
            date = RETURN_ARRIVED_TIME
        )
        val outStatus = WaybillSegmentStatusHistory(status = SegmentStatus.OUT)
        val firstSegment = WaybillSegment(segmentType = SegmentType.FULFILLMENT)
        if (hasCanceledCheckpoint) firstSegment.waybillSegmentStatusHistory.add(canceledStatus)
        if (hasInCheckpoint) firstSegment.waybillSegmentStatusHistory.add(inStatus)
        if (hasReturnArrivedCheckpoint) firstSegment.waybillSegmentStatusHistory.add(returnArrivedStatus)
        if (wasShipped) firstSegment.waybillSegmentStatusHistory.add(outStatus)
        val secondSegment = WaybillSegment()
        val order = joinInOrder(listOf(firstSegment, secondSegment))
            .apply {
                id = 1
                status = orderStatus
                barcode = BARCODE
            }
        return LomOrderStatusChangedContext(
            order = order,
            orderStatus = triggerStatus,
            orderPlanFacts = planFacts,
        )
    }

    private fun mockFirstCteEventContext(
        planFacts: List<PlanFact> = listOf(),
    ): FirstCteIntakeContext {
        val firstSegment = WaybillSegment()
        val secondSegment = WaybillSegment()
        val order = joinInOrder(listOf(firstSegment, secondSegment))
            .apply {
                id = 1
                status = OrderStatus.CANCELLED
                barcode = BARCODE
            }
        return FirstCteIntakeContext(
            order = order,
            intakeTime = DEFAULT_TIME,
            orderPlanFacts = planFacts,
        )
    }

    private fun mockSecondCteEventContext(
        planFacts: List<PlanFact> = listOf(),
    ): SecondCteIntakeContext {
        val firstSegment = WaybillSegment()
        val secondSegment = WaybillSegment()
        val order = joinInOrder(listOf(firstSegment, secondSegment))
            .apply {
                id = 1
                status = OrderStatus.CANCELLED
                barcode = BARCODE
            }
        return SecondCteIntakeContext(
            order = order,
            intakeTime = DEFAULT_TIME,
            orderPlanFacts = planFacts,
        )
    }

    companion object {
        private val DEFAULT_TIME = Instant.parse("2021-12-13T16:00:00.00Z")
        private val CANCELED_TIME = Instant.parse("2021-12-13T07:00:00.00Z")
        private val RETURN_ARRIVED_TIME = Instant.parse("2021-12-14T16:00:00.00Z")
        private const val BARCODE = "testBarcode"
    }
}

