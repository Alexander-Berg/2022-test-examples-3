package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Instant
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.geobase.beans.GeobaseRegionData
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamResponse
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderDeliveryDateChangedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderLastMileChangedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint

class CourierLastMileRecipientPlanFactProcessorTest: BaseRecipientPlanFactProcessorTest() {
    override fun getProcessor() = CourierLastMileRecipientPlanFactProcessor(
        clock,
        planFactService,
        geoBaseClientService,
        logService,
        lmsPartnerService
    )

    override fun getParamType(): PartnerExternalParamType = PartnerExternalParamType.LAST_MILE_RECIPIENT_DEADLINE

    override fun getExpectedStatus() = SegmentStatus.TRANSIT_TRANSPORTATION_RECIPIENT

    override fun createWaybillSegmentAddedContext(
        checkpoint: SegmentStatus,
        checkpointTime: Instant,
        withPlanFacts: Boolean,
    ) = createLomWaybillStatusAddedContext(
        checkpoint = checkpoint,
        checkpointTime = checkpointTime,
        withPlanFacts = withPlanFacts,
    )

    override fun createLomOrderEventContext(
        orderStatus: OrderStatus,
    ): LomOrderStatusChangedContext {
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

    override fun createLomOrderLastMileChangedContext(): LomOrderLastMileChangedContext =
        createLomOrderLastMileChangedContext(true)

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

    @DisplayName("Не создавать план-факт если сегмент не COURIER")
    @ParameterizedTest
    @EnumSource(
        value = SegmentType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["COURIER"]
    )
    fun doNotCreatePlanFactIfSegmentIsNotCourier(segmentType: SegmentType) {
        val context = createLomWaybillStatusAddedContext(
            lastMileType = segmentType
        )
        getProcessor().waybillSegmentStatusAdded(context)
        verifyNoMoreInteractions(planFactService)
    }

    @DisplayName("Не создавать план-факт если это Express или OnDemand заказ")
    @ParameterizedTest
    @EnumSource(
        value = WaybillSegmentTag::class,
        names = ["CALL_COURIER", "ON_DEMAND"]
    )
    fun doNotCreatePlanFactIfExpressOrOnDemandOrder(tag: WaybillSegmentTag) {
        val context = createLomWaybillStatusAddedContext()
        context.order.waybill[1].waybillSegmentTags = mutableSetOf(tag)
        getProcessor().waybillSegmentStatusAdded(context)
        verifyNoMoreInteractions(planFactService)
    }


    @DisplayName("Создать план-факт для МК с учетом тайм зоны для дедлайна в LMS (ЧП до ПДД и до дедлайна)")
    @Test
    fun createPlanFactUsingZonedMkPartnerDeadline() {
        val context = createLomWaybillStatusAddedContext()
        context.order.apply {
            recipient.addressGeoId = REGION_ID
            waybill[1].partnerSubtype = PartnerSubtype.MARKET_COURIER
        }
        whenever(
            lmsPartnerService.getPartnerExternalParam(
                PARTNER_ID,
                getParamType()
            )
        ).thenReturn(PartnerExternalParamResponse.newBuilder().value("18:00:00").build())
        whenever(geoBaseClientService.getRegion(REGION_ID))
            .thenReturn(GeobaseRegionData().apply { tzname = "Asia/Yekaterinburg" })
        getProcessor().waybillSegmentStatusAdded(context)
        verifyPlanFactExpectedTime(Instant.parse("2021-12-25T13:00:00.00Z"))
    }

    @DisplayName("Создать план-факт для контрактной доставки с тайм зоной Москвы по умолчанию")
    @Test
    fun createPlanFactUsingDefaultZoneForContractDelivery() {
        val context = createLomWaybillStatusAddedContext(
            checkpointTime = FIXED_TIME.minusSeconds(10)
        )
        context.order.apply {
            waybill[1].partnerSubtype = PartnerSubtype.PARTNER_CONTRACT_DELIVERY
        }
        whenever(
            lmsPartnerService.getPartnerExternalParam(
                PARTNER_ID,
                getParamType()
            )
        ).thenReturn(PartnerExternalParamResponse.newBuilder().value("15:00:00").build())
        getProcessor().waybillSegmentStatusAdded(context)
        verifyPlanFactExpectedTime(Instant.parse("2021-12-25T12:00:00.00Z"))
    }

    @DisplayName("Обработать план-факт по событию изменения типа последней мили, если сегмента для ПФ нет")
    @Test
    fun processPlanFactOnOrderLastMileChangedWithDeletedSegments() {
        val context = createLomOrderLastMileChangedContext(planFactWithEntity = false)
        val planFact = context.getPlanFactsFromProcessor(getProcessor().producerName()).first()
        planFact.expectedStatusDatetime = FIXED_TIME.minusSeconds(10)
        getProcessor().lomOrderLastMileChanged(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
        verifyPlanFactWithoutLmsSettingsSaved()
    }

    private fun createLomWaybillStatusAddedContext(
        checkpoint: SegmentStatus = SegmentStatus.IN,
        checkpointTime: Instant = FIXED_TIME,
        lastMileType: SegmentType = SegmentType.COURIER,
        withPlanFacts: Boolean = false,
        platformClient: PlatformClient = PlatformClient.BERU,
    ): LomWaybillStatusAddedContext {
        val newCheckpoint = WaybillSegmentStatusHistory(status = checkpoint, date = checkpointTime)
        val previousSegment =
            createPreviousSegment(partnerSubtype = PartnerSubtype.MARKET_COURIER, segmentType = SegmentType.MOVEMENT)
        val lastMileSegment = createLastMileSegment(lastMileType = lastMileType)
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
        return LomWaybillStatusAddedContext(newCheckpoint, order, orderPlanFacts)
    }

    private fun createStorageContext(planFactWithEntity: Boolean = true): StorageContext {
        val lastMileSegment = createLastMileSegment()
        writeWaybillSegmentCheckpoint(lastMileSegment, SegmentStatus.IN, FIXED_TIME)
        val lomOrder = createOrder(lastMileSegment = lastMileSegment)
        val planFact = if (planFactWithEntity)
            createBasePlanFact(lastMileSegment.id).apply { entity = lastMileSegment }
        else
            createBasePlanFact(100L)
        return StorageContext(lomOrder, planFact)
    }

    private fun createLomOrderLastMileChangedContext(
        planFactWithEntity: Boolean = true
    ): LomOrderLastMileChangedContext {
        val (lomOrder, planFact) = createStorageContext(planFactWithEntity = planFactWithEntity)
        return LomOrderLastMileChangedContext(
            order = lomOrder,
            changeOrderRequest = null,
            orderPlanFacts = listOf(planFact),
        )
    }

    companion object {
        const val REGION_ID = 123
    }
}
