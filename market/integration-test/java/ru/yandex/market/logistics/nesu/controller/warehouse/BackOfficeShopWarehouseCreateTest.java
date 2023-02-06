package ru.yandex.market.logistics.nesu.controller.warehouse;

import javax.annotation.Nonnull;

import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.base.AbstractShopWarehouseCreateTest;
import ru.yandex.market.logistics.nesu.request.warehouse.ShopWarehouseCreateRequest;

class BackOfficeShopWarehouseCreateTest extends AbstractShopWarehouseCreateTest<ShopWarehouseCreateRequest> {
    @Nonnull
    @Override
    protected String getObjectName() {
        return "shopWarehouseCreateRequest";
    }

    @Nonnull
    @Override
    protected ResultActions createWarehouse(Long shopId, ShopWarehouseCreateRequest request) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.POST, "/back-office/warehouses", request)
                .param("userId", "19216801")
                .param("shopId", String.valueOf(shopId))
        );
    }

    @Nonnull
    @Override
    protected ShopWarehouseCreateRequest createMinimalRequest() {
        return new ShopWarehouseCreateRequest();
    }
}
