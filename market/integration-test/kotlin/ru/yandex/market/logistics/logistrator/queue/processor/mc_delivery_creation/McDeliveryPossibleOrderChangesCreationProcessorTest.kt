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
import ru.yandex.market.logistics.management.entity.request.possibleOrderChanges.PossibleOrderChangeRequest
import ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeMethod
import ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeType

internal class McDeliveryPossibleOrderChangesCreationProcessorTest :
    AbstractQueueProcessorTest<McDeliveryPossibleOrderChangesCreationProcessor>(
        McDeliveryParametersSettingProcessor::class.java
    ) {

    @Autowired
    private lateinit var processor: McDeliveryPossibleOrderChangesCreationProcessor

    @Test
    @DatabaseSetup("/db/mc_delivery_creation/before/possible_order_changes_creation.xml")
    @ExpectedDatabase(
        "/db/mc_delivery_creation/after/parameters_setting.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecute() = abstractTestExecute(
        processor,
        verifyExecution = {
            verify(lmsClient).createMultiplePossibleOrderChanges(eq(LMS_POSSIBLE_ORDER_CHANGES))
        }
    )

    private companion object {
        val LMS_POSSIBLE_ORDER_CHANGES = listOf(
            PossibleOrderChangeRequest.builder()
                .partnerId(PARTNER_ID)
                .type(PossibleOrderChangeType.SHOW_RUNNING_COURIER)
                .method(PossibleOrderChangeMethod.PARTNER_PHONE)
                .checkpointStatusFrom(140)
                .checkpointStatusTo(141)
                .enabled(true)
                .build()!!
        )
    }
}
