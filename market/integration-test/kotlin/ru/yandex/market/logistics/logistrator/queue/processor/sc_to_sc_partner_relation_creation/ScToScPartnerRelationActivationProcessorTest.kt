package ru.yandex.market.logistics.logistrator.queue.processor.sc_to_sc_partner_relation_creation

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.logistrator.model.request.body.ScToScPartnerRelationCreationRequestBody
import ru.yandex.market.logistics.logistrator.queue.processor.AbstractQueueProcessorTest
import ru.yandex.market.logistics.logistrator.repository.RequestRepository
import ru.yandex.market.logistics.logistrator.utils.PARTNER_RELATION_ID
import ru.yandex.market.logistics.logistrator.utils.REQUEST_ID_PAYLOAD
import ru.yandex.market.logistics.management.entity.request.MultipleEntitiesActivationRequest

internal class ScToScPartnerRelationActivationProcessorTest :
    AbstractQueueProcessorTest<ScToScPartnerRelationActivationProcessor>() {

    @Autowired
    private lateinit var processor: ScToScPartnerRelationActivationProcessor

    @Autowired
    private lateinit var requestRepository: RequestRepository

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/sc_to_sc_partner_relation_creation/before/setup_movement_is_second_partner.xml"),
        DatabaseSetup(
            "/db/sc_to_sc_partner_relation_creation/before/activation_movement_is_second_partner.xml",
            type = DatabaseOperation.REFRESH
        )
    )
    @ExpectedDatabase(
        "/db/sc_to_sc_partner_relation_creation/after/processing_finished.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecuteMovementIsSecondPartner() {
        testExecute(setOf(1011))
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/db/sc_to_sc_partner_relation_creation/before/setup_movement_is_third_partner.xml"),
        DatabaseSetup(
            "/db/sc_to_sc_partner_relation_creation/before/activation_movement_is_third_partner.xml",
            type = DatabaseOperation.REFRESH
        )
    )
    @ExpectedDatabase(
        "/db/sc_to_sc_partner_relation_creation/after/processing_finished.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testExecuteMovementIsThirdPartner() {
        testExecute(setOf(1011, 1021))
    }

    private fun testExecute(activatedSegmentIds: Set<Long>) = abstractTestExecute(
        processor,
        initializeMocks = {
            val request = requestRepository.findById(REQUEST_ID_PAYLOAD.logistratorRequestId).orElseThrow()
            request.body = (request.body as ScToScPartnerRelationCreationRequestBody)
                .copy(partnerRelationId = PARTNER_RELATION_ID)
            requestRepository.save(request)
        },
        verifyExecution = {
            verify(lmsClient).activateMultipleEntities(eq(
                MultipleEntitiesActivationRequest.newBuilder()
                    .partnerIdsForActivationWithAllLogisticPoints(setOf(102))
                    .logisticSegmentIds(activatedSegmentIds)
                    .build()
            ))
        }
    )
}
