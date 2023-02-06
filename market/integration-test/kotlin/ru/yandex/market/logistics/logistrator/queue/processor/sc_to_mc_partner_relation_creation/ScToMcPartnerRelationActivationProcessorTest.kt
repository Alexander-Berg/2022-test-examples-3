package ru.yandex.market.logistics.logistrator.queue.processor.sc_to_mc_partner_relation_creation

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.logistrator.queue.processor.AbstractQueueProcessorTest
import ru.yandex.market.logistics.management.entity.request.MultipleEntitiesActivationRequest

internal class ScToMcPartnerRelationActivationProcessorTest :
    AbstractQueueProcessorTest<ScToMcPartnerRelationActivationProcessor>() {

    @Autowired
    private lateinit var processor: ScToMcPartnerRelationActivationProcessor

    @Test
    @DatabaseSetup("/db/sc_to_mc_partner_relation_creation/before/activation_all_active.xml")
    @ExpectedDatabase(
        "/db/sc_to_mc_partner_relation_creation/after/processing_finished_all_active.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecuteAllActive() = abstractTestExecute(
        processor,
        verifyExecution = {
            verify(lmsClient).activateMultipleEntities(eq(
                MultipleEntitiesActivationRequest.newBuilder()
                    .partnerIdsForActivationWithAllLogisticPoints(setOf(101, 102))
                    .logisticSegmentIds(setOf(1013, 1014, 1015))
                    .build()
            ))
        }
    )

    @Test
    @DatabaseSetup("/db/sc_to_mc_partner_relation_creation/before/activation_all_partners_inactive.xml")
    @ExpectedDatabase(
        "/db/sc_to_mc_partner_relation_creation/after/processing_finished_all_partners_inactive.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecuteAllInactive() = abstractTestExecute(
        processor,
        verifyExecution = {
            verify(lmsClient).activateMultipleEntities(eq(
                MultipleEntitiesActivationRequest.newBuilder()
                    .logisticSegmentIds(setOf(1013, 1014, 1015))
                    .build()
            ))
        }
    )
}
