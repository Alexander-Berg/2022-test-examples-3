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
import static ru.yandex.market.wms.radiator.test.TestData.unitId;
import static ru.yandex.market.wms.radiator.test.TestReferenceItemsData.M_SKU_AUTO_GET_STOCKS_TEST;
import static ru.yandex.market.wms.radiator.test.TestReferenceItemsData.M_SKU_REF_IDENTITIES;
import static ru.yandex.market.wms.radiator.test.TestReferenceItemsData.M_SKU_REF_MULTIBOX;
import static ru.yandex.market.wms.radiator.test.TestReferenceItemsData.VENDOR_ID;
import static ru.yandex.market.wms.radiator.test.TestReferenceItemsData.mSkuAutoGetStocksTest;
import static ru.yandex.market.wms.radiator.test.TestReferenceItemsData.mSkuRefIdentities;
import static ru.yandex.market.wms.radiator.test.TestReferenceItemsData.mSkuRefMultibox;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/dbStocks/stocks.xml", connection = "wh1Connection"),
})
class InforDbReferenceItemsServiceTest_getByIds extends IntegrationTestBackend {

    @Autowired
    private InforDbReferenceItemsService service;
    @Autowired
    private Dispatcher dispatcher;


    @Test
    void getByIds_1() {
        doGetByIds(
                WH_1_ID,
                List.of(unitId(M_SKU_REF_MULTIBOX, VENDOR_ID)),
                List.of(mSkuRefMultibox())
        );
    }


    @Test
    void getByIds_2() {
        doGetByIds(
                WH_1_ID,
                List.of(unitId(M_SKU_REF_MULTIBOX, VENDOR_ID), unitId(M_SKU_AUTO_GET_STOCKS_TEST, VENDOR_ID)),
                List.of(mSkuAutoGetStocksTest(), mSkuRefMultibox()) // ordered
        );
    }

    @Test
    void getByIds_3() {
        doGetByIds(
                WH_1_ID,
                List.of(
                        unitId(M_SKU_REF_MULTIBOX, VENDOR_ID),
                        unitId(M_SKU_AUTO_GET_STOCKS_TEST, VENDOR_ID),
                        unitId(M_SKU_REF_IDENTITIES, VENDOR_ID)),
                List.of(mSkuAutoGetStocksTest(), mSkuRefMultibox(), mSkuRefIdentities()) // ordered
        );
    }

    private void doGetByIds(String warehouseId, List<UnitId> unitIds, List<ItemReference> expected) {
        dispatcher.withWarehouseId(
                warehouseId, () -> assertThat(service.getByIds(unitIds), is(equalTo(expected)))
        );
    }
}
