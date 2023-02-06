package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Instant
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.DeliveryType
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderDeliveryDateChangedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderLastMileChangedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint

@ExtendWith(MockitoExtension::class)
class MkMiddleMileRecipientPlanFactProcessorTest: BaseRecipientPlanFactProcessorTest() {
    override fun getProcessor() = MkMiddleMileRecipientPlanFactProcessor(
        clock,
        planFactService,
        geoBaseClientService,
        logService,
        lmsPartnerService,
    )

    override fun getParamType() = PartnerExternalParamType.PICKUP_POST_TRANSFER_DEADLINE

    override fun getExpectedStatus() = SegmentStatus.TRANSIT_PICKUP

    override fun createWaybillSegmentAddedContext(
        checkpoint: SegmentStatus,
        checkpointTime: Instant,
        withPlanFacts: Boolean,
    ): LomWaybillStatusAddedContext =
        createWaybillSegmentStatusAddedMiddleMilePickupContext(
            checkpoint = checkpoint,
            checkpointTime = checkpointTime,
            withPlanFacts = withPlanFacts,
        )

    override fun createLomOrderEventContext(orderStatus: OrderStatus): LomOrderStatusChangedContext {
        val (lomOrder, planFact) = createStorageContext()
        return LomOrderStatusChangedContext(
            order = lomOrder,
            orderStatus = orderStatus,
            orderPlanFacts = listOf(planFact),
        )
    }

    override fun createLomOrderDeliveryDateChangedContext(): LomOrderDeliveryDateChangedContext {
        val (lomOrder, planFact) = createStorageContext()
        return LomOrderDeliveryDateChangedContext(
            order = lomOrder,
            changeOrderRequests = listOf(),
            orderPlanFacts = listOf(planFact),
        )
    }

    override fun createLomOrderLastMileChangedContext(): LomOrderLastMileChangedContext {
        val (lomOrder, planFact) = createStorageContext()
        return LomOrderLastMileChangedContext(
            order = lomOrder,
            changeOrderRequest = null,
            orderPlanFacts = listOf(planFact),
        )
    }

    @DisplayName("Успешное создание план-факта для заказа GO")
    @Test
    fun createPlanFactGoOrder() {
        val context = createWaybillSegmentStatusAddedMiddleMilePickupContext(platformClient = PlatformClient.YANDEX_GO)
        getProcessor().waybillSegmentStatusAdded(context)
        verify(lmsPartnerService).getPartnerExternalParam(
            PARTNER_ID,
            getParamType()
        )
        verifyPlanFactWithoutLmsSettingsSaved()
    }

    @DisplayName("Успешное создание план-факта в случае средней мили МК и доставки через ПВЗ без дедлайна СД в LMS")
    @ParameterizedTest
    @EnumSource(
        value = PartnerSubtype::class,
        names = ["PARTNER_PICKUP_POINT_IP", "MARKET_LOCKER", "MARKET_OWN_PICKUP_POINT"]
    )
    fun createPlanFactIfMKMiddleMilePickupDelivery(subtype: PartnerSubtype) {
        val context = createWaybillSegmentStatusAddedMiddleMilePickupContext(
            delivery = DeliveryType.PICKUP,
            nextSubtype = subtype,
            nextSegmentType = SegmentType.PICKUP
        )
        getProcessor().waybillSegmentStatusAdded(context)
        verify(lmsPartnerService).getPartnerExternalParam(
            PARTNER_ID,
            PartnerExternalParamType.PICKUP_POST_TRANSFER_DEADLINE
        )
        verifyPlanFactWithoutLmsSettingsSaved()
    }

    @DisplayName("Успешное создание план-факта в случае средней мили МК и курьерской доставки без дедлайна СД в LMS")
    @ParameterizedTest
    @EnumSource(
        value = PartnerSubtype::class,
        names = ["TAXI_LAVKA", "GO_PLATFORM"]
    )
    fun createPlanFactIfMkMiddleMileCourierDelivery(subtype: PartnerSubtype) {
        val context = createWaybillSegmentStatusAddedMiddleMilePickupContext(
            nextSubtype = subtype
        )
        getProcessor().waybillSegmentStatusAdded(context)
        verify(lmsPartnerService).getPartnerExternalParam(
            PARTNER_ID,
            PartnerExternalParamType.PICKUP_POST_TRANSFER_DEADLINE
        )
        verifyPlanFactWithoutLmsSettingsSaved()
    }

    @DisplayName("Не создавать план-факт если подтип партнера сегмента следующего за МК невалидный")
    @ParameterizedTest
    @EnumSource(
        value = PartnerSubtype::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["PARTNER_PICKUP_POINT_IP", "MARKET_LOCKER", "MARKET_OWN_PICKUP_POINT", "TAXI_LAVKA", "GO_PLATFORM"]
    )
    fun doNotCreatePlanIfFollowingMkSegmentHasInvalidPartnerSubtype(subtype: PartnerSubtype) {
        val context = createWaybillSegmentStatusAddedMiddleMilePickupContext(
            nextSubtype = subtype
        )
        getProcessor().waybillSegmentStatusAdded(context)
        verifyNoMoreInteractions(planFactService)
    }

    @DisplayName("Пометить план-факт EXPIRED по приходу следующего чекпоинта на этом сегменте до плана")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        names = ["TRANSIT_TRANSPORTATION_RECIPIENT",
            "TRANSIT_TRANSMITTED_TO_RECIPIENT",
            "OUT",
            "RETURN_ARRIVED",
            "RETURN_PREPARING_SENDER",
            "RETURNED"]
    )
    fun markExpiredOnCloseSegmentStatus(segmentStatus: SegmentStatus) {
        val context = createWaybillSegmentStatusAddedMiddleMilePickupContext(
            checkpoint = segmentStatus,
            checkpointTime = EXPECTED_TIME_NO_LMS_SETTINGS.minusSeconds(10),
            withPlanFacts = true,
        )
        val planFact = context.getPlanFactsFromProcessor(getProcessor().producerName()).first()
        getProcessor().waybillSegmentStatusAdded(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
        }
    }

    @DisplayName("Пометить план-факт NOT_ACTUAL по приходу следующего чекпоинта на этом сегменте")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        names = ["TRANSIT_TRANSPORTATION_RECIPIENT",
            "TRANSIT_TRANSMITTED_TO_RECIPIENT",
            "OUT",
            "RETURN_ARRIVED",
            "RETURN_PREPARING_SENDER",
            "RETURNED"]
    )
    fun markNotActualOnCloseSegmentStatus(segmentStatus: SegmentStatus) {
        val context = createWaybillSegmentStatusAddedMiddleMilePickupContext(
            checkpoint = segmentStatus,
            checkpointTime = EXPECTED_TIME_NO_LMS_SETTINGS.plusSeconds(10),
            withPlanFacts = true,
        )
        val planFact = context.getPlanFactsFromProcessor(getProcessor().producerName()).first()
        getProcessor().waybillSegmentStatusAdded(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    @DisplayName("Пометить план-факт NOT_ACTUAL по приходу следующего чекпоинта на следующем сегменте после плана")
    @Test
    fun markNotActualOnCloseSegmentStatusOnNextSegment() {
        val newCheckpoint = WaybillSegmentStatusHistory(
            status = SegmentStatus.TRANSIT_COURIER_FOUND,
            date = EXPECTED_TIME_NO_LMS_SETTINGS.plusSeconds(10)
        )
        val mkSegment = createMkSegment()
        val nextSegment = createNextSegment().apply {
            waybillSegmentStatusHistory = mutableSetOf(newCheckpoint)
            newCheckpoint.waybillSegment = this
        }
        val planFact = createBasePlanFact(nextSegment.id).apply { entity = mkSegment }
        val order = createOrder(mkSegment, nextSegment, DeliveryType.COURIER)
        val context = LomWaybillStatusAddedContext(newCheckpoint, order, listOf(planFact))
        getProcessor().waybillSegmentStatusAdded(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    @DisplayName("План-факт не создается, если на МК есть 44 или 47 статусы")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        names = ["TRANSIT_UPDATED_BY_DELIVERY", "TRANSIT_UPDATED_BY_SHOP"]
    )
    fun doNotCreatePlanIfMkHasNotAllowedStatuses(segmentStatus: SegmentStatus) {
        val context = createWaybillSegmentStatusAddedMiddleMilePickupContext(
            mkCheckpoint = segmentStatus
        )
        getProcessor().waybillSegmentStatusAdded(context)
        verifyNoMoreInteractions(planFactService)
    }

    private fun createWaybillSegmentStatusAddedMiddleMilePickupContext(
        checkpoint: SegmentStatus = SegmentStatus.IN,
        checkpointTime: Instant = FIXED_TIME,
        delivery: DeliveryType = DeliveryType.COURIER,
        nextSegmentType: SegmentType = SegmentType.COURIER,
        nextSubtype: PartnerSubtype = PartnerSubtype.TAXI_LAVKA,
        mkCheckpoint: SegmentStatus? = null,
        withPlanFacts: Boolean = false,
        platformClient: PlatformClient = PlatformClient.BERU
    ): LomWaybillStatusAddedContext {
        val mkSegment = createMkSegment()
        val orderPlanFacts = mutableListOf<PlanFact>()
        if (withPlanFacts) {
            val planFact = createBasePlanFact(mkSegment.id).apply { entity = mkSegment }
            orderPlanFacts.add(planFact)
        }
        val newCheckpoint = writeWaybillSegmentCheckpoint(mkSegment, checkpoint, checkpointTime)
        mkCheckpoint?.let { writeWaybillSegmentCheckpoint(mkSegment, it, FIXED_TIME) }
        val nextSegment = createNextSegment(nextSegmentType, nextSubtype)
        val order = createOrder(mkSegment, nextSegment, delivery).apply { platformClientId = platformClient.id }
        return LomWaybillStatusAddedContext(newCheckpoint, order, orderPlanFacts)
    }

    private fun createMkSegment() = WaybillSegment(
        id = 1,
        partnerId = PARTNER_ID,
        segmentType = SegmentType.MOVEMENT,
        partnerSubtype = PartnerSubtype.MARKET_COURIER
    )

    private fun createNextSegment(
        nextSegmentType: SegmentType = SegmentType.COURIER,
        nextSubtype: PartnerSubtype = PartnerSubtype.TAXI_LAVKA,
    ) = WaybillSegment(
        id = 2,
        partnerId = 12345L,
        segmentType = nextSegmentType,
        partnerSubtype = nextSubtype
    )

    private fun createOrder(
        mkSegment: WaybillSegment = createMkSegment(),
        nextSegment: WaybillSegment = createNextSegment(),
        delivery: DeliveryType = DeliveryType.COURIER,
    ): LomOrder {
        val order = joinInOrder(listOf(mkSegment, nextSegment))
            .apply {
                deliveryInterval = DEFAULT_INTERVAL
                deliveryType = delivery
            }
        return order
    }

    private fun createStorageContext(): StorageContext {
        val mkSegment = createMkSegment()
        writeWaybillSegmentCheckpoint(mkSegment, SegmentStatus.IN, FIXED_TIME)
        val nextSegment = createNextSegment(SegmentType.COURIER, PartnerSubtype.TAXI_LAVKA)
        val lomOrder = createOrder(mkSegment, nextSegment, DeliveryType.COURIER)
        val planFact = createBasePlanFact(mkSegment.id).apply { entity = mkSegment }
        return StorageContext(lomOrder, planFact)
    }
}
