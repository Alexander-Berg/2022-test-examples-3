package ru.yandex.market.logistics.nesu.controller.order;

import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.nesu.base.order.AbstractUpdateRecipientTest;
import ru.yandex.market.logistics.nesu.dto.order.OrderUpdateRecipientRequest;

import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Обновление данных о получателе заказа")
@ParametersAreNonnullByDefault
class BackOfficeUpdateRecipientTest extends AbstractUpdateRecipientTest {
    private static final long SHOP_ID = 1L;

    @Test
    @DisplayName("Недоступный магазин")
    void inaccessibleShop() throws Exception {
        mockMvc.perform(createRequest(ORDER_ID, defaultUpdateRecipientRequest(), -100L))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SENDER] with ids [1]"));

        verify(lomClient).getOrder(ORDER_ID, Set.of());
    }

    @Nonnull
    @Override
    protected ResultActions updateRecipient(long orderId, OrderUpdateRecipientRequest updateRequest)
        throws Exception {
        return mockMvc.perform(createRequest(orderId, updateRequest, SHOP_ID));
    }

    @Nonnull
    private MockHttpServletRequestBuilder createRequest(
        long orderId,
        OrderUpdateRecipientRequest updateRequest,
        long shopId
    ) throws Exception {
        return request(
            HttpMethod.PUT,
            "/back-office/orders/" + orderId + "/update-recipient",
            updateRequest
        )
            .param("userId", String.valueOf(SENDER_ID))
            .param("shopId", String.valueOf(shopId));
    }
}
