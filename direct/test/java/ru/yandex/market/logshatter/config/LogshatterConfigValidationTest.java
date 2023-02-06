package ru.yandex.market.logshatter.config;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.clickhouse.ddl.ClickHouseDdlServiceOld;
import ru.yandex.market.logshatter.rotation.DataRotationService;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 06.12.16
 */
public class LogshatterConfigValidationTest {
    private final ConfigurationService configurationService = createConfigurationService();
    private final List<LogShatterConfig> configs =
        Collections.unmodifiableList(configurationService.readConfiguration());

    @Test
    public void validateConfigs() throws IOException, ConfigValidationException {
        configurationService.setConfigs(configs);
        configurationService.checkDDL();
    }

    protected static ConfigurationService createConfigurationService() {
        ConfigurationService configurationService = new ConfigurationService();
        String configDirPath = new File("src/configs").getAbsolutePath();

        ClickHouseDdlServiceOld clickhouseDdlService = Mockito.mock(ClickHouseDdlServiceOld.class);

        configurationService.setConfigDir(configDirPath);
        configurationService.setClickhouseDdlService(clickhouseDdlService);
        configurationService.setDefaultClickHouseDatabase("market");
        return configurationService;
    }

    @Test
    public void checkWrongArchiveSettings() {
        final List<DataRotationService.ArchiveSettings> archiveSettings = DataRotationService.toArchiveSettings(configs);
        final boolean allTablesHasDatabases = archiveSettings.stream()
            .map(DataRotationService.ArchiveSettings::getTableName)
            .map(s -> s.split("\\."))
            .allMatch(a -> a.length == 2);

        Assert.assertTrue(allTablesHasDatabases);
    }
}
