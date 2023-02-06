package ru.yandex.market.core.outlet.db;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.outlet.ShopWarehouse;
import ru.yandex.market.tags.Components;
import ru.yandex.market.tags.Features;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@Tags({
        @Tag(Components.MBI_BILLING),
        @Tag(Features.DB_INTEGRATION)
})
@DbUnitDataSet
public class DbShopWarehouseServiceTest extends FunctionalTest {
    private static final long SHOP_WAREHOUSE_ID = 1;
    private static final long DATASOURCE_ID = 1234L;

    private static final ShopWarehouse SHOP_WAREHOUSE =
            ShopWarehouseTestUtils.getShopWarehouseForInsert(DATASOURCE_ID, SHOP_WAREHOUSE_ID);

    @Autowired
    private DbShopWarehouseService shopWarehouseService;

    @DisplayName("Проверяем создание, изменение, удаление и получение склада магазина")
    @Test
    public void manageShopWarehouse() {
        shopWarehouseService.createShopWarehouse(SHOP_WAREHOUSE);
        List<ShopWarehouse> shopWarehouses = shopWarehouseService.getShopWarehouses(singletonList(1L));

        assertThat("Size after insert", shopWarehouses.size(), equalTo(1));
        assertThat("ShopWarehouse after insert", shopWarehouses.get(0), equalTo(SHOP_WAREHOUSE));
    }

}
