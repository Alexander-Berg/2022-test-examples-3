package ru.yandex.market.wms.radiator.service.stocks.redis;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.fulfillment.ItemStocks;
import ru.yandex.market.wms.radiator.service.config.Dispatcher;
import ru.yandex.market.wms.radiator.test.IntegrationTestConstants;
import ru.yandex.market.wms.radiator.test.TestStocksData;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RedisCacheStocksServiceTest extends BaseRedisCacheItemStocksServiceTest {

    @Autowired
    RedisCacheStocksService service;
    @Autowired
    Dispatcher dispatcher;

    @Override
    protected RedisCacheStocksService service() {
        return service;
    }


    @Test
    void when_save_then_get() throws InterruptedException {
        do_when_save_then_get(IntegrationTestConstants.WH_1_ID);
        do_when_save_then_get(IntegrationTestConstants.WH_2_ID);
    }

    private void do_when_save_then_get(String warehouseId) throws InterruptedException {
        var writer = service().newMultiConsumer(warehouseId);
        writer.accept(2L, testPageAtOffset2(warehouseId));
        writer.accept(4L, testPageAtOffset4(warehouseId));
        writer.accept(6L, List.of());
        Thread.sleep(1000);
        service().setUpdated(warehouseId, IntegrationTestConstants.LOCAL_DATE_TIME);
        dispatcher.withWarehouseId(
                warehouseId,
                () -> {
                    // aligned access
                    assertThat(service().getByRange(2, 2), is(equalTo(testPageAtOffset2(warehouseId))));
                    assertThat(service().getByRange(4, 2), is(equalTo(testPageAtOffset4(warehouseId))));
                    assertThat(service().getByRange(6, 2), is(equalTo(List.of())));
                    assertThrows(IllegalStateException.class, () -> service().getByRange(8, 2));

                    // unaligned access
                    List<ItemStocks> byRange = service().getByRange(3, 2);
                    assertThat(
                            byRange,
                            is(equalTo(itemsAtOffset3Limit2(warehouseId)))
                    );

                    var expected = expected(warehouseId);

                    // getItem
                    assertThat(
                            service().getItem(expected.getUnitId().getVendorId(), expected.getUnitId().getArticle()),
                            is(equalTo(Optional.of(expected)))
                    );

                    // getByIds
                    assertThat(
                            service().getByIds(List.of(expected.getUnitId())),
                            is(equalTo(List.of(expected)))
                    );
                }
        );
    }

    private ItemStocks expected(String warehouseId) {
        return TestStocksData.mSku100_01(warehouseId);
    }

    private static List<ItemStocks> testPageAtOffset2(String warehouseId) {
        return List.of(TestStocksData.mSku100_01(warehouseId), TestStocksData.mSku100_02(warehouseId));
    }

    private static List<ItemStocks> testPageAtOffset4(String warehouseId) {
        return List.of(TestStocksData.mSku100_03(warehouseId), TestStocksData.mSku100_04(warehouseId));
    }

    private List<ItemStocks> itemsAtOffset3Limit2(String warehouseId) {
        return List.of(TestStocksData.mSku100_02(warehouseId), TestStocksData.mSku100_03(warehouseId));
    }
}
