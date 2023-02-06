package ru.yandex.market.mboc.common.ydb;


import java.util.concurrent.CompletableFuture;

import com.yandex.ydb.core.auth.AuthProvider;
import com.yandex.ydb.core.auth.NopAuthProvider;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.mbo.ydb.client.YdbClient;
import ru.yandex.market.mbo.ydb.config.BaseYdbConfig;
import ru.yandex.market.mboc.common.utils.TestYdbConfig;

@TestConfiguration
@EnableConfigurationProperties(TestYdbConfig.TestYdbProperties.class)
@ConditionalOnProperty(value = "ydbTestProfile", matchIfMissing = true)
public class TestYdbMockConfig extends BaseYdbConfig<TestYdbConfig.TestYdbProperties> {
    public TestYdbMockConfig(TestYdbConfig.TestYdbProperties ydbProperties) {
        super(ydbProperties);
    }

    @Bean
    @Primary
    @Override
    public AuthProvider authProvider() {
        return NopAuthProvider.INSTANCE;
    }

    @Bean
    @Primary
    @Override
    public YdbClient ydbClient() {
        var mock = Mockito.mock(YdbClient.class);
        Mockito.when(mock.executeRwAsync(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(null));
        return mock;
    }
}
