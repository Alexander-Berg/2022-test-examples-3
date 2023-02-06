package ru.yandex.market.wms.radiator.service.stocks.infordb;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.gateway.client.LogisticApiRequestsClient;
import ru.yandex.market.wms.radiator.repository.StocksRepository;
import ru.yandex.market.wms.radiator.service.config.Dispatcher;
import ru.yandex.market.wms.radiator.test.IntegrationTestBackend;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.wms.radiator.test.IntegrationTestConstants.WH_1_ID;

@MockBean(LogisticApiRequestsClient.class)
class InforDbStocksServiceWithPushTest extends IntegrationTestBackend {

    @Autowired
    private StocksRepository stocksRepository;

    @Autowired
    private Dispatcher dispatcher;

    @Autowired
    private LogisticApiRequestsClient lgwClient;


    @DatabaseSetups({
            @DatabaseSetup(value = "/fixtures/dbStocks/sku-1.xml", connection = "wh1Connection"),
            @DatabaseSetup(value = "/fixtures/dbSqlConfig/fallback-enabled.xml", connection = "wh1Connection"),
            @DatabaseSetup(value = "/fixtures/dbSqlConfig/push-stocks-enabled.xml", connection = "wh1Connection")
    })

    @Test
    void getStocksAndPush() {
        dispatcher.withWarehouseId(
                WH_1_ID, () -> stocksRepository.forEachRemaining(t -> {

                })
        );
        verify(lgwClient, times(2)).pushStocks(argThat((arg) -> arg.size() == 2
                && arg.get(0).getUnitId().equals(getUnitId("M_SKU_100_01", 100L))
                && arg.get(1).getUnitId().equals(getUnitId("M_SKU_100_02", 100L))));

        dispatcher.withWarehouseId(
                WH_1_ID, () -> stocksRepository.forEachRemaining(t -> {

                })
        );
    }

    private UnitId getUnitId(String sku, Long storer) {
        return new UnitId(sku, storer, sku);
    }

}
