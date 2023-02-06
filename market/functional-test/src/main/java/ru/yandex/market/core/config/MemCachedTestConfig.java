package ru.yandex.market.core.config;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.task.SyncTaskExecutor;

import ru.yandex.common.cache.memcached.client.MemCachedClientFactory;
import ru.yandex.market.common.test.mockito.MemCachedClientFactoryMock;

import static org.mockito.Mockito.mock;

// поднимай полноценную инфраструктуру для кеша в тестах и добавляй этот конфижек для почти честного поведения кеша
@Configuration
public class MemCachedTestConfig {
    @Bean
    public static PropertyResourceConfigurer mockMemCachedPropertyPlaceholderConfigurer(
            ConfigurableEnvironment environment,
            Collection<PlaceholderConfigurerSupport> existingConfigurers
    ) {
        // нам надо заинжектить необходимые проперти для кеша как можно раньше,
        // до того как начнут работать PropertySourcesPlaceholderConfigurer
        environment.getPropertySources().addFirst(new MapPropertySource("mockMemCachedProperties", Map.of(
                "vendors.memcached.server.list.global", "fake"
        )));
        // при этом мы не хотим дублировать бин, если он уже есть, поэтому возвращаем noop-мок
        return existingConfigurers.isEmpty()
                ? new PropertySourcesPlaceholderConfigurer()
                : mock(PropertyResourceConfigurer.class);
    }

    @Bean(name = {
            "mockMemCachedClientFactory",
            "memCachedClientFactory", // заменяем фабрику
    })
    public MemCachedClientFactory mockMemCachedClientFactory() {
        return new MemCachedClientFactoryMock();
    }

    @Bean(name = {
            "mockMemCachedSyncExecutor",
            "memCachedAgentTaskQueue", // заменяем executor на запись
            "memCachedLocalCacherExecutor", // заменяем executor для LocalCacher
    })
    public Executor mockMemCachedSyncExecutor() {
        return new SyncTaskExecutor();
    }
}
