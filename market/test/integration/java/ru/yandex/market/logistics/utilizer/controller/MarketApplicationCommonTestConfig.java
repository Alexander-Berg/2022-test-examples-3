package ru.yandex.market.logistics.utilizer.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import ru.yandex.market.application.MarketApplicationCommonConfig;
import ru.yandex.market.application.monitoring.MonitoringController;
import ru.yandex.market.request.trace.Module;

import static org.mockito.Mockito.mock;

@Import({MarketApplicationCommonConfig.class,
        MonitoringController.class})
public class MarketApplicationCommonTestConfig {


    @Bean
    public Module module() {
        return mock(Module.class);
    }

    @Bean
    public boolean separatePingAndMonitoring() {
        return true;
    }



}
