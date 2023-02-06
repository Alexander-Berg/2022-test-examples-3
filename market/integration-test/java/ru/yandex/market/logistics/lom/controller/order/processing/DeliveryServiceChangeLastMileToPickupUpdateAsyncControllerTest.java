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
import ru.yandex.market.logistics.lom.model.async.UpdateOrderErrorDto;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderSuccessDto;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Обработка заявок на изменение типа доставки на ПВЗ на сегменте MOVEMENT"
    + " после получения ответа на асинхронный запрос ds-update-order.")
@ParametersAreNonnullByDefault
@DatabaseSetup("/controller/order/processing/change_last_mile_to_pickup/setup.xml")
public class DeliveryServiceChangeLastMileToPickupUpdateAsyncControllerTest extends AbstractContextualTest {

    private static final Long MK_PARTNER_ID = 49L;
    private static final String BARCODE = "1001";

    @Test
    @DisplayName("Успешный ответ и обработка change_order_segment_request")
    @DatabaseSetup(
        value = "/controller/order/processing/change_last_mile_to_pickup/update/before/segment_request.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/change_last_mile_to_pickup/update/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successResponse() throws Exception {
        performSuccessRequest(new UpdateOrderSuccessDto(BARCODE, MK_PARTNER_ID, 10L))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Обработка ответа об ошибке")
    @DatabaseSetup(
        value = "/controller/order/processing/change_last_mile_to_pickup/update/before/segment_request.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/controller/order/processing/change_last_mile_to_pickup/update/after/error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void errorResponse() throws Exception {
        performErrorRequest(new UpdateOrderErrorDto(BARCODE, MK_PARTNER_ID, 10L, null, null, false))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешный ответ - у задачи нет сущности заявки изменения сегмента заказа")
    @DatabaseSetup(
        value = "/controller/order/processing/change_last_mile_to_pickup/update/before/no_segment_request.xml",
        type = DatabaseOperation.INSERT
    )
    void successResponseBusinessProcessWithoutEntity() throws Exception {
        performSuccessRequest(new UpdateOrderSuccessDto(BARCODE, MK_PARTNER_ID, 3L))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "BusinessProcessState with id 2 has no entity with type CHANGE_ORDER_SEGMENT_REQUEST"
            ));
    }

    @Test
    @DisplayName("Успешный ответ - невалидный sequenceId")
    @ExpectedDatabase(
        value = "/controller/order/processing/change_last_mile_to_pickup/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successResponseInvalidSequenceId() throws Exception {
        performSuccessRequest(new UpdateOrderSuccessDto(BARCODE, MK_PARTNER_ID, 1L))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [BUSINESS_PROCESS] with id [1]"));

    }

    @Nonnull
    private ResultActions performSuccessRequest(UpdateOrderSuccessDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/processing/ds/updateSuccess", request));
    }

    @Nonnull
    private ResultActions performErrorRequest(UpdateOrderErrorDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/orders/processing/ds/updateError", request));
    }
}
