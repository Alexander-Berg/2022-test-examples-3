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
import ru.yandex.market.logistics.logistrator.utils.createLmsAddress
import ru.yandex.market.logistics.logistrator.utils.createLmsSchedule
import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointCreateRequest
import ru.yandex.market.logistics.management.entity.response.core.Phone
import ru.yandex.market.logistics.management.entity.response.point.Contact
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse
import ru.yandex.market.logistics.management.entity.type.PhoneType
import ru.yandex.market.logistics.management.entity.type.PointType

internal class ScOrFfLogisticPointCreationProcessorTest :
    AbstractQueueProcessorTest<ScOrFfLogisticPointCreationProcessor>(
        ScOrFfLogisticSegmentsCreationProcessor::class.java
    ) {

    @Autowired
    private lateinit var processor: ScOrFfLogisticPointCreationProcessor

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/sc_or_ff_creation/before/setup.xml"),
        DatabaseSetup("/db/sc_or_ff_creation/before/logistic_point_creation.xml", type = DatabaseOperation.REFRESH)
    )
    @ExpectedDatabase(
        "/db/sc_or_ff_creation/after/logistic_segments_creation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecute() = abstractTestExecute(
        processor,
        initializeMocks = {
            whenever(lmsClient.createLogisticsPoint(eq(LMS_LOGISTIC_POINT_DTO)))
                .thenReturn(LogisticsPointResponse.newBuilder().id(LOGISTICS_POINT_ID).build())
        },
        verifyExecution = {
            verify(lmsClient).createLogisticsPoint(eq(LMS_LOGISTIC_POINT_DTO))
        }
    )


    private companion object {
        val LMS_LOGISTIC_POINT_DTO = LogisticsPointCreateRequest.newBuilder()
            .partnerId(PARTNER_ID)
            .name("Парсел СЦ")
            .externalId("parcel-666")
            .type(PointType.WAREHOUSE)
            .contact(
                Contact("Парселен", "Парселенов", "Парселенович")
            )
            .phones(setOf(Phone("+77777777777", null, null, PhoneType.PRIMARY)))
            .address(createLmsAddress(null))
            .schedule(createLmsSchedule())
            .build()!!
    }
}
