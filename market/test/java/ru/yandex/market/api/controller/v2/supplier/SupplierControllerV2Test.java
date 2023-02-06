package ru.yandex.market.api.controller.v2.supplier;

import org.junit.Test;
import ru.yandex.market.api.controller.v2.SupplierControllerV2;
import ru.yandex.market.api.error.NotFoundException;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.util.httpclient.clients.ShopInfoTestClient;

import javax.inject.Inject;

public class SupplierControllerV2Test extends BaseTest {

    @Inject
    private SupplierControllerV2 controller;

    @Inject
    private ShopInfoTestClient shopInfoTestClient;

    @Test
    public void notFoundIfSupplierNotInShopInfo() {
        shopInfoTestClient.supplier(1L, "supplier-empty.json");
        exception.expect(NotFoundException.class);
        expectMessage("Supplier", "1", "not found");
        controller.getSupplierById(1L).waitResult();
    }
}
