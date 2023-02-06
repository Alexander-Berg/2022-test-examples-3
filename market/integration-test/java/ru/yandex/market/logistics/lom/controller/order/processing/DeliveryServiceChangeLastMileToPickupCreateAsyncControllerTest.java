package ru.yandex.market.logistics.lom.controller.order.processing;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.enums.ApiType;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.async.CreateOrderErrorDto;
import ru.yandex.market.logistics.lom.model.async.CreateOrderSuccessDto;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Обработка заявок на изменение типа доставки на ПВЗ на сегменте PICKUP"
    + " после получения ответа на асинхронный запрос ds-create-order.")
@ParametersAreNonnullByDefault
@DatabaseSetup("/controller/order/processing/change_last_mile_to_pickup/setup.xml")
public class DeliveryServiceChangeLastMileToPickupCreateAsyncControllerTest extends AbstractContextualTest {
    private static final Long PICKUP_PARTNER_ID = 50L;
    private static final String BARCODE = "1001";
    private static final String EXTERNAL_ID = "pickup-external-id";

    @Test
    @DisplayName("Успешный ответ, создана задача PROCESS_CREATE_ORDER_ASYNC_SUCCESS_RESULT, заявки на сегментах "
        + "PICKUP и COURIER все еще PROCESSING")
    @DatabaseSetup("/controller/order/processing/change_last_mile_to_pickup/create/before/segment_request.xml")
    @ExpectedDatabase(
        value = "/controller/order/processing/change_last_mile_to_pickup/create/after/processing_enqueued.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successResponse() throws Exception {
        performSuccessRequest(new CreateOrderSuccessDto(EXTERNAL_ID, PICKUP_PARTNER_ID, BARCODE, 10L))
            .andExpect(status().isOk());
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_CREATE_ORDER_ASYNC_SUCCESS_RESULT,
            PayloadFactory.createOrderSuccessPayload(
                ApiType.DELIVERY,
                PICKUP_PARTNER_ID,
                4L,
                1L,
                new CreateOrderSuccessDto(
                    EXTERNAL_ID,
                    PICKUP_PARTNER_ID,
                    BARCODE,
                    10L
                ),
                "1",
                1L
            )
        );
    }

    @Test
    @DisplayName("Обработка ответа об ошибке, создана задача PROCESS_CREATE_ORDER_ASYNC_ERROR_RESULT, "
        + "заявки на сегментах все еще PROCESSING")
    @DatabaseSetup(
        value = "/controller/order/processing/change_last_mile_to_pickup/create/before/segment_request.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/change_last_mile_to_pickup/create/after/error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void errorResponse() throws Exception {
        performErrorRequest(new CreateOrderErrorDto(PICKUP_PARTNER_ID, null, null, BARCODE, 10L, false))
            .andExpect(status().isOk());
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.PROCESS_CREATE_ORDER_ASYNC_ERROR_RESULT,
            PayloadFactory.createOrderErrorPayload(
                ApiType.DELIVERY,
                PICKUP_PARTNER_ID,
                4L,
                1L,
                new CreateOrderErrorDto(
                    PICKUP_PARTNER_ID,
                    null,
                    null,
                    BARCODE,
                    10L,
                    false
                ),
                "1",
                1L
            )
        );
    }

    @Nonnull
    private ResultActions performSuccessRequest(CreateOrderSuccessDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/processing/ds/createSuccess", request));
    }

    @Nonnull
    private ResultActions performErrorRequest(CreateOrderErrorDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/processing/ds/createError", request));
    }
}
