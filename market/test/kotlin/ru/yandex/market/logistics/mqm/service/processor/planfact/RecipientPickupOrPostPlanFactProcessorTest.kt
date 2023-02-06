package ru.yandex.market.logistics.mqm.service.processor.planfact

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.DeliveryType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.utils.createWaybillSegmentWithCheckpoint
import ru.yandex.market.logistics.mqm.utils.getNextSegment
import ru.yandex.market.logistics.mqm.utils.joinInOrder

@Deprecated("Заменен на PickupOrPostLastMileRecipientPlanFactProcessorTest")
class RecipientPickupOrPostPlanFactProcessorTest : AbstractRecipientPlanFactProcessorTest() {

    @Test
    @DisplayName("Проверка применимости процессора для непоследних МК MOVEMENT сегментов")
    fun isEligibleOnMarketCourierToPickup() {
        assertThat(getProcessor().isEligible(createValidMCSegment())).isTrue
    }

    @Test
    @DisplayName("Процессор неприменим, если тип неверный")
    fun isNonEligibleIfRecipientTypeIsWrong() {
        checkNonEligible(createWaybillSegmentWithCheckpoint(SegmentType.COURIER, SegmentStatus.IN))
    }

    @Test
    @DisplayName("Процессор неприменим, если нет 10 или 110 чекпоинта в истории")
    fun isNonEligibleWithoutCheckpointInHistory() {
        checkNonEligible(createWaybillSegmentWithCheckpoint(SegmentType.POST, SegmentStatus.TRANSIT_PICKUP))
    }

    @Test
    @DisplayName("Процессор неприменим, если сегмент не последний с типом delivery и не МК")
    fun isNonEligibleIfNotLastDeliveryAndNotMC() {
        val segment = createValidMCSegment()
        segment.partnerSubtype = null
        checkNonEligible(segment)
    }

    @Test
    @DisplayName("Процессор неприменим, если сегмент не последний и МК, но заказ доставляется не в постамат")
    fun isNonEligibleIfNotPickupOrder() {
        val segment = createValidMCSegment()
        segment.order!!.deliveryType = DeliveryType.COURIER
        checkNonEligible(segment)
    }

    @Test
    @DisplayName("Процессор неприменим, если сегмент не последний и МК, но тип сегмента не MOVEMENT")
    fun isNonEligibleIfNotMovementSegmentType() {
        val segment = createValidMCSegment()
        segment.segmentType = SegmentType.PICKUP
        checkNonEligible(segment)
    }

    @Test
    @DisplayName("Процессор неприменим, если сегмент не последний и МК, но следующий сегмент не партнерский ПВЗ")
    fun isNonEligibleIfNextSegmentIsNotPartnerPickupPoint() {
        val segment = createValidMCSegment()
        segment.getNextSegment()
            .partnerSubtype = PartnerSubtype.MARKET_COURIER
        checkNonEligible(segment)
    }

    private fun checkNonEligible(segment: WaybillSegment) = assertThat(getProcessor().isEligible(segment)).isFalse

    private fun createValidMCSegment() : WaybillSegment {
        val currentMCSegment = createWaybillSegmentWithCheckpoint(SegmentType.MOVEMENT, SegmentStatus.IN)
        currentMCSegment.partnerSubtype = PartnerSubtype.MARKET_COURIER

        val nextPickupSegment = WaybillSegment(
            id = 2L,
            waybillSegmentIndex = 1,
            partnerId = 2L,
            externalId = "externalId2",
            partnerType = PartnerType.DELIVERY,
            segmentType = SegmentType.PICKUP
        )
        nextPickupSegment.partnerSubtype = PartnerSubtype.PARTNER_PICKUP_POINT_IP

        val order = joinInOrder(listOf(currentMCSegment, nextPickupSegment))
        order.deliveryType = DeliveryType.PICKUP

        return currentMCSegment
    }

    override fun createValidSegment(): WaybillSegment = createWaybillSegmentWithCheckpoint(
        SegmentType.PICKUP,
        SegmentStatus.IN
    )

    override fun getProcessor(): AbstractRecipientPlanFactProcessor = RecipientPickupOrPostPlanFactProcessor(
        lmsPartnerService,
        logService
    )

    override fun getParamType() = PartnerExternalParamType.PICKUP_POST_TRANSFER_DEADLINE
}
