package ru.yandex.market.logistics.lom.controller.order.processing;

import java.time.Instant;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.enums.ApiType;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.async.CreateOrderErrorDto;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.lom.controller.order.OrderTestUtil.asyncOrderCreate;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class CreateOrderErrorControllerTest extends AbstractContextualTest {

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2019-06-12T00:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    /**
     * Заказ с указанным id существует.
     * В заказе существует сегмент путевого листа, указанный в BusinessProcessState.
     * В результате статус заказа меняется на PROCESSING_ERROR.
     * История изменений сохраняется.
     */
    @Test
    @DisplayName("Успешно проставлен статус ошибки для СД")
    @DatabaseSetup("/controller/order/processing/create/error/before/order_create_error.xml")
    @DatabaseSetup("/controller/order/processing/create/error/before/fulfillment_create_order_external_1_enqueued.xml")
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/after/ff_create_order_external_1_enqueued.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/before/order_create_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderError() throws Exception {
        asyncOrderCreate(
            mockMvc,
            "ff/createError",
            "controller/order/processing/create/error/request/order_create_error.json"
        )
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_CREATE_ORDER_ASYNC_ERROR_RESULT,
            PayloadFactory.createOrderErrorPayload(
                ApiType.FULFILLMENT,
                1L,
                1L,
                1L,
                new CreateOrderErrorDto(
                    1L,
                    null,
                    null,
                    "LOinttest-1",
                    1001L,
                    false
                ),
                "1",
                1L
            )
        );
    }

    @Test
    @DisplayName("Успешно проставлен статус ошибки для СЦ")
    @DatabaseSetup("/controller/order/processing/create/error/before/fulfillment_create_order_external_1_enqueued.xml")
    @DatabaseSetup("/controller/order/processing/create/error/before/order_create_error_sc_and_ds.xml")
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/before/order_create_error_sc_and_ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderErrorScAndDs() throws Exception {
        asyncOrderCreate(
            mockMvc,
            "ff/createError",
            "controller/order/processing/create/error/request/order_create_error_sc.json"
        )
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_CREATE_ORDER_ASYNC_ERROR_RESULT,
            PayloadFactory.createOrderErrorPayload(
                ApiType.FULFILLMENT,
                1L,
                1L,
                1L,
                new CreateOrderErrorDto(
                    1L,
                    400,
                    "lgw SC error",
                    "LOinttest-1",
                    1001L,
                    false
                ),
                "1",
                1L
            )
        );
    }

    @Test
    @DisplayName("Успешно проставлен статус ошибки для СЦ сегмента с отменой")
    @DatabaseSetup("/controller/order/processing/create/error/before/fulfillment_create_order_external_1_enqueued.xml")
    @DatabaseSetup("/controller/order/processing/create/error/before/order_create_error_sc_and_ds_cancelled.xml")
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/before/order_create_error_sc_and_ds_cancelled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderErrorScAndDsCancelled() throws Exception {
        asyncOrderCreate(
            mockMvc,
            "ff/createError",
            "controller/order/processing/create/error/request/order_create_error_sc.json"
        )
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_CREATE_ORDER_ASYNC_ERROR_RESULT,
            PayloadFactory.createOrderErrorPayload(
                ApiType.FULFILLMENT,
                1L,
                1L,
                1L,
                new CreateOrderErrorDto(
                    1L,
                    400,
                    "lgw SC error",
                    "LOinttest-1",
                    1001L,
                    false
                ),
                "1",
                1L
            )
        );
    }

    @Test
    @DisplayName("Не найден бизнес-процесс")
    @DatabaseSetup("/controller/order/processing/create/error/before/order_create_error_sc_and_ds.xml")
    @ExpectedDatabase(
        value = "/controller/order/processing/create/error/before/order_create_error_sc_and_ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void businessProcessNotFound() throws Exception {
        asyncOrderCreate(
            mockMvc,
            "ff/createError",
            "controller/order/processing/create/error/request/order_create_error.json"
        )
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [BUSINESS_PROCESS] with id [1001]"));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Заказ не найден")
    void orderNotFound() throws Exception {
        asyncOrderCreate(
            mockMvc,
            "ff/createError",
            "controller/order/processing/create/error/request/order_create_error.json"
        )
            .andExpect(status().isNotFound())
            .andExpect(content().json(extractFileContent(
                "controller/order/processing/create/error/response/order_not_found.json"
            )));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Невалидный запрос")
    void badRequest() throws Exception {
        mockMvc.perform(put("/orders/processing/ff/createError")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(content().json(extractFileContent(
                "controller/order/processing/create/error/response/bad_request.json"
            )));

        queueTaskChecker.assertNoQueueTasksCreated();
    }
}
