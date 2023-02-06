package ru.yandex.market.loyalty.admin.yt;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.yt.service.CoinsRulesTableYtExporter;
import ru.yandex.market.loyalty.admin.yt.service.CoinsTableYtExporter;
import ru.yandex.market.loyalty.core.config.YtHahn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CoinRulesExporterTest extends MarketLoyaltyAdminMockedDbTest {
    @YtHahn
    @Autowired
    private CoinsRulesTableYtExporter coinsRulesTableYtExporter;
    @YtHahn
    @Autowired
    private CoinsTableYtExporter coinsTableYtExporter;

    /**
     * https://st.yandex-team.ru/MARKETDISCOUNT-1780#5d37025463890d001d471d08
     * <p>
     * Используем //home/market/production/market-promo/internal/coin/current и
     * //home/market/production/market-promo/internal/coin_rule/current.
     * Берём из coin/current id и coin_props_id. Из coin_rule тоже берём id и coin_props_id.
     * Потом оставляем только те монетки из coin/current, для которых нет строки с таким же coin_props_id в
     * coin_rule/current.
     * Считаем, что оставшиеся монетки ограничений не имеют.
     */
    @Test
    public void shouldExportCoinRules() {
        assertEquals("internal/coin", coinsTableYtExporter.getSettings().getDataDirName());
        assertTrue(coinsTableYtExporter.getTableDescription().containsColumn("id"));
        assertTrue(coinsTableYtExporter.getTableDescription().containsColumn("coin_props_id"));

        assertEquals("internal/coin_rule", coinsRulesTableYtExporter.getSettings().getDataDirName());
        assertTrue(coinsRulesTableYtExporter.getTableDescription().containsColumn("coin_props_id"));
        assertTrue(coinsRulesTableYtExporter.getTableDescription().containsColumn("id"));
    }
}
