package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment;

import java.time.Duration
import java.time.Instant
import ru.yandex.market.logistics.mqm.configuration.properties.TaxiExpressOrdersProperties
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.PartnerSettings
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag

class ExpressReadyToShipDropshipPlanFactProcessorTest: ExpressReadyToShipPlanFactProcessorTest() {

    override fun createProcessor(
        taxiExpressOrdersProperties: TaxiExpressOrdersProperties,
    ): ExpressReadyToShipPlanFactProcessor =
        ExpressReadyToShipDropshipPlanFactProcessor(
            clock,
            planFactService,
            taxiExpressOrdersProperties = taxiExpressOrdersProperties,
        )

    override fun createFirstSegment(
        dropshipExpress: Boolean,
        hasExpressTag: Boolean,
    ): WaybillSegment = WaybillSegment(
        id = 51L,
        externalId = "ws1",
        waybillSegmentIndex = 1,
        partnerType = PartnerType.DROPSHIP,
        segmentType = SegmentType.FULFILLMENT,
        partnerSettings = PartnerSettings(
            dropshipExpress = dropshipExpress,
        ),
        waybillSegmentTags = if (hasExpressTag) mutableSetOf(WaybillSegmentTag.EXPRESS_BATCH) else mutableSetOf(),
    )

    override fun createSuccessPlanFact(): PlanFact =
        PlanFact(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            entityId = 51L,
            expectedStatus = getExpectedStatus().name,
            expectedStatusDatetime = getExpectedTime(),
            producerName = getProducerName(),
            planFactStatus = PlanFactStatus.CREATED,
            processingStatus = ProcessingStatus.ENQUEUED,
            scheduleTime = getExpectedTime(),
        )

    override fun getExpectedStatus(): SegmentStatus = SegmentStatus.TRANSIT_PREPARED

    override fun createExistsPlanFact(waybillSegment: WaybillSegment, expectedTime: Instant): PlanFact =
        PlanFact(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            id = 51_51L,
            entityId = waybillSegment.id,
            expectedStatus = getExpectedStatus().name,
            planFactStatus = PlanFactStatus.CREATED,
            expectedStatusDatetime = expectedTime,
            producerName = getProducerName(),
        ).apply { entity = waybillSegment }

    override fun getExpectedTime(): Instant = CURRENT_TIME.plus(READY_TO_SHIP_DEADLINE)

    override fun getProducerName(): String = "ExpressReadyToShipDropshipPlanFactProcessor"

    companion object {
        private val READY_TO_SHIP_DEADLINE = Duration.ofMinutes(5)
    }
}
