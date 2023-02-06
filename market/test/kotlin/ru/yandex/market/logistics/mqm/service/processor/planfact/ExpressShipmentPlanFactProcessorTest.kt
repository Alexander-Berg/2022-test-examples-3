package ru.yandex.market.logistics.mqm.service.processor.planfact

import com.nhaarman.mockitokotlin2.argumentCaptor
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
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.PartnerSettings
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment.ExpressShipmentPlanFactProcessor
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.createDropshipSegment
import ru.yandex.market.logistics.mqm.utils.createExpressOrder
import ru.yandex.market.logistics.mqm.utils.createExpressShipmentSegment
import java.time.Duration
import java.time.Instant

@ExtendWith(MockitoExtension::class)
internal class ExpressShipmentPlanFactProcessorTest : BaseSelfSufficientPlanFactTest() {

    lateinit var processor: ExpressShipmentPlanFactProcessor

    private val settingService = TestableSettingsService()

    @BeforeEach
    fun setUp() {
        processor = ExpressShipmentPlanFactProcessor(
            planFactService,
            settingService,
            clock
        )
    }

    @Test
    @DisplayName("Проверка создания план-факта")
    fun planFactCreationSuccess() {
        val context = mockSegmentStatusAddedContext()
        processor.waybillSegmentStatusAdded(context)

        verifyPlanFactSaved()
    }

    @Test
    @DisplayName("Не создавать план-факт, если ожидаемый чекпоинт уже есть")
    fun createInTimePlanFactIfExpectedStatusAlreadyCame() {
        val context = mockSegmentStatusAddedContext(
            newSegmentStatus = SegmentStatus.TRANSIT_COURIER_RECEIVED,
            checkpointTime = EXPECTED_TIME.minusSeconds(1)
        )
        processor.waybillSegmentStatusAdded(context)
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
            newSegmentStatus = SegmentStatus.TRANSIT_COURIER_RECEIVED,
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
            newSegmentStatus = SegmentStatus.TRANSIT_COURIER_RECEIVED,
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
        names = ["TRANSIT_TRANSMITTED_TO_RECIPIENT", "OUT", "RETURN_ARRIVED", "RETURNED"]
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
        names = ["TRANSIT_TRANSMITTED_TO_RECIPIENT", "OUT", "RETURN_ARRIVED", "RETURNED"]
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
        val context = mockLomOrderStatusChangedContext(
            lomOrder = LomOrder(),
            orderStatus = triggerStatus,
            existingPlanFacts = mutableListOf(planFact)
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
        planFact.expectedStatusDatetime = CURRENT_TIME
            .minusSeconds(1)
        val context = mockLomOrderStatusChangedContext(
            lomOrder = LomOrder(),
            orderStatus = triggerStatus,
            existingPlanFacts = mutableListOf(planFact)
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
            planFact.scheduleTime shouldBe CURRENT_TIME
        }
    }

    private fun buildPlanFact() = PlanFact(
        producerName = "ExpressShipmentPlanFactProcessor",
        expectedStatus = EXPECTED_STATUS,
        planFactStatus = PlanFactStatus.CREATED,
        entityId = TEST_COURIER_SEGMENT_ID,
        entityType = EntityType.LOM_WAYBILL_SEGMENT,
        expectedStatusDatetime = EXPECTED_TIME
    )

    private fun verifyPlanFactSaved() {
        val planFact = captureSavedPlanFact()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe TEST_COURIER_SEGMENT_ID
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe EXPECTED_TIME
            planFact.producerName shouldBe "ExpressShipmentPlanFactProcessor"
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_TIME
        }
    }

    private fun mockSegmentStatusAddedContext(
        newSegmentStatus: SegmentStatus = SegmentStatus.TRANSIT_COURIER_ARRIVED_TO_SENDER,
        checkpointTime: Instant = CURRENT_TIME,
        existingPlanFacts: MutableList<PlanFact> = mutableListOf()
    ): LomWaybillStatusAddedContext {
        val newCheckpoint = WaybillSegmentStatusHistory(status = newSegmentStatus, date = checkpointTime)
        val order = createExpressOrder(
            firstSegment = createDropshipSegment().apply {
                partnerSettings = PartnerSettings(dropshipExpress = true)
            },
            secondSegment = createExpressShipmentSegment().apply {
                id = TEST_COURIER_SEGMENT_ID
                waybillSegmentStatusHistory.add(newCheckpoint)
                newCheckpoint .waybillSegment = this
            }
        )
        return LomWaybillStatusAddedContext(newCheckpoint, order, existingPlanFacts)
    }

    companion object {
        private const val TEST_COURIER_SEGMENT_ID = 1L
        private val EXPECTED_TIME = CURRENT_TIME.plus(Duration.ofMinutes(10))
        private val EXPECTED_STATUS = SegmentStatus.TRANSIT_COURIER_RECEIVED.name
    }
}
