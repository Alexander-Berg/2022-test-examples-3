package ru.yandex.market.logshatter.config;

import java.io.File;

import ru.yandex.devtools.test.Paths;
import ru.yandex.market.clickhouse.ClickHouseSource;
import ru.yandex.market.clickhouse.ddl.ClickHouseDdlServiceOld;

public class TestServiceBuilder {
    private static final String CONF_PATH = "market/infra/market-health/config-cs-logshatter/src/conf.d";

    private TestServiceBuilder() {
    }

    static ConfigurationService buildConfigurationService() {
        ConfigurationService configurationService = new ConfigurationService();
        String configDirPath = new File(Paths.getSourcePath(CONF_PATH)).getAbsolutePath();

        configurationService.setConfigDir(configDirPath);
        configurationService.setDefaultClickHouseDatabase("market");
        configurationService.setDefaultSource(
            "logbroker://market-health-testing--other," +
                "logbroker://market-health-dev--other");
        return configurationService;
    }

    static ClickHouseDdlServiceOld buildDdlService() {
        ClickHouseSource source = new ClickHouseSource();
        ClickHouseDdlServiceOld clickHouseDdlService = new ClickHouseDdlServiceOld();
        clickHouseDdlService.setClickHouseSource(source);
        return clickHouseDdlService;
    }
}
