package ru.yandex.market.mboc.common.utils;

import com.yandex.ydb.core.auth.AuthProvider;
import com.yandex.ydb.core.auth.NopAuthProvider;
import com.yandex.ydb.core.grpc.GrpcTransport;
import com.yandex.ydb.table.TableClient;
import com.yandex.ydb.table.rpc.grpc.GrpcTableRpc;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import ru.yandex.market.mbo.ydb.client.YdbClient;
import ru.yandex.market.mbo.ydb.config.BaseYdbConfig;
import ru.yandex.market.mbo.ydb.config.BaseYdbProperties;

@TestConfiguration
@EnableConfigurationProperties(TestYdbConfig.TestYdbProperties.class)
@ConditionalOnProperty(value = "ydbTestProfile", havingValue = "containered")
public class TestYdbConfig extends BaseYdbConfig<TestYdbConfig.TestYdbProperties> {

    public TestYdbConfig(TestYdbProperties ydbProperties) {
        super(ydbProperties);
    }

    @Override
    @Bean
    public AuthProvider authProvider() {
        return NopAuthProvider.INSTANCE;
    }

    @Override
    public GrpcTransport grpcTransport() {
        var endpoint = getYdbProperties().getEndpoint().split(":");
        return GrpcTransport.forHost(endpoint[0], Integer.parseInt(endpoint[1])).withDataBase("/local").build();
    }

    @Override
    public TableClient tableClient() {
        return TableClient.newClient(GrpcTableRpc.useTransport(grpcTransport())).build();
    }

    @Bean
    @Override
    public YdbClient ydbClient() {
        return super.ydbClient();
    }

    @ConfigurationProperties(prefix = "ydb")
    public static class TestYdbProperties extends BaseYdbProperties {
    }
}
