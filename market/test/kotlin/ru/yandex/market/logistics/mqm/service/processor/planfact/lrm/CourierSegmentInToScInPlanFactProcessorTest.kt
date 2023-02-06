package ru.yandex.market.logistics.mqm.service.processor.planfact.lrm

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Instant
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
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.returns.ReturnSegmentStatusChangedContext
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService

@ExtendWith(MockitoExtension::class)
class CourierSegmentInToScInPlanFactProcessorTest : AbstractLrmReturnPlanFactTest() {

    lateinit var processor: CourierSegmentInToScInPlanFactProcessor

    @Mock
    private lateinit var planFactService: PlanFactService

    @Mock
    private lateinit var clock: Clock

    private val settingService = TestableSettingsService()

    @BeforeEach
    fun setUp() {
        processor = CourierSegmentInToScInPlanFactProcessor(
            settingService,
            planFactService = planFactService,
            clock = clock,
            properties = ReturnSegmentPlanFactProcessorProperties(),
        )
    }

    private fun mockReturnSegmentStatusChangedEventContext(
        logisticPointType: LogisticPointType = LogisticPointType.COURIER,
        planFacts: List<PlanFact> = listOf(),
        status: ReturnSegmentStatus,
        nextSegmentStatus: ReturnSegmentStatus = ReturnSegmentStatus.CREATED
    ): ReturnSegmentStatusChangedContext {
        val returnEntity = mockLrmReturn()
        returnEntity.withSegment(
            mockLrmReturnSegment(
                status = nextSegmentStatus,
                type = LogisticPointType.SORTING_CENTER
            ).apply { lrmSegmentId = 2 })
        return ReturnSegmentStatusChangedContext(
            mockLrmReturnSegment(logisticPointType).apply {
                this.returnEntity = returnEntity
            },
            status = status,
            planFacts
        )
    }

    @DisplayName("Проверка создания план-факта")
    @Test
    fun planFactCreationSuccess() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockReturnSegmentStatusChangedEventContext(status = ReturnSegmentStatus.IN)
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LRM_RETURN_SEGMENT
            planFact.entityId shouldBe 0L
            planFact.expectedStatus shouldBe "IN"
            planFact.expectedStatusDatetime shouldBe Instant.parse("2021-12-14T02:00:00Z")
            planFact.producerName shouldBe "CourierSegmentInToScInPlanFactProcessor"
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe Instant.parse("2021-12-14T02:00:00Z")
        }
    }

    @DisplayName("Проверка создания план-факта, для сегмента с статусом больше нужного")
    @Test
    fun planFactCreationSkip() {
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockReturnSegmentStatusChangedEventContext(
            status = ReturnSegmentStatus.IN,
            nextSegmentStatus = ReturnSegmentStatus.IN
        )
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService, times(0)).save(planFactsCaptor.capture())
    }

    @DisplayName("Проставлять план-факт в InTime если факт успел вовремя")
    @Test
    fun setPlanFactInTime() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = CourierSegmentInToScInPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-14T16:01:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockReturnSegmentStatusChangedEventContext(
            logisticPointType = LogisticPointType.SORTING_CENTER,
            status = ReturnSegmentStatus.IN,
            planFacts = listOf(existingPlanFact)
        )
        processor.returnSegmentStatusWasChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
            existingPlanFact.factStatusDatetime shouldBe DEFAULT_TIME
        }
    }

    @DisplayName("Проставлять план-факт в NotActual если факт опоздал")
    @Test
    fun setPlanFactNotActualIfFactCameLate() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = CourierSegmentInToScInPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-13T15:59:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockReturnSegmentStatusChangedEventContext(
            logisticPointType = LogisticPointType.SORTING_CENTER,
            planFacts = listOf(existingPlanFact),
            status = ReturnSegmentStatus.IN
        )
        processor.returnSegmentStatusWasChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
            existingPlanFact.factStatusDatetime shouldBe DEFAULT_TIME
            existingPlanFact.scheduleTime shouldBe DEFAULT_TIME
        }
    }

    @DisplayName("Проставлять план-факт в Expired если пришел один из закрывающих статусов заказа до плана")
    @ParameterizedTest
    @EnumSource(
        value = ReturnSegmentStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["CANCELLED", "EXPIRED"]
    )
    fun setPlanFactExpiredIfCloseStatusCameOnTime(triggerStatus: ReturnSegmentStatus) {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = CourierSegmentInToScInPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-27T16:01:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockReturnSegmentStatusChangedEventContext(
            planFacts = listOf(existingPlanFact),
            logisticPointType = LogisticPointType.SORTING_CENTER,
            status = triggerStatus
        )
        processor.returnSegmentStatusWasChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.scheduleTime shouldBe DEFAULT_TIME
        }
    }

    companion object {
        private val DEFAULT_TIME = Instant.parse("2021-12-13T16:00:00.00Z")
    }
}
