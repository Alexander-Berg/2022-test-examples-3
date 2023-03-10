package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
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
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.service.FirstCteIntakeService
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.SecondCteIntakeService
import ru.yandex.market.logistics.mqm.service.event.cte.FirstCteIntakeContext
import ru.yandex.market.logistics.mqm.service.event.cte.SecondCteIntakeContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Clock
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class FfReturnPreparingSenderPlanFactProcessorTest {
    @Mock
    private lateinit var planFactService: PlanFactService

    @Mock
    private lateinit var clock: Clock

    @Mock
    private lateinit var firstCteIntakeService: FirstCteIntakeService

    @Mock
    private lateinit var secondCteIntakeService: SecondCteIntakeService

    lateinit var processor: FfReturnPreparingSenderPlanFactProcessor

    private val settingService = TestableSettingsService()

    @BeforeEach
    fun setUp() {
        processor = FfReturnPreparingSenderPlanFactProcessor(
            planFactService = planFactService,
            clock = clock,
            settingService = settingService,
            firstCteIntakeService = firstCteIntakeService,
            secondCteIntakeService = secondCteIntakeService,
        )
    }

    @DisplayName("???????????????? ???????????????? ????????-??????????")
    @Test
    fun planFactCreationSuccess() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        val context = mockFirstCteEventContext()
        processor.intakeInFirstCte(context)
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe 1L
            planFact.expectedStatus shouldBe "RETURN_PREPARING_SENDER"
            planFact.expectedStatusDatetime shouldBe INTAKE_FIRST_CTE_SCHEDULED_TIME
            planFact.producerName shouldBe "FfReturnPreparingSenderPlanFactProcessor"
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe INTAKE_FIRST_CTE_SCHEDULED_TIME
        }
    }

    @DisplayName("???? ?????????????????? ????????-????????, ???????? ???????????? ?? ?????????????????? ??????????????")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["RETURNED", "DELIVERED", "LOST"]
    )
    fun doNotCreatePlanFactIfOrderInFinalStatus(orderStatus: OrderStatus) {
        val context = mockFirstCteEventContext(orderStatus = orderStatus, hasFirstCteIntake = false)
        processor.intakeInFirstCte(context)
        verify(planFactService, never()).save(any())
        verify(firstCteIntakeService, never()).getFirstCteIntakeTime(any())
    }

    @DisplayName("???? ?????????????????? ????????-????????, ???????? ?????? ?????????????????? ?????????????? ???? ??????")
    @Test
    fun doNotCreatePlanFactIfNotExistsFirstCteIntake() {
        val context = mockFirstCteEventContext(hasFirstCteIntake = false)
        processor.intakeInFirstCte(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("???? ?????????????????? ????????-????????, ???????? ???????? ?????????????????? ?????????????? ???? ??????")
    @Test
    fun doNotCreatePlanFactIfExistsSecondCteIntake() {
        val context = mockFirstCteEventContext(hasSecondCteIntake = true)
        processor.intakeInFirstCte(context)
        verify(planFactService, never()).save(any())
    }

    @DisplayName("???? ?????????????????? ????????-????????, ???????? ???????? ???????????????? ????????-????????")
    @Test
    fun doNotCreatePlanFactIfExistsActive() {
        val existingPlanFact = mockPlanFact()
        val context = mockFirstCteEventContext(planFacts = listOf(existingPlanFact), hasFirstCteIntake = false)
        processor.intakeInFirstCte(context)
        verify(planFactService, never()).save(any())
        verify(firstCteIntakeService, never()).getFirstCteIntakeTime(any())
    }

    @DisplayName("?????????????????? ????????-????????, ???????? ???????? ???????????????? ????????-???????? ???? ???? ?????????? ????????????????????")
    @Test
    fun createPlanFactIfExistingActivePlanFactNotFromThisProducer() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = mockPlanFact(producerName = "TestProducer")
        val context = mockFirstCteEventContext(planFacts = listOf(existingPlanFact))
        processor.intakeInFirstCte(context)
        verify(planFactService).save(any())
    }

    @DisplayName("?????????????????????? ????????-???????? ?? NotActual ???????? ???????? ??????????????")
    @Test
    fun setPlanFactNotActualIfFactCameLate() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = mockPlanFact(plan = Instant.parse("2021-12-13T17:59:00.00Z"))
        val context = mockSecondCteEventContext(planFacts = listOf(existingPlanFact))
        processor.intakeInSecondCte(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
            existingPlanFact.factStatusDatetime shouldBe INTAKE_SECOND_CTE_TIME
            existingPlanFact.scheduleTime shouldBe DEFAULT_TIME
        }
    }

    @DisplayName("?????????????????????? ????????-???????? ?? InTime ???????? ???????? ?????????? ??????????????")
    @Test
    fun setPlanFactInTime() {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = mockPlanFact()
        val context = mockSecondCteEventContext(planFacts = listOf(existingPlanFact))
        processor.intakeInSecondCte(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
            existingPlanFact.factStatusDatetime shouldBe INTAKE_SECOND_CTE_TIME
        }
    }

    @DisplayName("?????????????????????? ????????-???????? ?? Expired ???????? ???????????? ???????? ???? ?????????????????????? ???????????????? ???????????? ???? ??????????")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["RETURNED", "DELIVERED", "LOST"]
    )
    fun setPlanFactExpiredIfCloseStatusCameOnTime(triggerStatus: OrderStatus) {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = mockPlanFact(plan = Instant.parse("2021-12-13T16:01:00.00Z"))
        val context = mockLomOrderEventContext(
            planFacts = listOf(existingPlanFact),
            orderStatus = triggerStatus
        )
        processor.lomOrderStatusChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.scheduleTime shouldBe DEFAULT_TIME
        }
    }

    @DisplayName("?????????????????????? ????????-???????? ?? NotActual ???????? ?????????????? ???????? ???? ?????????????????????? ???????????????? ????????????")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["RETURNED", "DELIVERED", "LOST"]
    )
    fun setPlanFactNotActualIfCloseStatusCameLate(triggerStatus: OrderStatus) {
        whenever(clock.instant()).thenReturn(DEFAULT_TIME)
        val existingPlanFact = mockPlanFact(plan = Instant.parse("2021-12-13T15:59:00.00Z"))
        val context = mockLomOrderEventContext(
            planFacts = listOf(existingPlanFact),
            orderStatus = triggerStatus
        )
        processor.lomOrderStatusChanged(context)
        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.scheduleTime shouldBe DEFAULT_TIME
        }
    }

    private fun mockFirstCteEventContext(
        planFacts: List<PlanFact> = listOf(),
        orderStatus: OrderStatus = OrderStatus.RETURNING,
        hasFirstCteIntake: Boolean = true,
        hasSecondCteIntake: Boolean = false,
    ): FirstCteIntakeContext {
        val order = mockOrder(orderStatus)
        if (hasFirstCteIntake) {
            whenever(firstCteIntakeService.getFirstCteIntakeTime(eq(BARCODE))).thenReturn(INTAKE_FIRST_CTE_TIME)
        }
        if (hasSecondCteIntake) {
            whenever(secondCteIntakeService.getSecondCteIntakeTime(eq(BARCODE))).thenReturn(DEFAULT_TIME)
        }
        return FirstCteIntakeContext(
            order = order,
            intakeTime = INTAKE_FIRST_CTE_TIME,
            orderPlanFacts = planFacts,
        )
    }

    private fun mockSecondCteEventContext(
        planFacts: List<PlanFact> = listOf(),
    ): SecondCteIntakeContext {
        val order = mockOrder()
        return SecondCteIntakeContext(
            order = order,
            intakeTime = INTAKE_SECOND_CTE_TIME,
            orderPlanFacts = planFacts,
        )
    }

    private fun mockLomOrderEventContext(
        planFacts: List<PlanFact> = listOf(),
        orderStatus: OrderStatus = OrderStatus.CANCELLED,
    ): LomOrderStatusChangedContext {
        val order = mockOrder(orderStatus = orderStatus)
        return LomOrderStatusChangedContext(
            order = order,
            orderStatus = orderStatus,
            orderPlanFacts = planFacts,
        )
    }

    private fun mockPlanFact(
        plan: Instant = Instant.parse("2021-12-13T18:01:00.00Z"),
        producerName: String = FfReturnPreparingSenderPlanFactProcessor::class.simpleName!!
    ): PlanFact {
        return PlanFact(
            producerName = producerName,
            expectedStatusDatetime = plan
        ).markCreated(DEFAULT_TIME)
            .apply { entity = mockOrder().waybill.first() }
    }

    private fun mockOrder(orderStatus: OrderStatus = OrderStatus.RETURNING): LomOrder {
        val firstSegment = WaybillSegment(id = 1, segmentType = SegmentType.FULFILLMENT)
            .apply { waybillSegmentTags!!.add(WaybillSegmentTag.RETURN) }
        val secondSegment = WaybillSegment()
        val order = joinInOrder(listOf(firstSegment, secondSegment))
            .apply {
                id = 1
                status = orderStatus
                barcode = BARCODE
            }
        return order
    }

    companion object {
        private val DEFAULT_TIME = Instant.parse("2021-12-13T16:00:00.00Z")
        private val INTAKE_FIRST_CTE_TIME = Instant.parse("2021-12-13T17:00:00.00Z")
        private val INTAKE_FIRST_CTE_SCHEDULED_TIME = Instant.parse("2021-12-15T09:00:00.00Z")
        private val INTAKE_SECOND_CTE_TIME = Instant.parse("2021-12-13T18:00:00.00Z")
        private const val BARCODE = "testBarcode"
    }
}
