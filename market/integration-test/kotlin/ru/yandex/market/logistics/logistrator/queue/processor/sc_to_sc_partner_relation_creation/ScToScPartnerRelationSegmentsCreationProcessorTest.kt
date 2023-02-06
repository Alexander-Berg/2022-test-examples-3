package ru.yandex.market.logistics.logistrator.queue.processor.sc_to_sc_partner_relation_creation

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.logistrator.queue.processor.AbstractQueueProcessorTest
import ru.yandex.market.logistics.logistrator.utils.createLmsSchedule
import ru.yandex.market.logistics.management.entity.request.logistic.edge.LogisticEdgeDto
import ru.yandex.market.logistics.management.entity.request.logistic.edge.UpdateLogisticEdgesRequest
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentCreateDto
import ru.yandex.market.logistics.management.entity.request.logistic.service.LogisticServiceCreateDto
import ru.yandex.market.logistics.management.entity.response.logistic.segment.BaseLogisticSegmentDto
import ru.yandex.market.logistics.management.entity.type.ActivityStatus
import ru.yandex.market.logistics.management.entity.type.EdgesFrozen
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName

internal class ScToScPartnerRelationSegmentsCreationProcessorTest :
    AbstractQueueProcessorTest<ScToScPartnerRelationSegmentsCreationProcessor>(
        ScToScPartnerRelationActivationProcessor::class.java
    ) {

    @Autowired
    private lateinit var processor: ScToScPartnerRelationSegmentsCreationProcessor

    @Test
    @DatabaseSetup("/db/sc_to_sc_partner_relation_creation/before/setup_movement_is_second_partner.xml")
    @ExpectedDatabase(
        "/db/sc_to_sc_partner_relation_creation/after/activation_movement_is_second_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecuteMovementIsSecondPartner() = abstractTestExecute(
        processor,
        initializeMocks = {
            whenever(lmsClient.createLogisticSegment(eq(createLmsLogisticSegmentDto(
                102,
                LogisticSegmentType.MOVEMENT,
                ServiceCodeName.MOVEMENT,
                ActivityStatus.INACTIVE
            )))).thenReturn(BaseLogisticSegmentDto().setId(1022))
        },
        verifyExecution = {
            verify(lmsClient).createLogisticSegment(eq(createLmsLogisticSegmentDto(
                102,
                LogisticSegmentType.MOVEMENT,
                ServiceCodeName.MOVEMENT,
                ActivityStatus.INACTIVE
            )))

            verify(lmsClient).updateLogisticEdges(
                UpdateLogisticEdgesRequest.newBuilder()
                    .createEdges(setOf(LogisticEdgeDto.of(1011, 1022), LogisticEdgeDto.of(1022, 1021)))
                    .build()
            )
        }
    )

    @Test
    @DatabaseSetup("/db/sc_to_sc_partner_relation_creation/before/setup_movement_is_third_partner.xml")
    @ExpectedDatabase(
        "/db/sc_to_sc_partner_relation_creation/after/activation_movement_is_third_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecuteMovementIsThirdPartner() = abstractTestExecute(
        processor,
        initializeMocks = {
            whenever(lmsClient.createLogisticSegment(eq(createLmsLogisticSegmentDto(
                102,
                LogisticSegmentType.WAREHOUSE,
                ServiceCodeName.PROCESSING,
                ActivityStatus.ACTIVE
            )))).thenReturn(BaseLogisticSegmentDto().setId(1021))

            whenever(lmsClient.createLogisticSegment(eq(createLmsLogisticSegmentDto(
                103,
                LogisticSegmentType.MOVEMENT,
                ServiceCodeName.MOVEMENT,
                ActivityStatus.INACTIVE
            )))).thenReturn(BaseLogisticSegmentDto().setId(1031))
        },
        verifyExecution = {
            verify(lmsClient).createLogisticSegment(eq(createLmsLogisticSegmentDto(
                102,
                LogisticSegmentType.WAREHOUSE,
                ServiceCodeName.PROCESSING,
                ActivityStatus.ACTIVE
            )))

            verify(lmsClient).createLogisticSegment(eq(createLmsLogisticSegmentDto(
                103,
                LogisticSegmentType.MOVEMENT,
                ServiceCodeName.MOVEMENT,
                ActivityStatus.INACTIVE
            )))

            verify(lmsClient).updateLogisticEdges(
                UpdateLogisticEdgesRequest.newBuilder()
                    .createEdges(setOf(LogisticEdgeDto.of(1011, 1031), LogisticEdgeDto.of(1031, 1021)))
                    .build()
            )
        }
    )

    @Test
    @DatabaseSetup("/db/sc_to_sc_partner_relation_creation/before/setup_movement_is_third_partner_with_relation.xml")
    @ExpectedDatabase(
        "/db/sc_to_sc_partner_relation_creation/after/activation_movement_is_third_partner_with_relation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecuteMovementIsThirdPartnerWithExistingPartnerRelation() = abstractTestExecute(
        processor,
        initializeMocks = {
            whenever(lmsClient.createLogisticSegment(eq(createLmsLogisticSegmentDto(
                102,
                LogisticSegmentType.WAREHOUSE,
                ServiceCodeName.PROCESSING,
                ActivityStatus.ACTIVE
            )))).thenReturn(BaseLogisticSegmentDto().setId(1021))

            whenever(lmsClient.createLogisticSegment(eq(createLmsLogisticSegmentDto(
                103,
                LogisticSegmentType.MOVEMENT,
                ServiceCodeName.MOVEMENT,
                ActivityStatus.INACTIVE
            )))).thenReturn(BaseLogisticSegmentDto().setId(1031))
        },
        verifyExecution = {
            verify(lmsClient).createLogisticSegment(eq(createLmsLogisticSegmentDto(
                102,
                LogisticSegmentType.WAREHOUSE,
                ServiceCodeName.PROCESSING,
                ActivityStatus.ACTIVE
            )))

            verify(lmsClient).createLogisticSegment(eq(createLmsLogisticSegmentDto(
                103,
                LogisticSegmentType.MOVEMENT,
                ServiceCodeName.MOVEMENT,
                ActivityStatus.INACTIVE
            )))

            verify(lmsClient).updateLogisticEdges(
                UpdateLogisticEdgesRequest.newBuilder()
                    .createEdges(setOf(LogisticEdgeDto.of(1011, 1031), LogisticEdgeDto.of(1031, 1021)))
                    .build()
            )
        }
    )

    private fun createLmsLogisticSegmentDto(
        partnerId: Long,
        type: LogisticSegmentType,
        code: ServiceCodeName,
        status: ActivityStatus,
    ) = LogisticSegmentCreateDto.newBuilder()
        .name("Сегмент сегмента")
        .partnerId(partnerId)
        .type(type)
        .locationId(213)
        .edgesFrozen(if (type == LogisticSegmentType.MOVEMENT) EdgesFrozen.MANUALLY else null)
        .services(listOf(
            LogisticServiceCreateDto.newBuilder()
                .code(code)
                .duration(1200)
                .status(status)
                .frozen(true)
                .schedule(createLmsSchedule())
                .build()
        ))
        .build()
}
