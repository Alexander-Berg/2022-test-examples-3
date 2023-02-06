package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import java.time.Instant
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderDeliveryDateChangedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderLastMileChangedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint

class PickupOrPostLastMileRecipientPlanFactProcessorTest: BaseRecipientPlanFactProcessorTest() {
    override fun getProcessor() = PickupOrPostLastMileRecipientPlanFactProcessor(
        clock,
        planFactService,
        geoBaseClientService,
        logService,
        lmsPartnerService
    )

    override fun getParamType() = PartnerExternalParamType.PICKUP_POST_TRANSFER_DEADLINE

    override fun getExpectedStatus() = SegmentStatus.TRANSIT_PICKUP

    override fun createWaybillSegmentAddedContext(
        checkpoint: SegmentStatus,
        checkpointTime: Instant,
        withPlanFacts: Boolean,
    ) = createLomWaybillStatusAddedContext(
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
        val context = createLomWaybillStatusAddedContext(platformClient = PlatformClient.YANDEX_GO)
        getProcessor().waybillSegmentStatusAdded(context)
        verify(lmsPartnerService).getPartnerExternalParam(
            PARTNER_ID,
            getParamType()
        )
        verifyPlanFactWithoutLmsSettingsSaved()
    }

    @DisplayName("Не создавать план-факт, если сегмент не PICKUP или POST")
    @ParameterizedTest
    @EnumSource(
        value = SegmentType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["PICKUP", "POST"]
    )
    fun doNotCreatePlanFactIfSegmentTypeIsNotPickupOrPost(segmentType: SegmentType) {
        val context = createLomWaybillStatusAddedContext(
            lastMileType = segmentType
        )
        getProcessor().waybillSegmentStatusAdded(context)
        verifyNoMoreInteractions(planFactService)
    }

    @DisplayName("Не создавать план-факт, если нет ожидаемого статуса на предыдущем сегменте для MARKET_COURIER и MOVEMENT")
    @Test
    fun doNotCreatePlanFactIfPreviousSegmentDoesNotHaveExpectedStatus() {
        val context = createLomWaybillStatusAddedContext(
            lastMileType = SegmentType.PICKUP,
            hasCheckpoint = true,
        )
        getProcessor().waybillSegmentStatusAdded(context)
        verifyNoMoreInteractions(planFactService)
    }

    private fun createLomWaybillStatusAddedContext(
        checkpoint: SegmentStatus = SegmentStatus.IN,
        checkpointTime: Instant = FIXED_TIME,
        lastMileType: SegmentType = SegmentType.PICKUP,
        withPlanFacts: Boolean = false,
        hasCheckpoint: Boolean = false,
        platformClient: PlatformClient = PlatformClient.BERU,
    ): LomWaybillStatusAddedContext {
        val newCheckpoint = WaybillSegmentStatusHistory(status = checkpoint, date = checkpointTime)
        val previousSegment =
            createPreviousSegment(
                partnerSubtype = PartnerSubtype.PARTNER_SORTING_CENTER,
                segmentType = SegmentType.MOVEMENT
            )
        val lastMileSegment = createLastMileSegment(lastMileType)
        lastMileSegment.apply {
            waybillSegmentStatusHistory = mutableSetOf(newCheckpoint)
            newCheckpoint.waybillSegment = this
        }
        val orderPlanFacts = mutableListOf<PlanFact>()
        if (withPlanFacts) {
            val planFact = createBasePlanFact(lastMileSegment.id).apply { entity = lastMileSegment }
            orderPlanFacts.add(planFact)
        }
        val order = createOrder(
            lastMileType = lastMileType,
            previousSegment = previousSegment,
            lastMileSegment = lastMileSegment,
            platformClient = platformClient
        )
        if (hasCheckpoint) {
            writeWaybillSegmentCheckpoint(lastMileSegment, SegmentStatus.TRANSIT_PICKUP, FIXED_TIME)
        }
        return LomWaybillStatusAddedContext(newCheckpoint, order, orderPlanFacts)
    }

    private fun createStorageContext(): StorageContext {
        val previousSegment = createPreviousSegment(partnerSubtype = PartnerSubtype.PARTNER_SORTING_CENTER)
        val lastMileSegment = createLastMileSegment(lastMileType = SegmentType.PICKUP)
        writeWaybillSegmentCheckpoint(lastMileSegment, SegmentStatus.IN, FIXED_TIME)
        val lomOrder = createOrder(previousSegment = previousSegment, lastMileSegment = lastMileSegment)
        val planFact = createBasePlanFact(lastMileSegment.id).apply { entity = lastMileSegment }
        return StorageContext(lomOrder, planFact)
    }
}
