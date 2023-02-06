package ru.yandex.market.logistics.logistrator.queue.processor.mc_delivery_creation

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.logistrator.queue.processor.AbstractQueueProcessorTest
import ru.yandex.market.logistics.logistrator.utils.PARTNER_ID
import ru.yandex.market.logistics.management.entity.request.partner.PartnerExternalParamRequest
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType

internal class McDeliveryParametersSettingProcessorTest :
    AbstractQueueProcessorTest<McDeliveryParametersSettingProcessor>(
        McDeliveryCargoTypesAddingProcessor::class.java
    ) {

    @Autowired
    private lateinit var processor: McDeliveryParametersSettingProcessor

    @Test
    @DatabaseSetup("/db/mc_delivery_creation/before/parameters_setting.xml")
    @ExpectedDatabase(
        "/db/mc_delivery_creation/after/cargo_types_adding.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecute() = abstractTestExecute(
        processor,
        verifyExecution = {
            verify(lmsClient).addOrUpdatePartnerExternalParams(PARTNER_ID, LMS_PARTNER_EXTERNAL_PARAM_REQUESTS)
        }
    )

    private companion object {
        val LMS_PARTNER_EXTERNAL_PARAM_REQUESTS = listOf(
            PartnerExternalParamRequest(PartnerExternalParamType.DESCRIPTION, "Dorivery"),
            PartnerExternalParamRequest(PartnerExternalParamType.MARKET_PICKUP_AVAILABLE, "false")
        )
    }
}
