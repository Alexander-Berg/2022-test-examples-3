package ru.yandex.market.logistics.logistrator.queue.processor.mc_delivery_creation

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

internal class McDeliveryPartnerCreationProcessorTest :
    AbstractQueueProcessorTest<McDeliveryPartnerCreationProcessor>(
        McDeliveryPlatformClientsAddingProcessor::class.java
    ) {

    @Autowired
    private lateinit var processor: McDeliveryPartnerCreationProcessor

    @Test
    @DatabaseSetup("/db/mc_delivery_creation/before/partner_creation.xml")
    @ExpectedDatabase(
        "/db/mc_delivery_creation/after/platform_clients_adding.xml",
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
            .name("DoriDelivery")
            .readableName("Dori Delivery")
            .partnerType(PartnerType.DELIVERY)
            .subtypeId(2)
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
            .marketId(22224444)
            .locationId(1)
            .billingClientId(88362541)
            .build()!!
    }
}
