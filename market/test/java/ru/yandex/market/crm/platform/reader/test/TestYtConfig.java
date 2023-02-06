package ru.yandex.market.crm.platform.reader.test;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.crm.chyt.services.ChytQueryExecutor;
import ru.yandex.market.crm.platform.config.ClusterGroup;
import ru.yandex.market.crm.platform.reader.yt.ReplicationUtils;
import ru.yandex.market.crm.platform.reader.yt.YtClients;
import ru.yandex.market.crm.platform.reader.yt.YtClients.YtCluster;
import ru.yandex.market.crm.platform.test.yt.CoreTestYtConfig;
import ru.yandex.market.crm.yt.YtClientBuilder;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.crm.yt.operations.OperationHandler;
import ru.yandex.market.crm.yt.operations.YtOperationStrategy;
import ru.yandex.market.crm.yt.tx.CallbackExecutor;

import static org.mockito.Mockito.mock;

/**
 * @author apershukov
 */
@Configuration
@Import(CoreTestYtConfig.class)
class TestYtConfig {

    @Bean
    public Yt yt() {
        return YtClientBuilder.forCluster(CoreTestYtConfig.getProxy(), "")
                .setStatePingTimeoutSec(10L)
                .build();
    }

    @Bean
    public YtClients ytClients(Yt yt) {
        YtCluster ytCluster = new YtCluster("plato", ClusterGroup.ONLINE, yt);

        return new YtClients(
                ytCluster,
                Collections.singletonList(ytCluster)
        );
    }

    @Bean
    public ReplicationUtils replicationUtils(YtClients ytClients) {
        return new ReplicationUtils(ytClients);
    }

    @Bean
    public YtClient mrClusterClient(Yt yt, ru.yandex.yt.ytclient.proxy.YtClient ytRpcClient) {
        var operationHandler = new OperationHandler<>(
                "yt",
                new CallbackExecutor(),
                new YtOperationStrategy()
        );

        return new YtClient(
                "market-lilucrm",
                "default",
                yt,
                ytRpcClient,
                operationHandler
        );
    }

    @Bean
    public ChytQueryExecutor chytQueryExecutor() {
        return mock(ChytQueryExecutor.class);
    }
}
