package ru.yandex.market.analytics.platform.admin.facade;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.analytics.platform.admin.FunctionalTest;
import ru.yandex.market.vendors.analytics.core.service.config.ConfigService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.dao.clickhouse.sales.SalesTable;
import ru.yandex.market.vendors.analytics.core.dao.hiding.WhitelistTable;
import ru.yandex.market.vendors.analytics.core.model.environment.EnvironmentKey;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тесты для {@link ConfigService}.
 *
 * @author ogonek.
 */
@ClickhouseDbUnitDataSet(before = "ConfigFacade.clickhouse.before.csv")
public class ConfigServiceTest extends FunctionalTest {

    @Autowired
    private ConfigService configService;

    @Test
    @DisplayName("Получение текущей конфигурации")
    @DbUnitDataSet(before = "ConfigFacade.before.csv")
    void getCurrentConfig() {
        var expected = Map.of(
                EnvironmentKey.CLICKHOUSE_SALES_TABLE, "analytics.ref_panel_aggregated",
                EnvironmentKey.CLICKHOUSE_WHITELIST_TABLE, "analytics.whitelist",
                EnvironmentKey.YT_WHITELIST_TABLE, "home/market/production/analytics_platform/whitelist",
                EnvironmentKey.SALES_DATA_START_DATE, "2019-01-01",
                EnvironmentKey.SALES_DATA_END_DATE, "2019-04-29",
                EnvironmentKey.LAST_SALES_DATE_CORRECTION, "2",
                EnvironmentKey.PROMO_PERIOD_DAYS, "7"
        );
        var actual = configService.getCurrentConfig();
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Изменение активной таблицы с данными")
    @DbUnitDataSet(
            before = "ConfigFacade.before.csv",
            after = "ConfigFacade.updateSalesTable.after.csv"
    )
    void updateActiveSalesTable() {
        configService.activateSalesTable(SalesTable.REF_PANEL_AGGREGATED);
    }

    @Test
    @DisplayName("Обновление активной таблицы со скрытиями")
    @DbUnitDataSet(
            before = "ConfigFacade.before.csv",
            after = "ConfigFacade.updateWhitelistTable.after.csv"
    )
    void updateActiveWhitelistTable() {
        configService.activateWhitelistTable(WhitelistTable.WHITELIST);
    }

    @Test
    @DisplayName("Изменение, за сколько последних дней не показываем данные")
    @DbUnitDataSet(
            before = "ConfigFacade.before.csv",
            after = "ConfigFacade.changeLastDateCorrection.after.csv"
    )
    void changeLastDateCorrection() {
        configService.changeLastDateCorrection(5);
    }

    @Test
    @DisplayName("Изменяем, за сколько последних дней не показываем данные на отрицательное число")
    void changeLastDateCorrectionError() {
        assertThrows(
                IllegalArgumentException.class,
                () -> configService.changeLastDateCorrection(-5),
                "400 Bad Request"
        );
    }

    @Test
    @DisplayName("Изменение промо-периода")
    @DbUnitDataSet(
            before = "ConfigFacade.before.csv",
            after = "ConfigFacade.changePromoPeriod.after.csv"
    )
    void changePromoPeriod() {
        configService.changePromoPeriod(5);
    }

    @Test
    @DisplayName("Изменяем промо-период на отрицательное число")
    void changePromoPeriodError() {
        assertThrows(
                IllegalArgumentException.class,
                () -> configService.changePromoPeriod(-5),
                "400 Bad Request"
        );
    }
}
