package ru.yandex.market.logistics.nesu.controller.warehouse;

import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.base.AbstractShopWarehouseReplaceTest;
import ru.yandex.market.logistics.nesu.request.warehouse.ShopWarehouseUpdateRequest;

import static ru.yandex.market.logistics.nesu.model.ModelFactory.scheduleDay;

@DisplayName("Замена склада для DAAS через личный кабинет")
class BackOfficeShopWarehouseReplaceTest extends AbstractShopWarehouseReplaceTest<ShopWarehouseUpdateRequest> {
    @Nonnull
    @Override
    protected ShopWarehouseUpdateRequest createMinimalValidRequest() {
        ShopWarehouseUpdateRequest request = new ShopWarehouseUpdateRequest();
        request.setSchedule(Set.of(scheduleDay()));

        return request;
    }

    @Override
    @Nonnull
    protected ResultActions updateWarehouse(Long shopId, ShopWarehouseUpdateRequest request) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.POST, "/back-office/warehouses/1", request)
                .param("userId", "19216801")
                .param("shopId", String.valueOf(shopId))
        );
    }

    @Nonnull
    @Override
    protected String getObjectName() {
        return "shopWarehouseUpdateRequest";
    }
}
