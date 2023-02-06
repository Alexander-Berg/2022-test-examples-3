package ru.yandex.market.logistics.mqm.service.processor.planfact

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.lom.model.enums.CargoType
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.LomItem
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class OrderWithoutRequiredCISPlanFactProcessorTest : AbstractTest() {

    @Mock
    private lateinit var planFactService: PlanFactService

    private val clock = TestableClock()

    lateinit var processor: OrderWithoutRequiredCISPlanFactProcessor

    @BeforeEach
    fun setUp() {
        processor = OrderWithoutRequiredCISPlanFactProcessor(
            planFactService = planFactService,
            clock = clock
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
    @DisplayName("ПФ не создается если нет предыдущего сегмента, либо партнер не DROPSHIP")
    fun notCreateForFF() {
        val context = mockSegmentStatusAddedContext().apply {
            order.waybill.forEach { it.partnerType = PartnerType.DELIVERY }
        }
        processor.waybillSegmentStatusAdded(context)

        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        assertSoftly {
            planFactsCaptor.allValues.size shouldBe 0
        }
    }

    @Test
    @DisplayName("Проверка закрытия план-факта после 120ЧП")
    fun planFactClosedByReadyToShip() {
        val planFact = createPlanFact()
        val context = mockSegmentStatusAddedContext(
            newSegmentStatus = SegmentStatus.TRANSIT_PREPARED,
            newStatusSegmentPartnerType = PartnerType.DROPSHIP,
            existingPlanFacts = mutableListOf(planFact)
        )

        processor.waybillSegmentStatusAdded(context)

        assertSoftly { planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL }
    }

    @Test
    @DisplayName("Проверка закрытия план-факта после отмены заказа")
    fun planFactClosedByCancel() {
        val planFact = createPlanFact()
        val context = mockOrderStatusChangedContext(existingPlanFacts = mutableListOf(planFact))
        processor.lomOrderStatusChanged(context)

        assertSoftly { planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL }
    }

    @Test
    @DisplayName("Не создавать план-факт, если чекпоинт уже есть")
    fun doNotCreatePlanFactIfCheckpointAlreadyExists() {
        val planFact = createPlanFact()
        val context = mockSegmentStatusAddedContext(existingPlanFacts = mutableListOf(planFact))
        planFact.entity.let { it as LomOrder}.waybill.first().apply {
            val readyToShipCheckpoint = WaybillSegmentStatusHistory(
                status = SegmentStatus.IN,
                date = TEST_NOW
            )
            waybillSegmentStatusHistory.add(readyToShipCheckpoint)
            readyToShipCheckpoint.waybillSegment = this
        }

        processor.waybillSegmentStatusAdded(context)

        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        assertSoftly {
            planFactsCaptor.allValues.size shouldBe 0
        }
    }

    private fun createPlanFact() =
        PlanFact(
            entityType = EntityType.LOM_ORDER,
            entityId = LOM_ORDER_ID,
            producerName = "OrderWithoutRequiredCISPlanFactProcessor",
            expectedStatusDatetime = TEST_NOW,
            expectedStatus = UNKNOWN,
            scheduleTime = TEST_NOW
        ).apply { markCreated(TEST_NOW) }

    private fun verifyPlanFactSaved() {
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        val quantity = planFactsCaptor.firstValue.count()
        assertSoftly {
            quantity shouldBe 1
            planFact.entityType shouldBe EntityType.LOM_ORDER
            planFact.entityId shouldBe LOM_ORDER_ID
            planFact.expectedStatus shouldBe UNKNOWN
            planFact.expectedStatusDatetime shouldBe TEST_NOW
            planFact.producerName shouldBe "OrderWithoutRequiredCISPlanFactProcessor"
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe TEST_NOW
        }
    }

    private fun mockOrderStatusChangedContext(
        newOrderStatus: OrderStatus = OrderStatus.CANCELLED,
        existingPlanFacts: MutableList<PlanFact> = mutableListOf()
    ): LomOrderStatusChangedContext {
        val doSegment = WaybillSegment(
            id = 1,
            segmentType = SegmentType.FULFILLMENT,
            partnerType = PartnerType.DROPSHIP
        )
        val dsSegment = WaybillSegment(
            id = 2,
            segmentType = SegmentType.FULFILLMENT,
            partnerType = PartnerType.DELIVERY
        )

        val order = joinInOrder(listOf(doSegment, dsSegment))
            .apply { id = LOM_ORDER_ID }
            .apply { items = listOf(
                LomItem(
                    cargoTypes = setOf(CargoType.CIS_REQUIRED)
                )
            ) }
        existingPlanFacts.forEach { it.apply {
            entity=order
        } }
        return LomOrderStatusChangedContext(order, newOrderStatus, existingPlanFacts)
    }

    private fun mockSegmentStatusAddedContext(
        newSegmentStatus: SegmentStatus = SegmentStatus.IN,
        newStatusSegmentPartnerType: PartnerType = PartnerType.DELIVERY,
        checkpointTime: Instant = TEST_NOW,
        existingPlanFacts: MutableList<PlanFact> = mutableListOf()
    ): LomWaybillStatusAddedContext {
        val doSegment = WaybillSegment(
            id = 1,
            segmentType = SegmentType.FULFILLMENT,
            partnerType = PartnerType.DROPSHIP
        )
        val dsSegment = WaybillSegment(
            id = 2,
            segmentType = SegmentType.FULFILLMENT,
            partnerType = PartnerType.DELIVERY
        )

        val newCheckpoint = WaybillSegmentStatusHistory(status = newSegmentStatus, date = checkpointTime)
        listOf(doSegment, dsSegment).filter { it.partnerType == newStatusSegmentPartnerType }
            .forEach {
                it.waybillSegmentStatusHistory.add(newCheckpoint)
                newCheckpoint.waybillSegment = it
            }

        val order = joinInOrder(listOf(doSegment, dsSegment))
            .apply { id = LOM_ORDER_ID }
            .apply { items = listOf(
                LomItem(
                    cargoTypes = setOf(CargoType.CIS_REQUIRED)
                )
            ) }
        existingPlanFacts.forEach { it.apply {
            entity=order
        } }
        return LomWaybillStatusAddedContext(newCheckpoint, order, existingPlanFacts)
    }

    companion object {
        private val TEST_NOW = Instant.parse("2021-12-21T12:00:00.00Z")
        private const val LOM_ORDER_ID = 1234L
        private const val UNKNOWN = "UNKNOWN"
    }
}
