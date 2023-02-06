package ru.yandex.market.logistics.mqm.service.processor.planfact.lrm

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
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
import ru.yandex.market.logistics.mqm.entity.enums.lrm.ShipmentDestinationType
import ru.yandex.market.logistics.mqm.entity.lrm.LrmLogisticPointFields
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnSegmentEntity
import ru.yandex.market.logistics.mqm.entity.lrm.LrmShipmentFields
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.TransitScheduleCalculationServiceImpl
import ru.yandex.market.logistics.mqm.service.event.returns.ReturnSegmentStatusChangedContext
import ru.yandex.market.logistics.mqm.service.yt.YtScScTransitScheduleService
import ru.yandex.market.logistics.mqm.service.yt.dto.YtScScSchedule
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService

@ExtendWith(MockitoExtension::class)
@DisplayName("Тесты, проверяющие создание план-факта перехода статусов OUT для СЦ статус IN для другого СЦ на возвратном сегменте")
internal class ScOutToScInPlanFactProcessorTest : AbstractLrmReturnPlanFactTest() {

    lateinit var processor: ScOutToScInPlanFactProcessor

    @Mock
    private lateinit var planFactService: PlanFactService

    @Mock
    private lateinit var clock: Clock

    private val settingService = TestableSettingsService()

    @Mock
    private lateinit var ytScScTransitScheduleService: YtScScTransitScheduleService

    @BeforeEach
    fun setUp() {
        processor = ScOutToScInPlanFactProcessor(
            settingService = settingService,
            clock = clock,
            planFactService = planFactService,
            properties = ReturnSegmentPlanFactProcessorProperties(),
            ytScScTransitScheduleService = ytScScTransitScheduleService,
            transitScheduleCalculationService = TransitScheduleCalculationServiceImpl(clock),
        )
    }

    private fun mockReturnSegmentEventContext(
        logisticPointType: LogisticPointType = LogisticPointType.SORTING_CENTER,
        planFacts: List<PlanFact> = listOf(),
        segmentStatus: ReturnSegmentStatus = ReturnSegmentStatus.OUT,
        nextSegmentStatus: ReturnSegmentStatus = ReturnSegmentStatus.CREATED
    ): ReturnSegmentStatusChangedContext {
        val segment = LrmReturnSegmentEntity(
            id = 0L,
            status = ReturnSegmentStatus.CREATED,
            logisticPoint = LrmLogisticPointFields(
                type = logisticPointType,
                partnerId = 1L
            ),
            shipment = LrmShipmentFields(
                destination = LrmShipmentFields.Destination(
                    ShipmentDestinationType.SORTING_CENTER,
                    partnerId = 2L,
                    returnSegmentId = 15L
                )
            )
        )
        val segmentNext = LrmReturnSegmentEntity(
            id = 1L, lrmReturnId = 1L, lrmSegmentId = 15L, status = nextSegmentStatus,
            logisticPoint = LrmLogisticPointFields(
                partnerId = 10L,
                logisticPointExternalId = "13",
                logisticPointId = 200L,
                type = LogisticPointType.SORTING_CENTER
            ),
            shipment = LrmShipmentFields(
                destination = LrmShipmentFields.Destination(
                    type = ShipmentDestinationType.SORTING_CENTER,
                    name = "22",
                    partnerId = 20L,
                    logisticPointId = 21L,
                    returnSegmentId = 23L
                )
            )
        )
        mockLrmReturn().withSegment(segment).withSegment(segmentNext)
        return ReturnSegmentStatusChangedContext(
            segment,
            segmentStatus,
            planFacts
        )
    }

    @DisplayName("Проверка создания план-факта с дефолтным таймаутом")
    @Test
    fun planFactCreationSuccessWithDefaultTimeout() {
        whenever(ytScScTransitScheduleService.loadSchedule(any(), any())).thenReturn(null)
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockReturnSegmentEventContext()
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LRM_RETURN_SEGMENT
            planFact.entityId shouldBe 1L
            planFact.expectedStatus shouldBe "IN"
            planFact.expectedStatusDatetime shouldBe Instant.parse("2021-12-21T09:00:00.00Z")
            planFact.producerName shouldBe "ScOutToScInPlanFactProcessor"
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe Instant.parse("2021-12-21T09:00:00.00Z")
        }
    }

    @DisplayName("Проверка создания план-факта для сегмента со статусом большим нужного")
    @Test
    fun planFactCreationSuccessWithAlreadyStatusGreaterTarget() {
        PlanFact(
            producerName = PvzInToOutPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-13T15:59:00.00Z"),
            entityType = EntityType.LRM_RETURN_SEGMENT
        ).apply { entity = LrmReturnSegmentEntity(status = ReturnSegmentStatus.IN) }.markCreated(DEFAULT_TIME)
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockReturnSegmentEventContext(nextSegmentStatus = ReturnSegmentStatus.TRANSIT_PREPARED)
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService, times(0)).save(planFactsCaptor.capture())
    }

    @DisplayName("Проверка создания план-факта с таймаутом из расписания - 1 час, ежедневно")
    @Test
    fun planFactCreationSuccessWith1HourEverydaySchedule() {
        whenever(ytScScTransitScheduleService.loadSchedule(any(), any())).thenReturn(
            YtScScSchedule(1L, 2L, Duration.ofHours(1), "Ежедневно", "", LocalTime.NOON, LocalTime.NOON)
        )
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockReturnSegmentEventContext()
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LRM_RETURN_SEGMENT
            planFact.entityId shouldBe 1L
            planFact.expectedStatus shouldBe "IN"
            planFact.expectedStatusDatetime shouldBe Instant.parse("2021-12-14T09:00:00Z")
            planFact.producerName shouldBe "ScOutToScInPlanFactProcessor"
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe Instant.parse("2021-12-14T09:00:00Z")
        }
    }

    @DisplayName("Проверка создания план-факта с таймаутом из расписания 20 часов, 1 раз в 2 недели по нечетным неделям")
    @Test
    fun planFactCreationSuccessWithOneceAnOddWeekSchedule() {
        whenever(ytScScTransitScheduleService.loadSchedule(any(), any())).thenReturn(
            YtScScSchedule(
                1L, 2L, Duration.ofHours(20), "1 раз в 2 недели по нечетным неделям", "6",
                LocalTime.NOON, LocalTime.NOON
            )
        )
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockReturnSegmentEventContext()
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LRM_RETURN_SEGMENT
            planFact.entityId shouldBe 1L
            planFact.expectedStatus shouldBe "IN"
            planFact.expectedStatusDatetime shouldBe Instant.parse("2021-12-26T09:00:00Z")
            planFact.producerName shouldBe "ScOutToScInPlanFactProcessor"
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe Instant.parse("2021-12-26T09:00:00Z")
        }
    }

    @DisplayName("Не создавать план-факт, если лог.точка не СЦ")
    @Test
    fun doNotCreatePlanFactIfLogisticPointNotSC() {
        val context = mockReturnSegmentEventContext(LogisticPointType.DROPOFF)
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Не создавать план-факт, если есть активный план-факт")
    @Test
    fun doNotCreatePlanFactIfExistsActive() {
        // whenever(ytScScTransitScheduleService.loadSchedule(any(), any())).thenReturn(null)
        val existingPlanFact = PlanFact(
            producerName = ScOutToScInPlanFactProcessor::class.simpleName
        ).markCreated(DEFAULT_TIME)
        val context = mockReturnSegmentEventContext(planFacts = listOf(existingPlanFact))
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("Создавать план-факт, если есть активный план-факт не от этого продьюсера")
    @Test
    fun createPlanFactIfExistingActivePlanFactNotFromThisProducer() {
        whenever(ytScScTransitScheduleService.loadSchedule(any(), any())).thenReturn(null)
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
            producerName = ScOutToScInPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-13T15:59:00.00Z")
        ).markCreated(DEFAULT_TIME)
        val context = mockReturnSegmentEventContext(
            planFacts = listOf(existingPlanFact),
            segmentStatus = ReturnSegmentStatus.IN
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
            producerName = ScOutToScInPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-14T16:01:00.00Z")
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
            producerName = ScOutToScInPlanFactProcessor::class.simpleName,
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

    @DisplayName("Закрытие предыдущего план/факта, если пришло следующее событие")
    @Test
    fun closePreviousPlanFacts() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)

        val existingPlanFact1 = PlanFact(
            producerName = ScOutToScInPlanFactProcessor::class.simpleName,
            expectedStatusDatetime = Instant.parse("2021-12-14T16:01:00.00Z"),
            expectedStatus = ReturnSegmentStatus.IN.toString(),
            planFactStatus = PlanFactStatus.ACTIVE
        ).markCreated(DEFAULT_TIME).markActive()

        val context = mockReturnSegmentEventContext(
            planFacts = listOf(existingPlanFact1),
            segmentStatus = ReturnSegmentStatus.TRANSIT_PREPARED
        )
        processor.returnSegmentStatusWasChanged(context)

        assertSoftly {
            existingPlanFact1.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
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
            producerName = ScOutToScInPlanFactProcessor::class.simpleName,
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

    companion object {
        private val DEFAULT_TIME = Instant.parse("2021-12-13T16:00:00.00Z")
    }
}
