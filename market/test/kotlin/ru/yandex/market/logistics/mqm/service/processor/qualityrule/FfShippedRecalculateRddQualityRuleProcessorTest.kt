package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.lom.client.LomClient
import ru.yandex.market.logistics.lom.model.dto.RecalculateRouteDatesRequestDto
import ru.yandex.market.logistics.mqm.configuration.properties.RecalculateRddProcessingProperties
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.additionaldata.RddPlanFactAdditionalData
import ru.yandex.market.logistics.mqm.entity.additionaldata.RecalculationRddPlanFactAdditionalData
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.PartnerSettings
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.WaybillShipment
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.utils.convertEnum
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.toInstant
import ru.yandex.market.logistics.mqm.utils.toLocalDate
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class FfShippedRecalculateRddQualityRuleProcessorTest: BaseRecalculateRddQualityRuleProcessorTest() {

    @Test
    @DisplayName(
        "Процессор отправляет нотификацию в первый раз, если ПФ актуальный, " +
            "не было нотификаций и есть additionalData со временем старта"
    )
    fun successProcessingIfPlanFactActualAndHasAdditionalData() {
        val (existingPlanFact, _, expectedSegmentStatus, expectedSegmentId) = createActivePlanFactContext()
        mockLomClient()
        val processor = createProcessor(lomClient)
        val requestCaptor = argumentCaptor<RecalculateRouteDatesRequestDto>()
        existingPlanFact.setData(RddPlanFactAdditionalData(FF_SHIPMENT_DEFAULT_PLANNED_SHIPMENT_DATETIME))
        val scheduleInstant = processor.process(createQualityRule(), existingPlanFact)

        verify(lomClient).recalculateRouteDates(requestCaptor.capture())
        val requestDto = requestCaptor.firstValue
        val expectedStartTime = LocalDateTime.of(2021, 10, 2, 0, 0)
            .atZone(DateTimeUtils.MOSCOW_ZONE)
            .toOffsetDateTime()
        val expectedRequestDto = RecalculateRouteDatesRequestDto.builder()
            .segmentId(expectedSegmentId)
            .startDateTime(expectedStartTime)
            .segmentStatus(convertEnum<ru.yandex.market.logistics.lom.model.enums.SegmentStatus>(expectedSegmentStatus))
            .notifyUser(false)
            .build()
        val recalculationRddData = existingPlanFact.getRecalculationRddData()!!

        assertSoftly {
            scheduleInstant shouldBe null
            requestDto shouldBe expectedRequestDto
            recalculationRddData.delayInvocation shouldBe true
            recalculationRddData.corIdForDelay shouldBe COR_ID
            recalculationRddData.factInvocation shouldBe false
        }
    }

    override fun createActivePlanFactContext(): PlanFactContext {
        val currentSegment = createFfShipmentCurrentSegment()
        createFfShipmentLomOrder(currentSegment = currentSegment)
        val existingPlanFact = createFfShipmentPlanFact(
            waybillSegment = currentSegment,
        )
        val expectedFactDatetime = existingPlanFact.expectedStatusDatetime!!
            .toLocalDate().atStartOfDay().plus(FF_SHIPMENT_TIMEOUT).toInstant()
        val expectedSegmentStatus = SegmentStatus.OUT
        val expectedSegmentId = currentSegment.id

        return PlanFactContext(existingPlanFact, expectedFactDatetime, expectedSegmentStatus, expectedSegmentId)
    }

    override fun createPlanFactNotActualAndFact(): PlanFactContext {
        val currentSegment = createFfShipmentCurrentSegment()
        createFfShipmentLomOrder(currentSegment = currentSegment)
        val existingPlanFact = createFfShipmentPlanFact(
            waybillSegment = currentSegment,
            delayInvocation = true,
            factInvocation = false,
            planFactStatus = PlanFactStatus.NOT_ACTUAL,
        )
        val expectedFactDatetime = existingPlanFact.expectedStatusDatetime!!
        val expectedSegmentStatus = SegmentStatus.OUT
        val expectedSegmentId = currentSegment.id

        return PlanFactContext(existingPlanFact, expectedFactDatetime, expectedSegmentStatus, expectedSegmentId)
    }

    override fun createPlanFactNotActualAndFactAndAllInvocations(): PlanFactContext {
        val currentSegment = createFfShipmentCurrentSegment()
        createFfShipmentLomOrder(currentSegment = currentSegment)
        val existingPlanFact = createFfShipmentPlanFact(
            waybillSegment = currentSegment,
            delayInvocation = true,
            factInvocation = true,
            planFactStatus = PlanFactStatus.NOT_ACTUAL,
        )
        val expectedFactDatetime = existingPlanFact.expectedStatusDatetime!!
        val expectedSegmentStatus = SegmentStatus.IN
        val expectedSegmentId = currentSegment.id

        return PlanFactContext(existingPlanFact, expectedFactDatetime, expectedSegmentStatus, expectedSegmentId)
    }

    // Вспомогательные методы.

    override fun createProcessor(
        lomClient: LomClient,
        properties: RecalculateRddProcessingProperties
    ): BaseRecalculateRddQualityRuleProcessor =
        FfShippedRecalculateRddQualityRuleProcessor(lomClient, properties)

    private fun createFfShipmentCurrentSegment(
        shipmentDateTime: Instant? = FF_SHIPMENT_DEFAULT_PLANNED_SHIPMENT_DATETIME,
        partnerType: PartnerType = PartnerType.FULFILLMENT,
        isFromExpress: Boolean = false,
        isFromOnDemand: Boolean = false,
    ): WaybillSegment {
        val waybillShipment = WaybillShipment(dateTime = shipmentDateTime)
        val segment = WaybillSegment(
            id = 51,
            partnerId = 1,
            partnerType = partnerType,
            segmentType = SegmentType.FULFILLMENT,
            shipment = waybillShipment,
            partnerSettings = PartnerSettings(dropshipExpress = isFromExpress),
            segmentStatus = SegmentStatus.OUT,
        )
        if (isFromOnDemand) {
            segment.apply {
                waybillSegmentTags = mutableSetOf(WaybillSegmentTag.ON_DEMAND)
            }
        }
        return segment
    }

    private fun createFfShipmentNextSegment(): WaybillSegment =
        WaybillSegment(
            id = 52,
            partnerId = 2,
            partnerType = PartnerType.DELIVERY
        )

    private fun createFfShipmentLomOrder(
        nextSegment: WaybillSegment = createFfShipmentNextSegment(),
        currentSegment: WaybillSegment = createFfShipmentCurrentSegment(),
        orderStatus: OrderStatus = OrderStatus.ENQUEUED,
    ): LomOrder {
        val lomOrder = joinInOrder(listOf(currentSegment, nextSegment))
        lomOrder.apply {
            status = orderStatus
            id = 1L
        }
        return lomOrder
    }

    private fun createFfShipmentPlanFact(
        expectedTime: Instant = FF_SHIPMENT_EXPECTED_TIME,
        waybillSegment: WaybillSegment,
        delayInvocation: Boolean = false,
        factInvocation: Boolean = false,
        planFactStatus: PlanFactStatus = PlanFactStatus.ACTIVE,
    ): PlanFact {
        val planFact = PlanFact(
            entityId = waybillSegment.id,
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            planFactStatus = planFactStatus,
            expectedStatusDatetime = expectedTime,
            producerName = FF_SHIPMENT_PRODUCER_NAME,
            factStatusDatetime = expectedTime,
            expectedStatus = SegmentStatus.OUT.name,
        )
        planFact.apply {
            entity = waybillSegment
            setData(
                RecalculationRddPlanFactAdditionalData(
                    delayInvocation = delayInvocation,
                    factInvocation = factInvocation
                )
            )
        }
        return planFact
    }

    companion object {
        private val START_DATE = LocalDateTime
            .of(2021, 10, 1, 15, 0, 0)
            .atZone(DateTimeUtils.MOSCOW_ZONE)
            .toInstant()

        private val FF_SHIPMENT_DEFAULT_PLANNED_SHIPMENT_DATETIME = Instant.parse("2021-10-01T10:00:00.00Z")
        private val FF_SHIPMENT_DEFAULT_TIMEOUT = Duration.ofHours(5)
        private val FF_SHIPMENT_EXPECTED_TIME =
            FF_SHIPMENT_DEFAULT_PLANNED_SHIPMENT_DATETIME.plus(FF_SHIPMENT_DEFAULT_TIMEOUT)
        private const val FF_SHIPMENT_PRODUCER_NAME = "FfShippedRddPlanFactProcessor"
        private val FF_SHIPMENT_TIMEOUT: Duration = Duration.ofDays(1)
    }
}
