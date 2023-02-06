package ru.yandex.market.logistics.lom.controller.order;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.common.collect.ImmutableSortedSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Инициация возврата закакзов")
class StartReturnSegmentProcessingTest extends AbstractContextualTest {

    @Test
    @DatabaseSetup("/controller/order/return/before/before_return_processing.xml")
    @ExpectedDatabase(
        value = "/controller/order/return/before/before_return_processing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Успешная инициция возврата заказов")
    void startReturnProcessingSuccessful() throws Exception {
        mockMvc.perform(
            post("/orders/return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/order/request/start_tracks.json"))
        )
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.REGISTER_DELIVERY_TRACK,
            PayloadFactory.createOrderIdPartnerIdWaybillSegmentIdPayload(4L, 1L, 5L, "1", 1L)
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.START_DELIVERY_TRACKS,
            PayloadFactory.createDeliveryTrackIdsPayload(ImmutableSortedSet.of(100L, 200L), "2", 2L)
        );
    }

    @Test
    @DatabaseSetup("/controller/order/return/before/before_return_processing.xml")
    @DatabaseSetup(
        value = "/controller/order/return/before/before_return_processing_wo_tracker_id.xml",
        type = DatabaseOperation.UPDATE
    )
    @DisplayName("Часть заказов без trackerId")
    void startReturnProcessingSkipOrdersWithoutTrackerId() throws Exception {
        mockMvc.perform(
            post("/orders/return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/order/request/start_tracks.json"))
        )
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.REGISTER_DELIVERY_TRACK,
            PayloadFactory.createOrderIdPartnerIdWaybillSegmentIdPayload(4L, 1L, 5L, "1", 1L)
        );

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.START_DELIVERY_TRACKS,
            PayloadFactory.createDeliveryTrackIdsPayload(Set.of(100L), "2", 2L)
        );
    }

    @Test
    @DisplayName("Попытка инициации возврата заказов, заказы не найдены")
    void noQueueTasksCreatedIfOrdersNotFound() throws Exception {
        mockMvc.perform(
            post("/orders/return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/order/request/start_tracks.json"))
        )
            .andExpect(status().isOk());

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Пустой список заказов")
    void noQueueTasksCreatedIfOrderListIsEmpty() throws Exception {
        mockMvc.perform(
            post("/orders/return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/order/request/start_tracks_empty_request.json"))
        )
            .andExpect(status().isBadRequest());

        queueTaskChecker.assertNoQueueTasksCreated();
    }
}
