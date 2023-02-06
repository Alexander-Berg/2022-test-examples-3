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
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.toInstant
import ru.yandex.market.logistics.mqm.utils.toLocalDate
import java.time.Duration
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class ScScShipmentRecalculateRddQualityRuleProcessorTest: BaseRecalculateRddQualityRuleProcessorTest() {

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

        val expectedFactDatetime = EXPECTED_TIME.toLocalDate().atStartOfDay().plus(TIMEOUT).toInstant()
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
            factStatusDatetime = EXPECTED_TIME,
            delayInvocation = true,
            factInvocation = false,
            planFactStatus = PlanFactStatus.NOT_ACTUAL,
        )

        val expectedFactDatetime = EXPECTED_TIME
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
            factStatusDatetime = EXPECTED_TIME,
            delayInvocation = true,
            factInvocation = true,
            planFactStatus = PlanFactStatus.NOT_ACTUAL,
        )

        val expectedFactDatetime = EXPECTED_TIME
        val expectedSegmentStatus = SegmentStatus.OUT
        val expectedSegmentId = currentSegment.id

        return PlanFactContext(existingPlanFact, expectedFactDatetime, expectedSegmentStatus, expectedSegmentId)
    }

    // Вспомогательные методы.

    override fun createProcessor(lomClient: LomClient, properties: RecalculateRddProcessingProperties) =
        ScScShipmentRecalculateRddQualityRuleProcessor(lomClient, properties)

    private fun createCurrentSegment(
        segmentType: SegmentType = SegmentType.SORTING_CENTER,
    ) =
        WaybillSegment(
            id = 51,
            segmentType = segmentType,
            partnerId = TEST_CURRENT_PARTNER_ID,
        )

    private fun createNextSegment(
        segmentType: SegmentType = SegmentType.SORTING_CENTER,
    ) =
        WaybillSegment(
            id = 52,
            partnerId = TEST_NEXT_PARTNER_ID,
            segmentType = segmentType,
        )

    private fun createLomOrder(
        currentSegment: WaybillSegment = createCurrentSegment(),
        nextSegment: WaybillSegment = createNextSegment(),
    ) = joinInOrder(listOf(currentSegment, nextSegment)).apply { id = TEST_ORDER_ID }

    private fun createPlanFact(
        expectedTime: Instant = EXPECTED_TIME,
        waybillSegment: WaybillSegment,
        delayInvocation: Boolean = false,
        factInvocation: Boolean = false,
        planFactStatus: PlanFactStatus = PlanFactStatus.ACTIVE,
        factStatusDatetime: Instant? = EXPECTED_TIME,
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
        private val FIXED_TIME = Instant.ofEpochSecond(1635444000)
        private val TEST_COMBINATOR_TIME = FIXED_TIME
        private val ADDITIONAL_TIME = Duration.ofHours(1)
        private const val PRODUCER_NAME = "ScScShipmentRddPlanFactProcessor"
        private val EXPECTED_TIME = TEST_COMBINATOR_TIME.plus(ADDITIONAL_TIME)
        private const val TEST_NEXT_PARTNER_ID = 2L
        private const val TEST_ORDER_ID = 1L
        private const val TEST_CURRENT_PARTNER_ID = 1L
        private val TIMEOUT: Duration = Duration.ofDays(1)
        private val CURRENT_CHECKPOINT_TIME = Instant.parse("2021-10-01T02:00:00.00Z")
    }
}
