package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

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
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.service.FirstCteIntakeService
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.event.cte.FirstCteIntakeContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Duration
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class DsCteIntakeReturnPlanFactProcessorTest {

    @Mock
    private lateinit var planFactService: PlanFactService

    @Mock
    private lateinit var firstCteIntakeService: FirstCteIntakeService

    private val clock = TestableClock()

    lateinit var processor: DsCteIntakeReturnPlanFactProcessor

    private val settingService = TestableSettingsService()

    @BeforeEach
    fun setUp() {
        processor = DsCteIntakeReturnPlanFactProcessor(
            settingService = settingService,
            clock = clock,
            planFactService = planFactService,
            firstCteIntakeService = firstCteIntakeService
        )
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE)
    }

    @Test
    @DisplayName("Проверка создания план-факта")
    fun planFactCreationSuccess() {
        val context = mockSegmentStatusAddedContext()
        processor.waybillSegmentStatusAdded(context)

        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe 1L
            planFact.expectedStatus shouldBe "RETURN_ARRIVED"
            planFact.expectedStatusDatetime shouldBe EXPECTED_TIME
            planFact.producerName shouldBe "DsCteIntakeReturnPlanFactProcessor"
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_TIME
        }
    }


    @DisplayName("План-факт не создается если чекпоинт пришел не на втором сегменте")
    @Test
    fun notCreateIfReturnedNotOnSecondSegment() {
        val context = mockSegmentStatusAddedContext(segmentIndex = 0)
        processor.waybillSegmentStatusAdded(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("План-факт не создается если последний FF сегмент имеет тип партнера дропшип")
    @Test
    fun notCreateIfLastFFSegmentHasDropshipType() {
        val context = mockSegmentStatusAddedContext(partnerType = PartnerType.DROPSHIP)
        processor.waybillSegmentStatusAdded(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("План-факт не создается если пришел не RETURNED чекпоинт на втором сегменте")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["RETURNED"]
    )
    fun notCreateIfNotReturnedSegment(segmentStatus: SegmentStatus) {
        val context = mockSegmentStatusAddedContext(newSegmentStatus = segmentStatus)
        processor.waybillSegmentStatusAdded(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("План-факт не создается если есть первичная приемка на CTE")
    @Test
    fun notCreateIfFirstCteIntake() {
        whenever(firstCteIntakeService.getFirstCteIntakeTime(BARCODE)).thenReturn(FIXED_TIME)
        val context = mockSegmentStatusAddedContext()
        processor.waybillSegmentStatusAdded(context)
        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("Если существует план-факт с таким-же планом, то новый пф не сохраняется")
    fun doNotSaveNewIfExistsSame() {
        val oldPlanFact = createPlanFact()
        val context = mockSegmentStatusAddedContext(existingPlanFacts = mutableListOf(oldPlanFact))
        processor.waybillSegmentStatusAdded(context)
        verify(planFactService, never()).save(any())
        oldPlanFact.planFactStatus shouldBe PlanFactStatus.CREATED
    }

    @Test
    @DisplayName("Если существует план-факт с другим планом, то он помечается как outdated")
    fun markOldPlanFactOutdated() {
        val oldPlanFact = createPlanFact(EXPECTED_TIME.minusSeconds(1))
        val context = mockSegmentStatusAddedContext(existingPlanFacts = mutableListOf(oldPlanFact))
        processor.waybillSegmentStatusAdded(context)
        verify(planFactService).save(any())
        oldPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED
    }

    @DisplayName("Проставлять план-факт в InTime если факт успел вовремя")
    @Test
    fun setPlanFactInTime() {
        val existingPlanFact = createPlanFact()
        val context = mockFirstCteIntakeContext(existingPlanFacts = mutableListOf(existingPlanFact))
        processor.intakeInFirstCte(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
            existingPlanFact.factStatusDatetime shouldBe CTE_INTAKE_TIME
        }
    }

    @DisplayName("Проставлять план-факт в NotActual если факт опоздал")
    @Test
    fun setPlanFactNotActualIfFactCameLate() {
        val existingPlanFact = createPlanFact(EXPECTED_TIME.minus(Duration.ofHours(2)))
        val context = mockFirstCteIntakeContext(existingPlanFacts = mutableListOf(existingPlanFact))
        processor.intakeInFirstCte(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
            existingPlanFact.factStatusDatetime shouldBe CTE_INTAKE_TIME
            existingPlanFact.scheduleTime shouldBe FIXED_TIME
        }
    }

    @DisplayName("Проставлять план-факт в Expired если пришел один из закрывающих статусов заказа до плана")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["RETURNED", "DELIVERED", "LOST"]
    )
    fun setPlanFactExpiredIfCloseStatusCameOnTime(triggerStatus: OrderStatus) {
        val existingPlanFact = createPlanFact(expectedTime = EXPECTED_TIME)
        val context = mockLomOrderEventContext(
            existingPlanFacts = mutableListOf(existingPlanFact),
            orderStatus = triggerStatus
        )
        processor.lomOrderStatusChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.scheduleTime shouldBe FIXED_TIME
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
        val existingPlanFact = createPlanFact(expectedTime = FIXED_TIME.minusSeconds(1))
        val context = mockLomOrderEventContext(
            existingPlanFacts = mutableListOf(existingPlanFact),
            orderStatus = triggerStatus
        )
        processor.lomOrderStatusChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.scheduleTime shouldBe FIXED_TIME
        }
    }

    private fun mockSegmentStatusAddedContext(
        newSegmentStatus: SegmentStatus = SegmentStatus.RETURNED,
        segmentIndex: Int = 1,
        checkpointTime: Instant = FIXED_TIME,
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
        partnerType: PartnerType = PartnerType.FULFILLMENT
    ): LomWaybillStatusAddedContext {
        val firstSegment = WaybillSegment(
            id = 1,
            segmentType = SegmentType.FULFILLMENT,
            partnerType = partnerType
        )
            .apply { waybillSegmentTags!!.add(WaybillSegmentTag.RETURN) }
        val secondSegment = WaybillSegment(id = 2)
        val order = joinInOrder(listOf(firstSegment, secondSegment))
            .apply { barcode = BARCODE }
        val newCheckpoint = WaybillSegmentStatusHistory(status = newSegmentStatus, date = checkpointTime)
        order.waybill[segmentIndex]
            .apply {
                waybillSegmentStatusHistory.add(newCheckpoint)
                newCheckpoint.waybillSegment = this
            }

        return LomWaybillStatusAddedContext(newCheckpoint, order, existingPlanFacts)
    }

    private fun mockFirstCteIntakeContext(
        existingPlanFacts: MutableList<PlanFact> = mutableListOf()
    ): FirstCteIntakeContext {
        return FirstCteIntakeContext(LomOrder(), CTE_INTAKE_TIME, existingPlanFacts)
    }

    private fun mockLomOrderEventContext(
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
        orderStatus: OrderStatus = OrderStatus.CANCELLED,
    ): LomOrderStatusChangedContext {
        return LomOrderStatusChangedContext(LomOrder(), orderStatus, existingPlanFacts)
    }

    private fun createPlanFact(expectedTime: Instant = EXPECTED_TIME): PlanFact {
        return PlanFact(
            entityId = 1,
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            planFactStatus = PlanFactStatus.CREATED,
            expectedStatusDatetime = expectedTime,
            producerName = "DsCteIntakeReturnPlanFactProcessor"
        )
    }

    companion object {
        private val FIXED_TIME = Instant.parse("2021-12-21T12:00:00.00Z")
        private val EXPECTED_TIME = Instant.parse("2021-12-29T09:00:00.00Z")
        private val CTE_INTAKE_TIME = Instant.parse("2021-12-29T08:00:00.00Z")
        private const val BARCODE = "testBarcode"
    }
}
