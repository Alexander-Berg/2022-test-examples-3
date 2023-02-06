package ru.yandex.market.logistics.lom.controller.order;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderErrorDto;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderSuccessDto;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class FulfillmentUpdateOrderItemsAsyncResultControllerTest extends AbstractContextualTest {

    @Test
    @DisplayName("Обработка успешного ответа для changeRequest флоу")
    @DatabaseSetup("/controller/order/updateitems/async/before/setup_change_request_flow.xml")
    @ExpectedDatabase(
        value = "/controller/order/updateitems/async/after/success_async_result_change_request_flow.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void asyncResultSuccessProcessingChangeRequestFlow() throws Exception {
        sendAsyncResultSuccess(2L)
            .andExpect(status().isOk())
            .andExpect(noContent());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_ITEMS_REQUEST_STATUS_UPDATE,
            PayloadFactory.createChangeOrderSegmentRequestPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName("Обработка успешного ответа для changeRequest флоу")
    @DatabaseSetup("/controller/order/updateitems/async/before/setup_change_request_flow.xml")
    @DatabaseSetup(
        value = "/controller/order/updateitems/async/before/business_process_without_change_order_segment.xml",
        type = DatabaseOperation.INSERT
    )
    void asyncResultSuccessProcessingChangeRequestFlowBusinessProcessWithoutEntity() throws Exception {
        sendAsyncResultSuccess(3L)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "BusinessProcessState with id 2 has no entity with type CHANGE_ORDER_SEGMENT_REQUEST"
            ));
    }

    @Test
    @DisplayName("Обработка ответа об ошибке для changeRequest флоу")
    @DatabaseSetup("/controller/order/updateitems/async/before/setup_change_request_flow.xml")
    @ExpectedDatabase(
        value = "/controller/order/updateitems/async/after/error_async_result_change_request_flow.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void asyncResultErrorProcessingChangeRequestFlow() throws Exception {
        sendAsyncResultError(2L)
            .andExpect(status().isOk())
            .andExpect(noContent());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.UPDATE_ORDER_ITEMS_REQUEST_STATUS_UPDATE,
            PayloadFactory.createChangeOrderSegmentRequestPayload(1, "1", 1)
        );
    }

    @Test
    @DisplayName("Обработка успешного ответа для changeRequest флоу - у задачи нет сущности заявки заказа на сегменте")
    @DatabaseSetup("/controller/order/updateitems/async/before/setup_change_request_flow.xml")
    @DatabaseSetup(
        value = "/controller/order/updateitems/async/before/business_process_without_change_order_segment.xml",
        type = DatabaseOperation.INSERT
    )
    void asyncResultErrorProcessingChangeRequestFlowBusinessProcessWithoutEntity() throws Exception {
        sendAsyncResultError(3L)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "BusinessProcessState with id 2 has no entity with type CHANGE_ORDER_SEGMENT_REQUEST"
            ));
    }

    @Nonnull
    private ResultActions sendAsyncResultSuccess(Long sequenceId) throws Exception {
        return mockMvc.perform(request(
            HttpMethod.PUT,
            "/orders/ff/update-items/success",
            new UpdateOrderSuccessDto("LOinttest-1", 1L, sequenceId)
        ));
    }

    @Nonnull
    private ResultActions sendAsyncResultError(Long sequenceId) throws Exception {
        return mockMvc.perform(request(
            HttpMethod.PUT,
            "/orders/ff/update-items/error",
            new UpdateOrderErrorDto("LOinttest-1", 1L, sequenceId, "Something went wrong", 9999, false)
        ));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ValueSource(strings = {"/orders/ff/update-items/success", "/orders/ff/update-items/error"})
    @DisplayName("Невалидный запрос")
    void badRequest(String url) throws Exception {
        mockMvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/order/updateitems/async/response/validation_error.json"));
    }
}
