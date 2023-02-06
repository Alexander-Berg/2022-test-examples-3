package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
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
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(MockitoExtension::class)
class CourierFinalStatusPlanFactProcessorTest: AbstractTest() {
    @Mock
    private lateinit var planFactService: PlanFactService

    private val clock = TestableClock()

    lateinit var processor: CourierFinalStatusPlanFactProcessor

    @BeforeEach
    fun setUp() {
        processor = CourierFinalStatusPlanFactProcessor(planFactService, clock)
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Успешное создание план-факта для МК")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["BERU", "YANDEX_GO"]
    )
    fun createPlanFactForMk(platformClient: PlatformClient) {
        val context = createLomWaybillStatusAddedContext(platformClient = platformClient)
        processor.waybillSegmentStatusAdded(context)
        verifyPlanFactSaved()
    }

    @DisplayName("Успешное создание план-факта для контрактной доставки")
    @Test
    fun createPlanFactForContractDelivery() {
        val context = createLomWaybillStatusAddedContext(
            partnerSubtype = PartnerSubtype.PARTNER_CONTRACT_DELIVERY
        )
        processor.waybillSegmentStatusAdded(context)
        verifyPlanFactSaved(FIXED_TIME.plus(3, ChronoUnit.DAYS))
    }

    @DisplayName("Не создавать план-факт если пришел не 49 чекпоинт")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["TRANSIT_TRANSMITTED_TO_RECIPIENT"]
    )
    fun doNotCreatePlanFactIfCheckpointIsInvalid(segmentStatus: SegmentStatus) {
        val context = createLomWaybillStatusAddedContext(
            checkpoint = segmentStatus
        )
        processor.waybillSegmentStatusAdded(context)
        verifyNoMoreInteractions(planFactService)
    }

    @DisplayName("Не создавать план-факт в случае OnDemand или Express заказа")
    @ParameterizedTest
    @EnumSource(
        value = WaybillSegmentTag::class,
        names = ["ON_DEMAND", "CALL_COURIER"]
    )
    fun doNotCreatePlanFactIfOnDemandOrExpress(waybillSegmentTag: WaybillSegmentTag) {
        val context = createLomWaybillStatusAddedContext()
            .apply {
                this.order.waybill[1].waybillSegmentTags = mutableSetOf(waybillSegmentTag)
            }
        processor.waybillSegmentStatusAdded(context)
        verifyNoMoreInteractions(planFactService)
    }

    @DisplayName("Не создавать план-факт если подтип партнера не МК или контрактная доставка")
    @ParameterizedTest
    @EnumSource(
        value = PartnerSubtype::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["MARKET_COURIER", "PARTNER_CONTRACT_DELIVERY"]
    )
    fun doNotCreatePlanFactIfPartnerSubtypeIsInvalid(partnerSubtype: PartnerSubtype) {
        val context = createLomWaybillStatusAddedContext(
            partnerSubtype = partnerSubtype
        )
        processor.waybillSegmentStatusAdded(context)
        verifyNoMoreInteractions(planFactService)
    }


    @DisplayName("Пометить существующий план-факт как OUTDATED и сохранить новый")
    @Test
    fun markOutdatedAndSaveNewPlanFact() {
        val oldPlanFact = createPlanFact()
            .apply {
                expectedStatusDatetime = FIXED_TIME.minusSeconds(10)
            }
        val context = createLomWaybillStatusAddedContext(
            planFacts = listOf(oldPlanFact)
        )
        processor.waybillSegmentStatusAdded(context)
        verifyPlanFactSaved()
        assertSoftly {
            oldPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED
        }
    }

    @DisplayName("Пометить план-факт IN_TIME")
    @Test
    fun markInTime() {
        val planFact = createPlanFact()
        val context = createLomWaybillStatusAddedContext(
            checkpoint = EXPECTED_STATUS,
            planFacts = listOf(planFact)
        )
        processor.waybillSegmentStatusAdded(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
        }
    }

    @DisplayName("Пометить план-факт NOT_ACTUAL")
    @Test
    fun markNotActual() {
        val planFact = createPlanFact()
        val context = createLomWaybillStatusAddedContext(
            checkpoint = EXPECTED_STATUS,
            checkpointTime = DEFAULT_MK_EXPECTED_TIME.plusSeconds(10),
            planFacts = listOf(planFact)
        )
        processor.waybillSegmentStatusAdded(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    @DisplayName("Пометить план-факт EXPIRED по приходу финального статуса заказа до плана")
    @ParameterizedTest
    @EnumSource(
        OrderStatus::class,
        names = ["RETURNED", "LOST", "DELIVERED", "RETURNING"]
    )
    fun markExpiredOnCloseOrderStatus(orderStatus: OrderStatus) {
        val planFact = createPlanFact()
        val context = createLomOrderStatusChangedContext(
            orderStatus = orderStatus,
            planFacts = listOf(planFact)
        )
        processor.lomOrderStatusChanged(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
        }
    }

    @DisplayName("Пометить план-факт NOT_ACTUAL по приходу финального статуса заказа после плана")
    @ParameterizedTest
    @EnumSource(
        OrderStatus::class,
        names = ["RETURNED", "LOST", "DELIVERED", "RETURNING"]
    )
    fun markNotActualOnCloseOrderStatus(orderStatus: OrderStatus) {
        val planFact = createPlanFact()
            .apply {
                expectedStatusDatetime = FIXED_TIME.minusSeconds(10)
            }
        val context = createLomOrderStatusChangedContext(
            orderStatus = orderStatus,
            planFacts = listOf(planFact)
        )
        processor.lomOrderStatusChanged(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    private fun verifyPlanFactSaved(expectedTime: Instant = DEFAULT_MK_EXPECTED_TIME) {
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.expectedStatus shouldBe EXPECTED_STATUS.name
            planFact.expectedStatusDatetime shouldBe expectedTime
            planFact.producerName shouldBe processor.producerName()
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe expectedTime
        }
    }

    private fun createLomWaybillStatusAddedContext(
        checkpoint: SegmentStatus = SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT,
        checkpointTime: Instant = FIXED_TIME,
        partnerSubtype: PartnerSubtype = PartnerSubtype.MARKET_COURIER,
        planFacts: List<PlanFact> = listOf(),
        platformClient: PlatformClient = PlatformClient.BERU,
    ): LomWaybillStatusAddedContext {
        val newCheckpoint = WaybillSegmentStatusHistory(status = checkpoint, date = checkpointTime)
        val previousSegment = WaybillSegment(id = 1)
        val lastMileSegment = WaybillSegment(
            id = 2,
            partnerType = PartnerType.DELIVERY,
            segmentType = SegmentType.COURIER,
            partnerSubtype = partnerSubtype
        ).apply {
            waybillSegmentStatusHistory = mutableSetOf(newCheckpoint)
            newCheckpoint.waybillSegment = this
            planFacts.forEach { pf -> pf.entity = this }
        }
        val order = joinInOrder(listOf(previousSegment, lastMileSegment))
            .apply { platformClientId = platformClient.id }
        return LomWaybillStatusAddedContext(newCheckpoint, order, planFacts)
    }

    private fun createLomOrderStatusChangedContext(
        orderStatus: OrderStatus,
        planFacts: List<PlanFact>
    ) = LomOrderStatusChangedContext(LomOrder(), orderStatus, planFacts)

    private fun createPlanFact() = PlanFact(
        producerName = processor.producerName(),
        expectedStatus = EXPECTED_STATUS.name,
        planFactStatus = PlanFactStatus.CREATED,
        entityType = EntityType.LOM_WAYBILL_SEGMENT,
        entityId = 2L,
        expectedStatusDatetime = DEFAULT_MK_EXPECTED_TIME
    )

    companion object {
        private val FIXED_TIME = Instant.parse("2021-12-21T12:00:00.00Z")
        private val DEFAULT_MK_EXPECTED_TIME = FIXED_TIME.plus(1, ChronoUnit.DAYS)
        private val EXPECTED_STATUS = SegmentStatus.OUT
    }
}
