package ru.yandex.market.logistics.cs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.logistics.cs.dbqueue.common.AccountingTaskLifecycleListener;

@Configuration
public class AccountingTaskLifecycleListenerConfig {

    @Bean
    @Primary
    public AccountingTaskLifecycleListener taskLifecycleListener() {
        return new AccountingTaskLifecycleListener();
    }
}
