package ru.yandex.market.wms.radiator.service.stocks.infordb;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.ItemReference;
import ru.yandex.market.wms.radiator.service.config.Dispatcher;
import ru.yandex.market.wms.radiator.test.IntegrationTestBackend;
import ru.yandex.market.wms.radiator.test.TestData;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.wms.radiator.test.IntegrationTestConstants.WH_1_ID;
import static ru.yandex.market.wms.radiator.test.TestExpirationItemsData.M_SKU_AUTOLIFETIMECHANGED;
import static ru.yandex.market.wms.radiator.test.TestExpirationItemsData.VENDOR_ID;
import static ru.yandex.market.wms.radiator.test.TestExpirationItemsData.mSkuAutoLifetimeChanged;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/dbStocks/stocks-AUTOLIFETIMECHANGED.xml", connection = "wh1Connection"),
})
class InforDbExpirationItemsServiceTest extends IntegrationTestBackend {

    @Autowired
    private InforDbExpirationItemsService service;
    @Autowired
    private Dispatcher dispatcher;


    @Test
    @Disabled
    void getByIds() {
        var unitIds = List.of(
                TestData.unitId(M_SKU_AUTOLIFETIMECHANGED, VENDOR_ID)
        );
        // DBUNIT:
        // Column "BOM_ITEMS.BOM_SERIAL_KEY" not found;
        // ?
        doGetByIds(WH_1_ID, unitIds, List.of(mSkuAutoLifetimeChanged()));
    }

    private void doGetByIds(String warehouseId, List<UnitId> unitIds, List<ItemReference> expected) {
        dispatcher.withWarehouseId(
                warehouseId, () -> {
                    assertThat(service.getByIds(unitIds), is(equalTo(expected)));
                }
        );
    }
}
