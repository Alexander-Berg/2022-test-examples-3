package ru.yandex.logbroker.client;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ru.yandex.http.config.HttpHostConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.util.server.BaseServerConfigBuilder;
import ru.yandex.logbroker.config.LogConfig;
import ru.yandex.logbroker.config.LogbrokerConsumerConfigBuilder;
import ru.yandex.logbroker.config.LogbrokerConsumerServerConfigBuilder;
import ru.yandex.logbroker.server.LogbrokerConsumerServer;

public class LogbrokerConsumerClusterBuilder {
    private static final int DEFAULT_CONNECTIONS = 20;
    private static final int STOP_WAIT_TO = 1000;

    private Map<String, LogConfig> logs;
    private String currentDc;
    private Set<String> dcList;
    private int nodesPerDc = 2;

    public LogbrokerConsumerClusterBuilder() {
    }

    public LogbrokerConsumerClusterBuilder currentDc(final String currentDc) {
        this.currentDc = currentDc;
        return this;
    }

    public LogbrokerConsumerClusterBuilder logs(final LogConfig log) {
        return this.logs(Collections.singletonMap(log.name(), log));
    }

    public LogbrokerConsumerClusterBuilder logs(
        final Map<String, LogConfig> logs)
    {
        this.logs = logs;
        return this;
    }

    public void dcs(final String... dcs) {
        this.dcList = new HashSet<>();

        for (String dc: dcs) {
            this.dcList.add(dc);
        }
    }

    public LogbrokerConsumerCluster build() throws Exception {
        LogbrokerConsumerConfigBuilder config =
            new LogbrokerConsumerConfigBuilder();

        if (dcList == null || dcList.isEmpty()) {
            dcList = Collections.singleton(LogbrokerConsumerCluster.SAS);
        }

        System.setProperty("MAJOR_SEARCHMAP_PATH", "src/major/main/bundle");

        LogbrokerMetaServer metaServer =
            new LogbrokerMetaServer(
                new BaseServerConfigBuilder(Configs.baseConfig("LogbrokerMeta"))
                    .connections(DEFAULT_CONNECTIONS).port(0).build(),
                dcList,
                nodesPerDc);

        if (currentDc == null) {
            currentDc = LogbrokerConsumerCluster.SAS;
        }

        config.dc(currentDc);
        config.logConfig(logs);
        config.stopWaitTimeout(STOP_WAIT_TO);
        config.logbrokerHostConfig(
            new HttpHostConfigBuilder()
                .host(metaServer.host())
                .connections(DEFAULT_CONNECTIONS).build());

        LogbrokerConsumerServerConfigBuilder serverConfig =
            new LogbrokerConsumerServerConfigBuilder();

        serverConfig.port(0)
            .connections(DEFAULT_CONNECTIONS).consumerConfig(config);
        LogbrokerConsumerServer server =
            new LogbrokerConsumerServer(serverConfig.build());

        LogbrokerConsumerCluster.setUpDns(dcList, nodesPerDc);
        return new LogbrokerConsumerCluster(metaServer, server);
    }
}
