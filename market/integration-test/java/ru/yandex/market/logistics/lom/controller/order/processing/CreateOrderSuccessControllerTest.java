package ru.yandex.market.logistics.lom.controller.order.processing;

import java.time.Instant;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.enums.ApiType;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.async.CreateOrderSuccessDto;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.lom.controller.order.OrderTestUtil.asyncOrderCreate;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class CreateOrderSuccessControllerTest extends AbstractContextualTest {

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2019-06-12T00:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    /**
     * Заказ с указанным id существует.
     * В заказе существует сегмент путевого листа, указанный в BusinessProcessState, с еще незаполненным externalId.
     * После теста у сегмента появляется значение externalId. История изменений сохраняется.
     */
    @Test
    @DisplayName("Успешный сценарий для СД")
    @DatabaseSetup("/controller/order/processing/create/success/before/order_create_success.xml")
    @DatabaseSetup(
        "/controller/order/processing/create/success/before/delivery_service_create_order_external_1_enqueued.xml"
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/ds_create_order_external_1_success_enqueued.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/before/order_create_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderSuccess() throws Exception {
        asyncOrderCreate(
            mockMvc,
            "ds/createSuccess",
            "controller/order/processing/create/success/request/order_create_success.json"
        )
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_CREATE_ORDER_ASYNC_SUCCESS_RESULT,
            PayloadFactory.createOrderSuccessPayload(
                ApiType.DELIVERY,
                1L,
                1L,
                1L,
                new CreateOrderSuccessDto(
                    "test-external-id-2",
                    1L,
                    "LOinttest-1",
                    1001L
                ),
                "1",
                1L
            )
        );
    }

    @Test
    @DisplayName("Успешный сценарий для СД после отмены")
    @DatabaseSetup("/controller/order/processing/create/success/before/order_create_success_cancelled.xml")
    @DatabaseSetup(
        "/controller/order/processing/create/success/before/delivery_service_create_order_external_1_enqueued.xml"
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/after/ds_create_order_external_1_success_enqueued.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/before/order_create_success_cancelled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderSuccessCancelled() throws Exception {
        asyncOrderCreate(
            mockMvc,
            "ds/createSuccess",
            "controller/order/processing/create/success/request/order_create_success.json"
        )
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_CREATE_ORDER_ASYNC_SUCCESS_RESULT,
            PayloadFactory.createOrderSuccessPayload(
                ApiType.DELIVERY,
                1L,
                1L,
                1L,
                new CreateOrderSuccessDto(
                    "test-external-id-2",
                    1L,
                    "LOinttest-1",
                    1001L
                ),
                "1",
                1L
            )
        );
    }

    @Test
    @DisplayName("Успешный сценарий для СД - возвратный СЦ")
    @DatabaseSetup("/controller/order/processing/create/success/before/order_create_success_return.xml")
    @DatabaseSetup("/service/business_process_state/delivery_service_create_order_external_1_enqueued.xml")
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/before/order_create_success_return.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void createOrderSuccessReturn() throws Exception {
        asyncOrderCreate(
            mockMvc,
            "ds/createSuccess",
            "controller/order/processing/create/success/request/order_create_success_return.json"
        )
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_CREATE_ORDER_ASYNC_SUCCESS_RESULT,
            PayloadFactory.createOrderSuccessPayload(
                ApiType.DELIVERY,
                2L,
                1L,
                1L,
                new CreateOrderSuccessDto(
                    "test-external-id-2",
                    2L,
                    "LOinttest-1",
                    1001L
                ),
                "1",
                1L
            )
        );

        asyncOrderCreate(
            mockMvc,
            "ff/createSuccess",
            "controller/order/processing/create/success/request/order_create_success_sc_return.json"
        )
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_CREATE_ORDER_ASYNC_SUCCESS_RESULT,
            PayloadFactory.createOrderSuccessPayload(
                ApiType.FULFILLMENT,
                1L,
                2L,
                1L,
                new CreateOrderSuccessDto(
                    "test-external-id-1",
                    1L,
                    "LOinttest-1",
                    1002L
                ),
                "2",
                2L
            )
        );
    }

    @Test
    @DisplayName("Идемпотентность метода")
    @DatabaseSetup("/controller/order/processing/create/success/before/order_create_success.xml")
    @DatabaseSetup(
        "/controller/order/processing/create/success/before/delivery_service_create_order_external_1_enqueued.xml"
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/create/success/before/order_create_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void alreadyReported() throws Exception {
        asyncOrderCreate(
            mockMvc,
            "ds/createSuccess",
            "controller/order/processing/create/success/request/order_create_success_already_reported.json"
        )
            .andExpect(status().isOk());

        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_CREATE_ORDER_ASYNC_SUCCESS_RESULT,
            PayloadFactory.createOrderSuccessPayload(
                ApiType.DELIVERY,
                2L,
                1L,
                1L,
                new CreateOrderSuccessDto(
                    "test-external-id-1",
                    2L,
                    "LOinttest-1",
                    1001L
                ),
                "1",
                1L
            )
        );
    }

    @Test
    @DisplayName("Заказ не найден")
    void orderNotFound() throws Exception {
        asyncOrderCreate(
            mockMvc,
            "ds/createSuccess",
            "controller/order/processing/create/success/request/order_create_success.json"
        )
            .andExpect(status().isNotFound())
            .andExpect(content().json(extractFileContent(
                "controller/order/processing/create/success/response/order_not_found.json"
            )));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Невалидный запрос")
    void badRequest() throws Exception {
        asyncOrderCreate(
            mockMvc,
            "ds/createSuccess",
            "controller/order/processing/create/success/request/empty.json"
        )
            .andExpect(status().isBadRequest())
            .andExpect(content().json(extractFileContent(
                "controller/order/processing/create/success/response/bad_request.json"
            )));

        queueTaskChecker.assertNoQueueTasksCreated();
    }
}
