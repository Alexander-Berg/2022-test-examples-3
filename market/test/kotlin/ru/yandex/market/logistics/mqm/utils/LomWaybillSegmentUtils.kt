package ru.yandex.market.logistics.mqm.utils

import java.time.Instant
import java.time.LocalDate
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.embedded.DeliveryInterval
import ru.yandex.market.logistics.mqm.entity.lom.embedded.Recipient
import ru.yandex.market.logistics.mqm.entity.lom.embedded.WaybillShipment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType

const val TEST_ORDER_ID = 1L

fun writeWaybillSegmentCheckpoint(
    segment: WaybillSegment,
    status: SegmentStatus,
    checkpointReceivedDatetime: Instant
): WaybillSegmentStatusHistory {
    val history = createHistory(status, checkpointReceivedDatetime)
    addWaybillSegmentStatusHistory(segment, history, history.status!!)
    return history
}

fun joinInOrder(segments: List<WaybillSegment>): LomOrder {
    val order = LomOrder().apply { platformClientId = PlatformClient.BERU.id }
    overrideOrderWaybill(order, segments)
    return order
}

fun overrideOrderWaybill(order: LomOrder, newWaybill: List<WaybillSegment>) {
    val waybill = ArrayList(newWaybill)
    order.waybill = waybill
    for (index in order.waybill.indices) {
        order.waybill[index].waybillSegmentIndex = index
        order.waybill[index].order = order
    }
}

private fun addWaybillSegmentStatusHistory(
    segment: WaybillSegment,
    waybillSegmentStatusHistory: WaybillSegmentStatusHistory,
    actualStatus: SegmentStatus
) {
    waybillSegmentStatusHistory.waybillSegment = segment
    segment.waybillSegmentStatusHistory.add(waybillSegmentStatusHistory)
    segment.segmentStatus = actualStatus
}

fun createHistory(
    segmentStatus: SegmentStatus,
    checkpointReceivedDatetime: Instant
) = WaybillSegmentStatusHistory().apply {
    status = segmentStatus
    created = checkpointReceivedDatetime
    date = checkpointReceivedDatetime
}

fun createWaybillSegmentWithCheckpoint(
    segmentType: SegmentType?,
    checkpoint: SegmentStatus
): WaybillSegment {
    val segment = WaybillSegment(
        id = 1L,
        waybillSegmentIndex = 0,
        partnerId = 1L,
        externalId = "externalId",
        partnerType = PartnerType.DELIVERY,
        segmentType = segmentType
    )
    joinInOrder(listOf(segment)).apply {
        deliveryInterval = DeliveryInterval(deliveryDateMax = LocalDate.of(2021, 1, 2))
    }
    writeWaybillSegmentCheckpoint(segment, checkpoint, Instant.parse("2021-01-01T15:00:00.00Z"))
    return segment
}

fun createMkOrder(
    firstSegment: WaybillSegment = createFFSegment(),
    secondSegment: WaybillSegment = createScSegment(),
    thirdSegment: WaybillSegment = createScMkSegment(),
    fourthSegment: WaybillSegment = createMkSegment()
) = joinInOrder(listOf(firstSegment, secondSegment, thirdSegment, fourthSegment)).apply {
    id = TEST_ORDER_ID
    deliveryInterval = DeliveryInterval(deliveryDateMax = LocalDate.parse("2022-03-19"))
    recipient = Recipient(addressGeoId = 54)
}

fun createExpressOrder(
    firstSegment: WaybillSegment = createDropshipSegment(),
    secondSegment: WaybillSegment = createExpressShipmentSegment()
) = joinInOrder(listOf(firstSegment, secondSegment))

fun createPvzOrder(
    firstSegment: WaybillSegment = createFFSegment(),
    secondSegment: WaybillSegment = createScSegment(),
    thirdSegment: WaybillSegment = createScMkSegment(),
    fourthSegment: WaybillSegment = createMkSegment(),
    fifthSegment: WaybillSegment = createPvzSegment(),
    platformClient: PlatformClient = PlatformClient.BERU,
) = joinInOrder(listOf(firstSegment, secondSegment, thirdSegment, fourthSegment, fifthSegment)).apply {
    id = TEST_ORDER_ID
    platformClientId = platformClient.id
    deliveryInterval = DeliveryInterval(deliveryDateMax = LocalDate.parse("2022-03-19"))
    recipient = Recipient(addressGeoId = 54)
}

fun createFFSegment(): WaybillSegment = WaybillSegment(
    id = 101,
    partnerType = PartnerType.FULFILLMENT,
    segmentType = SegmentType.FULFILLMENT,
    shipment = WaybillShipment(dateTime = Instant.parse("2022-03-16T16:00:00.00Z")),
)

fun createScSegment(): WaybillSegment = WaybillSegment(
    id = 102,
    partnerType = PartnerType.SORTING_CENTER,
    segmentType = SegmentType.SORTING_CENTER,
    partnerName = "Имя партнера СЦ",
    shipment = WaybillShipment(dateTime = Instant.parse("2022-03-17T16:00:00.00Z")),
)

fun createScMkSegment(): WaybillSegment = WaybillSegment(
    id = 103,
    partnerType = PartnerType.SORTING_CENTER,
    segmentType = SegmentType.SORTING_CENTER,
    partnerSubtype = PartnerSubtype.MARKET_COURIER_SORTING_CENTER,
    partnerId = 1003,
    partnerName = "Имя партнера СЦ МК",
    shipment = WaybillShipment(dateTime = Instant.parse("2022-03-18T16:00:00.00Z"))
)

fun createMkSegment(): WaybillSegment = WaybillSegment(
    id = 104,
    externalId = "externalMkId",
    partnerType = PartnerType.DELIVERY,
    segmentType = SegmentType.MOVEMENT,
    partnerSubtype = PartnerSubtype.MARKET_COURIER,
    shipment = WaybillShipment(dateTime = Instant.parse("2022-03-19T19:00:00.00Z")),
    combinatorSegmentIds = listOf(1041, 1042)
)

fun createPvzSegment(): WaybillSegment = WaybillSegment(
    id = 105,
    partnerType = PartnerType.DELIVERY,
    segmentType = SegmentType.PICKUP,
    partnerSubtype = PartnerSubtype.PARTNER_PICKUP_POINT_IP,
)

fun createDropshipSegment(): WaybillSegment = WaybillSegment(
    id = 106,
    partnerType = PartnerType.DROPSHIP,
    segmentType = SegmentType.FULFILLMENT,
    partnerName = "Test dropship",
    shipment = WaybillShipment(dateTime = Instant.parse("2022-03-16T10:00:00.00Z")),
)

fun createDropoffSegment(): WaybillSegment = WaybillSegment(
    id = 107,
    partnerType = PartnerType.DELIVERY,
    segmentType = SegmentType.SORTING_CENTER,
    partnerName = "Test dropoff",
    shipment = WaybillShipment(dateTime = Instant.parse("2022-03-16T19:00:00.00Z")),
)

fun createExpressShipmentSegment(): WaybillSegment = WaybillSegment(
    id = 108,
    partnerType = PartnerType.DELIVERY,
    segmentType = SegmentType.COURIER,
    partnerName = "Test express shipment",
    shipment = WaybillShipment(dateTime = Instant.parse("2022-03-16T19:00:00.00Z")),
)
