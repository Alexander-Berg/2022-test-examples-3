package ru.yandex.market.logistics.mqm.service.processor.planfact.lrm

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
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
import ru.yandex.market.logistics.mqm.entity.lrm.LrmLogisticPointFields
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnSegmentEntity
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.returns.ReturnSegmentStatusChangedContext
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService

@ExtendWith(MockitoExtension::class)
@DisplayName("Тесты, проверяющие создание план-факта перехода статусов IN в OUT для ПВЗ на возвратном сегменте")
class PvzInToOutPlanFactProcessorTest {

    lateinit var processor: PvzInToOutPlanFactProcessor

    @Mock
    private lateinit var planFactService: PlanFactService

    @Mock
    private lateinit var clock: Clock

    private val settingService = TestableSettingsService()

    @BeforeEach
    fun setUp() {
        processor = PvzInToOutPlanFactProcessor(
            planFactService = planFactService,
            clock = clock,
            settingService = settingService,
            properties = ReturnSegmentPlanFactProcessorProperties()
        )
    }

    private fun mockReturnSegmentEventContext(
        logisticPointType: LogisticPointType = LogisticPointType.PICKUP,
        planFacts: List<PlanFact> = listOf(),
        segmentStatus : ReturnSegmentStatus = ReturnSegmentStatus.IN
    ): ReturnSegmentStatusChangedContext {
        return ReturnSegmentStatusChangedContext(
            LrmReturnSegmentEntity(
                status = ReturnSegmentStatus.CREATED,
                logisticPoint = LrmLogisticPointFields(
                    type = logisticPointType
                )
            ),
            segmentStatus,
            planFacts
        )
    }

    @DisplayName("Проверка создания план-факта")
    @Test
    fun planFactCreationSuccess() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockReturnSegmentEventContext()
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LRM_RETURN_SEGMENT
            planFact.entityId shouldBe 0L
            planFact.expectedStatus shouldBe "OUT"
            planFact.expectedStatusDatetime shouldBe Instant.parse("2021-12-16T16:00:00.00Z")
            planFact.producerName shouldBe "PvzInToOutPlanFactProcessor"
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe Instant.parse("2021-12-16T16:00:00.00Z")
        }
    }

    @DisplayName("Не создавать план-факт, если лог.точка не ПВЗ")
    @Test
    fun doNotCreatePlanFactIfLogisticPointNotPickup() {
        val context = mockReturnSegmentEventContext(LogisticPointType.DROPOFF)
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Не создавать план-факт, если есть активный план-факт")
    @Test
    fun doNotCreatePlanFactIfExistsActive() {
        val existingPlanFact = PlanFact(
            producerName = PvzInToOutPlanFactProcessor::class.simpleName,
            planFactStatus = PlanFactStatus.ACTIVE,
            expectedStatus = ReturnSegmentStatus.OUT.toString()
        ).markCreated(DEFAULT_TIME)
        val context = mockReturnSegmentEventContext(planFacts = listOf(existingPlanFact))
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Создавать план-факт, если есть активный план-факт не от этого продьюсера")
    @Test
    fun createPlanFactIfExistingActivePlanFactNotFromThisProducer() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = "TestProducer"
        ).markCreated(DEFAULT_TIME)
        val context = mockReturnSegmentEventContext(planFacts = listOf(existingPlanFact))
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService).save(any())
    }

    @DisplayName("Проставлять план-факт в NotActual если факт опоздал")
    @Test
    fun setPlanFactNotActualIfFactCameLate() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = PvzInToOutPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-13T15:59:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockReturnSegmentEventContext(
            planFacts = listOf(existingPlanFact),
            segmentStatus = ReturnSegmentStatus.OUT
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
            producerName = PvzInToOutPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-14T16:01:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockReturnSegmentEventContext(
            planFacts = listOf(existingPlanFact),
            segmentStatus = ReturnSegmentStatus.OUT
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
        names = ["CANCELLED", "EXPIRED"]
    )
    fun setPlanFactNotActualIfCloseStatusCameLate(triggerStatus: ReturnSegmentStatus) {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = PvzInToOutPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-13T15:59:00.00Z")
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
        names = ["CANCELLED", "EXPIRED"]
    )
    fun setPlanFactExpiredIfCloseStatusCameOnTime(triggerStatus: ReturnSegmentStatus) {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = PvzInToOutPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-14T16:01:00.00Z")
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

    @DisplayName("Проставлять план-факт в NotActual если пришла отмена статуса приемки")
    @Test
    fun setPlanFactNotActualIfFactStatusCameBackward() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = PvzInToOutPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-13T15:59:00.00Z"),
            entityType = EntityType.LRM_RETURN_SEGMENT
        ).apply { entity = LrmReturnSegmentEntity(status = ReturnSegmentStatus.IN) }.markCreated(DEFAULT_TIME)
        val context = mockReturnSegmentEventContext(
            planFacts = listOf(existingPlanFact),
            segmentStatus = ReturnSegmentStatus.CREATED
        )
        processor.returnSegmentStatusWasChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    companion object {
        private val DEFAULT_TIME = Instant.parse("2021-12-13T16:00:00.00Z")
    }
}
