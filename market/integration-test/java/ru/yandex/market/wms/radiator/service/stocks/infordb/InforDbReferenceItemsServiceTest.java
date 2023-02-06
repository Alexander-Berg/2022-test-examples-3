package ru.yandex.market.wms.radiator.service.stocks.infordb;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
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
import static ru.yandex.market.wms.radiator.test.IntegrationTestConstants.WH_2_ID;
import static ru.yandex.market.wms.radiator.test.TestReferenceItemsData.mSku100_02;
import static ru.yandex.market.wms.radiator.test.TestReferenceItemsData.mSku100_03;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/dbStocks/sku-1.xml", connection = "wh1Connection"),
        @DatabaseSetup(value = "/fixtures/dbStocks/pack-1.xml", connection = "wh1Connection"),
        @DatabaseSetup(value = "/fixtures/dbStocks/sku-1.xml", connection = "wh2Connection"),
        @DatabaseSetup(value = "/fixtures/dbStocks/pack-1.xml", connection = "wh2Connection")
})
class InforDbReferenceItemsServiceTest extends IntegrationTestBackend {

    @Autowired
    private InforDbReferenceItemsService service;
    @Autowired
    private Dispatcher dispatcher;


    @Test
    void getByIds() {
        var unitIds = List.of(TestData.unitId(TestData.M_SKU_100_02), TestData.unitId(TestData.M_SKU_100_03));

        doGetByIds(WH_1_ID, unitIds, List.of(mSku100_02(), mSku100_03()));
        doGetByIds(WH_2_ID, unitIds, List.of(mSku100_02(), mSku100_03()));
    }

    private void doGetByIds(String id, List<UnitId> unitIds, List<ItemReference> expected) {
        dispatcher.withWarehouseId(
                id, () -> assertThat(service.getByIds(unitIds), is(equalTo(expected)))
        );
    }


    @Test
    void getByRange() {
        doGetByRange(WH_1_ID, List.of(mSku100_02(), mSku100_03()));
        doGetByRange(WH_2_ID, List.of(mSku100_02(), mSku100_03()));
    }

    private void doGetByRange(String id, List<ItemReference> expected) {
        dispatcher.withWarehouseId(
                id, () -> assertThat(service.getByRange(1, 2), is(equalTo(expected)))
        );
    }
}
