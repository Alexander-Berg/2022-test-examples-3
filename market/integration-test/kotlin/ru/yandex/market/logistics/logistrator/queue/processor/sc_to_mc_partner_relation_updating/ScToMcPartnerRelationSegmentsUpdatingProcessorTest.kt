package ru.yandex.market.logistics.logistrator.queue.processor.sc_to_mc_partner_relation_updating

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.refEq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.logistrator.queue.processor.AbstractQueueProcessorTest
import ru.yandex.market.logistics.logistrator.queue.processor.sc_to_mc_partner_relation_editing.ScToMcPartnerRelationSegmentsUpdatingProcessor
import ru.yandex.market.logistics.logistrator.utils.createLmsSchedule
import ru.yandex.market.logistics.management.entity.request.logistic.edge.LogisticEdgeDto
import ru.yandex.market.logistics.management.entity.request.logistic.edge.UpdateLogisticEdgesRequest
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentFilter
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentUpdateDto
import ru.yandex.market.logistics.management.entity.request.logistic.service.LogisticServiceUpdateDto
import ru.yandex.market.logistics.management.entity.response.logistic.segment.BaseLogisticSegmentDto
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentDto
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentServiceDto
import ru.yandex.market.logistics.management.entity.type.ActivityStatus
import ru.yandex.market.logistics.management.entity.type.DeliveryType
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName
import java.time.Duration

internal class ScToMcPartnerRelationSegmentsUpdatingProcessorTest :
    AbstractQueueProcessorTest<ScToMcPartnerRelationSegmentsUpdatingProcessor>() {

    @Autowired
    private lateinit var processor: ScToMcPartnerRelationSegmentsUpdatingProcessor

    @Test
    @DatabaseSetup("/db/sc_to_mc_partner_relation_updating/before/setup_full_update.xml")
    @ExpectedDatabase(
        "/db/sc_to_mc_partner_relation_updating/after/full_update_finished.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testFullUpdate() = abstractTestExecute(
        processor,
        initializeMocks = {
            whenever(lmsClient.searchLogisticSegments(
                refEq(
                    LogisticSegmentFilter.builder()
                        .setIds(setOf(50))
                        .build()
                )
            ))
                .thenReturn(listOf(LOGISTIC_SEGMENT_LMS_RESPONSE))

            whenever(lmsClient.updateLogisticSegment(
                refEq(
                    LogisticSegmentUpdateDto.newBuilder()
                        .id(50)
                        .name("SC-MC movement")
                        .services(listOf(
                            updateLmsServiceDto(1001, ActivityStatus.INACTIVE, true, 20),
                            updateLmsServiceDto(1002, ActivityStatus.INACTIVE, true, 30),
                            updateLmsServiceDto(1004, ActivityStatus.INACTIVE, false, 50),
                            updateLmsServiceDto(1005, ActivityStatus.INACTIVE, true, 60),
                            updateLmsServiceDto(1006, ActivityStatus.INACTIVE, false, 1),
                        ))
                        .build()
                )
            ))
                .thenReturn(
                    BaseLogisticSegmentDto().setId(50)
                )
        },
        verifyExecution = {
            verify(lmsClient).searchLogisticSegments(
                refEq(
                    LogisticSegmentFilter.builder()
                        .setIds(setOf(50))
                        .build()
                )
            )

            verify(lmsClient).updateLogisticSegment(
                refEq(
                    LogisticSegmentUpdateDto.newBuilder()
                        .id(50)
                        .name("SC-MC movement")
                        .services(listOf(
                            updateLmsServiceDto(1001, ActivityStatus.INACTIVE, true, 20),
                            updateLmsServiceDto(1002, ActivityStatus.INACTIVE, true, 30),
                            updateLmsServiceDto(1004, ActivityStatus.INACTIVE, false, 50),
                            updateLmsServiceDto(1005, ActivityStatus.INACTIVE, true, 60),
                            updateLmsServiceDto(1006, ActivityStatus.INACTIVE, false, 1),
                        ))
                        .build()
                )
            )

            verify(lmsClient).updateLogisticEdges(
                refEq(
                    UpdateLogisticEdgesRequest.newBuilder()
                        .createEdges(setOf(
                            LogisticEdgeDto.of(607, 50),
                        ))
                        .deleteEdges(setOf(
                            LogisticEdgeDto.of(503, 50),
                        ))
                        .build()
                )
            )
        }
    )

    @Test
    @DatabaseSetup("/db/sc_to_mc_partner_relation_updating/before/setup_partial_update.xml")
    @ExpectedDatabase(
        "/db/sc_to_mc_partner_relation_updating/after/partial_update_finished.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testPartialUpdate() = abstractTestExecute(
        processor,
        initializeMocks = {
            whenever(lmsClient.searchLogisticSegments(
                refEq(
                    LogisticSegmentFilter.builder()
                        .setIds(setOf(50))
                        .build()
                )
            ))
                .thenReturn(listOf(LOGISTIC_SEGMENT_LMS_RESPONSE))

            whenever(lmsClient.updateLogisticSegment(
                refEq(
                    LogisticSegmentUpdateDto.newBuilder()
                        .id(50)
                        .name("SC-MC movement")
                        .services(listOf(
                            updateLmsServiceDto(1001, ActivityStatus.INACTIVE, true, 20),
                            updateLmsServiceDto(1002, ActivityStatus.INACTIVE, true, 30),
                            updateLmsServiceDto(1004, ActivityStatus.INACTIVE, false, 50),
                            updateLmsServiceDto(1005, ActivityStatus.INACTIVE, true, 60),
                            updateLmsServiceDto(1006, ActivityStatus.INACTIVE, false, 1),
                        ))
                        .build()
                )
            ))
                .thenReturn(
                    BaseLogisticSegmentDto().setId(50)
                )
        },
        verifyExecution = {
            verify(lmsClient).searchLogisticSegments(
                refEq(
                    LogisticSegmentFilter.builder()
                        .setIds(setOf(50))
                        .build()
                )
            )

            verify(lmsClient).updateLogisticSegment(
                refEq(
                    LogisticSegmentUpdateDto.newBuilder()
                        .id(50)
                        .name("SC-MC movement")
                        .services(listOf(
                            updateLmsServiceDto(1001, ActivityStatus.INACTIVE, true, 20),
                            updateLmsServiceDto(1002, ActivityStatus.INACTIVE, true, 30),
                            updateLmsServiceDto(1004, ActivityStatus.INACTIVE, false, 50),
                            updateLmsServiceDto(1005, ActivityStatus.INACTIVE, true, 60),
                            updateLmsServiceDto(1006, ActivityStatus.INACTIVE, false, 1),
                        ))
                        .build()
                )
            )
        }
    )

    private companion object {
        val LOGISTIC_SEGMENT_LMS_RESPONSE = LogisticSegmentDto()
            .setId(50)
            .setName("SC-MC movement")
            .setType(LogisticSegmentType.MOVEMENT)
            .setPartnerId(10000)
            .setPreviousSegmentIds(listOf(501, 503))
            .setServices(
                listOf(
                    LogisticSegmentServiceDto.builder()
                        .setId(1001)
                        .setStatus(ActivityStatus.INACTIVE)
                        .setCode(ServiceCodeName.MOVEMENT)
                        .setDuration(Duration.ofMinutes(20))
                        .setDeliveryType(DeliveryType.COURIER)
                        .setPrice(0)
                        .setSchedule(createLmsSchedule(8, 20).toList())
                        .build(),
                    LogisticSegmentServiceDto.builder()
                        .setId(1002)
                        .setStatus(ActivityStatus.ACTIVE)
                        .setCode(ServiceCodeName.MOVEMENT)
                        .setDuration(Duration.ofMinutes(30))
                        .setDeliveryType(DeliveryType.PICKUP)
                        .setPrice(0)
                        .setSchedule(createLmsSchedule(8, 20).toList())
                        .build(),
                    LogisticSegmentServiceDto.builder()
                        .setId(1004)
                        .setStatus(ActivityStatus.ACTIVE)
                        .setCode(ServiceCodeName.OTHER)
                        .setPrice(0)
                        .setDuration(Duration.ofMinutes(50))
                        .build(),
                    LogisticSegmentServiceDto.builder()
                        .setId(1005)
                        .setStatus(ActivityStatus.ACTIVE)
                        .setCode(ServiceCodeName.SHIPMENT)
                        .setPrice(0)
                        .setDuration(Duration.ofMinutes(60))
                        .build(),
                    LogisticSegmentServiceDto.builder()
                        .setId(1006)
                        .setStatus(ActivityStatus.ACTIVE)
                        .setCode(ServiceCodeName.INBOUND)
                        .setPrice(0)
                        .setDuration(Duration.ofMinutes(1))
                        .build()
                )
            )
    }

    private fun updateLmsServiceDto(
        id: Long,
        status: ActivityStatus,
        setSchedule: Boolean,
        duration: Int,
    ) = LogisticServiceUpdateDto.newBuilder()
        .id(id)
        .status(status)
        .duration(duration)
        .price(0)
        .frozen(false)
        .schedule(if (setSchedule) createLmsSchedule(11, 19, intArrayOf(1, 2, 3, 4, 6)) else null)
        .build()

}
