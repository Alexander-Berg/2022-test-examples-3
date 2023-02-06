package ru.yandex.market.wms.radiator.service.stocks.redis;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.fulfillment.response.entities.ItemReference;
import ru.yandex.market.wms.radiator.service.config.Dispatcher;
import ru.yandex.market.wms.radiator.test.IntegrationTestConstants;
import ru.yandex.market.wms.radiator.test.TestReferenceItemsData;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class RedisCacheReferenceItemsServiceTest extends BaseRedisCacheItemStocksServiceTest {

    @Autowired
    RedisCacheReferenceItemsService service;
    @Autowired
    Dispatcher dispatcher;

    @Override
    protected RedisCacheReferenceItemsService service() {
        return service;
    }


    @Test
    void when_save_then_get() throws InterruptedException {
        do_when_save_then_get(IntegrationTestConstants.WH_1_ID);
        do_when_save_then_get(IntegrationTestConstants.WH_2_ID);
    }

    private void do_when_save_then_get(String warehouseId) throws InterruptedException {
        var writer = service().newMultiConsumer(warehouseId);
        writer.accept(2L, testPageAtOffset2());
        writer.accept(4L, testPageAtOffset4());
        service().setUpdated(warehouseId, IntegrationTestConstants.LOCAL_DATE_TIME);
        //redis client write asynchronously, wherefore wait 1 sec for complete write
        Thread.sleep(1000);
        dispatcher.withWarehouseId(
                warehouseId,
                () -> {
                    // aligned access
                    assertThat(service().getByRange(2, 2), is(equalTo(testPageAtOffset2())));
                    assertThat(service().getByRange(4, 2), is(equalTo(testPageAtOffset4())));

                    // unaligned access
                    List<ItemReference> byRange = service().getByRange(3, 2);
                    assertThat(
                            byRange,
                            is(equalTo(itemsAtOffset3Limit2()))
                    );

                    var expected = expected();

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


    private ItemReference expected() {
        return TestReferenceItemsData.mSku100_01();
    }

    private static List<ItemReference> testPageAtOffset2() {
        return List.of(TestReferenceItemsData.mSku100_01(), TestReferenceItemsData.mSku100_02());
    }

    private static List<ItemReference> testPageAtOffset4() {
        return List.of(TestReferenceItemsData.mSku100_03(), TestReferenceItemsData.mSku100_04());
    }

    private List<ItemReference> itemsAtOffset3Limit2() {
        return List.of(TestReferenceItemsData.mSku100_02(), TestReferenceItemsData.mSku100_03());
    }
}
