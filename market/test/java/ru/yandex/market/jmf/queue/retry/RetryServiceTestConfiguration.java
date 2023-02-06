package ru.yandex.market.jmf.queue.retry;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.jmf.lock.LockServiceTestConfiguration;
import ru.yandex.market.jmf.module.metric.test.MetricsModuleTestConfiguration;
import ru.yandex.market.jmf.queue.retry.internal.TestContext;
import ru.yandex.market.request.trace.Module;

@Configuration
@Import({
        RetryTaskServiceConfiguration.class,
        RetryServiceRunnerConfiguration.class,
        LockServiceTestConfiguration.class,
        MetricsModuleTestConfiguration.class,
})
@PropertySource(
        name = "retryTestProperties",
        value = "classpath:/ru/yandex/market/jmf/queue/retry/test.properties"
)
public class RetryServiceTestConfiguration {

    public static final String CYCLICAL_HANDLER_BEAN_NAME = "cyclicalRetryTaskTestHandler";

    @Bean
    public Module module() {
        return Module.MARKET_CAMPAIGN;
    }

    @Bean(name = CYCLICAL_HANDLER_BEAN_NAME)
    public CyclicalRetryTaskHandler<TestContext> cyclicalRetryTaskTestHandler() {
        return Mockito.mock(CyclicalRetryTaskHandler.class);
    }
}
