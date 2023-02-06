package ru.yandex.market.core.config;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.task.SyncTaskExecutor;

import ru.yandex.common.cache.memcached.client.MemCachedClientFactory;
import ru.yandex.market.common.test.mockito.MemCachedClientFactoryMock;

import static org.mockito.Mockito.mock;

@Configuration
@ImportResource("classpath:common/common-memcached.xml") // поднимаем всю инфраструктуру для кеша
public class MemCachedTestConfig {
    @Bean
    public static PropertyResourceConfigurer mockMemCachedPropertyPlaceholderConfigurer(
            ConfigurableEnvironment environment,
            Collection<PlaceholderConfigurerSupport> existingConfigurers
    ) {
        // нам надо заинжектить необходимые проперти для common-memcached.xml как можно раньше,
        // до того как начнут работать PropertySourcesPlaceholderConfigurer
        environment.getPropertySources().addFirst(new MapPropertySource("mockMemCachedProperties", Map.of(
                "mbi.memcached.server.list.global", "fake"
        )));
        // при этом мы не хотим дублировать бин, если он уже есть, поэтому возвращаем noop-мок
        return existingConfigurers.isEmpty()
                ? new PropertySourcesPlaceholderConfigurer()
                : mock(PropertyResourceConfigurer.class);
    }

    @Bean(name = {
            "mockMemCachedClientFactory",
            "defaultMemCachedClientFactory", // заменяем фабрику
    })
    public MemCachedClientFactory mockMemCachedClientFactory() {
        return new MemCachedClientFactoryMock();
    }

    @Bean(name = {
            "mockMemCachedSyncExecutor",
            "defaultMemCachedAgentTaskQueue", // заменяем executor на запись
            "defaultMemCachedLocalCacherExecutor", // заменяем executor для LocalCacher
    })
    public Executor mockMemCachedSyncExecutor() {
        return new SyncTaskExecutor();
    }
}
