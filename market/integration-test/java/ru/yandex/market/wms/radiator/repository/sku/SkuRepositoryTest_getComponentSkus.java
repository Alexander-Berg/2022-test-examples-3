package ru.yandex.market.wms.radiator.repository.sku;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.radiator.service.config.Dispatcher;
import ru.yandex.market.wms.radiator.test.IntegrationTestBackend;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.wms.radiator.test.IntegrationTestConstants.WH_1_ID;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/dbStocks/stocks.xml", connection = "wh1Connection"),
})
class SkuRepositoryTest_getComponentSkus extends IntegrationTestBackend {

    @Autowired
    private SkuRepository skuRepository;
    @Autowired
    private Dispatcher dispatcher;


    @Test
    void findByManufacturerSkuList() {
        doFindComponentSkus(WH_1_ID);
    }


    private void doFindComponentSkus(String warehouseId) {
        dispatcher.withWarehouseId(
                warehouseId, () -> {
                    assertThat(
                            skuRepository.findComponentSkus(),
                            is(equalTo(Set.of("ROV0000000000000000421BOM1", "ROV0000000000000000421BOM2")))
                    );
                }
        );
    }
}
