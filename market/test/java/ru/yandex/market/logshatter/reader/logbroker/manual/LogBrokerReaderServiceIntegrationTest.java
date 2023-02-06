package ru.yandex.market.logshatter.reader.logbroker.manual;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.auth.Credentials;
import ru.yandex.kikimr.persqueue.proxy.ProxyBalancer;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.health.KeyValueMetricSupplier;
import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;
import ru.yandex.market.health.configs.logshatter.config.LogSource;
import ru.yandex.market.health.configs.logshatter.useragent.FakeUserAgentDetector;
import ru.yandex.market.logbroker.pull.LogBrokerClient;
import ru.yandex.market.logshatter.LogShatterMonitoring;
import ru.yandex.market.logshatter.LogShatterParserWorker;
import ru.yandex.market.logshatter.LogShatterService;
import ru.yandex.market.logshatter.config.ConfigurationService;
import ru.yandex.market.logshatter.logging.BatchErrorLoggerFactory;
import ru.yandex.market.logshatter.logging.ErrorBoosterLogger;
import ru.yandex.market.logshatter.logging.LogSamplingPropertiesService;
import ru.yandex.market.logshatter.logging.StuffFileInfoLogger;
import ru.yandex.market.logshatter.parser.LogParserProvider;
import ru.yandex.market.logshatter.parser.internal.LogshatterPerformanceLog;
import ru.yandex.market.logshatter.reader.ReadSemaphore;
import ru.yandex.market.logshatter.reader.logbroker.LogBrokerConfigurationService;
import ru.yandex.market.logshatter.reader.logbroker.LogBrokerReaderService;
import ru.yandex.market.logshatter.reader.logbroker.dc.LbDataCenterReaderServiceFactory;
import ru.yandex.market.logshatter.reader.logbroker.dc.LbLogshatterDataCenters;
import ru.yandex.market.logshatter.reader.logbroker.threads.SingleThreadExecutorServiceFactoryImpl;
import ru.yandex.market.logshatter.reader.logbroker.topic.LbApiStreamConsumerFactory;
import ru.yandex.market.logshatter.reader.logbroker.topic.LbTopicReaderServiceFactory;
import ru.yandex.market.logshatter.sharding.LogShatterShardingService;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * <br>
 * Date: 26.12.2018
 */
@Ignore
public class LogBrokerReaderServiceIntegrationTest {
    @Test
    public void test() throws Exception {
        LogShatterService logShatterService = new LogShatterService();
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setConfigs(
            Arrays.asList(
                LogShatterConfig.newBuilder()
                    .setConfigId("/1")
                    .setLogPath("**/push-client.log")
                    .setSources(Collections.singletonList(LogSource.create("logbroker://market-health-testing")))
                    .setLogHosts("*")
                    .setDataClickHouseTable(
                        new ClickHouseTableDefinitionImpl("db.first", Collections.emptyList(), null))
                    .setParserProvider(new LogParserProvider(LogshatterPerformanceLog.class.getName(), null, null))
                    .build()
            )
        );

        ReadSemaphore readSemaphore = new ReadSemaphore();
        readSemaphore.setZookeeperQuorum("localhost:2181");
        readSemaphore.setMonitoring(new LogShatterMonitoring());
        readSemaphore.afterPropertiesSet();

        LogBrokerClient logBrokerClient =
            new LogBrokerClient("http://logbroker-pre.yandex.net:8999/", "market-health-dev", "man");
        logBrokerClient.afterPropertiesSet();

        LogBrokerConfigurationService logBrokerConfigurationService = new LogBrokerConfigurationService(
            configurationService,
            new LogShatterShardingService(null, false, null),
            "",
            ""
        );

        LogBrokerReaderService logBrokerReaderService = new LogBrokerReaderService(
            new SingleThreadExecutorServiceFactoryImpl(),
            new LbDataCenterReaderServiceFactory(
                new LbTopicReaderServiceFactory(
                    new SingleThreadExecutorServiceFactoryImpl(),
                    new LbApiStreamConsumerFactory(
                        ImmutableMap.of(
                            "man", new LogbrokerClientFactory(new ProxyBalancer("man.logbroker-prestable.yandex.net")),
                            "myt", new LogbrokerClientFactory(new ProxyBalancer("myt.logbroker-prestable.yandex.net"))
                        ),
                        () -> Credentials.oauth("token"),
                        "clientId",
                        1,
                        2,
                        3,
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE
                    ),
                    readSemaphore,
                    logShatterService,
                    logBrokerConfigurationService,
                    null,
                    null,
                    new BatchErrorLoggerFactory(
                        10, 100,
                        new ErrorBoosterLogger(false, "test"),
                        new LogSamplingPropertiesService(1.0f, Collections.emptyMap())
                    ),
                    new StuffFileInfoLogger(),
                    10
                ),
                new SingleThreadExecutorServiceFactoryImpl(),
                new LbLogshatterDataCenters("man", Collections.singletonList("man")),
                logBrokerConfigurationService,
                new LogShatterMonitoring(),
                Arrays.asList("man", "iva", "sas", "vla", "myt", "kafka-bs"),
                new KeyValueMetricSupplier(),
                true
            ),
            ImmutableList.of("man", "myt")
        );

        logBrokerReaderService.startAsync();

        TimeUnit.MINUTES.sleep(1);
    }

    @Test
    public void protobufTest() throws Exception {
        LogShatterService logShatterService = new LogShatterService();
        ConfigurationService configurationService = new ConfigurationService();

        configurationService.setConfigs(Arrays.asList(configurationService.readFile(
            new File(ClassLoader.getSystemResource("configs/protoConverter/protoConverter.json").getPath()))
        ));

        configurationService.setUserAgentDetector(new FakeUserAgentDetector());
        logShatterService.setConfigurationService(configurationService);

        ReadSemaphore readSemaphore = new ReadSemaphore();
        readSemaphore.setZookeeperQuorum("localhost:2181");
        readSemaphore.setMonitoring(new LogShatterMonitoring());
        readSemaphore.afterPropertiesSet();

        logShatterService.setReadSemaphore(readSemaphore);

        LogBrokerClient logBrokerClient =
            new LogBrokerClient("http://logbroker.yandex.net:8999/", "/megamind/vins-log-test-consumer", "man");
        logBrokerClient.afterPropertiesSet();

        LogBrokerConfigurationService logBrokerConfigurationService = new LogBrokerConfigurationService(
            configurationService,
            new LogShatterShardingService(null, false, null),
            "",
            ""
        );

        LogBrokerReaderService logBrokerReaderService = new LogBrokerReaderService(
            new SingleThreadExecutorServiceFactoryImpl(),
            new LbDataCenterReaderServiceFactory(
                new LbTopicReaderServiceFactory(
                    new SingleThreadExecutorServiceFactoryImpl(),
                    new LbApiStreamConsumerFactory(
                        ImmutableMap.of(
                            "iva", new LogbrokerClientFactory(new ProxyBalancer("iva.logbroker.yandex.net")),
                            "vla", new LogbrokerClientFactory(new ProxyBalancer("vla.logbroker.yandex.net"))
                        ),
                        () -> Credentials.oauth("token"),
                        "/megamind/vins-log-test-consumer",
                        1,
                        2,
                        3,
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE
                    ),
                    readSemaphore,
                    logShatterService,
                    logBrokerConfigurationService,
                    null,
                    null,
                    new BatchErrorLoggerFactory(
                        10, 100,
                        new ErrorBoosterLogger(false, "test"),
                        new LogSamplingPropertiesService(1.0f, Collections.emptyMap())
                    ),
                    new StuffFileInfoLogger(),
                    10
                ),
                new SingleThreadExecutorServiceFactoryImpl(),
                new LbLogshatterDataCenters("iva", Collections.singletonList("iva")),
                logBrokerConfigurationService,
                new LogShatterMonitoring(),
                Arrays.asList("iva", "man", "sas", "vla", "myt", "kafka-bs"),
                new KeyValueMetricSupplier(),
                true
            ),
            ImmutableList.of("iva", "vla")
        );

        logBrokerReaderService.startAsync();
        new LogShatterParserWorker(logShatterService).run();
        TimeUnit.SECONDS.sleep(10);
    }
}
