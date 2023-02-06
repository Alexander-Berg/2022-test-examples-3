package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
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
import ru.yandex.market.logistics.mqm.entity.additionaldata.RddPlanFactAdditionalData
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.embedded.WaybillShipment
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderCombinatorRouteWasUpdatedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.toInstant
import ru.yandex.market.logistics.mqm.utils.toLocalDate
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint

@ExtendWith(MockitoExtension::class)
class DropshipDropoffScShippedRddPlanFactProcessorTest {
    private lateinit var processor: DropshipDropoffScShippedRddPlanFactProcessor

    private val settingsService = TestableSettingsService()

    private val clock = TestableClock()

    @Mock
    private lateinit var planFactService: PlanFactService

    @BeforeEach
    fun setUp() {
        processor = DropshipDropoffScShippedRddPlanFactProcessor(
            settingsService,
            clock,
            planFactService,
        )
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE)
    }

    // Создание ПФ.

    @Test
    @DisplayName("Проверка создания план-факта")
    fun planFactCreationSuccess() {
        val firstSegment = createFirstSegment()
        val secondSegment = createSecondSegment()
        val lomOrder = createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment)
        val newCheckpoint = createNewCheckpoint(waybillSegment = firstSegment)

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        val planFact = getSavedPlanFact()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe secondSegment.id
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe EXPECTED_TIME
            planFact.producerName shouldBe PRODUCER_NAME
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_TIME
            planFact.getAdditionalDataData()!!.recalculateRddFrom shouldBe EXPECTED_TIME_FOR_RECALCULATION
        }
    }

    // Проверки обработки при существующих ПФ.

    @Test
    @DisplayName("Если существует план-факт с таким-же планом, то новый план-факт не сохраняется")
    fun doNotSaveNewIfExistsSame() {
        val firstSegment = createFirstSegment()
        val secondSegment = createSecondSegment()
        val lomOrder = createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment)
        val newCheckpoint = createNewCheckpoint(waybillSegment = firstSegment)
        val existingPlanFact = createPlanFact(waybillSegment = secondSegment)
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockSegmentStatusAddedContext(
            lomOrder = lomOrder,
            newCheckpoint = newCheckpoint,
            existingPlanFacts = existingPlanFacts,
        )

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.CREATED
    }

    @Test
    @DisplayName("Если существует план-факт с другим планом, то он помечается как OUTDATED")
    fun markOldPlanFactOutdated() {
        val firstSegment = createFirstSegment()
        val secondSegment = createSecondSegment()
        val lomOrder = createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment)
        val newCheckpoint = createNewCheckpoint(waybillSegment = firstSegment)
        val existingPlanFact = createPlanFact(
            waybillSegment = secondSegment,
            expectedTime = EXPECTED_TIME.minusSeconds(1),
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockSegmentStatusAddedContext(
            lomOrder = lomOrder,
            newCheckpoint = newCheckpoint,
            existingPlanFacts = existingPlanFacts,
        )

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService).save(any())
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED
    }

    @Test
    @DisplayName("Проверка обновления план-факта, если изменился route")
    fun planFactCreationSuccessIfRouteChanged() {
        val lomOrder = createLomOrder()
        val existingPlanFact = createPlanFact(
            waybillSegment = lomOrder.waybill[1],
            expectedTime = EXPECTED_TIME.minusSeconds(1),
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockLomOrderCombinatorRouteWasUpdatedContext(
            lomOrder = lomOrder,
            existingPlanFacts = existingPlanFacts,
        )

        processor.combinatorRouteWasUpdated(context)

        val planFact = getSavedPlanFact()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe lomOrder.waybill[1].id
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe EXPECTED_TIME
            planFact.producerName shouldBe PRODUCER_NAME
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_TIME
            planFact.getAdditionalDataData()!!.recalculateRddFrom shouldBe EXPECTED_TIME_FOR_RECALCULATION
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED
        }
    }

    // Проверки при ЧП.

    @DisplayName("Проставлять план-факт в IN_TIME, если факт успел вовремя")
    @Test
    fun setPlanFactInTime() {
        val secondSegment = createSecondSegment(isDropoff = false)
        val lomOrder = createLomOrder(secondSegment = secondSegment)
        val newCheckpoint = createNewCheckpoint(waybillSegment = secondSegment, newSegmentStatus = SegmentStatus.IN)
        val existingPlanFact = createPlanFact(waybillSegment = secondSegment)
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockSegmentStatusAddedContext(
            lomOrder = lomOrder,
            newCheckpoint = newCheckpoint,
            existingPlanFacts = existingPlanFacts,
        )

        processor.waybillSegmentStatusAdded(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
    }

    @DisplayName("Закрывать план-факт как NOT_ACTUAL, если факт опоздал")
    @Test
    fun setPlanFactNotActual() {
        val secondSegment = createSecondSegment(isDropoff = false)
        val lomOrder = createLomOrder(secondSegment = secondSegment)
        val newCheckpoint = createNewCheckpoint(
            waybillSegment = secondSegment,
            newSegmentStatus = SegmentStatus.IN,
            newCheckpointTime = EXPECTED_TIME.plus(Duration.ofSeconds(1)),
        )
        val existingPlanFact = createPlanFact(
            waybillSegment = secondSegment,
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockSegmentStatusAddedContext(
            lomOrder = lomOrder,
            newCheckpoint = newCheckpoint,
            existingPlanFacts = existingPlanFacts,
        )

        processor.waybillSegmentStatusAdded(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
    }

    @DisplayName("Закрывать план-факт как NOT_ACTUAL, если факт пришел со следующим статусом")
    @Test
    fun closePlanFactIfNextSegmentStatus() {
        val secondSegment = createSecondSegment(isDropoff = false)
        val lomOrder = createLomOrder(secondSegment = secondSegment)
        val newCheckpoint = createNewCheckpoint(
            waybillSegment = secondSegment,
            newSegmentStatus = SegmentStatus.OUT,
            newCheckpointTime = EXPECTED_TIME.plus(Duration.ofSeconds(1)),
        )
        val existingPlanFact = createPlanFact(
            waybillSegment = secondSegment,
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockSegmentStatusAddedContext(
            lomOrder = lomOrder,
            newCheckpoint = newCheckpoint,
            existingPlanFacts = existingPlanFacts,
        )

        processor.waybillSegmentStatusAdded(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
    }

    @DisplayName("Закрывать план-факт как NOT_ACTUAL, если на следующий сегмент пришел закрывающий статус после плана")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["IN", "OUT"]
    )
    fun closePlanFactAsNotActualOnCloseNextSegmentStatusAfterPlan(segmentStatus: SegmentStatus) {
        val secondSegment = createSecondSegment(isDropoff = false)
        val thirdSegment = createThirdSegment()
        val lomOrder = createLomOrder(secondSegment = secondSegment, thirdSegment = thirdSegment)
        val newCheckpoint = createNewCheckpoint(
            waybillSegment = thirdSegment,
            newSegmentStatus = segmentStatus,
            newCheckpointTime = EXPECTED_TIME.plus(Duration.ofSeconds(1)),
        )
        val existingPlanFact = createPlanFact(
            waybillSegment = secondSegment,
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockSegmentStatusAddedContext(
            lomOrder = lomOrder,
            newCheckpoint = newCheckpoint,
            existingPlanFacts = existingPlanFacts,
        )

        processor.waybillSegmentStatusAdded(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
    }

    @DisplayName("Закрывать план-факт как EXPIRED, если на следующий сегмент пришел закрывающий статус до плана")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["OUT"]
    )
    fun closePlanFactAsExpiredOnCloseNextSegmentStatusBeforePlan(segmentStatus: SegmentStatus) {
        val secondSegment = createSecondSegment(isDropoff = false)
        val thirdSegment = createThirdSegment()
        val lomOrder = createLomOrder(secondSegment = secondSegment, thirdSegment = thirdSegment)
        val newCheckpoint = createNewCheckpoint(
            waybillSegment = thirdSegment,
            newSegmentStatus = segmentStatus,
            newCheckpointTime = EXPECTED_TIME.minus(Duration.ofSeconds(1)),
        )
        val existingPlanFact = createPlanFact(
            waybillSegment = secondSegment,
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockSegmentStatusAddedContext(
            lomOrder = lomOrder,
            newCheckpoint = newCheckpoint,
            existingPlanFacts = existingPlanFacts,
        )

        processor.waybillSegmentStatusAdded(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
    }

    @DisplayName("Не закрывать план-факт, если есть статусы не для закрытия на следующих сегментах")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["STARTED", "TRACK_RECEIVED", "PENDING", "INFO_RECEIVED"]
    )
    fun notClosePlanFactAsExpiredOnCloseNextSegmentsCp(segmentStatus: SegmentStatus) {
        val secondSegment = createSecondSegment(isDropoff = false)
        val thirdSegment = createThirdSegment()
        val lomOrder = createLomOrder(secondSegment = secondSegment, thirdSegment = thirdSegment)
        val newCheckpoint = createNewCheckpoint(
            waybillSegment = thirdSegment,
            newSegmentStatus = segmentStatus,
            newCheckpointTime = EXPECTED_TIME.minus(Duration.ofSeconds(1)),
        )
        val existingPlanFact = createPlanFact(
            waybillSegment = secondSegment,
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockSegmentStatusAddedContext(
            lomOrder = lomOrder,
            newCheckpoint = newCheckpoint,
            existingPlanFacts = existingPlanFacts,
        )

        processor.waybillSegmentStatusAdded(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.CREATED
    }

    // Проверки создания ПФ, при различных условиях.

    @DisplayName("ПФ не создается, если 1-й сегмент не SORTING_CENTER")
    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["SORTING_CENTER"]
    )
    fun planFactNotCreateIfSegmentNotSc(partnerType: PartnerType) {
        val firstSegment = createFirstSegment()
        val secondSegment = createSecondSegment(partnerType = partnerType, isDropoff = false)
        val lomOrder = createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment)
        val newCheckpoint = createNewCheckpoint(waybillSegment = firstSegment)

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
    }

    @DisplayName("ПФ не создается, если 0-й сегмент не DROPSHIP")
    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["DROPSHIP"]
    )
    fun planFactNotCreateIfNotDr(partnerType: PartnerType) {
        val firstSegment = createFirstSegment(partnerType = partnerType)
        val secondSegment = createSecondSegment(isDropoff = false)
        val lomOrder = createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment)
        val newCheckpoint = createNewCheckpoint(waybillSegment = firstSegment)

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("ПФ не создается, если не Dropoff")
    fun planFactNotCreateIfDropoff() {
        val firstSegment = createFirstSegment()
        val secondSegment = createSecondSegment(isDropoff = false)
        val lomOrder = createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment)
        val newCheckpoint = createNewCheckpoint(waybillSegment = firstSegment)

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
    }

    @DisplayName("ПФ не создается, если 3-й сегмент MARKET_COURIER")
    @ParameterizedTest
    @EnumSource(
        value = PartnerSubtype::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["MARKET_COURIER"]
    )
    fun planFactNotCreateIfMc(partnerSubtype: PartnerSubtype) {
        val firstSegment = createFirstSegment()
        val secondSegment = createSecondSegment(isDropoff = false)
        val thirdSegment = createThirdSegment(partnerSubtype = partnerSubtype)
        val lomOrder =
            createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment, thirdSegment = thirdSegment)
        val newCheckpoint = createNewCheckpoint(waybillSegment = firstSegment)

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("ПФ не создается, если нет shipmentDate")
    fun planFactNotCreateIfNoShipmentDate() {
        val firstSegment = createFirstSegment()
        val secondSegment = createSecondSegment(shipmentDate = null)
        val lomOrder = createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment)
        val newCheckpoint = createNewCheckpoint(waybillSegment = firstSegment)

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("ПФ не создается, если на сегментах после сегмента для ПФ есть один из закрывающихся статусов")
    fun planFactNotCreateIfNextSegmentsWithCloseStatuses() {
        val firstSegment = createFirstSegment()
        val secondSegment = createSecondSegment()
        val lomOrder = createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment)
        val newCheckpoint = createNewCheckpoint(waybillSegment = firstSegment)
        addWaybillSegmentCheckpoint(waybillSegment = secondSegment)

        val context = mockSegmentStatusAddedContext(lomOrder = lomOrder, newCheckpoint = newCheckpoint)

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
    }

    // Проверки при обработки заказа.

    @DisplayName("Проставлять план-факт в EXPIRED, если пришел один из закрывающих статусов заказа до плана")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["RETURNED", "DELIVERED", "LOST", "RETURNING"]
    )
    fun setPlanFactExpiredIfCloseStatusCameOnTime(triggerStatus: OrderStatus) {
        val context = mockLomOrderEventContext(
            orderStatus = triggerStatus,
        )
        processor.lomOrderStatusChanged(context)
        val existingPlanFact = context.getPlanFactsFromProcessor(PRODUCER_NAME).single()
        checkPlanFactClosedAs(existingPlanFact, PlanFactStatus.EXPIRED)
    }

    @DisplayName("Проставлять план-факт в NOT_ACTUAL, если опоздал один из закрывающих статусов заказа")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["RETURNED", "DELIVERED", "LOST", "RETURNING"]
    )
    fun setPlanFactNotActualIfCloseStatusCameLate(triggerStatus: OrderStatus) {
        val context = mockLomOrderEventContext(
            orderStatus = triggerStatus,
            expectedTime = FIXED_TIME.minusSeconds(1),
        )
        processor.lomOrderStatusChanged(context)
        val existingPlanFact = context.getPlanFactsFromProcessor(PRODUCER_NAME).single()
        checkPlanFactClosedAs(existingPlanFact, PlanFactStatus.NOT_ACTUAL)
    }

    // Вспомогательные методы.

    private fun getSavedPlanFact(): PlanFact {
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        verify(planFactService).save(planFactsCaptor.capture())
        return planFactsCaptor.firstValue.single()
    }

    private fun mockSegmentStatusAddedContext(
        lomOrder: LomOrder = createLomOrder(),
        newCheckpoint: WaybillSegmentStatusHistory,
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
    ) = LomWaybillStatusAddedContext(newCheckpoint, lomOrder, existingPlanFacts)

    private fun createLomOrder(
        firstSegment: WaybillSegment = createFirstSegment(),
        secondSegment: WaybillSegment = createSecondSegment(),
        thirdSegment: WaybillSegment = createThirdSegment(),
    ) = joinInOrder(listOf(firstSegment, secondSegment, thirdSegment)).apply { id = 1 }

    private fun createNewCheckpoint(
        newSegmentStatus: SegmentStatus = SegmentStatus.PENDING,
        newCheckpointTime: Instant = FIXED_TIME,
        waybillSegment: WaybillSegment,
    ): WaybillSegmentStatusHistory {
        val newCheckpoint = WaybillSegmentStatusHistory(status = newSegmentStatus, date = newCheckpointTime)
        waybillSegment.apply {
            waybillSegmentStatusHistory.add(newCheckpoint)
            newCheckpoint.waybillSegment = this
        }
        return newCheckpoint
    }

    private fun addWaybillSegmentCheckpoint(
        newSegmentStatus: SegmentStatus = SegmentStatus.IN,
        waybillSegment: WaybillSegment,
    ) = writeWaybillSegmentCheckpoint(waybillSegment, newSegmentStatus, NEW_CHECKPOINT_TIME)

    private fun createFirstSegment(
        partnerType: PartnerType = PartnerType.DROPSHIP,
    ) =
        WaybillSegment(
            id = 51,
            partnerType = partnerType,
            segmentType = SegmentType.MOVEMENT,
        )

    private fun createSecondSegment(
        partnerType: PartnerType = PartnerType.SORTING_CENTER,
        externalId: String? = "123",
        isDropoff: Boolean = true,
        shipmentDate: LocalDate? = LocalDate.ofInstant(DROPSHIP_TIME, DateTimeUtils.MOSCOW_ZONE),
    ): WaybillSegment {
        val waybillSegment = WaybillSegment(
            id = 52,
            partnerId = 2,
            partnerType = partnerType,
            externalId = externalId,
            shipment = WaybillShipment(
                date = shipmentDate,
            ),
        )
        if (isDropoff) {
            waybillSegment.partnerType = PartnerType.DELIVERY
            waybillSegment.segmentType = SegmentType.SORTING_CENTER
        }
        return waybillSegment
    }

    private fun createThirdSegment(
        partnerType: PartnerType = PartnerType.SORTING_CENTER,
        partnerSubtype: PartnerSubtype? = null,
    ) =
        WaybillSegment(
            id = 53,
            partnerType = partnerType,
            partnerSubtype = partnerSubtype,
        )

    private fun createPlanFact(
        expectedTime: Instant = EXPECTED_TIME,
        waybillSegment: WaybillSegment,
    ): PlanFact {
        return PlanFact(
            id = 102,
            entityId = waybillSegment.id,
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            planFactStatus = PlanFactStatus.CREATED,
            expectedStatusDatetime = expectedTime,
            expectedStatus = SegmentStatus.IN.name,
            producerName = PRODUCER_NAME,
        ).apply { entity = waybillSegment }
    }

    private fun mockLomOrderEventContext(
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
        orderStatus: OrderStatus = OrderStatus.CANCELLED,
        expectedTime: Instant = EXPECTED_TIME,
    ): LomOrderStatusChangedContext {
        val secondSegment = createSecondSegment(isDropoff = false)
        val lomOrder = createLomOrder(secondSegment = secondSegment)
        existingPlanFacts.add(createPlanFact(expectedTime = expectedTime, waybillSegment = secondSegment))
        return LomOrderStatusChangedContext(lomOrder, orderStatus, existingPlanFacts)
    }

    private fun mockLomOrderCombinatorRouteWasUpdatedContext(
        lomOrder: LomOrder = createLomOrder(),
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
    ) = LomOrderCombinatorRouteWasUpdatedContext(lomOrder, null, existingPlanFacts)

    private fun checkPlanFactClosedAs(planFact: PlanFact, planFactStatus: PlanFactStatus) {
        assertSoftly {
            planFact.planFactStatus shouldBe planFactStatus
            planFact.factStatusDatetime shouldBe null
            planFact.scheduleTime shouldBe FIXED_TIME
        }
    }

    private fun PlanFact.getAdditionalDataData(): RddPlanFactAdditionalData? {
        return getData(RddPlanFactAdditionalData::class.java)
    }

    companion object {
        private val PLAN_FACT_TIMEOUT: Duration = Duration.ofHours(2)
        private val BASE_TIMEOUT: Duration = Duration.ofDays(1)
        private val FIXED_TIME = Instant.parse("2021-01-03T15:00:00.00Z")
        private val DROPSHIP_TIME = Instant.parse("2021-01-03T15:00:00.00Z")
        private val EXPECTED_TIME = FIXED_TIME.toLocalDate().atStartOfDay().toInstant().plus(BASE_TIMEOUT).plus(PLAN_FACT_TIMEOUT)
        private const val PRODUCER_NAME = "DropshipDropoffScShippedRddPlanFactProcessor"
        private const val EXPECTED_STATUS = "IN"
        private val NEW_CHECKPOINT_TIME = Instant.parse("2021-01-03T15:00:00.00Z")
        private val EXPECTED_TIME_FOR_RECALCULATION = FIXED_TIME.toLocalDate().atStartOfDay().toInstant().plus(BASE_TIMEOUT)
    }
}
