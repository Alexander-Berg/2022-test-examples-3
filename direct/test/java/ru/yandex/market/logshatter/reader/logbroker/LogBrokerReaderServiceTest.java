package ru.yandex.market.logshatter.reader.logbroker;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.logbroker.pull.LogBrokerClient;
import ru.yandex.market.logshatter.config.ConfigValidationException;
import ru.yandex.market.logshatter.config.ConfigurationService;
import ru.yandex.market.logshatter.config.LogShatterConfig;
import ru.yandex.market.logshatter.config.LogSource;
import ru.yandex.market.logshatter.parser.LogParserProvider;
import ru.yandex.market.logshatter.parser.internal.LogshatterPerformanceLog;
import ru.yandex.market.logshatter.reader.logbroker.monitoring.MonitoringConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 21.08.17
 */
public class LogBrokerReaderServiceTest {
    private final ConfigurationService configurationService = Mockito.mock(ConfigurationService.class);
    private final MonitoringConfig monitoringConfig = new MonitoringConfig(null, null, null, 42, 42);


    @Test
    public void readConf() throws Exception {

        List<LogSource> sources = Arrays.asList(
            LogSource.create("logbroker://market-health-prestable"),
            LogSource.create("logbroker://market-health-prestable--log-type"),
            LogSource.create("logbroker://market-health-stable--log-type")
        );

        LogBrokerClient logBrokerClient = Mockito.mock(LogBrokerClient.class);
        Mockito.when(logBrokerClient.getDc()).thenReturn("iva");

        LogBrokerReaderService sut = new LogBrokerReaderService(logBrokerClient, null, null, monitoringConfig,
            new LogBrokerConfigurationService(Collections.singletonList(createConfig(sources)), ""));

        sut.setConfigurationService(configurationService);

        Assert.assertEquals(
            Arrays.asList(
                new LogbrokerSource("market-health-prestable"),
                new LogbrokerSource("market-health-stable", "log-type")
            ),
            sut.getSources()
        );
    }

    @Test
    public void testDisabledSources() throws Exception {
        List<LogSource> sources = Arrays.asList(
            LogSource.create("logbroker://ident1"),
            LogSource.create("logbroker://ident2--log-type2"),
            LogSource.create("logbroker://ident3--log-type3")
        );

        LogBrokerReaderService sut = new LogBrokerReaderService(null, null, null, monitoringConfig,
            new LogBrokerConfigurationService(Collections.singletonList(createConfig(sources)), "ident1--logtype1,ident2,ident3--log-type33"));
        sut.setConfigurationService(configurationService);

        Assert.assertEquals(
            Arrays.asList(
                new LogbrokerSource("ident1"),
                new LogbrokerSource("ident3", "log-type3")
            ),
            sut.getSources()
        );

    }

    private LogShatterConfig createConfig(List<LogSource> sources) throws ConfigValidationException {
        return LogShatterConfig.newBuilder()
            .setLogPath("/var/log/log.log")
            .setConfigFileName("log.log")
            .setParserProvider(new LogParserProvider(LogshatterPerformanceLog.class.getName(), null, null))
            .setSources(sources)
            .build();
    }


}