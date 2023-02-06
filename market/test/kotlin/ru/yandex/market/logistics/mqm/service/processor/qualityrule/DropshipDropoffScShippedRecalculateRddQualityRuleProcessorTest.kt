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
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.WaybillShipment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.toInstant
import ru.yandex.market.logistics.mqm.utils.toLocalDate
import ru.yandex.market.logistics.mqm.utils.convertEnum
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class DropshipDropoffScShippedRecalculateRddQualityRuleProcessorTest: BaseRecalculateRddQualityRuleProcessorTest() {

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
        existingPlanFact.setData(RddPlanFactAdditionalData(EXPECTED_TIME))

        val scheduleInstant = processor.process(createQualityRule(), existingPlanFact)

        verify(lomClient).recalculateRouteDates(requestCaptor.capture())

        val requestDto = requestCaptor.firstValue

        val expectedStartTime = LocalDateTime.of(2021, 1, 5, 0, 0)
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
        val firstSegment = createFirstSegment()
        val secondSegment = createSecondSegment()
        createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment)
        val existingPlanFact = createPlanFact(
            waybillSegment = secondSegment,
            factStatusDatetime = null,
            delayInvocation = false,
            factInvocation = false,
            planFactStatus = PlanFactStatus.ACTIVE,
        )

        val expectedFactDatetime = EXPECTED_TIME.toLocalDate().atStartOfDay().plus(TIMEOUT).toInstant()
        val expectedSegmentStatus = SegmentStatus.IN
        val expectedSegmentId = secondSegment.id

        return PlanFactContext(existingPlanFact, expectedFactDatetime, expectedSegmentStatus, expectedSegmentId)
    }

    override fun createPlanFactNotActualAndFact(): PlanFactContext {
        val firstSegment = createFirstSegment()
        val secondSegment = createSecondSegment()
        createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment)
        val existingPlanFact = createPlanFact(
            waybillSegment = secondSegment,
            factStatusDatetime = EXPECTED_TIME,
            delayInvocation = true,
            factInvocation = false,
            planFactStatus = PlanFactStatus.NOT_ACTUAL,
        )

        val expectedFactDatetime = EXPECTED_TIME
        val expectedSegmentStatus = SegmentStatus.IN
        val expectedSegmentId = secondSegment.id

        return PlanFactContext(existingPlanFact, expectedFactDatetime, expectedSegmentStatus, expectedSegmentId)
    }

    override fun createPlanFactNotActualAndFactAndAllInvocations(): PlanFactContext {
        val firstSegment = createFirstSegment()
        val secondSegment = createSecondSegment()
        createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment)
        val existingPlanFact = createPlanFact(
            waybillSegment = secondSegment,
            factStatusDatetime = EXPECTED_TIME,
            delayInvocation = true,
            factInvocation = true,
            planFactStatus = PlanFactStatus.NOT_ACTUAL,
        )

        val expectedFactDatetime = EXPECTED_TIME
        val expectedSegmentStatus = SegmentStatus.IN
        val expectedSegmentId = secondSegment.id

        return PlanFactContext(existingPlanFact, expectedFactDatetime, expectedSegmentStatus, expectedSegmentId)
    }

    // Вспомогательные методы.

    override fun createProcessor(
        lomClient: LomClient,
        properties: RecalculateRddProcessingProperties
    ): BaseRecalculateRddQualityRuleProcessor =
        DropshipDropoffScShippedRecalculateRddQualityRuleProcessor(lomClient, properties)

    private fun createLomOrder(
        firstSegment: WaybillSegment = createFirstSegment(),
        secondSegment: WaybillSegment = createSecondSegment(),
        thirdSegment: WaybillSegment = createThirdSegment(),
    ) = joinInOrder(listOf(firstSegment, secondSegment, thirdSegment)).apply { id = 1 }

    private fun createFirstSegment(
        partnerType: PartnerType = PartnerType.DROPSHIP,
        shipmentDate: LocalDate? = LocalDate.ofInstant(DROPSHIP_TIME, DateTimeUtils.MOSCOW_ZONE),
    ) =
        WaybillSegment(
            id = 51,
            partnerType = partnerType,
            shipment = WaybillShipment(
                date = shipmentDate,
            ),
            segmentType = SegmentType.MOVEMENT,
        )

    private fun createSecondSegment(
        partnerType: PartnerType = PartnerType.SORTING_CENTER,
        externalId: String? = "123",
    ): WaybillSegment {
        val waybillSegment = WaybillSegment(
            id = 52,
            partnerId = 2,
            partnerType = partnerType,
            externalId = externalId,
        )
        return waybillSegment
    }

    private fun createThirdSegment(
        partnerType: PartnerType = PartnerType.SORTING_CENTER,
        partnerSubtype: PartnerSubtype? = null,
    ) =
        WaybillSegment(
            id = 53,
            partnerType = partnerType,
            partnerSubtype = partnerSubtype,
        )

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
            expectedStatus = SegmentStatus.IN.name,
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
        private val TIMEOUT: Duration = Duration.ofDays(1)
        private val FIXED_TIME = Instant.parse("2021-01-03T15:00:00.00Z")
        private val DROPSHIP_TIME = Instant.parse("2021-01-03T15:00:00.00Z")
        private val EXPECTED_TIME = FIXED_TIME.toLocalDate().atStartOfDay().toInstant().plus(TIMEOUT)
        private const val PRODUCER_NAME = "DropshipDropoffScShippedRddPlanFactProcessor"
    }
}
