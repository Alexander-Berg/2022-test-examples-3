package ru.yandex.market.wms.common.spring.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.wms.common.spring.service.notifier.TransportUnitEvent;
import ru.yandex.market.wms.common.spring.service.notifier.TransportUnitEventNotifier;

@TestConfiguration
public class BalanceServiceTestConfig {

    @Bean
    @Primary
    public TransportUnitEventNotifier transportUnitEventNotifier() {
        return new TransportUnitEventNotifier() {
            @Override
            public void notify(TransportUnitEvent event) {
                //dummy
            }
        };
    }
}
