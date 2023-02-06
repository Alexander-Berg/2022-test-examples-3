package ru.yandex.market.logistics.nesu.controller.order;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.model.enums.tags.OrderTag;
import ru.yandex.market.logistics.nesu.base.order.AbstractSubmitOrderTest;
import ru.yandex.market.logistics.nesu.dto.order.OrdersSubmitRequest;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Оформление заказа")
@DatabaseSetup("/repository/settings/delivery_type_service_settings.xml")
@DatabaseSetup("/controller/order/submit/submit_data.xml")
class BackOfficeSubmitOrderTest extends AbstractSubmitOrderTest {

    private long shopId = SHOP_ID;

    @Test
    @DisplayName("Недоступный магазин")
    void unavailableShop() throws Exception {
        shopId = 1;

        submitOrders(List.of(102L))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/order/submit/submit_shop_not_available.json"));
    }

    @Nonnull
    @Override
    protected ResultActions submitOrders(OrdersSubmitRequest request) throws Exception {
        return mockMvc.perform(request(HttpMethod.POST, "/back-office/orders/submit", request)
            .param("userId", "100")
            .param("shopId", String.valueOf(shopId)));
    }

    @Nonnull
    @Override
    protected OrderTag getTag() {
        return OrderTag.COMMITTED_VIA_DAAS_BACK_OFFICE;
    }

}
