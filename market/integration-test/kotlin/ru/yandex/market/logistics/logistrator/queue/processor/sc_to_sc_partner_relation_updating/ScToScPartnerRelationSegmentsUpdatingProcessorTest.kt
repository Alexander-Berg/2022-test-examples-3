package ru.yandex.market.logistics.logistrator.queue.processor.sc_to_sc_partner_relation_updating

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.refEq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.logistrator.queue.processor.AbstractQueueProcessorTest
import ru.yandex.market.logistics.logistrator.queue.processor.sc_to_sc_partner_relation_editing.ScToScPartnerRelationSegmentsUpdatingProcessor
import ru.yandex.market.logistics.logistrator.utils.createLmsSchedule
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentFilter
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentUpdateDto
import ru.yandex.market.logistics.management.entity.request.logistic.service.LogisticServiceCreateDto
import ru.yandex.market.logistics.management.entity.request.logistic.service.LogisticServiceUpdateDto
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentDto
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentServiceDto
import ru.yandex.market.logistics.management.entity.type.ActivityStatus
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName
import java.time.Duration

internal class ScToScPartnerRelationSegmentsUpdatingProcessorTest :
    AbstractQueueProcessorTest<ScToScPartnerRelationSegmentsUpdatingProcessor>() {

    @Autowired
    private lateinit var processor: ScToScPartnerRelationSegmentsUpdatingProcessor

    @Test
    @DatabaseSetup("/db/sc_to_sc_partner_relation_updating/before/setup.xml")
    @ExpectedDatabase(
        "/db/sc_to_sc_partner_relation_updating/after/processing_finished.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecute() = abstractTestExecute(
        processor,
        initializeMocks = {
            whenever(lmsClient.searchLogisticSegments(refEq(LogisticSegmentFilter.builder()
                .setIds(setOf(100))
                .setServiceCodes(setOf(ServiceCodeName.MOVEMENT))
                .build()
            )))
                .thenReturn(listOf(LOGISTIC_SEGMENT_LMS_RESPONSE))

            whenever(lmsClient.createLogisticService(refEq(LogisticServiceCreateDto.newBuilder()
                .segmentId(100)
                .code(ServiceCodeName.MOVEMENT)
                .duration(60)
                .price(0)
                .status(ActivityStatus.ACTIVE)
                .frozen(true)
                .schedule(createLmsSchedule())
                .build()
            )))
                .thenReturn(LogisticSegmentServiceDto.builder()
                    .setId(1000)
                    .build()
                )

            whenever(lmsClient.createLogisticService(refEq(LogisticServiceCreateDto.newBuilder()
                .segmentId(100)
                .code(ServiceCodeName.PROCESSING)
                .duration(240)
                .price(0)
                .status(ActivityStatus.ACTIVE)
                .frozen(true)
                .build()
            )))
                .thenReturn(LogisticSegmentServiceDto.builder()
                    .setId(1003)
                    .build()
                )
        },
        verifyExecution = {
            verify(lmsClient).searchLogisticSegments(refEq(LogisticSegmentFilter.builder()
                .setIds(setOf(100))
                .setServiceCodes(setOf(ServiceCodeName.MOVEMENT))
                .build()
            ))

            verify(lmsClient).deleteLogisticService(eq(1002))
            verify(lmsClient).deleteLogisticService(eq(1005))

            verify(lmsClient).createLogisticService(refEq(LogisticServiceCreateDto.newBuilder()
                .segmentId(100)
                .code(ServiceCodeName.MOVEMENT)
                .duration(60)
                .price(0)
                .status(ActivityStatus.ACTIVE)
                .frozen(true)
                .schedule(createLmsSchedule())
                .build()
            ))
            verify(lmsClient).createLogisticService(refEq(LogisticServiceCreateDto.newBuilder()
                .segmentId(100)
                .code(ServiceCodeName.PROCESSING)
                .duration(240)
                .price(0)
                .status(ActivityStatus.ACTIVE)
                .frozen(true)
                .build()
            ))

            verify(lmsClient).updateLogisticSegment(refEq(LOGISTIC_SEGMENT_UPDATE_REQUEST))
        }
    )

    private companion object {
        val LOGISTIC_SEGMENT_LMS_RESPONSE = LogisticSegmentDto()
            .setId(100)
            .setName("Old segment name")
            .setType(LogisticSegmentType.MOVEMENT)
            .setLocationId(123)
            .setPartnerId(10000)
            .setLogisticsPointId(100000)
            .setServices(listOf(
                LogisticSegmentServiceDto.builder()
                    .setId(1001)
                    .setStatus(ActivityStatus.INACTIVE)
                    .setCode(ServiceCodeName.MOVEMENT)
                    .setDuration(Duration.ofHours(20))
                    .setSchedule(createLmsSchedule(8, 18).toList())
                    .build(),
                LogisticSegmentServiceDto.builder()
                    .setId(1002)
                    .setStatus(ActivityStatus.ACTIVE)
                    .setCode(ServiceCodeName.MOVEMENT)
                    .setDuration(Duration.ofHours(30))
                    .setSchedule(createLmsSchedule(8, 18).toList())
                    .build(),
                LogisticSegmentServiceDto.builder()
                    .setId(1004)
                    .setStatus(ActivityStatus.ACTIVE)
                    .setCode(ServiceCodeName.OTHER)
                    .setDuration(Duration.ofHours(50))
                    .build(),
                LogisticSegmentServiceDto.builder()
                    .setId(1005)
                    .setStatus(ActivityStatus.ACTIVE)
                    .setCode(ServiceCodeName.RETURN)
                    .setDuration(Duration.ofHours(60))
                    .build(),
            ))

        val LOGISTIC_SEGMENT_UPDATE_REQUEST = LogisticSegmentUpdateDto.newBuilder()
            .id(100)
            .name("New segment name")
            .locationId(123)
            .logisticPointId(100000)
            .services(listOf(
                LogisticServiceUpdateDto.newBuilder()
                    .id(1001)
                    .status(ActivityStatus.ACTIVE)
                    .duration(120)
                    .price(0)
                    .frozen(false)
                    .schedule(createLmsSchedule())
                    .build(),
                LogisticServiceUpdateDto.newBuilder()
                    .id(1004)
                    .status(ActivityStatus.ACTIVE)
                    .duration(300)
                    .price(0)
                    .frozen(false)
                    .build(),
            ))
            .build()!!
    }
}
