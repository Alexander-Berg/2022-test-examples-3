package ru.yandex.market.logistics.mqm.service.processor.planfact.lrm

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.logistics.mqm.configuration.properties.ReturnSegmentPlanFactProcessorProperties
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.enums.courier.CourierStatus
import ru.yandex.market.logistics.mqm.entity.enums.lrm.LogisticPointType
import ru.yandex.market.logistics.mqm.entity.enums.lrm.ReturnSegmentStatus
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnBoxEntity
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.returns.LrmCourierStatusChangedContext
import ru.yandex.market.logistics.mqm.service.event.returns.ReturnSegmentStatusChangedContext
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@ExtendWith(MockitoExtension::class)
class CourierReceivedPvzToScInPlanFactProcessorTest : AbstractLrmReturnPlanFactTest() {

    lateinit var processor: CourierReceivedPvzToScInPlanFactProcessor

    @Mock
    private lateinit var clock: Clock

    @Mock
    private lateinit var planFactService: PlanFactService

    @BeforeEach
    fun setUp() {
        processor = CourierReceivedPvzToScInPlanFactProcessor(
            planFactService = planFactService,
            clock = clock,
            properties = ReturnSegmentPlanFactProcessorProperties(),
        )
    }

    private fun mockCourierReceivedPickupContext(planFacts: List<PlanFact> = listOf()): LrmCourierStatusChangedContext {
        val lrmReturn = mockLrmReturn()
        val box = LrmReturnBoxEntity(externalId = "aboba")
        lrmReturn.withBox(box)
        return LrmCourierStatusChangedContext(
                CourierStatus.RECEIVED_PICKUP,
                box,
                planFacts
        )
    }

    private fun mockReturnSegmentEventContext(
        planFacts: List<PlanFact> = listOf(),
        segmentStatus: ReturnSegmentStatus = ReturnSegmentStatus.OUT
    ): ReturnSegmentStatusChangedContext {
        return ReturnSegmentStatusChangedContext(
                mockLrmReturnSegment(LogisticPointType.SORTING_CENTER, ReturnSegmentStatus.CREATED).apply {
                    externalBoxId = "nox-ext-id"
                },
                segmentStatus,
                planFacts
        )
    }

    @DisplayName("Проверка создания план-факта")
    @Test
    fun planFactCreationSuccess() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        whenever(clock.zone).thenReturn(ZoneId.of("UTC"))
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockCourierReceivedPickupContext()
        processor.courierStatusChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LRM_RETURN_BOX
            planFact.entityId shouldBe 0L
            planFact.expectedStatus shouldBe "IN"
            planFact.expectedStatusDatetime shouldBe Instant.parse("2021-12-14T02:00:00Z")
            planFact.producerName shouldBe "CourierReceivedPvzToScInPlanFactProcessor"
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe Instant.parse("2021-12-14T02:00:00Z")
        }
    }

    @DisplayName("Проверка создания план-факта и последующего закрытия")
    @Test
    fun planFactCreationImmediatelyClose() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        whenever(clock.zone).thenReturn(ZoneId.of("UTC"))
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockCourierReceivedPickupContext()
        context.box.returnEntity!!.withSegment(
                mockLrmReturnSegment(LogisticPointType.SORTING_CENTER)
        )
        processor.courierStatusChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LRM_RETURN_BOX
            planFact.entityId shouldBe 0L
            planFact.expectedStatus shouldBe "IN"
            planFact.expectedStatusDatetime shouldBe Instant.parse("2021-12-14T02:00:00Z")
            planFact.producerName shouldBe "CourierReceivedPvzToScInPlanFactProcessor"
            planFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
            planFact.processingStatus shouldBe ProcessingStatus.PROCESSED
        }
    }

    @DisplayName("Проставлять план-факт в NotActual если факт опоздал")
    @Test
    fun setPlanFactNotActualIfFactCameLate() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = CourierReceivedPvzToScInPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-13T15:59:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context =
            mockReturnSegmentEventContext(segmentStatus = ReturnSegmentStatus.IN, planFacts = listOf(existingPlanFact))
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
            producerName = CourierReceivedPvzToScInPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-14T16:01:00.00Z"),
            expectedStatus = "IN"
        ).markCreated(DEFAULT_TIME)
        val context = mockReturnSegmentEventContext(
            planFacts = listOf(existingPlanFact),
            segmentStatus = ReturnSegmentStatus.IN
        )
        processor.returnSegmentStatusWasChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
            existingPlanFact.factStatusDatetime shouldBe DEFAULT_TIME
        }
    }


    companion object {
        private val DEFAULT_TIME = Instant.parse("2021-12-13T16:00:00.00Z")
    }
}

