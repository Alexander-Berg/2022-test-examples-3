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
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnEntity
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnSegmentEntity
import ru.yandex.market.logistics.mqm.entity.lrm.LrmShipmentFields
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.TransitScheduleCalculationServiceImpl
import ru.yandex.market.logistics.mqm.service.event.returns.ReturnSegmentStatusChangedContext
import ru.yandex.market.logistics.mqm.service.yt.YtScScTransitScheduleService
import ru.yandex.market.logistics.mqm.service.yt.dto.YtScScSchedule
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.roundToNextMoscowMiddayWithDelta
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

@ExtendWith(MockitoExtension::class)
@DisplayName("Тесты, проверяющие создание план-факта перехода статусов TRANSIT_PREPARED в OUT для СЦ на возвратном сегменте")
class ScTransitPreparedToOutPlanFactProcessorTest {

    lateinit var processor: ScTransitPreparedToOutPlanFactProcessor

    @Mock
    private lateinit var planFactService: PlanFactService

    @Mock
    private lateinit var clock: Clock

    private val settingService = TestableSettingsService()

    @Mock
    private lateinit var ytScScTransitScheduleService: YtScScTransitScheduleService

    private var properties = ReturnSegmentPlanFactProcessorProperties()

    @BeforeEach
    fun setUp() {
        processor = ScTransitPreparedToOutPlanFactProcessor(
            planFactService = planFactService,
            clock = clock,
            settingService = settingService,
            properties = ReturnSegmentPlanFactProcessorProperties(),
            ytScScTransitScheduleService = ytScScTransitScheduleService,
            transitScheduleCalculationService = TransitScheduleCalculationServiceImpl(clock)
        )
    }

    private fun LrmReturnEntity.addSegment(partnerId: Long = 10L) {
        returnSegments.add(
            LrmReturnSegmentEntity(
                id = 1L, lrmReturnId = 1L, lrmSegmentId = 15L, status = ReturnSegmentStatus.IN,
                logisticPoint = LrmLogisticPointFields(
                    partnerId = partnerId,
                    logisticPointExternalId = "13",
                    logisticPointId = 200L,
                    type = LogisticPointType.SHOP
                ),
                shipment = LrmShipmentFields(
                    destination = LrmShipmentFields.Destination(
                        type = ShipmentDestinationType.SHOP,
                        name = "22",
                        partnerId = 20L,
                        logisticPointId = 21L,
                        returnSegmentId = 23L
                    )
                )
            )
        )
    }


    private fun mockReturnSegmentEventContext(
        logisticPointType: LogisticPointType = LogisticPointType.SORTING_CENTER,
        planFacts: List<PlanFact> = listOf(),
        segmentStatus : ReturnSegmentStatus = ReturnSegmentStatus.TRANSIT_PREPARED,
        destinationType: ShipmentDestinationType = ShipmentDestinationType.SHOP,
        partnerId: Long = 1L
    ): ReturnSegmentStatusChangedContext {
        return ReturnSegmentStatusChangedContext(
            LrmReturnSegmentEntity(
                id = 0L,
                status = ReturnSegmentStatus.CREATED,
                logisticPoint = LrmLogisticPointFields(
                    type = logisticPointType,
                    partnerId = partnerId),
                shipment = LrmShipmentFields(
                    destination = LrmShipmentFields.Destination(
                        destinationType,
                        partnerId = 2L,
                        returnSegmentId = 15L))
            ).apply {
                returnEntity = LrmReturnEntity()
            },
            segmentStatus,
            planFacts
        )
    }

    @DisplayName("Проверка создания план-факта с отгрузкой в магазин")
    @Test
    fun planFactCreationSuccessShipToShop() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockReturnSegmentEventContext()
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        val expectedDateTime = DEFAULT_TIME.plus(properties.scProcessedToOutShopTimeout)
        assertSoftly {
            planFact.entityType shouldBe EntityType.LRM_RETURN_SEGMENT
            planFact.entityId shouldBe 0L
            planFact.expectedStatus shouldBe "OUT"
            planFact.expectedStatusDatetime shouldBe expectedDateTime
            planFact.producerName shouldBe ScTransitPreparedToOutPlanFactProcessor::class.simpleName
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe expectedDateTime
        }
    }

    @DisplayName("Проверка создания план-факта с отгрузкой в DO")
    @Test
    fun planFactCreationSuccessShipToDo() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockReturnSegmentEventContext(destinationType = ShipmentDestinationType.DROPOFF)
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        val expectedDateTime = DEFAULT_TIME.plus(properties.scProcessedToOutDropoffTimeout)
        assertSoftly {
            planFact.entityType shouldBe EntityType.LRM_RETURN_SEGMENT
            planFact.entityId shouldBe 0L
            planFact.expectedStatus shouldBe "OUT"
            planFact.expectedStatusDatetime shouldBe expectedDateTime
            planFact.producerName shouldBe ScTransitPreparedToOutPlanFactProcessor::class.simpleName
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe expectedDateTime
        }
    }

    @DisplayName("Проверка создания план-факта с отгрузкой в FF без записи расписания маршрута")
    @Test
    fun planFactCreationSuccessShipToScWithDefaultTimeout() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockReturnSegmentEventContext(destinationType = ShipmentDestinationType.FULFILLMENT)
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        val expectedDateTime = DEFAULT_TIME.plus(maxOf(properties.scProcessedToOutDropoffTimeout, properties.scProcessedToOutShopTimeout))
        assertSoftly {
            planFact.entityType shouldBe EntityType.LRM_RETURN_SEGMENT
            planFact.entityId shouldBe 0L
            planFact.expectedStatus shouldBe "OUT"
            planFact.expectedStatusDatetime shouldBe expectedDateTime
            planFact.producerName shouldBe ScTransitPreparedToOutPlanFactProcessor::class.simpleName
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe expectedDateTime
        }
    }

    @DisplayName("Проверка создания план-факта с отгрузкой в SC")
    @Test
    fun planFactCreationSuccessShipToSc() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        whenever(ytScScTransitScheduleService.loadSchedule(any(), any())).thenReturn(
            YtScScSchedule(1L, 2L, Duration.ofHours(3), "Ежедневно", "", LocalTime.NOON.minusHours(1L), LocalTime.NOON)
        )
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockReturnSegmentEventContext(destinationType = ShipmentDestinationType.SORTING_CENTER)
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        val expectedDateTime = roundToNextMoscowMiddayWithDelta(DEFAULT_TIME.plus(Duration.ofHours(3)), clock)
        assertSoftly {
            planFact.entityType shouldBe EntityType.LRM_RETURN_SEGMENT
            planFact.entityId shouldBe 0L
            planFact.expectedStatus shouldBe "OUT"
            planFact.expectedStatusDatetime shouldBe expectedDateTime
            planFact.producerName shouldBe ScTransitPreparedToOutPlanFactProcessor::class.simpleName
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe expectedDateTime
        }
    }

    @DisplayName("Проверка создания план-факта с отгрузкой в SC и таймаутом отгрузки меньше, чем minGap")
    @Test
    fun planFactCreationSuccessShipToScWithTimeoutLessThanMinGap() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        whenever(ytScScTransitScheduleService.loadSchedule(any(), any())).thenReturn(
            YtScScSchedule(1L, 2L, Duration.ofMinutes(10), "Ежедневно", "", LocalTime.NOON.minusHours(1L), LocalTime.NOON)
        )
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockReturnSegmentEventContext(destinationType = ShipmentDestinationType.SORTING_CENTER)
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        val expectedDateTime = roundToNextMoscowMiddayWithDelta(DEFAULT_TIME.plus(Duration.ofHours(3)), clock)
        assertSoftly {
            planFact.entityType shouldBe EntityType.LRM_RETURN_SEGMENT
            planFact.entityId shouldBe 0L
            planFact.expectedStatus shouldBe "OUT"
            planFact.expectedStatusDatetime shouldBe expectedDateTime
            planFact.producerName shouldBe ScTransitPreparedToOutPlanFactProcessor::class.simpleName
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe expectedDateTime
        }
    }

    @DisplayName("Не создавать план-факт, если лог.точка не SC")
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
            producerName = ScTransitPreparedToOutPlanFactProcessor::class.simpleName,
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
            producerName = ScTransitPreparedToOutPlanFactProcessor::class.simpleName,
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
            producerName = ScTransitPreparedToOutPlanFactProcessor::class.simpleName,
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
            producerName = ScTransitPreparedToOutPlanFactProcessor::class.simpleName,
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
            producerName = ScTransitPreparedToOutPlanFactProcessor::class.simpleName,
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

    @DisplayName("Проверка создания план-факта для СЦ с коротким сроком вывоза")
    @Test
    fun planFactCreationSuccessWithShortageExpectedDateTime() {
        val context = mockReturnSegmentEventContext(partnerId = 84687L)
        context.segment.returnEntity!!.addSegment(84687)
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        processor.returnSegmentStatusWasChanged(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LRM_RETURN_SEGMENT
            planFact.entityId shouldBe 0L
            planFact.expectedStatus shouldBe "OUT"
            planFact.expectedStatusDatetime shouldBe Instant.parse("2022-01-12T16:00:00Z")
            planFact.producerName shouldBe ScTransitPreparedToOutPlanFactProcessor::class.simpleName
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe Instant.parse("2022-01-12T16:00:00Z")
        }
    }

    companion object {
        private val DEFAULT_TIME = Instant.parse("2021-12-13T16:00:00.00Z")
    }
}