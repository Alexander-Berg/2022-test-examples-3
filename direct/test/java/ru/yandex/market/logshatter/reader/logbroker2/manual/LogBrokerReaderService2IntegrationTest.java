package ru.yandex.market.logshatter.reader.logbroker2.manual;

import com.github.fakemongo.Fongo;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.auth.Credentials;
import ru.yandex.kikimr.persqueue.proxy.ProxyBalancer;
import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.logbroker.pull.LogBrokerClient;
import ru.yandex.market.logshatter.LogShatterMonitoring;
import ru.yandex.market.logshatter.LogShatterService;
import ru.yandex.market.logshatter.config.LogShatterConfig;
import ru.yandex.market.logshatter.config.LogSource;
import ru.yandex.market.logshatter.logging.BatchErrorLoggerFactory;
import ru.yandex.market.logshatter.parser.LogParserProvider;
import ru.yandex.market.logshatter.parser.internal.LogshatterPerformanceLog;
import ru.yandex.market.logshatter.reader.ReadSemaphore;
import ru.yandex.market.logshatter.reader.logbroker.LogBrokerConfigurationService;
import ru.yandex.market.logshatter.reader.logbroker.OldApiLogBrokerClients;
import ru.yandex.market.logshatter.reader.logbroker.PartitionDao;
import ru.yandex.market.logshatter.reader.logbroker2.LogBrokerReaderService2;
import ru.yandex.market.logshatter.reader.logbroker2.dc.LbDataCenterReaderServiceFactory;
import ru.yandex.market.logshatter.reader.logbroker2.threads.SingleThreadExecutorServiceFactoryImpl;
import ru.yandex.market.logshatter.reader.logbroker2.topic.LbApiStreamConsumerFactory;
import ru.yandex.market.logshatter.reader.logbroker2.topic.LbTopicReaderServiceFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 26.12.2018
 */
@Ignore
public class LogBrokerReaderService2IntegrationTest {
    @Test
    public void test() throws Exception {
        Fongo fongo = new Fongo("");

        LogShatterService logShatterService = new LogShatterService();

        ReadSemaphore readSemaphore = new ReadSemaphore();
        readSemaphore.setZookeeperQuorum("localhost:2181");
        readSemaphore.setMonitoring(new LogShatterMonitoring());
        readSemaphore.afterPropertiesSet();

        LogBrokerClient logBrokerClient = new LogBrokerClient("http://logbroker-pre.yandex.net:8999/", "market-health-dev", "man");
        logBrokerClient.afterPropertiesSet();

        PartitionDao partitionDao = new PartitionDao(fongo.getDatabase("db"));

        LogBrokerConfigurationService logBrokerConfigurationService = new LogBrokerConfigurationService(
            Arrays.asList(
                LogShatterConfig.newBuilder()
                    .setConfigFileName("/1")
                    .setLogPath("**/push-client.log")
                    .setSources(Collections.singletonList(LogSource.create("logbroker://market-health-testing")))
                    .setLogHosts("*")
                    .setDataClickHouseTable(new ClickHouseTableDefinitionImpl("db.first", Collections.emptyList(), null))
                    .setParserProvider(new LogParserProvider(LogshatterPerformanceLog.class.getName(), null, null))
                    .build()
            ),
            ""
        );

        LogBrokerReaderService2 logBrokerReaderService = new LogBrokerReaderService2(
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
                        3
                    ),
                    partitionDao,
                    readSemaphore,
                    logShatterService,
                    logBrokerConfigurationService,
                    null,
                    null,
                    new BatchErrorLoggerFactory(10, 100)
                ),
                new SingleThreadExecutorServiceFactoryImpl(),
                new OldApiLogBrokerClients(Collections.singletonMap("man", logBrokerClient)),
                logBrokerConfigurationService,
                partitionDao,
                new LogShatterMonitoring(),
                1,
                2
            ),
            ImmutableList.of("man", "myt")
        );

        logBrokerReaderService.startAsync();

        TimeUnit.MINUTES.sleep(1);
    }
}
