package ru.yandex.market.wms.radiator.service.stocks.infordb;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.ItemReference;
import ru.yandex.market.wms.radiator.service.config.Dispatcher;
import ru.yandex.market.wms.radiator.test.IntegrationTestBackend;
import ru.yandex.market.wms.radiator.test.IntegrationTestConstants;
import ru.yandex.market.wms.radiator.test.TestData;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static ru.yandex.market.wms.radiator.test.TestReferenceItemsUpdatedData.*;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/dbStocks/updated.xml", connection = "wh1Connection")
})
public class InforDbStocksServiceTest_updated extends IntegrationTestBackend {

    @Autowired
    private InforDbReferenceItemsService service;

    @Autowired
    private Dispatcher dispatcher;

    @Test
    public void checkUpdatedMaxSku() {
        doCheckUpdated(List.of(TestData.unitId(CHECK_UPDATED_MAX_SKU, VENDOR_ID)), MAX_SKU_UPDATED_DATE);
    }

    @Test
    public void checkUpdatedMaxPack() {
        doCheckUpdated(List.of(TestData.unitId(CHECK_UPDATED_MAX_PACK, VENDOR_ID)), MAX_PACK_UPDATED_DATE);
    }

    @Test
    public void checkUpdatedMaxAltSku() {
        doCheckUpdated(List.of(TestData.unitId(CHECK_UPDATED_MAX_ALT_SKU, VENDOR_ID)), MAX_ALT_SKU_UPDATED_DATE);
    }

    private void doCheckUpdated(List<UnitId> unitIds, LocalDate date) {
        dispatcher.withWarehouseId(IntegrationTestConstants.WH_1_ID, () -> {
            List<ItemReference> items = service.getByIds(unitIds);
            assertThat(items, not(empty()));
            items.forEach(item ->
                    assertThat(item.getItem().getUpdated().getOffsetDateTime().toLocalDate(), is(equalTo(date))));
        });
    }
}
