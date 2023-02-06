package ru.yandex.market.logistics.logistrator.controller

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.refEq
import com.nhaarman.mockitokotlin2.whenever
import net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.logistrator.AbstractContextualTest
import ru.yandex.market.logistics.logistrator.utils.createLmsSchedule
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentFilter
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentDto
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentServiceDto
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse
import ru.yandex.market.logistics.management.entity.type.ActivityStatus
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType
import ru.yandex.market.logistics.management.entity.type.PartnerStatus
import ru.yandex.market.logistics.management.entity.type.PartnerType
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Optional
import java.util.stream.Stream

internal class ScToMcPartnerRelationControllerGetTest : AbstractContextualTest() {

    @BeforeEach
    fun setUp() {
        whenever(clock.zone).thenReturn(ZoneId.of("UTC"))
        whenever(clock.instant()).thenReturn(LocalDateTime.of(2022, 1, 7, 12, 0, 0).toInstant(ZoneOffset.UTC))
        mockPartnersLmsResponse()
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getScToMcPartnerRelationSuccessProvider")
    fun getScToScPartnerRelationSuccess(
        @Suppress("UNUSED_PARAMETER") caseName: String,
        toSegmentPartnerId: Long,
        movementSegmentPartnerId: Long,
        movementSegmentToPartnerId: Long,
        expectedResultPath: String
    ) {
        mockSegmentsLmsResponse(
            toSegmentPartnerId,
            movementSegmentPartnerId,
            listOf(100L),
            listOf(1000L),
            listOf(movementSegmentToPartnerId),
            listOf(1001L)
        )
        mockMvc.perform(MockMvcRequestBuilders.get("/partner-relations/sc-to-mc/1002"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                JsonUnitResultMatchers.json().isEqualTo(IntegrationTestUtils.extractFileContent(expectedResultPath)))
    }

    @Test
    fun testGetScToMcPartnerRelation_movementSegmentIsNotFound() {
        val partnerRelationId = 1003L
        whenever(lmsClient.searchLogisticSegments(
            refEq(
                LogisticSegmentFilter.builder()
            .setIds(setOf(partnerRelationId))
            .setTypes(setOf(LogisticSegmentType.MOVEMENT))
            .build()
        )
        ))
            .thenReturn(emptyList())

        mockMvc.perform(MockMvcRequestBuilders.get("/partner-relations/sc-to-mc/$partnerRelationId"))
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(
                IntegrationTestUtils.errorMessage(
                    "Partner relation between SC/FF and MC partners with with id $partnerRelationId is not found"
                )
            )
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getScToMcPartnerRelationValidationErrorProvider")
    fun getScToMcPartnerRelationValidationError(
        @Suppress("UNUSED_PARAMETER") caseName: String,
        movementSegmentFromPartnerIds: List<Long>,
        movementSegmentFromSegmentIds: List<Long>,
        movementSegmentToPartnerIds: List<Long>,
        movementSegmentToSegmentIds: List<Long>,
        expectedErrorMessage: String
    ) {
        mockSegmentsLmsResponse(
            101L,
            102L,
            movementSegmentFromPartnerIds,
            movementSegmentFromSegmentIds,
            movementSegmentToPartnerIds,
            movementSegmentToSegmentIds
        )
        mockMvc.perform(MockMvcRequestBuilders.get("/partner-relations/sc-to-mc/1002"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(IntegrationTestUtils.errorMessage(expectedErrorMessage))
    }

    private fun mockPartnersLmsResponse() {
        val fromPartner = PartnerResponse.newBuilder()
            .id(100)
            .partnerType(PartnerType.SORTING_CENTER)
            .subtype(PartnerSubtypeResponse.newBuilder().id(100500).build())
            .name("FromPartner")
            .readableName("From Partner")
            .status(PartnerStatus.ACTIVE)
            .locationId(200)
            .build()
        val toPartner = PartnerResponse.newBuilder()
            .id(101)
            .partnerType(PartnerType.DELIVERY)
            .subtype(PartnerSubtypeResponse.newBuilder().id(2).name("Маркет Курьер").build())
            .name("FromPartner")
            .readableName("From Partner")
            .status(PartnerStatus.ACTIVE)
            .locationId(201)
            .build()
        val movementPartner = PartnerResponse.newBuilder()
            .id(102)
            .partnerType(PartnerType.DELIVERY)
            .subtype(PartnerSubtypeResponse.newBuilder().id(102502).build())
            .name("DeliveryPartner")
            .readableName("Delivery Partner")
            .status(PartnerStatus.ACTIVE)
            .locationId(202)
            .build()

        whenever(lmsClient.getPartner(eq(fromPartner.id)))
            .thenReturn(Optional.of(fromPartner))
        whenever(lmsClient.getPartner(eq(toPartner.id)))
            .thenReturn(Optional.of(toPartner))
        whenever(lmsClient.getPartner(eq(movementPartner.id)))
            .thenReturn(Optional.of(movementPartner))
    }

    private fun mockSegmentsLmsResponse(
        toSegmentPartnerId: Long,
        movementSegmentPartnerId: Long,
        movementSegmentFromPartnerIds: List<Long>,
        movementSegmentFromSegmentIds: List<Long>,
        movementSegmentToPartnerIds: List<Long>,
        movementSegmentToSegmentIds: List<Long>
    ) {
        val fromPartnerSegment = LogisticSegmentDto()
            .setId(1000)
            .setName("From Segment")
            .setPartnerId(100)
            .setType(LogisticSegmentType.WAREHOUSE)
            .setLocationId(2000)
            .setServices(listOf(
                LogisticSegmentServiceDto.builder()
                    .setId(3000)
                    .setStatus(ActivityStatus.ACTIVE)
                    .setCode(ServiceCodeName.SHIPMENT)
                    .setDuration(Duration.ofHours(1))
                    .build()
            ))
        val toPartnerSegment = LogisticSegmentDto()
            .setId(1001)
            .setName("To Segment")
            .setPartnerId(toSegmentPartnerId)
            .setType(LogisticSegmentType.LINEHAUL)
            .setServices(listOf(
                LogisticSegmentServiceDto.builder()
                    .setId(3001)
                    .setStatus(ActivityStatus.ACTIVE)
                    .setCode(ServiceCodeName.INBOUND)
                    .setDuration(Duration.ofHours(3))
                    .build()
            ))
        val movementSegment = LogisticSegmentDto()
            .setId(1002)
            .setName("Movement Segment")
            .setPartnerId(movementSegmentPartnerId)
            .setType(LogisticSegmentType.MOVEMENT)
            .setLocationId(2002)
            .setServices(listOf(
                LogisticSegmentServiceDto.builder()
                    .setId(3002)
                    .setStatus(ActivityStatus.ACTIVE)
                    .setCode(ServiceCodeName.SHIPMENT)
                    .setDuration(Duration.ofHours(2))
                    .setSchedule(createLmsSchedule().toList())
                    .build(),
                LogisticSegmentServiceDto.builder()
                    .setId(3003)
                    .setStatus(ActivityStatus.ACTIVE)
                    .setCode(ServiceCodeName.MOVEMENT)
                    .setDuration(Duration.ofHours(2))
                    .setSchedule(createLmsSchedule().toList())
                    .build()
            ))
            .setPreviousSegmentIds(movementSegmentFromSegmentIds)
            .setPreviousSegmentPartnerIds(movementSegmentFromPartnerIds)
            .setNextSegmentIds(movementSegmentToSegmentIds)
            .setNextSegmentPartnerIds(movementSegmentToPartnerIds)

        whenever(lmsClient.searchLogisticSegments(refEq(LogisticSegmentFilter.builder()
            .setIds(setOf(fromPartnerSegment.id))
            .build()
        )))
            .thenReturn(listOf(fromPartnerSegment))
        whenever(lmsClient.searchLogisticSegments(refEq(LogisticSegmentFilter.builder()
            .setIds(setOf(toPartnerSegment.id))
            .build()
        )))
            .thenReturn(listOf(toPartnerSegment))
        whenever(lmsClient.searchLogisticSegments(refEq(LogisticSegmentFilter.builder()
            .setIds(setOf(movementSegment.id))
            .setTypes(setOf(LogisticSegmentType.MOVEMENT))
            .build()
        )))
            .thenReturn(listOf(movementSegment))
    }

    private companion object {
        @JvmStatic
        private fun getScToMcPartnerRelationSuccessProvider() = Stream.of(
            Arguments.of(
                "Movement is \"to\" partner",
                101L,
                101L,
                101L,
                "response/sc_to_mc_get/get_movement_is_to_partner.json"
            ),
            Arguments.of(
                "Movement is third partner",
                101L,
                102L,
                101L,
                "response/sc_to_mc_get/get_movement_is_third_partner.json"
            ),
        )

        @JvmStatic
        private fun getScToMcPartnerRelationValidationErrorProvider() = Stream.of(
            Arguments.of(
                "No \"from\" segments",
                emptyList<Long>(),
                emptyList<Long>(),
                listOf(101L),
                listOf(1001L),
                "No \"from\" partner segments found"
            ),
            Arguments.of(
                "Multiple \"from\" partners",
                listOf(100L, 110L),
                listOf(1000L, 1100L),
                listOf(101L),
                listOf(1001L),
                "Movement segment with id 1002 is expected to have exactly one previous partner"
            ),
        )
    }
}
