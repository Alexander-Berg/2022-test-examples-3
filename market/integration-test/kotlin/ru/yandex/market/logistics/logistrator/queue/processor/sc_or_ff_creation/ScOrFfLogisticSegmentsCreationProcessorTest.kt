package ru.yandex.market.logistics.logistrator.queue.processor.sc_or_ff_creation

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.logistrator.queue.processor.AbstractQueueProcessorTest
import ru.yandex.market.logistics.logistrator.utils.LOGISTICS_POINT_ID
import ru.yandex.market.logistics.logistrator.utils.PARTNER_ID
import ru.yandex.market.logistics.logistrator.utils.createLmsSchedule
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentCreateDto
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentFilter
import ru.yandex.market.logistics.management.entity.request.logistic.service.LogisticServiceCreateDto
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentDto
import ru.yandex.market.logistics.management.entity.type.ActivityStatus
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName

internal class ScOrFfLogisticSegmentsCreationProcessorTest :
    AbstractQueueProcessorTest<ScOrFfLogisticSegmentsCreationProcessor>(
        ScOrFfCapacitySettingProcessor::class.java
    ) {

    @Autowired
    private lateinit var processor: ScOrFfLogisticSegmentsCreationProcessor

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/sc_or_ff_creation/before/setup.xml"),
        DatabaseSetup("/db/sc_or_ff_creation/before/logistic_segments_creation.xml", type = DatabaseOperation.REFRESH)
    )
    @ExpectedDatabase(
        "/db/sc_or_ff_creation/after/capacity_setting.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecute() = abstractTestExecute(
        processor,
        initializeMocks = {
            whenever(lmsClient.searchLogisticSegments(eq(LMS_LOGISTIC_SEGMENT_FILTER)))
                .thenReturn(listOf(LogisticSegmentDto().setId(111L)))
        },
        verifyExecution = {
            // TODO: Починить и вернуть (https://st.yandex-team.ru/DELIVERY-39243).
            // verify(lmsClient).searchLogisticSegments(eq(lmsLogisticSegmentFilter))
            // verify(lmsClient).deleteLogisticSegment(111L)

            verify(lmsClient).createLogisticSegment(eq(LMS_LOGISTIC_SEGMENT_DTO))
        }
    )

    private companion object {
        val LMS_LOGISTIC_SEGMENT_FILTER = LogisticSegmentFilter.builder()
            .setPartnerIds(setOf(PARTNER_ID))
            .build()!!
        val LMS_LOGISTIC_SEGMENT_DTO = LogisticSegmentCreateDto.newBuilder()
            .name(null)
            .partnerId(PARTNER_ID)
            .type(LogisticSegmentType.WAREHOUSE)
            .logisticPointId(LOGISTICS_POINT_ID)
            .locationId(213)
            .services(listOf(
                LogisticServiceCreateDto.newBuilder()
                    .code(ServiceCodeName.PROCESSING)
                    .duration(1200)
                    .status(ActivityStatus.INACTIVE)
                    .frozen(true)
                    .schedule(createLmsSchedule())
                    .build()
            ))
            .build()!!
    }
}
