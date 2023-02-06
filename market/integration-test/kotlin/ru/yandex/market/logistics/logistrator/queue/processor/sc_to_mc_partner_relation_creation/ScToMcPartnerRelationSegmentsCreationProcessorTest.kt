package ru.yandex.market.logistics.logistrator.queue.processor.sc_to_mc_partner_relation_creation

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.logistrator.queue.processor.AbstractQueueProcessorTest
import ru.yandex.market.logistics.logistrator.utils.REQUEST_ID_PAYLOAD
import ru.yandex.market.logistics.logistrator.utils.createLmsSchedule
import ru.yandex.market.logistics.management.entity.request.logistic.edge.LogisticEdgeDto
import ru.yandex.market.logistics.management.entity.request.logistic.edge.UpdateLogisticEdgesRequest
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentCreateDto
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentFilter
import ru.yandex.market.logistics.management.entity.request.logistic.service.LogisticServiceCreateDto
import ru.yandex.market.logistics.management.entity.response.logistic.segment.BaseLogisticSegmentDto
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentDto
import ru.yandex.market.logistics.management.entity.type.ActivityStatus
import ru.yandex.market.logistics.management.entity.type.DeliveryType
import ru.yandex.market.logistics.management.entity.type.EdgesFrozen
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName

internal class ScToMcPartnerRelationSegmentsCreationProcessorTest :
    AbstractQueueProcessorTest<ScToMcPartnerRelationSegmentsCreationProcessor>(
        ScToMcPartnerRelationActivationProcessor::class.java
    ) {

    @Autowired
    private lateinit var processor: ScToMcPartnerRelationSegmentsCreationProcessor

    @Test
    @DatabaseSetup("/db/sc_to_mc_partner_relation_creation/before/setup_all_active.xml")
    @ExpectedDatabase(
        "/db/sc_to_mc_partner_relation_creation/after/activation_all_active.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecuteAllActive() = testExecute(ActivityStatus.ACTIVE)

    @Test
    @DatabaseSetup("/db/sc_to_mc_partner_relation_creation/before/setup_all_inactive.xml")
    @ExpectedDatabase(
        "/db/sc_to_mc_partner_relation_creation/after/activation_all_inactive.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecuteAllInactive() = testExecute(ActivityStatus.INACTIVE)

    @Test
    @DatabaseSetup("/db/sc_to_mc_partner_relation_creation/before/setup_no_from_segments.xml")
    fun testExecuteNoFromSegments() {
        val exception = assertThrows<RuntimeException> {
            processor.execute(REQUEST_ID_PAYLOAD)
        }

        assertSoftly {
            exception.message shouldBe "No segments of \"from\" partner 101 are provided"
        }

        verifyNoMoreInteractions(lmsClient)
        verifyNoMoreInteractions(dbQueueService)
    }

    @Test
    @DatabaseSetup("/db/sc_to_mc_partner_relation_creation/before/setup_no_to_segments.xml")
    fun testExecuteNoToSegments() {
        whenever(lmsClient.searchLogisticSegments(eq(LMS_MC_LINEHAUL_SEGMENTS_FILTER)))
            .thenReturn(emptyList())

        val exception = assertThrows<RuntimeException> {
            processor.execute(REQUEST_ID_PAYLOAD)
        }

        assertSoftly {
            exception.message shouldBe "No LINEHAUL segments of \"to\" partner 102 are found"
        }

        verify(lmsClient).searchLogisticSegments(eq(LMS_MC_LINEHAUL_SEGMENTS_FILTER))

        verifyNoMoreInteractions(lmsClient)
        verifyNoMoreInteractions(dbQueueService)
    }

    private fun testExecute(status: ActivityStatus) = abstractTestExecute(
        processor,
        initializeMocks = {
            whenever(lmsClient.createLogisticSegment(eq(
                createLmsLogisticSegmentDto(1, status, ServiceCodeName.MOVEMENT)
            )))
                .thenReturn(BaseLogisticSegmentDto().setId(1013))
            whenever(lmsClient.createLogisticSegment(eq(
                createLmsLogisticSegmentDto(2, status, ServiceCodeName.SHIPMENT)
            )))
                .thenReturn(BaseLogisticSegmentDto().setId(1014))

            whenever(lmsClient.searchLogisticSegments(eq(LMS_MC_LINEHAUL_SEGMENTS_FILTER)))
                .thenReturn(listOf(
                    LogisticSegmentDto().setId(1021),
                    LogisticSegmentDto().setId(1022)
                ))

            whenever(lmsClient.createLogisticSegment(eq(createLmsScToMcMovementSegmentDto(status))))
                .thenReturn(BaseLogisticSegmentDto().setId(1015))
        },
        verifyExecution = {
            verify(lmsClient).createLogisticSegment(eq(
                createLmsLogisticSegmentDto(1, status, ServiceCodeName.MOVEMENT)
            ))
            verify(lmsClient).createLogisticSegment(eq(
                createLmsLogisticSegmentDto(2, status, ServiceCodeName.SHIPMENT)
            ))

            verify(lmsClient).searchLogisticSegments(eq(LMS_MC_LINEHAUL_SEGMENTS_FILTER))

            verify(lmsClient).createLogisticSegment(eq(createLmsScToMcMovementSegmentDto(status)))

            verify(lmsClient).updateLogisticEdges(
                UpdateLogisticEdgesRequest.newBuilder()
                    .createEdges(setOf(
                        LogisticEdgeDto.of(1011, 1015),
                        LogisticEdgeDto.of(1012, 1015),
                        LogisticEdgeDto.of(1013, 1015),
                        LogisticEdgeDto.of(1014, 1015),
                        LogisticEdgeDto.of(1015, 1021),
                        LogisticEdgeDto.of(1015, 1022),
                    ))
                    .build()
            )
        }
    )

    private fun createLmsLogisticSegmentDto(
        order: Int,
        status: ActivityStatus,
        code: ServiceCodeName
    ) = LogisticSegmentCreateDto.newBuilder()
        .name("Сегмент сегмента $order")
        .partnerId(101)
        .type(LogisticSegmentType.MOVEMENT)
        .locationId(213)
        .edgesFrozen(EdgesFrozen.MANUALLY)
        .services(listOf(
            LogisticServiceCreateDto.newBuilder()
                .code(code)
                .duration(1200)
                .status(status)
                .frozen(true)
                .build()
        ))
        .build()

    private fun createLmsScToMcMovementSegmentDto(status: ActivityStatus) = LogisticSegmentCreateDto.newBuilder()
        .partnerId(102)
        .type(LogisticSegmentType.MOVEMENT)
        .edgesFrozen(EdgesFrozen.MANUALLY)
        .services(listOf(
            createLmsScToMcMovementServiceDto(ServiceCodeName.INBOUND, status, false),
            createLmsScToMcMovementServiceDto(ServiceCodeName.SHIPMENT, status, true),
            createLmsScToMcMovementServiceDto(ServiceCodeName.MOVEMENT, status, true, DeliveryType.COURIER),
            createLmsScToMcMovementServiceDto(ServiceCodeName.MOVEMENT, status, true, DeliveryType.PICKUP)
        ))
        .build()

    private fun createLmsScToMcMovementServiceDto(
        code: ServiceCodeName,
        status: ActivityStatus,
        setSchedule: Boolean,
        deliveryType: DeliveryType? = null
    ) = LogisticServiceCreateDto.newBuilder()
        .code(code)
        .deliveryType(deliveryType)
        .duration(0)
        .status(status)
        .frozen(true)
        .schedule(if (setSchedule) createLmsSchedule() else null)
        .build()

    private companion object {
        val LMS_MC_LINEHAUL_SEGMENTS_FILTER = LogisticSegmentFilter.builder()
            .setPartnerIds(setOf(102))
            .setTypes(setOf(LogisticSegmentType.LINEHAUL))
            .build()!!
    }
}
