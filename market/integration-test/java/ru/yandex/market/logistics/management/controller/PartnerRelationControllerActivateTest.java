package ru.yandex.market.logistics.management.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.queue.producer.LogisticSegmentValidationProducer;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup({
    "/data/controller/partnerRelation/prepare_data.xml",
    "/data/controller/partnerRelation/additional_logistic_segment_services.xml"
})
class PartnerRelationControllerActivateTest extends AbstractContextualTest {

    @Autowired
    private LogisticSegmentValidationProducer logisticSegmentValidationProducer;

    @BeforeEach
    void setup() {
        doNothing().when(logisticSegmentValidationProducer).produceTask(anyLong());
    }

    @AfterEach
    void teardown() {
        verifyNoMoreInteractions(logisticSegmentValidationProducer);
    }

    @Test
    @DisplayName("Связка изначально неактивна, оба партнера неактивны. Активируем только связку.")
    @ExpectedDatabase(
        value = "/data/controller/partnerRelation/after/activated_logistic_segment_services_4_none.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testInactiveRelationToActiveNone() throws Exception {
        executeActivateRelation(4, "activate_partner_relation_none.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName(
        "Связка изначально неактивна, оба партнера неактивны. Передан сегмент перемещения для активации. "
            + "Активируем связку и сегмент перемещения."
    )
    @ExpectedDatabase(
        value = "/data/controller/partnerRelation/after/activated_logistic_segment_services_4_with_movement.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testInactiveRelationToActiveWithMovement() throws Exception {
        executeActivateRelation(4, "activate_partner_relation_with_movement.json")
            .andExpect(status().isOk());
        verify(logisticSegmentValidationProducer).produceTask(6);
    }

    @Test
    @DisplayName(
        "Связка изначально неактивна, оба партнера неактивны. Активируем связку, активируем только from-партнера и "
            + "его зависимости."
    )
    @ExpectedDatabase(
        value = "/data/controller/partnerRelation/after/activated_logistic_segment_services_4_from.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testInactiveRelationToActiveFrom() throws Exception {
        executeActivateRelation(4, "activate_partner_relation_from.json")
            .andExpect(status().isOk());
        verify(logisticSegmentValidationProducer).produceTask(2);
    }

    @Test
    @DisplayName(
        "Связка изначально неактивна, оба партнера неактивны. Активируем связку, активируем только to-партнера и его "
            + "зависимости."
    )
    @ExpectedDatabase(
        value = "/data/controller/partnerRelation/after/activated_logistic_segment_services_4_to.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testInactiveRelationToActiveTo() throws Exception {
        executeActivateRelation(4, "activate_partner_relation_to.json")
            .andExpect(status().isOk());
        verify(logisticSegmentValidationProducer).produceTask(3);
    }

    @Test
    @DisplayName(
        "Связка изначально неактивна, оба партнера неактивны. Активируем связку, активируем обоих партнеров и их "
            + "зависимости."
    )
    @ExpectedDatabase(
        value = "/data/controller/partnerRelation/after/activated_logistic_segment_services_4_both.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testInactiveRelationToActiveBoth() throws Exception {
        executeActivateRelation(4, "activate_partner_relation_both.json")
            .andExpect(status().isOk());
        verify(logisticSegmentValidationProducer).produceTask(2);
        verify(logisticSegmentValidationProducer).produceTask(3);
    }

    @Test
    @DisplayName(
        "Связка изначально неактивна, активен только to-партнер. Активируем связку и обоих партнеров, активация "
            + "to-партнера не активирует его зависимости."
    )
    @ExpectedDatabase(
        value = "/data/controller/partnerRelation/after/activated_logistic_segment_services_5_both.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testInactiveRelationToActiveAlreadyActivePartnerIntact() throws Exception {
        executeActivateRelation(5, "activate_partner_relation_both.json")
            .andExpect(status().isOk());
        verify(logisticSegmentValidationProducer).produceTask(4);
    }

    @Test
    @DisplayName(
        "Связка изначально неактивна, активен только to-партнер. Активируем связку и обоих партнеров, активируются "
            + "только зависимости ранее не активного from-партнера."
    )
    @ExpectedDatabase(
        value = "/data/controller/partnerRelation/after/activated_logistic_segment_services_5_to.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testInactiveRelationToActivePartiallyIntact() throws Exception {
        executeActivateRelation(5, "activate_partner_relation_to.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Связка изначально активна. Активация приводит к ошибке.")
    @ExpectedDatabase(
        value = "/data/controller/partnerRelation/after/activated_logistic_segment_services_intact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testAlreadyActiveRelation() throws Exception {
        executeActivateRelation(2, "activate_partner_relation_both.json")
            .andExpect(status().isConflict());
    }

    private ResultActions executeActivateRelation(long partnerRelationId, String path) throws Exception {
        return mockMvc.perform(
            MockMvcRequestBuilders
                .request(HttpMethod.PUT, "/externalApi/partner-relation/{id}/activate", partnerRelationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/partnerRelation/" + path))
        );
    }
}
