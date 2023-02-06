package ru.yandex.market.partner.security.checker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;

@DbUnitDataSet(before = "supplierWarehouseExistsCheckerTest.csv")
class SupplierWarehouseExistsCheckerTest extends FunctionalTest {

    @Autowired
    private SupplierWarehouseExistsChecker supplierWarehouseExistsChecker;

    @Test
    @DisplayName("Склад есть Дропшип")
    void testSupplierWarehouseExists() {
        Assertions.assertTrue(checkTyped(1L));
    }

    @Test
    @DisplayName("Склад есть ФФ")
    void testSupplierWarehouseExistsFF() {
        Assertions.assertTrue(checkTyped(3L));
    }

    @Test
    @DisplayName("Склада нет")
    void testSupplierWarehouseNotExists() {
        Assertions.assertFalse(checkTyped(2L));
    }

    @Test
    @DisplayName("Кампания не найдена")
    void testCampaignNotExist() {
        Assertions.assertFalse(checkTyped(404L));
    }

    private boolean checkTyped(long l) {
        return supplierWarehouseExistsChecker.checkTyped(
                new MockPartnerRequest(-1L, -1L, -1L, l),
                null);
    }
}
