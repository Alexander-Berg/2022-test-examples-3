package ru.yandex.market.wms.radiator.repository;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.radiator.dto.VInventoryMasterDTO;
import ru.yandex.market.wms.radiator.service.config.Dispatcher;
import ru.yandex.market.wms.radiator.test.IntegrationTestBackend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.wms.radiator.test.IntegrationTestConstants.WH_1_ID;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/dbStocksInTransit/db.xml", connection = "wh1Connection"),
        @DatabaseSetup(value = "/fixtures/dbStocksInTransit/db.xml", connection = "wh2Connection"),
        @DatabaseSetup(value = "/fixtures/dbSqlConfig/fallback-enabled.xml", connection = "wh1Connection"),
        @DatabaseSetup(value = "/fixtures/dbSqlConfig/fallback-enabled.xml", connection = "wh2Connection"),
})
class StocksRepositoryInTransitTest extends IntegrationTestBackend {

    @Autowired
    private StocksRepository repository;
    @Autowired
    private Dispatcher dispatcher;

    @Test
    @DatabaseSetups({
            @DatabaseSetup(value = "/fixtures/dbStocksInTransit/config_enabled.xml", connection = "wh1Connection"),
            @DatabaseSetup(value = "/fixtures/dbStocksInTransit/config_enabled.xml", connection = "wh2Connection"),
    })
    void getAllStocksFromDbIntransitEnabled() {
        dispatcher.withWarehouseId(
                WH_1_ID, () -> {
                    List<VInventoryMasterDTO> result = repository.getAllStocksFromDb();
                    assertEquals(1, result.size());
                    verifyQtys(result.get(0), 5, 2);
                }
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup(value = "/fixtures/dbStocksInTransit/config_disabled.xml", connection = "wh1Connection"),
            @DatabaseSetup(value = "/fixtures/dbStocksInTransit/config_disabled.xml", connection = "wh2Connection"),
    })
    void getAllStocksFromDbIntransitDisabled() {
        dispatcher.withWarehouseId(
                WH_1_ID, () -> {
                    List<VInventoryMasterDTO> result = repository.getAllStocksFromDb();
                    assertEquals(1, result.size());
                    verifyQtys(result.get(0), 5, 0);
                }
        );
    }

    @Test
    void getAllStocksFromDbIntransitUnset() {
        dispatcher.withWarehouseId(
                WH_1_ID, () -> {
                    List<VInventoryMasterDTO> result = repository.getAllStocksFromDb();
                    assertEquals(1, result.size());
                    verifyQtys(result.get(0), 5, 2);
                }
        );
    }

    private void verifyQtys(VInventoryMasterDTO entry, int qty, int qtyQuarantine) {
        assertEquals(qty, entry.getQty().intValue());
        assertEquals(qtyQuarantine, entry.getQtyquarantine().intValue());
    }
}
