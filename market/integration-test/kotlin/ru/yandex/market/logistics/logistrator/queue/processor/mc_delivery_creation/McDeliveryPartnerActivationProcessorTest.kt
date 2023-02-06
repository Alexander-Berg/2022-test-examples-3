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
import ru.yandex.market.logistics.management.entity.request.MultipleEntitiesActivationRequest

internal class McDeliveryPartnerActivationProcessorTest :
    AbstractQueueProcessorTest<McDeliveryPartnerActivationProcessor>() {

    @Autowired
    private lateinit var processor: McDeliveryPartnerActivationProcessor

    @Test
    @DatabaseSetup("/db/mc_delivery_creation/before/partner_activation.xml")
    @ExpectedDatabase(
        "/db/mc_delivery_creation/after/processing_finished.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun testExecute() = abstractTestExecute(
        processor,
        verifyExecution = {
            verify(lmsClient).activateMultipleEntities(eq(
                MultipleEntitiesActivationRequest.newBuilder()
                    .partnerIdsForActivationWithAllLogisticPoints(setOf(PARTNER_ID))
                    .build()
            ))
        }
    )
}
