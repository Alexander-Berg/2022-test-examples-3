package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.configuration.properties.ShootingProperties
import ru.yandex.market.logistics.mqm.converter.AggregationEntityConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.Recipient
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment.ShootingPlanFactProcessor
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.sdk.userinfo.service.UidConstants
import java.time.Instant

class ShootingPartnerAndDateProducerTest {

    private val aggregationEntityConverter = AggregationEntityConverter()

    private val shootingProperties = ShootingProperties(
        processShootingOrders = true,
    )

    private val producer = ShootingPartnerAndDateProducer(
        aggregationEntityConverter = aggregationEntityConverter,
        shootingProperties = shootingProperties,
    )

    @Test
    @DisplayName("Проверка созданной AggregationEntity")
    fun produceEntity() {
        val firstSegment = createFirstSegment()
        createLomOrder(firstSegment = firstSegment)
        val planFact = createPlanFact(waybillSegment = firstSegment)
        val aggregationEntity = producer.produceEntity(planFact)
        Assertions.assertThat(aggregationEntity).isEqualTo(
            AggregationEntity(
                date = planFact.expectedStatusDatetime?.atZone(DateTimeUtils.MOSCOW_ZONE)?.toLocalDate(),
                partner = aggregationEntityConverter.toPartnerAggregationEntity(planFact.entity as? WaybillSegment),
                groupNumber = (planFact.entity as WaybillSegment).order!!.id % shootingProperties.groupsCount
            )
        )
    }

    private fun createLomOrder(
        firstSegment: WaybillSegment = createFirstSegment(),
    ) = joinInOrder(listOf(firstSegment)).apply {
        id = LOM_ORDER_ID
        recipient = Recipient(uid = UidConstants.NO_SIDE_EFFECT_UID)
    }

    private fun createFirstSegment(
        segmentType: SegmentType = SegmentType.FULFILLMENT,
        partnerType: PartnerType = PartnerType.FULFILLMENT,
    ) =
        WaybillSegment(
            id = FIRST_SEGMENT_ID,
            segmentType = segmentType,
            partnerType = partnerType,
            partnerId = FIRST_PARTNER_ID,
        )

    private fun createPlanFact(
        expectedTime: Instant = EXPECTED_PLAN_TIME,
        waybillSegment: WaybillSegment,
    ): PlanFact {
        return PlanFact(
            id = FIRST_PLAN_FACT_ID,
            entityId = waybillSegment.id,
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            planFactStatus = PlanFactStatus.CREATED,
            expectedStatusDatetime = expectedTime,
            expectedStatus = EXPECTED_STATUS.name,
            producerName = ShootingPlanFactProcessor::class.simpleName,
        ).apply { entity = waybillSegment }
    }

    companion object {
        private const val LOM_ORDER_ID = 1L
        private const val FIRST_SEGMENT_ID = 51L
        private const val FIRST_PLAN_FACT_ID = 101L
        private val EXPECTED_STATUS = SegmentStatus.OUT
        private val EXPECTED_PLAN_TIME: Instant = Instant.parse("2022-03-18T11:00:00.00Z")
        private const val FIRST_PARTNER_ID = 42L
    }
}
