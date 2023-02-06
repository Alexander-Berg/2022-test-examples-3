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

internal class McDeliveryCargoTypesAddingProcessorTest :
    AbstractQueueProcessorTest<McDeliveryCargoTypesAddingProcessor>(McDeliveryPartnerActivationProcessor::class.java) {

    @Autowired
    private lateinit var processor: McDeliveryCargoTypesAddingProcessor

    @Test
    @DatabaseSetup("/db/mc_delivery_creation/before/cargo_types_adding.xml")
    @ExpectedDatabase(
        "/db/mc_delivery_creation/after/partner_activation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecute() = abstractTestExecute(
        processor,
        verifyExecution = {
            verify(lmsClient).addPartnerForbiddenCargoTypes(eq(PARTNER_ID), eq(setOf(110)))
        }
    )
}
