package ru.yandex.market.wms.radiator.service.stocks.infordb;

import java.util.ArrayList;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import one.util.streamex.StreamEx;
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
import static ru.yandex.market.wms.radiator.test.TestReferenceItemsData.M_SKU_AUTO_GET_STOCKS_TEST;
import static ru.yandex.market.wms.radiator.test.TestReferenceItemsData.M_SKU_REF_MULTIBOX;
import static ru.yandex.market.wms.radiator.test.TestReferenceItemsData.VENDOR_ID;
import static ru.yandex.market.wms.radiator.test.TestReferenceItemsData.mSkuAutoGetStocksTest;
import static ru.yandex.market.wms.radiator.test.TestReferenceItemsData.mSkuRefIdentities;
import static ru.yandex.market.wms.radiator.test.TestReferenceItemsData.mSkuRefMultibox;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/dbStocks/stocks.xml", connection = "wh1Connection"),
})
class InforDbReferenceItemsServiceTest_forEachRemaining extends IntegrationTestBackend {

    @Autowired
    private InforDbReferenceItemsService service;
    @Autowired
    private Dispatcher dispatcher;


    @Test
    void withAll() {
        doWithAll(WH_1_ID);
    }

    private void doWithAll(String warehouseId) {
        dispatcher.withWarehouseId(
                warehouseId, () -> {
                    List<ItemReference> result = new ArrayList<>();
                    service.forEachRemaining(result::add);
                    assertThat(
                            result,
                            is(equalTo(List.of(mSkuAutoGetStocksTest(), mSkuRefMultibox(), mSkuRefIdentities())))
                    );
                }
        );
    }
}
