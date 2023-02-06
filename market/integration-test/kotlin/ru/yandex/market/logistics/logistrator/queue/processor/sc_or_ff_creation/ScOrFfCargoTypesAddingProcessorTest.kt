package ru.yandex.market.logistics.logistrator.queue.processor.sc_or_ff_creation

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.logistrator.queue.processor.AbstractQueueProcessorTest
import ru.yandex.market.logistics.logistrator.utils.PARTNER_ID

internal class ScOrFfCargoTypesAddingProcessorTest : AbstractQueueProcessorTest<ScOrFfCargoTypesAddingProcessor>(
    ScOrFfPartnerActivationProcessor::class.java,
) {

    @Autowired
    private lateinit var processor: ScOrFfCargoTypesAddingProcessor

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/sc_or_ff_creation/before/setup.xml"),
        DatabaseSetup("/db/sc_or_ff_creation/before/cargo_types_adding.xml", type = DatabaseOperation.REFRESH)
    )
    @ExpectedDatabase(
        "/db/sc_or_ff_creation/after/partner_activation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecute() = abstractTestExecute(
        processor,
        verifyExecution = {
            verify(lmsClient).addPartnerForbiddenCargoTypes(eq(PARTNER_ID), eq(setOf(302, 303)))
        }
    )
}
