package ru.yandex.market.checkout.checkouter.test.config.services;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.checkout.common.web.HealthStateCachedProvider;
import ru.yandex.market.checkout.common.web.MonitoringHandler;
import ru.yandex.market.checkout.common.web.PingCheckHelper;
import ru.yandex.market.checkout.common.web.PingHandler;
import ru.yandex.market.common.zk.ZooClient;

@Configuration
public class IntTestHealthConfig {
    @Bean
    public PingCheckHelper checkouterDependenciesCheckHelper() {
        return new PingCheckHelper();
    }

    @Bean
    public PingCheckHelper devMonitoringHandler() {
        return new PingCheckHelper();
    }

    @Bean
    public PingHandler pingHandler() {
        PingHandler pingHandler = new PingHandler();
        pingHandler.setTarget("/ping");
        pingHandler.setPingTimeout(5000);
        return pingHandler;
    }

    @Bean
    public MonitoringHandler adminMonitoringHandler(ZooClient zooClient) {
        MonitoringHandler monitoringHandler = new MonitoringHandler();
        monitoringHandler.setPingTimeout(5000);
        monitoringHandler.setPokeTargets(List.of(zooClient));
        return monitoringHandler;
    }

    @Bean
    public PingHandler pingAliveHandler() {
        PingHandler pingHandler = new PingHandler();
        pingHandler.setTarget("/ping-alive");
        pingHandler.setOkAnswer("0;OK\n");
        pingHandler.setPingTimeout(5000L);

        return pingHandler;
    }

    @Bean
    public HealthStateCachedProvider healthStateProvider() {
        HealthStateCachedProvider healthStateCachedProvider = new HealthStateCachedProvider();
        healthStateCachedProvider.setCheckHelper(checkouterDependenciesCheckHelper());
        return healthStateCachedProvider;
    }
}
