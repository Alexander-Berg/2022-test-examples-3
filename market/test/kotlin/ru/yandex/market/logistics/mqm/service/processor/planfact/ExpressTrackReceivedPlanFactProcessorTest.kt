package ru.yandex.market.logistics.mqm.service.processor.planfact

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.PartnerSettings
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Duration
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class ExpressTrackReceivedPlanFactProcessorTest {

    @Mock
    private lateinit var planFactService: PlanFactService

    private val clock = TestableClock()

    lateinit var processor: ExpressTrackReceivedPlanFactProcessor

    private val settingService = TestableSettingsService()

    @BeforeEach
    fun setUp() {
        processor = ExpressTrackReceivedPlanFactProcessor(
            settingService,
            clock,
            planFactService
        )
        clock.setFixed(TEST_NOW, DateTimeUtils.MOSCOW_ZONE)
    }

    @Test
    @DisplayName("Проверка создания план-факта")
    fun planFactCreationSuccess() {
        val context = mockSegmentStatusAddedContext()
        processor.waybillSegmentStatusAdded(context)

        verifyPlanFactSaved()
    }

    @Test
    @DisplayName("Создать план-факт в статусе IN_TIME, если ожидаемый чекпоинт уже есть")
    fun createInTimePlanFactIfExpectedStatusAlreadyCame() {
        val context = mockSegmentStatusAddedContext(
            newSegmentStatus = SegmentStatus.TRANSIT_PREPARED,
            checkpointTime = EXPECTED_TIME.minusSeconds(1)
        )

        processor.waybillSegmentStatusAdded(context)

        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe TEST_DROPSHIP_SEGMENT_ID
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe EXPECTED_TIME
            planFact.producerName shouldBe "ExpressTrackReceivedPlanFactProcessor"
            planFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
            planFact.processingStatus shouldBe ProcessingStatus.PROCESSED
            planFact.scheduleTime shouldBe null
        }
    }

    @Test
    @DisplayName("Не создавать план-факт на не экспресс")
    fun doNotCreatePlanFactForNotExpress() {
        val context = mockSegmentStatusAddedContext()
        context.order.waybill[0].segmentType = SegmentType.SORTING_CENTER
        processor.waybillSegmentStatusAdded(context)
    }

    @Test
    @DisplayName("Не создавать повторяющийся план-факт")
    fun doNotCreateRepeatedPlanFact() {
        val planFact = buildPlanFact()
        val context = mockSegmentStatusAddedContext(existingPlanFacts = mutableListOf(planFact))
        planFact.apply { entity = context.checkpoint.waybillSegment }
        processor.waybillSegmentStatusAdded(context)
    }

    @Test
    @DisplayName("Не создавать план-факт, если чекпоинт уже есть")
    fun doNotCreatePlanFactIfTooLate() {
        val planFact = buildPlanFact()
        val context = mockSegmentStatusAddedContext(
            newSegmentStatus = SegmentStatus.OUT,
            existingPlanFacts = mutableListOf(planFact))
        planFact.apply { entity = context.checkpoint.waybillSegment }
        processor.waybillSegmentStatusAdded(context)
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        assertSoftly {
            planFactsCaptor.allValues.size shouldBe 0
        }
    }

    @Test
    @DisplayName("Пометить старый план-факт как outdated и сохранить новый")
    fun markOldPlanFactsAsOutdatedAndSaveNewOne() {
        val oldPlanFact = buildPlanFact()
        oldPlanFact.expectedStatusDatetime = EXPECTED_TIME.minusSeconds(10)
        val context = mockSegmentStatusAddedContext(existingPlanFacts = mutableListOf(oldPlanFact))
        oldPlanFact.apply { entity = context.checkpoint.waybillSegment }
        processor.waybillSegmentStatusAdded(context)

        verifyPlanFactSaved()
        assertSoftly {
            oldPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED
        }
    }

    @DisplayName("Проставлять план-факт в InTime если факт успел вовремя")
    @Test
    fun setPlanFactInTime() {
        val planFact = buildPlanFact()
        val context = mockSegmentStatusAddedContext(
            checkpointTime = EXPECTED_TIME.minusSeconds(1),
            newSegmentStatus = SegmentStatus.TRANSIT_PREPARED,
            existingPlanFacts = mutableListOf(planFact)
        )
        planFact.apply { entity = context.checkpoint.waybillSegment }
        processor.waybillSegmentStatusAdded(context)

        assertSoftly { planFact.planFactStatus shouldBe PlanFactStatus.IN_TIME }
    }

    @DisplayName("Закрывать план-факт как NotActual если факт опоздал")
    @Test
    fun setPlanFactNotActual() {
        val planFact = buildPlanFact()
        val context = mockSegmentStatusAddedContext(
            newSegmentStatus = SegmentStatus.TRANSIT_PREPARED,
            checkpointTime = EXPECTED_TIME.plus(Duration.ofSeconds(1)),
            existingPlanFacts = mutableListOf(planFact)
        )
        planFact.apply { entity = context.checkpoint.waybillSegment }
        processor.waybillSegmentStatusAdded(context)

        assertSoftly { planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL }
    }

    @DisplayName("Закрывать план-факт как NotActual если на сегмент пришел закрывающий статус после плана")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["OUT", "RETURN_PREPARING", "RETURN_ARRIVED", "RETURNED"]
    )
    fun closePlanFactAsNotActualOnCloseSegmentStatusAfterPlan(closeSegmentStatus: SegmentStatus) {
        val planFact = buildPlanFact()
        val context = mockSegmentStatusAddedContext(
            newSegmentStatus = closeSegmentStatus,
            checkpointTime = EXPECTED_TIME.plusSeconds(1),
            existingPlanFacts = mutableListOf(planFact)
        )
        planFact.apply { entity = context.checkpoint.waybillSegment }
        processor.waybillSegmentStatusAdded(context)

        checkPlanFactClosedAs(planFact, PlanFactStatus.NOT_ACTUAL)
    }

    @DisplayName("Закрывать план-факт как Expired если на сегмент пришел закрывающий статус до плана")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["OUT", "RETURN_PREPARING", "RETURN_ARRIVED", "RETURNED"]
    )
    fun closePlanFactAsExpiredOnCloseSegmentStatusBeforePlan(closeSegmentStatus: SegmentStatus) {
        val planFact = buildPlanFact()
        val context = mockSegmentStatusAddedContext(
            newSegmentStatus = closeSegmentStatus,
            checkpointTime = EXPECTED_TIME.minusSeconds(1),
            existingPlanFacts = mutableListOf(planFact)
        )
        planFact.apply { entity = context.checkpoint.waybillSegment }
        processor.waybillSegmentStatusAdded(context)

        checkPlanFactClosedAs(planFact, PlanFactStatus.EXPIRED)
    }

    @DisplayName("Проставлять план-факт в Expired если пришел один из закрывающих статусов заказа до плана")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["RETURNED", "DELIVERED", "LOST", "RETURNING"]
    )
    fun setPlanFactExpiredIfCloseStatusCameOnTime(triggerStatus: OrderStatus) {
        val testNow = EXPECTED_TIME.minusSeconds(1)
        clock.setFixed(testNow, DateTimeUtils.MOSCOW_ZONE)
        val planFact = buildPlanFact()
        val context = mockLomOrderEventContext(
            existingPlanFacts = mutableListOf(planFact),
            orderStatus = triggerStatus
        )
        processor.lomOrderStatusChanged(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
            planFact.factStatusDatetime shouldBe null
            planFact.endOfProcessingDatetime shouldBe testNow
            planFact.scheduleTime shouldBe testNow
        }
    }

    @DisplayName("Проставлять план-факт в NotActual если опоздал один из закрывающих статусов заказа")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["RETURNED", "DELIVERED", "LOST"]
    )
    fun setPlanFactNotActualIfCloseStatusCameLate(triggerStatus: OrderStatus) {
        val planFact = buildPlanFact()
        planFact.expectedStatusDatetime = TEST_NOW
            .minusSeconds(1)
        val context = mockLomOrderEventContext(
            existingPlanFacts = mutableListOf(planFact),
            orderStatus = triggerStatus
        )
        processor.lomOrderStatusChanged(context)
        checkPlanFactClosedAs(planFact, PlanFactStatus.NOT_ACTUAL)
    }

    @AfterEach
    fun verifyNoMoreInteractionsAfterTests() {
        verifyNoMoreInteractions(planFactService)
    }

    private fun checkPlanFactClosedAs(planFact: PlanFact, planFactStatus: PlanFactStatus) {
        assertSoftly {
            planFact.planFactStatus shouldBe planFactStatus
            planFact.factStatusDatetime shouldBe null
            planFact.scheduleTime shouldBe TEST_NOW
        }
    }

    private fun buildPlanFact() = PlanFact(
        producerName = "ExpressTrackReceivedPlanFactProcessor",
        expectedStatus = EXPECTED_STATUS,
        planFactStatus = PlanFactStatus.CREATED,
        entityId = 1L,
        entityType = EntityType.LOM_WAYBILL_SEGMENT,
        expectedStatusDatetime = EXPECTED_TIME
    )

    private fun verifyPlanFactSaved() {
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        val quantity = planFactsCaptor.firstValue.count()
        assertSoftly {
            quantity shouldBe 1
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe TEST_DROPSHIP_SEGMENT_ID
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe EXPECTED_TIME
            planFact.producerName shouldBe "ExpressTrackReceivedPlanFactProcessor"
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_TIME
        }
    }

    private fun mockSegmentStatusAddedContext(
        newSegmentStatus: SegmentStatus = SegmentStatus.TRACK_RECEIVED,
        checkpointTime: Instant = TEST_NOW,
        existingPlanFacts: MutableList<PlanFact> = mutableListOf()
    ): LomWaybillStatusAddedContext {
        val ffSegment = WaybillSegment(
            id = TEST_DROPSHIP_SEGMENT_ID,
            segmentType = SegmentType.FULFILLMENT,
            partnerType = PartnerType.DROPSHIP,
            partnerSettings = PartnerSettings(dropshipExpress = true),
        )
        val courierSegment = WaybillSegment(
            id = 2,
            segmentType = SegmentType.COURIER,
            partnerType = PartnerType.DROPSHIP,
            callCourierTime = TEST_NOW,
        )
            .apply { waybillSegmentTags!!.add(WaybillSegmentTag.CALL_COURIER) }
        val newCheckpoint = WaybillSegmentStatusHistory(status = newSegmentStatus, date = checkpointTime)
        ffSegment
            .apply {
                waybillSegmentStatusHistory.add(newCheckpoint)
                newCheckpoint.waybillSegment = this
            }
        val order = joinInOrder(listOf(ffSegment, courierSegment))
            .apply { barcode = BARCODE }
        return LomWaybillStatusAddedContext(newCheckpoint, order, existingPlanFacts)
    }

    private fun mockLomOrderEventContext(
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
        orderStatus: OrderStatus = OrderStatus.CANCELLED,
    ) = LomOrderStatusChangedContext(LomOrder(), orderStatus, existingPlanFacts)

    companion object {
        private const val TEST_DROPSHIP_SEGMENT_ID = 1L
        private val TEST_NOW = Instant.parse("2021-12-21T12:00:00.00Z")
        private val EXPECTED_TIME = TEST_NOW.minus(Duration.ofMinutes(20))
        private val EXPECTED_STATUS = SegmentStatus.TRANSIT_PREPARED.name
        private const val BARCODE = "express-order-1"
    }
}
