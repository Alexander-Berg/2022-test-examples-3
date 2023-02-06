package ru.yandex.market.logistics.nesu.api.order;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.services.auth.blackbox.BlackboxService;
import ru.yandex.market.logistics.nesu.api.auth.ApiAuthHolder;
import ru.yandex.market.logistics.nesu.base.order.AbstractOrderStatusHistoryTest;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение истории статусов заказа в Open API")
@DatabaseSetup("/repository/settings/delivery_type_service_settings.xml")
@DatabaseSetup("/repository/order/database_order_prepare.xml")
class ApiOrderStatusHistoryTest extends AbstractOrderStatusHistoryTest {

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
    void noShopAccess() throws Exception {
        authHolder.mockNoAccess(mbiApiClient, 1L);

        getHistory()
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/order/statuses/not_found_sender.json"));
    }

    @Nonnull
    @Override
    protected ResultActions getHistory() throws Exception {
        return mockMvc.perform(
            get("/api/orders/" + ORDER_ID + "/statuses")
                .headers(authHolder.authHeaders())
        );
    }

}
