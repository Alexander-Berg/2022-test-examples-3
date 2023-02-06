package ru.yandex.market.mbi.api.controller.contact;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.open.api.client.model.SyncWarehouseRolesRequest;

/**
 * Тесты для {@link ContactController}.
 */
@DbUnitDataSet(before = "ContactControllerTest.before.csv")
public class ContactControllerTest extends FunctionalTest {
    /**
     * Проверяет успешную синхронизацию ролей.
     */
    @Test
    @DbUnitDataSet(after = "testSyncWarehouseRoles.after.csv")
    void testSyncWarehouseRoles() {
        getMbiOpenApiClient().syncWarehouseRoles(1,
                new SyncWarehouseRolesRequest().warehouseIds(List.of(10L, 20L, 30L)));
    }
}
