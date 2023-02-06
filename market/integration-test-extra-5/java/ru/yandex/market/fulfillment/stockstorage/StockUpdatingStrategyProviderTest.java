package ru.yandex.market.fulfillment.stockstorage;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.service.stocks.updating.strategy.CheckOnlyDateStrategy;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.updating.strategy.DefaultStockUpdatingStrategy;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.updating.strategy.StockUpdatingStrategy;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.updating.strategy.StockUpdatingStrategyProvider;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StockUpdatingStrategyProviderTest extends AbstractContextualTest {

    @Autowired
    private StockUpdatingStrategyProvider stockUpdatingStrategyProvider;

    @Test
    @DatabaseSetup("classpath:database/states/warehouse_property/2.xml")
    public void provide() {
        StockUpdatingStrategy strategy = stockUpdatingStrategyProvider.provide(145);
        assertNotNull(strategy);
        assertTrue(strategy instanceof CheckOnlyDateStrategy);
    }

    @Test
    @DatabaseSetup("classpath:database/states/warehouse_property/2.xml")
    public void provideDefaultStrategyForNonexistentWarehouse() {
        StockUpdatingStrategy strategy = stockUpdatingStrategyProvider.provide(147147);
        assertNotNull(strategy);
        assertTrue(strategy instanceof DefaultStockUpdatingStrategy);
    }

    @Test
    public void provideDefaultStrategyIfDBIsEmpty() {
        StockUpdatingStrategy strategy = stockUpdatingStrategyProvider.provide(148);
        assertNotNull(strategy);
        assertTrue(strategy instanceof DefaultStockUpdatingStrategy);
    }
}
