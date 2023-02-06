package ru.yandex.market.logistics.nesu.controller.order;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.base.order.AbstractOrderStatusHistoryTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение истории статусов заказа")
@DatabaseSetup("/repository/settings/delivery_type_service_settings.xml")
@DatabaseSetup("/repository/order/database_order_prepare.xml")
public class BackOfficeOrderStatusHistoryTest extends AbstractOrderStatusHistoryTest {

    @Test
    @DisplayName("Несуществующий магазин")
    void invalidShop() throws Exception {
        getHistory(3L)
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/order/statuses/not_found_sender.json"));
    }

    @Test
    @DisplayName("Недоступный магазин")
    void noAccessToShop() throws Exception {
        getHistory(2L)
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/order/statuses/not_found_sender.json"));
    }

    @Nonnull
    @Override
    protected ResultActions getHistory() throws Exception {
        return getHistory(1L);
    }

    @Nonnull
    private ResultActions getHistory(Long shopId) throws Exception {
        return mockMvc.perform(
            get("/back-office/orders/" + ORDER_ID + "/statuses")
                .param("shopId", String.valueOf(shopId))
                .param("userId", "-100")
        );
    }
}
