package ru.yandex.market.fulfillment.stockstorage;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.service.stocks.freezing.strategy.CheckStockWasUpdatedStrategy;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.freezing.strategy.DefaultStockUnfreezingStrategy;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.freezing.strategy.StockUnfreezingStrategy;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.freezing.strategy.StockUnfreezingStrategyProvider;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StockUnfreezingStrategyProviderTest extends AbstractContextualTest {

    @Autowired
    private StockUnfreezingStrategyProvider stockUnfreezingStrategyProvider;

    @Test
    @DatabaseSetup("classpath:database/states/warehouse_property/2.xml")
    public void provide() {
        StockUnfreezingStrategy strategy = stockUnfreezingStrategyProvider.provide(145);
        assertNotNull(strategy);
        assertTrue(strategy instanceof CheckStockWasUpdatedStrategy);
    }

    @Test
    @DatabaseSetup("classpath:database/states/warehouse_property/2.xml")
    public void provideDefaultStrategyForNonexistentWarehouse() {
        StockUnfreezingStrategy strategy = stockUnfreezingStrategyProvider.provide(147);
        assertNotNull(strategy);
        assertTrue(strategy instanceof DefaultStockUnfreezingStrategy);
    }

    @Test
    public void provideDefaultStrategyIfDBIsEmpty() {
        StockUnfreezingStrategy strategy = stockUnfreezingStrategyProvider.provide(148);
        assertNotNull(strategy);
        assertTrue(strategy instanceof DefaultStockUnfreezingStrategy);
    }
}
