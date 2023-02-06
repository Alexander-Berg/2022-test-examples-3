package ru.yandex.market.logistics.logistrator.queue.processor.sc_or_ff_creation

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.logistrator.queue.processor.AbstractQueueProcessorTest
import ru.yandex.market.logistics.logistrator.utils.PARTNER_ID
import ru.yandex.market.logistics.logistrator.utils.createLmsAddress
import ru.yandex.market.logistics.management.entity.request.partner.CreatePartnerDto
import ru.yandex.market.logistics.management.entity.request.partner.LegalInfoDto
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse
import ru.yandex.market.logistics.management.entity.type.PartnerType

internal class ScOrFfPartnerCreationProcessorTest :
    AbstractQueueProcessorTest<ScOrFfPartnerCreationProcessor>(
        ScOrFfLogisticPointCreationProcessor::class.java
    ) {

    @Autowired
    private lateinit var processor: ScOrFfPartnerCreationProcessor

    @Test
    @DatabaseSetup("/db/sc_or_ff_creation/before/setup.xml")
    @ExpectedDatabase(
        "/db/sc_or_ff_creation/after/logistic_point_creation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecute() = abstractTestExecute(
        processor,
        initializeMocks = {
            whenever(lmsClient.createPartner(eq(LMS_PARTNER_DTO)))
                .thenReturn(PartnerResponse.newBuilder().id(PARTNER_ID).build())
        },
        verifyExecution = {
            verify(lmsClient).createPartner(eq(LMS_PARTNER_DTO))
        }
    )

    private companion object {
        val LMS_PARTNER_DTO = CreatePartnerDto.newBuilder()
            .id(123)
            .name("Парсел Бар СЦ Лимитед")
            .readableName("Сорри, коллеги...")
            .partnerType(PartnerType.SORTING_CENTER)
            .subtypeId(42)
            .legalInfo(
                LegalInfoDto.newBuilder()
                    .id(1)
                    .incorporation("ООО \"Парсел Бар\"")
                    .ogrn(666)
                    .url("https://parcel.bar/")
                    .legalForm("ООО")
                    .inn("777")
                    .phone("+77777777777")
                    .legalAddress(createLmsAddress())
                    .postAddress(createLmsAddress())
                    .build()
            )
            .marketId(12345)
            .locationId(213)
            .build()!!
    }
}
