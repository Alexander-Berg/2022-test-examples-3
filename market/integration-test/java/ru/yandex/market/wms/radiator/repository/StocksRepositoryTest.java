package ru.yandex.market.wms.radiator.repository;

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
        @DatabaseSetup(value = "/fixtures/dbStocks/sku-1.xml", connection = "wh1Connection"),
        @DatabaseSetup(value = "/fixtures/dbStocks/sku-1.xml", connection = "wh2Connection"),
})
class StocksRepositoryTest extends IntegrationTestBackend {

    @Autowired
    private StocksRepository repository;
    @Autowired
    private Dispatcher dispatcher;


    @Test
    void getAllStocksFromDb_justSqlIsCorrect() {
        dispatcher.withWarehouseId(
                WH_1_ID, () -> {
                    var result = repository.getAllStocksFromDb();
                    assertThat(
                            result.size(),
                            is(equalTo(4))
                    );
                }
        );
    }
}
