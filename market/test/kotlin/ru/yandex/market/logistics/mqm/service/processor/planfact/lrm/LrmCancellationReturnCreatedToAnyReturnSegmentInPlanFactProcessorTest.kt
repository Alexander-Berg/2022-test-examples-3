package ru.yandex.market.logistics.mqm.service.processor.planfact.lrm

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
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
import ru.yandex.market.logistics.mqm.configuration.properties.ReturnSegmentPlanFactProcessorProperties
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.enums.lrm.LogisticPointType
import ru.yandex.market.logistics.mqm.entity.enums.lrm.ReturnSegmentStatus
import ru.yandex.market.logistics.mqm.entity.enums.lrm.ReturnSource
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lrm.LrmLogisticPointFields
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnEntity
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnSegmentEntity
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.returns.LrmReturnCreatedContext
import ru.yandex.market.logistics.mqm.service.event.returns.ReturnSegmentStatusChangedContext
import ru.yandex.market.logistics.mqm.service.lom.LomOrderService
import java.time.Clock
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class LrmCancellationReturnCreatedToAnyReturnSegmentInPlanFactProcessorTest {
    lateinit var processor: LrmCancellationReturnCreatedToAnyReturnSegmentInPlanFactProcessor

    @Mock
    private lateinit var planFactService: PlanFactService

    @Mock
    private lateinit var clock: Clock


    @Mock
    private lateinit var lomOrderService: LomOrderService

    @BeforeEach
    fun setUp() {
        processor = LrmCancellationReturnCreatedToAnyReturnSegmentInPlanFactProcessor(
            planFactService = planFactService,
            clock = clock,
            properties = ReturnSegmentPlanFactProcessorProperties(),
            lomOrderService = lomOrderService
        )
    }

    private fun mockLrmReturnCreatedContext(
        source: ReturnSource = ReturnSource.CANCELLATION,
        planFacts: List<PlanFact> = listOf()
    ): LrmReturnCreatedContext {
        return LrmReturnCreatedContext(
            LrmReturnEntity(source = source, orderExternalId = "ORDER1"),
            planFacts
        )
    }

    private fun mockReturnSegmentEventContext(
        logisticPointType: LogisticPointType = LogisticPointType.DROPOFF,
        planFacts: List<PlanFact> = listOf(),
        segmentStatus: ReturnSegmentStatus = ReturnSegmentStatus.IN
    ): ReturnSegmentStatusChangedContext {
        return ReturnSegmentStatusChangedContext(
            LrmReturnSegmentEntity(
                status = segmentStatus,
                logisticPoint = LrmLogisticPointFields(
                    type = logisticPointType
                )
            ),
            segmentStatus,
            planFacts
        )
    }

    @DisplayName("Проверка создания план-факта по созданию возврата")
    @Test
    fun planFactCreationSuccess() {
        whenever(lomOrderService.getByBarcode(eq("ORDER1"))).thenReturn(LomOrder().apply {
            waybill = mutableListOf(
                WaybillSegment().apply {
                    waybillSegmentStatusHistory = mutableSetOf(WaybillSegmentStatusHistory(status = SegmentStatus.IN))
                }
            )
        })
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockLrmReturnCreatedContext()
        processor.lrmReturnCreated(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LRM_RETURN
            planFact.entityId shouldBe 0L
            planFact.expectedStatus shouldBe "IN"
            planFact.expectedStatusDatetime shouldBe Instant.parse("2021-12-20T15:00:00.00Z")
            planFact.producerName shouldBe LrmCancellationReturnCreatedToAnyReturnSegmentInPlanFactProcessor::class.simpleName
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe Instant.parse("2021-12-20T15:00:00.00Z")
        }
    }

    @DisplayName("Проверка, что план-факт не создался, т.к заказ не был в логистике")
    @Test
    fun planFactNotCreated() {
        whenever(lomOrderService.getByBarcode(eq("ORDER1"))).thenReturn(LomOrder())
        val context = mockLrmReturnCreatedContext()
        processor.lrmReturnCreated(context)
        verifyZeroInteractions(planFactService)
    }

    @DisplayName("Проверка создания план-факта и его закрытие если факт уже состоялся")
    @Test
    fun planFactCreatedAndImmediatelyFactReceived() {
        whenever(lomOrderService.getByBarcode(eq("ORDER1"))).thenReturn(LomOrder().apply {
            waybill = mutableListOf(
                WaybillSegment().apply {
                    waybillSegmentStatusHistory = mutableSetOf(WaybillSegmentStatusHistory(status = SegmentStatus.IN))
                }
            )
        })
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockLrmReturnCreatedContext()
        context.apply {
            lrmReturn.returnSegments.add(LrmReturnSegmentEntity(status = ReturnSegmentStatus.IN))
        }
        processor.lrmReturnCreated(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
            planFact.factStatusDatetime shouldBe DEFAULT_TIME
        }
    }

    @DisplayName("Не создавать план-факт, если не невыкуп")
    @ParameterizedTest
    @EnumSource(
        value = ReturnSource::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["CANCELLATION"]
    )
    fun doNotCreatePlanFactIfReturnSourceNotCancellation(returnSource: ReturnSource) {
        val context = mockLrmReturnCreatedContext(returnSource)
        processor.lrmReturnCreated(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Проставлять план-факт в NotActual если факт опоздал")
    @Test
    fun setPlanFactNotActualIfFactCameLate() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = LrmCancellationReturnCreatedToAnyReturnSegmentInPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-13T14:59:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockReturnSegmentEventContext(
            planFacts = listOf(existingPlanFact)
        )
        processor.returnSegmentStatusWasChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
            existingPlanFact.factStatusDatetime shouldBe DEFAULT_TIME
            existingPlanFact.scheduleTime shouldBe DEFAULT_TIME
        }
    }

    @DisplayName("Проставлять план-факт в InTime если факт успел вовремя")
    @Test
    fun setPlanFactInTime() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = LrmCancellationReturnCreatedToAnyReturnSegmentInPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = DEFAULT_TIME.plusSeconds(1)
        ).markCreated(DEFAULT_TIME)
        val context = mockReturnSegmentEventContext(
            planFacts = listOf(existingPlanFact)
        )
        processor.returnSegmentStatusWasChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
            existingPlanFact.factStatusDatetime shouldBe DEFAULT_TIME
        }
    }

    @DisplayName("Проставлять план-факт в NotActual если опоздал один из закрывающих статусов заказа")
    @ParameterizedTest
    @EnumSource(
        value = ReturnSegmentStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["CANCELLED", "EXPIRED", "OUT", "TRANSIT_PREPARED", "WAITING_FOR_CANCELLATION"]
    )
    fun setPlanFactNotActualIfCloseStatusCameLate(triggerStatus: ReturnSegmentStatus) {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = LrmCancellationReturnCreatedToAnyReturnSegmentInPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = DEFAULT_TIME.minusSeconds(1)
        ).markCreated(DEFAULT_TIME)
        val context = mockReturnSegmentEventContext(
            planFacts = listOf(existingPlanFact),
            segmentStatus = triggerStatus
        )
        processor.returnSegmentStatusWasChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.scheduleTime shouldBe DEFAULT_TIME
        }
    }

    @DisplayName("Проставлять план-факт в Expired если пришел один из закрывающих статусов заказа до плана")
    @ParameterizedTest
    @EnumSource(
        value = ReturnSegmentStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["CANCELLED", "EXPIRED", "OUT", "TRANSIT_PREPARED", "WAITING_FOR_CANCELLATION"]
    )
    fun setPlanFactExpiredIfCloseStatusCameOnTime(triggerStatus: ReturnSegmentStatus) {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = LrmCancellationReturnCreatedToAnyReturnSegmentInPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = DEFAULT_TIME.plusSeconds(1)
        ).markCreated(DEFAULT_TIME)
        val context = mockReturnSegmentEventContext(
            planFacts = listOf(existingPlanFact),
            segmentStatus = triggerStatus
        )
        processor.returnSegmentStatusWasChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.scheduleTime shouldBe DEFAULT_TIME
        }
    }

    companion object {
        private val DEFAULT_TIME = Instant.parse("2021-12-13T15:00:00.00Z")
    }
}
