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

@ParametersAreNonnullByDefault
@DatabaseSetup("/controller/order/lastmile/async/before/setup.xml")
public class DeliveryServiceUpdateLastMileAsyncControllerTest extends AbstractContextualTest {

    @Test
    @DisplayName("Успешный ответ и обработка change_order_segment_request")
    @ExpectedDatabase(
        value = "/controller/order/lastmile/async/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successResponse() throws Exception {
        performSuccessRequest(new UpdateOrderSuccessDto("1001", 48L, 10L))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешный ответ - у задачи нет сущности заявки изменения сегмента заказа")
    @DatabaseSetup(
        value = "/controller/order/lastmile/async/before/business_process_without_cosr.xml",
        type = DatabaseOperation.INSERT
    )
    void successResponseBusinessProcessWithoutEntity() throws Exception {
        performSuccessRequest(new UpdateOrderSuccessDto("1001", 48L, 3L))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "BusinessProcessState with id 2 has no entity with type CHANGE_ORDER_SEGMENT_REQUEST"
            ));
    }

    @Test
    @DisplayName("Успешный ответ - невалидный sequenceId")
    @ExpectedDatabase(
        value = "/controller/order/lastmile/async/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void invalidSuccessResponseNotFoundSequenseId() throws Exception {
        performSuccessRequest(new UpdateOrderSuccessDto("1001", 48L, 1L))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [BUSINESS_PROCESS] with id [1]"));
    }

    @Test
    @DisplayName("Обработка ответа об ошибке")
    @ExpectedDatabase(
        value = "/controller/order/lastmile/async/after/error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void errorResponse() throws Exception {
        performErrorRequest(new UpdateOrderErrorDto("1001", 48L, 10L, null, null, false))
            .andExpect(status().isOk());
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
