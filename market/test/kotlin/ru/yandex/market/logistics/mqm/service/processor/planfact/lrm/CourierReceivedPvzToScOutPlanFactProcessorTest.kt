package ru.yandex.market.logistics.mqm.service.processor.planfact.lrm

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
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
import ru.yandex.market.logistics.mqm.configuration.properties.CourierPlanfactsProperties
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.enums.courier.CourierStatus
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnBoxEntity
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.returns.LrmCourierStatusChangedContext
import ru.yandex.market.logistics.mqm.service.returns.CourierEventHistoryService
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@ExtendWith(MockitoExtension::class)
class CourierReceivedPvzToScOutPlanFactProcessorTest {
    lateinit var processor: CourierReceivedPvzToScOutPlanFactProcessor

    @Mock
    private lateinit var clock: Clock

    @Mock
    private lateinit var planFactService: PlanFactService

    @Mock
    private lateinit var courierEventHistoryService: CourierEventHistoryService

    @BeforeEach
    fun setUp() {
        processor = CourierReceivedPvzToScOutPlanFactProcessor(
            planFactService = planFactService,
            clock = clock,
            properties = CourierPlanfactsProperties(),
            courierEventHistoryService = courierEventHistoryService
        )
    }

    private fun mockCourierReceivedPickupContext(
        status: CourierStatus = CourierStatus.RECEIVED_PICKUP,
        planFacts: List<PlanFact> = listOf()
    ) = LrmCourierStatusChangedContext(status, LrmReturnBoxEntity(externalId = "aboba"), planFacts)

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
            planFact.expectedStatus shouldBe "DELIVERED_TO_SC"
            planFact.expectedStatusDatetime shouldBe Instant.parse("2021-12-13T17:00:00Z")
            planFact.producerName shouldBe "CourierReceivedPvzToScOutPlanFactProcessor"
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe Instant.parse("2021-12-13T17:00:00Z")
        }
    }

    @DisplayName("Проверка создания и последующего закрытия")
    @Test
    fun planFactCreationImmediatelyClose() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        whenever(clock.zone).thenReturn(ZoneId.of("UTC"))
        whenever(
            courierEventHistoryService.existsByExternalBoxIdAndStatus(
                eq("aboba"),
                eq(CourierStatus.DELIVERED_TO_SC)
            )
        ).thenReturn(
            true
        )
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockCourierReceivedPickupContext()

        processor.courierStatusChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
            planFact.factStatusDatetime shouldBe DEFAULT_TIME
            planFact.entityType shouldBe EntityType.LRM_RETURN_BOX
            planFact.entityId shouldBe 0L
            planFact.expectedStatus shouldBe "DELIVERED_TO_SC"
            planFact.expectedStatusDatetime shouldBe Instant.parse("2021-12-13T17:00:00Z")
            planFact.producerName shouldBe "CourierReceivedPvzToScOutPlanFactProcessor"
            planFact.processingStatus shouldBe ProcessingStatus.PROCESSED
        }
    }

    @DisplayName("Проставлять план-факт в NotActual если факт опоздал")
    @Test
    fun setPlanFactNotActualIfFactCameLate() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = CourierReceivedPvzToScOutPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-13T15:59:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockCourierReceivedPickupContext(
            planFacts = listOf(existingPlanFact),
            status = CourierStatus.DELIVERED_TO_SC
        )
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
            producerName = CourierReceivedPvzToScOutPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-14T16:01:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockCourierReceivedPickupContext(
            planFacts = listOf(existingPlanFact),
            status = CourierStatus.DELIVERED_TO_SC
        )
        processor.courierStatusChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
            existingPlanFact.factStatusDatetime shouldBe DEFAULT_TIME
        }
    }

    companion object {
        private val DEFAULT_TIME = Instant.parse("2021-12-13T16:00:00.00Z")
    }
}
