package ru.yandex.market.logshatter.reader.logbroker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;
import ru.yandex.market.logshatter.config.ConfigurationService;
import ru.yandex.market.logshatter.reader.logbroker.common.LogbrokerSource;
import ru.yandex.market.logshatter.sharding.LogShatterShardingService;
import ru.yandex.market.logshatter.sharding.mongo.LogShatterShardEntity;
import ru.yandex.market.logshatter.sharding.mongo.LogShatterShardingDao;

/**
 * @author Mishunin Andrei <a href="mailto:mishunin@yandex-team.ru"></a>
 * @date 17.03.2020
 */
public class LogBrokerConfigurationServiceTest {

    @Test
    public void disabledByFullSourceTest() {
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConfigs(createTestConfigs());
        LogBrokerConfigurationService logBrokerConfigurationService = new LogBrokerConfigurationService(
            configurationService, simpleShardingService(), "dc1--ident1--topic1", ""
        );
        testSources(
            logBrokerConfigurationService.getSourcesList(),
            Arrays.asList(
                "dc1--ident1--topic2",
                "dc1--ident2--topic1",
                "dc1--ident2--topic2",
                "dc2--ident1--topic1",
                "dc2--ident1--topic2",
                "dc2--ident2--topic1",
                "dc2--ident2--topic2",
                "kafka-bs--ident1--topic1",
                "kafka-bs--ident1--topic2"
            )
        );
    }

    @Test
    public void disabledByIdentAndTopicTest() {
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConfigs(createTestConfigs());
        LogBrokerConfigurationService logBrokerConfigurationService = new LogBrokerConfigurationService(
            configurationService, simpleShardingService(), "ident1--topic1", ""
        );
        testSources(
            logBrokerConfigurationService.getSourcesList(),
            Arrays.asList(
                "dc1--ident1--topic2",
                "dc1--ident2--topic1",
                "dc1--ident2--topic2",
                "dc2--ident1--topic2",
                "dc2--ident2--topic1",
                "dc2--ident2--topic2",
                "kafka-bs--ident1--topic2"
            )
        );
    }

    @Test
    public void disabledByIdentTest() {
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConfigs(createTestConfigs());
        LogBrokerConfigurationService logBrokerConfigurationService = new LogBrokerConfigurationService(
            configurationService, simpleShardingService(), "ident1", ""
        );
        testSources(
            logBrokerConfigurationService.getSourcesList(),
            Arrays.asList(
                "dc1--ident2--topic1",
                "dc1--ident2--topic2",
                "dc2--ident2--topic1",
                "dc2--ident2--topic2"
            )
        );
    }

    @Test
    public void disabledByTable() {
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConfigs(createTestConfigs());
        LogBrokerConfigurationService logBrokerConfigurationService = new LogBrokerConfigurationService(
            configurationService, simpleShardingService(), "", LbReadingTester.getTableNameWithDb("table1")
        );

        testSources(
            logBrokerConfigurationService.getSourcesList(),
            Arrays.asList(
                "dc2--ident1--topic1",
                "dc2--ident1--topic2",
                "dc2--ident2--topic1",
                "dc2--ident2--topic2",
                "kafka-bs--ident1--topic2"
            )
        );
    }

    @Test
    public void removeLessCommonSources() {
        ConfigurationService configurationService = new ConfigurationService();
        List<LogShatterConfig> configs = new ArrayList<>(createTestConfigs());
        configs.add(LbReadingTester.config("ident1--topic1"));
        configurationService.setConfigs(configs);
        LogBrokerConfigurationService logBrokerConfigurationService = new LogBrokerConfigurationService(
            configurationService, simpleShardingService(), "", ""
        );

        testSources(
            logBrokerConfigurationService.getSourcesList(),
            Arrays.asList(
                "ident1--topic1",
                "dc1--ident1--topic2",
                "dc1--ident2--topic1",
                "dc1--ident2--topic2",
                "dc2--ident1--topic2",
                "dc2--ident2--topic1",
                "dc2--ident2--topic2",
                "kafka-bs--ident1--topic1",
                "kafka-bs--ident1--topic2"
            )
        );
    }

    @Test
    public void disabledByIdentForShardTest() {
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConfigs(createTestConfigs());
        LogBrokerConfigurationService logBrokerConfigurationService = new LogBrokerConfigurationService(
            configurationService, shardingServiceWithDisabledSources("ident1--.*"), "", ""
        );
        testSources(
            logBrokerConfigurationService.getSourcesList(),
            Arrays.asList(
                "dc1--ident2--topic1",
                "dc1--ident2--topic2",
                "dc2--ident2--topic1",
                "dc2--ident2--topic2"
            )
        );
    }

    @Test
    public void enabledByIdentForShardTest() {
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConfigs(createTestConfigs());
        LogBrokerConfigurationService logBrokerConfigurationService = new LogBrokerConfigurationService(
            configurationService, shardingServiceWithEnablededSources("ident1--.*"), "", ""
        );
        testSources(
            logBrokerConfigurationService.getSourcesList(),
            Arrays.asList(
                "dc1--ident1--topic1",
                "dc1--ident1--topic2",
                "dc2--ident1--topic1",
                "dc2--ident1--topic2",
                "kafka-bs--ident1--topic1",
                "kafka-bs--ident1--topic2"
            )
        );
    }

    @Test
    public void disabledByIdentAndTopicForShardTest() {
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConfigs(createTestConfigs());

        LogBrokerConfigurationService logBrokerConfigurationService = new LogBrokerConfigurationService(
            configurationService, shardingServiceWithDisabledSources("ident1--topic1"), "", ""
        );
        testSources(
            logBrokerConfigurationService.getSourcesList(),
            Arrays.asList(
                "dc1--ident1--topic2",
                "dc1--ident2--topic1",
                "dc1--ident2--topic2",
                "dc2--ident1--topic2",
                "dc2--ident2--topic1",
                "dc2--ident2--topic2",
                "kafka-bs--ident1--topic2"
            )
        );
    }

    @Test
    public void enabledByIdentAndTopicForShardTest() {
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConfigs(createTestConfigs());

        LogBrokerConfigurationService logBrokerConfigurationService = new LogBrokerConfigurationService(
            configurationService, shardingServiceWithEnablededSources("ident1--topic1"), "", ""
        );
        testSources(
            logBrokerConfigurationService.getSourcesList(),
            Arrays.asList(
                "dc1--ident1--topic1",
                "dc2--ident1--topic1",
                "kafka-bs--ident1--topic1"
            )
        );
    }

    private List<LogShatterConfig> createTestConfigs() {
        return Arrays.asList(
            LbReadingTester.config("dc1--ident1--topic1", "table1"),
            LbReadingTester.config("dc1--ident1--topic2", "table1"),
            LbReadingTester.config("dc1--ident2--topic1", "table1"),
            LbReadingTester.config("dc1--ident2--topic2", "table1"),
            LbReadingTester.config("dc2--ident1--topic1", "table2"),
            LbReadingTester.config("dc2--ident1--topic2", "table2"),
            LbReadingTester.config("dc2--ident2--topic1", "table2"),
            LbReadingTester.config("dc2--ident2--topic2", "table2"),
            LbReadingTester.config("kafka-bs--ident1--topic1", "table1"),
            LbReadingTester.config("kafka-bs--ident1--topic2", "table2")
        );
    }

    private void testSources(List<LogbrokerSource> sources, List<String> enabledSources) {
        Set<String> sourcePaths = sources.stream()
            .map(source -> (source.getDc() != null ? source.getDc() + "--" : "") +
                source.getIdent() + "--" + source.getLogType())
            .collect(Collectors.toSet());

        sourcePaths.forEach(source -> Assert.assertTrue(
            String.format("Source %s must be disabled", source),
            enabledSources.contains(source))
        );
        enabledSources.forEach(source -> Assert.assertTrue(
            String.format("Source %s must be enabled", source),
            sourcePaths.contains(source)
        ));
    }

    private LogShatterShardingService simpleShardingService() {
        LogShatterShardingService logShatterShardingService = new LogShatterShardingService(null, false, null);
        return logShatterShardingService;
    }

    private LogShatterShardingService shardingServiceWithDisabledSources(
        String disabledSourcePattern
    ) {
        LogShatterShardEntity logShatterShardEntity = new LogShatterShardEntity(
            "testShard",
            true,
            Collections.singletonList(disabledSourcePattern),
            null,
            null,
            null,
            null,
            null
        );
        return getShardingServiceForEntity(logShatterShardEntity);
    }

    private LogShatterShardingService shardingServiceWithEnablededSources(
        String enabledSourcePatterns
    ) {
        LogShatterShardEntity logShatterShardEntity = new LogShatterShardEntity(
            "testShard",
            false,
            null,
            Collections.singletonList(enabledSourcePatterns),
            null,
            null,
            null,
            null
        );
        return getShardingServiceForEntity(logShatterShardEntity);
    }

    private LogShatterShardingService getShardingServiceForEntity(LogShatterShardEntity logShatterShardEntity) {
        LogShatterShardingDao logShatterShardingDao = Mockito.mock(LogShatterShardingDao.class);
        Mockito.when(logShatterShardingDao.getLogShatterShardByName(Mockito.any()))
            .thenReturn(logShatterShardEntity);

        return new LogShatterShardingService(
            logShatterShardingDao,
            true,
            () -> "testShard"
        );
    }
}
