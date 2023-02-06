package ru.yandex.market.logistics.logistrator.queue.processor.mc_delivery_creation

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.logistrator.queue.processor.AbstractQueueProcessorTest
import ru.yandex.market.logistics.logistrator.utils.PARTNER_ID
import ru.yandex.market.logistics.management.entity.type.PartnerStatus

internal class McDeliveryPlatformClientsAddingProcessorTest :
    AbstractQueueProcessorTest<McDeliveryPlatformClientsAddingProcessor>(
        McDeliveryCapacitySettingProcessor::class.java
    ) {

    @Autowired
    private lateinit var processor: McDeliveryPlatformClientsAddingProcessor

    @Test
    @DatabaseSetup("/db/mc_delivery_creation/before/platform_clients_adding.xml")
    @ExpectedDatabase(
        "/db/mc_delivery_creation/after/capacity_setting.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecute() = abstractTestExecute(
        processor,
        verifyExecution = {
            (1L..2L).forEach { verify(lmsClient).setPlatformClient(eq(PARTNER_ID), eq(it), eq(PartnerStatus.ACTIVE)) }
        }
    )
}
