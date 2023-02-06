package ru.yandex.market.logistics.mqm.service.processor.planfact.lrm

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
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
import ru.yandex.market.logistics.mqm.entity.lrm.LrmLogisticPointFields
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnBoxEntity
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnSegmentEntity
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.returns.LrmCourierStatusChangedContext
import ru.yandex.market.logistics.mqm.service.event.returns.ReturnSegmentStatusChangedContext
import ru.yandex.market.logistics.mqm.service.returns.CourierEventHistoryService
import ru.yandex.market.logistics.mqm.service.returns.LrmReturnBoxService
import java.time.Clock
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class PvzOutToCourierReceivedPvzPlanFactProcessorTest {

    lateinit var processor: PvzOutToCourierReceivedPvzPlanFactProcessor

    @Mock
    private lateinit var planFactService: PlanFactService

    @Mock
    private lateinit var clock: Clock

    @Mock
    private lateinit var courierEventHistoryService: CourierEventHistoryService

    @Mock
    private lateinit var lrmReturnBoxService: LrmReturnBoxService

    @BeforeEach
    fun setUp() {
        processor = PvzOutToCourierReceivedPvzPlanFactProcessor(
            planFactService = planFactService,
            clock = clock,
            properties = ReturnSegmentPlanFactProcessorProperties(),
            courierEventHistoryService = courierEventHistoryService,
            lrmReturnBoxService = lrmReturnBoxService,
        )
    }

    private fun mockReturnSegmentEventContext(
        logisticPointType: LogisticPointType = LogisticPointType.PICKUP,
        planFacts: List<PlanFact> = listOf(),
        segmentStatus: ReturnSegmentStatus = ReturnSegmentStatus.OUT
    ): ReturnSegmentStatusChangedContext {
        return ReturnSegmentStatusChangedContext(
            LrmReturnSegmentEntity(
                status = segmentStatus,
                externalBoxId = "box-ext-id",
                logisticPoint = LrmLogisticPointFields(
                    type = logisticPointType
                )
            ),
            segmentStatus,
            planFacts
        )
    }

    private fun mockCourierReceivedPickupContext(planFacts: List<PlanFact> = listOf()) = LrmCourierStatusChangedContext(
        CourierStatus.RECEIVED_PICKUP,
        LrmReturnBoxEntity(externalId = "aboba"),
        planFacts
    )

    @DisplayName("Проверка создания план-факта")
    @Test
    fun planFactCreationSuccess() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        whenever(lrmReturnBoxService.findByExternalId(any())).thenReturn(LrmReturnBoxEntity(externalId = "aboba"))
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockReturnSegmentEventContext()
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LRM_RETURN_BOX
            planFact.entityId shouldBe 0L
            planFact.expectedStatus shouldBe "RECEIVED_PICKUP"
            planFact.expectedStatusDatetime shouldBe Instant.parse("2021-12-13T17:00:00Z")
            planFact.producerName shouldBe "PvzOutToCourierReceivedPvzPlanFactProcessor"
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe Instant.parse("2021-12-13T17:00:00Z")
        }
    }

    @DisplayName("Проверка создания план-факта, курьер уже получил коробку.")
    @Test
    fun planFactCreationAfterCourierReceived() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockReturnSegmentEventContext()
        whenever(lrmReturnBoxService.findByExternalId(any())).thenReturn(LrmReturnBoxEntity(externalId = "aboba"))
        whenever(courierEventHistoryService.existsByExternalBoxIdAndStatus(any(), any())).thenReturn(true)
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LRM_RETURN_BOX
            planFact.entityId shouldBe 0L
            planFact.expectedStatus shouldBe "RECEIVED_PICKUP"
            planFact.expectedStatusDatetime shouldBe Instant.parse("2021-12-13T17:00:00Z")
            planFact.producerName shouldBe "PvzOutToCourierReceivedPvzPlanFactProcessor"
            planFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
            planFact.processingStatus shouldBe ProcessingStatus.PROCESSED
        }
    }

    @DisplayName("Не создавать план-факт, если лог.точка не ПВЗ")
    @Test
    fun doNotCreatePlanFactIfLogisticPointNotPickup() {
        val context = mockReturnSegmentEventContext(LogisticPointType.DROPOFF, segmentStatus = ReturnSegmentStatus.CREATED)
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Создавать план-факт, если есть активный план-факт не от этого продьюсера")
    @Test
    fun createPlanFactIfExistingActivePlanFactNotFromThisProducer() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        whenever(lrmReturnBoxService.findByExternalId(any())).thenReturn(LrmReturnBoxEntity(externalId = "aboba"))
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
            producerName = PvzOutToCourierReceivedPvzPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-13T15:59:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockCourierReceivedPickupContext(planFacts = listOf(existingPlanFact))
        processor.courierStatusChanged(context)
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
            producerName = PvzOutToCourierReceivedPvzPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-14T16:01:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockCourierReceivedPickupContext(
            planFacts = listOf(existingPlanFact)
        )
        processor.courierStatusChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
            existingPlanFact.factStatusDatetime shouldBe DEFAULT_TIME
        }
    }

    @DisplayName("Закрыть планфакт, если на СЦ IN")
    @Test
    fun closePlanFactIfScIn() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = PvzOutToCourierReceivedPvzPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-14T16:01:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockReturnSegmentEventContext(
            logisticPointType = LogisticPointType.SORTING_CENTER,
            segmentStatus = ReturnSegmentStatus.IN,
            planFacts = listOf(existingPlanFact)
        )
        processor.returnSegmentStatusWasChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
        }
    }

    companion object {
        private val DEFAULT_TIME = Instant.parse("2021-12-13T16:00:00.00Z")
    }
}
