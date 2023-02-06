package ru.yandex.market.logistics.nesu.api.order;

import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.services.auth.blackbox.BlackboxService;
import ru.yandex.market.logistics.nesu.api.auth.ApiAuthHolder;
import ru.yandex.market.logistics.nesu.base.order.AbstractUpdateRecipientTest;
import ru.yandex.market.logistics.nesu.dto.order.OrderUpdateRecipientRequest;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Обновление получателя заказа в Open API")
class ApiUpdateRecipientTest extends AbstractUpdateRecipientTest {

    @Autowired
    private BlackboxService blackboxService;

    @Autowired
    private MbiApiClient mbiApiClient;

    private ApiAuthHolder authHolder;

    @BeforeEach
    void setupAuth() {
        authHolder = new ApiAuthHolder(blackboxService, objectMapper);
        authHolder.mockAccess(mbiApiClient, SENDER_ID);
    }

    @Test
    @DisplayName("Недоступный магазин")
    void inaccessibleShop() throws Exception {
        authHolder.mockNoAccess(mbiApiClient, SENDER_ID);

        updateRecipient(ORDER_ID, defaultUpdateRecipientRequest())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SENDER] with ids [1]"));

        verify(lomClient).getOrder(ORDER_ID, Set.of());
    }

    @Nonnull
    @Override
    protected ResultActions updateRecipient(long orderId, OrderUpdateRecipientRequest updateRequest)
        throws Exception {
        return mockMvc.perform(request(
            HttpMethod.PUT,
            "/api/orders/" + orderId + "/update-recipient",
            updateRequest
        )
            .headers(authHolder.authHeaders()));
    }
}
