package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.logistics.lom.client.LomClient
import ru.yandex.market.logistics.mqm.configuration.properties.RecalculateRddProcessingProperties
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.additionaldata.RecalculationRddPlanFactAdditionalData
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.WaybillShipment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.toInstant
import ru.yandex.market.logistics.mqm.utils.toLocalDate
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ScDsShippedRecalculateRddQualityRuleProcessorTest: BaseRecalculateRddQualityRuleProcessorTest() {

    override fun createActivePlanFactContext(): PlanFactContext {
        val currentSegment = createCurrentSegment()
        val nextSegment = createNextSegment()
        createLomOrder(currentSegment = currentSegment, nextSegment = nextSegment)
        val existingPlanFact = createPlanFact(
            waybillSegment = currentSegment,
            factStatusDatetime = null,
            delayInvocation = false,
            factInvocation = false,
            planFactStatus = PlanFactStatus.ACTIVE,
        )

        val expectedFactDatetime = EXPECTED_DATETIME_BEFORE_8.toLocalDate().atStartOfDay().plus(TIMEOUT).toInstant()
        val expectedSegmentStatus = SegmentStatus.OUT
        val expectedSegmentId = currentSegment.id

        return PlanFactContext(existingPlanFact, expectedFactDatetime, expectedSegmentStatus, expectedSegmentId)
    }

    override fun createPlanFactNotActualAndFact(): PlanFactContext {
        val currentSegment = createCurrentSegment()
        val nextSegment = createNextSegment()
        createLomOrder(currentSegment = currentSegment, nextSegment = nextSegment)
        val existingPlanFact = createPlanFact(
            waybillSegment = currentSegment,
            factStatusDatetime = EXPECTED_DATETIME_BEFORE_8,
            delayInvocation = true,
            factInvocation = false,
            planFactStatus = PlanFactStatus.NOT_ACTUAL,
        )

        val expectedFactDatetime = EXPECTED_DATETIME_BEFORE_8
        val expectedSegmentStatus = SegmentStatus.OUT
        val expectedSegmentId = currentSegment.id

        return PlanFactContext(existingPlanFact, expectedFactDatetime, expectedSegmentStatus, expectedSegmentId)
    }

    override fun createPlanFactNotActualAndFactAndAllInvocations(): PlanFactContext {
        val currentSegment = createCurrentSegment()
        val nextSegment = createNextSegment()
        createLomOrder(currentSegment = currentSegment, nextSegment = nextSegment)
        val existingPlanFact = createPlanFact(
            waybillSegment = currentSegment,
            factStatusDatetime = EXPECTED_DATETIME_BEFORE_8,
            delayInvocation = true,
            factInvocation = true,
            planFactStatus = PlanFactStatus.NOT_ACTUAL,
        )

        val expectedFactDatetime = EXPECTED_DATETIME_BEFORE_8
        val expectedSegmentStatus = SegmentStatus.OUT
        val expectedSegmentId = currentSegment.id

        return PlanFactContext(existingPlanFact, expectedFactDatetime, expectedSegmentStatus, expectedSegmentId)
    }

    // Вспомогательные методы.

    override fun createProcessor(
        lomClient: LomClient,
        properties: RecalculateRddProcessingProperties
    ): BaseRecalculateRddQualityRuleProcessor =
        ScDsShippedRecalculateRddQualityRuleProcessor(lomClient, properties)

    private fun createCurrentSegment(
        segmentType: SegmentType = SegmentType.SORTING_CENTER,
        shipmentDate: LocalDate = SHIPMENT_DATE,
    ) =
        WaybillSegment(
            id = 51,
            segmentType = segmentType,
            partnerId = TEST_CURRENT_PARTNER_ID,
            shipment = WaybillShipment(
                date = shipmentDate
            ),
        )

    private fun createNextSegment(
        partnerType: PartnerType = PartnerType.DELIVERY,
    ) =
        WaybillSegment(
            id = 52,
            partnerId = TEST_NEXT_PARTNER_ID,
            partnerType = partnerType,
        )

    private fun createLomOrder(
        currentSegment: WaybillSegment = createCurrentSegment(),
        nextSegment: WaybillSegment = createNextSegment(),
    ) = joinInOrder(listOf(currentSegment, nextSegment)).apply { id = TEST_ORDER_ID }

    private fun createPlanFact(
        expectedTime: Instant = EXPECTED_DATETIME_BEFORE_8,
        waybillSegment: WaybillSegment,
        delayInvocation: Boolean = false,
        factInvocation: Boolean = false,
        planFactStatus: PlanFactStatus = PlanFactStatus.ACTIVE,
        factStatusDatetime: Instant? = EXPECTED_DATETIME_BEFORE_8,
    ): PlanFact {
        return PlanFact(
            id = 102,
            entityId = waybillSegment.id,
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            planFactStatus = planFactStatus,
            expectedStatusDatetime = expectedTime,
            producerName = PRODUCER_NAME,
            factStatusDatetime = factStatusDatetime,
            expectedStatus = SegmentStatus.OUT.name,
        ).apply {
            entity = waybillSegment
            setData(
                RecalculationRddPlanFactAdditionalData(
                    delayInvocation = delayInvocation,
                    factInvocation = factInvocation
                )
            )
        }
    }

    companion object {
        private val SHIPMENT_DATE: LocalDate = LocalDate.of(2021, 4, 2)
        private val EXPECTED_DATETIME_BEFORE_8 = Instant.parse("2021-04-03T04:00:00Z")
        private const val TEST_ORDER_ID = 1L
        private const val TEST_CURRENT_PARTNER_ID = 1L
        private const val TEST_NEXT_PARTNER_ID = 2L
        private const val PRODUCER_NAME = "ScDsShippedRddPlanFactProcessor"
        private val TIMEOUT: Duration = Duration.ofDays(1)
    }
}
