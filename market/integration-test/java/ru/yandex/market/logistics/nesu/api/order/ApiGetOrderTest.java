package ru.yandex.market.logistics.nesu.api.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.common.services.auth.blackbox.BlackboxService;
import ru.yandex.market.logistics.nesu.api.auth.ApiAuthHolder;
import ru.yandex.market.logistics.nesu.base.order.AbstractGetOrderTest;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение заказа в Open API")
@DatabaseSetup("/controller/order/get/data.xml")
class ApiGetOrderTest extends AbstractGetOrderTest {

    @Autowired
    private BlackboxService blackboxService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MbiApiClient mbiApiClient;

    private ApiAuthHolder authHolder;

    @BeforeEach
    void setupAuth() {
        authHolder = new ApiAuthHolder(blackboxService, objectMapper);

        authHolder.mockAccess(mbiApiClient, 1L);
    }

    @Test
    @DisplayName("Недоступный магазин")
    void inaccessibleShop() throws Exception {
        authHolder.mockNoAccess(mbiApiClient, 1L);

        getOrder()
            .andExpect(status().isNotFound());
    }

    @Override
    protected ResultActions getOrder() throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/" + orderId)
            .headers(authHolder.authHeaders()));
    }
}
