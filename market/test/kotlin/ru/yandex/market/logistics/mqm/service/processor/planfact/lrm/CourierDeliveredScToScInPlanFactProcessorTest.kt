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
import ru.yandex.market.logistics.mqm.entity.lrm.LrmLogisticPointFields
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnBoxEntity
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnEntity
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnSegmentEntity
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.returns.LrmCourierStatusChangedContext
import ru.yandex.market.logistics.mqm.service.event.returns.ReturnSegmentStatusChangedContext
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@ExtendWith(MockitoExtension::class)
class CourierDeliveredScToScInPlanFactProcessorTest {

    lateinit var processor: CourierDeliveredScToScInPlanFactProcessor

    @Mock
    private lateinit var clock: Clock

    @Mock
    private lateinit var planFactService: PlanFactService

    @BeforeEach
    fun setUp() {
        processor = CourierDeliveredScToScInPlanFactProcessor(
            planFactService = planFactService,
            clock = clock,
            properties = ReturnSegmentPlanFactProcessorProperties(),
        )
    }

    private fun mockCourierDeliveredToScContext(
        planFacts: List<PlanFact> = listOf(),
        segments: List<LrmReturnSegmentEntity> = listOf()
    ): LrmCourierStatusChangedContext {
        val lrmReturn = LrmReturnEntity()
        lrmReturn.returnSegments = segments.toMutableSet()
        val returnBox = LrmReturnBoxEntity(externalId = "aboba")
        returnBox.returnEntity = lrmReturn
        return LrmCourierStatusChangedContext(
            CourierStatus.DELIVERED_TO_SC,
            returnBox,
            planFacts
        )
    }

    private fun mockReturnSegmentEventContext(
        planFacts: List<PlanFact> = listOf(),
        segmentStatus: ReturnSegmentStatus = ReturnSegmentStatus.OUT
    ): ReturnSegmentStatusChangedContext {
        return ReturnSegmentStatusChangedContext(
            LrmReturnSegmentEntity(
                status = ReturnSegmentStatus.CREATED,
                externalBoxId = "box-ext-id",
                logisticPoint = LrmLogisticPointFields(
                    type = LogisticPointType.SORTING_CENTER
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
        whenever(clock.zone).thenReturn(ZoneId.of("UTC"))
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockCourierDeliveredToScContext()

        processor.courierStatusChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LRM_RETURN_BOX
            planFact.entityId shouldBe 0L
            planFact.expectedStatus shouldBe "IN"
            planFact.expectedStatusDatetime shouldBe Instant.parse("2021-12-13T17:00:00Z")
            planFact.producerName shouldBe "CourierDeliveredScToScInPlanFactProcessor"
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
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val segment = LrmReturnSegmentEntity(
            status = ReturnSegmentStatus.IN,
            logisticPoint = LrmLogisticPointFields(type = LogisticPointType.SORTING_CENTER)
        )
        val context =
            mockCourierDeliveredToScContext(
                segments = listOf(
                   segment
                )
            )
        processor.courierStatusChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
            planFact.factStatusDatetime shouldBe DEFAULT_TIME
            planFact.entityType shouldBe EntityType.LRM_RETURN_BOX
            planFact.entityId shouldBe 0L
            planFact.expectedStatus shouldBe "IN"
            planFact.expectedStatusDatetime shouldBe Instant.parse("2021-12-13T17:00:00Z")
            planFact.producerName shouldBe "CourierDeliveredScToScInPlanFactProcessor"
            planFact.processingStatus shouldBe ProcessingStatus.PROCESSED
        }
    }

    @DisplayName("Проставлять план-факт в NotActual если факт опоздал")
    @Test
    fun setPlanFactNotActualIfFactCameLate() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = PlanFact(
            producerName = CourierDeliveredScToScInPlanFactProcessor::class.simpleName,
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
            producerName = CourierDeliveredScToScInPlanFactProcessor::class.simpleName,
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
