package ru.yandex.market.mcrm.queue.retry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.mcrm.db.ChangelogProvider;
import ru.yandex.market.mcrm.db.test.TestMasterReadOnlyDataSourceConfiguration;
import ru.yandex.market.mcrm.handshake.HandshakeServiceConfiguration;
import ru.yandex.market.mcrm.lock.LockServiceConfiguration;
import ru.yandex.market.mcrm.queue.retry.internal.TestContext;
import ru.yandex.market.request.trace.Module;

@Configuration
@Import({
        TestMasterReadOnlyDataSourceConfiguration.class,
        RetryTaskServiceConfiguration.class,
        HandshakeServiceConfiguration.class,
        RetryServiceRunnerConfiguration.class,
        LockServiceConfiguration.class
})
public class RetryServiceTestConfiguration {

    public static final String CYCLICAL_HANDLER_BEAN_NAME = "cyclicalRetryTaskTestHandler";

    @Bean
    public Module module() {
        return Module.MARKET_CAMPAIGN;
    }

    @Bean
    public ChangelogProvider queueChangelogProvider() {
        return () -> "classpath:/ru/yandex/market/mcrm/queue/retry/queue-changelog.xml";
    }

    @Bean(name = CYCLICAL_HANDLER_BEAN_NAME)
    public CyclicalRetryTaskHandler<TestContext> cyclicalRetryTaskTestHandler() {
        return Mockito.mock(CyclicalRetryTaskHandler.class);
    }

    @Bean
    public ObjectMapper jsonObjectMapper() {
        return new ObjectMapper();
    }
}
