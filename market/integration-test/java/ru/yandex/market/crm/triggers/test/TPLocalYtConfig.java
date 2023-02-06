package ru.yandex.market.crm.triggers.test;

import javax.inject.Named;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.crm.core.test.LocalYtConfig;
import ru.yandex.market.crm.core.yt.KeyValueStorage;
import ru.yandex.market.crm.core.yt.YtReplicationConfig;
import ru.yandex.yt.ytclient.proxy.YtClient;

import static ru.yandex.market.crm.core.yt.CoreYtConfig.MAIN_CLUSTER_CLIENT;

/**
 * @author apershukov
 */
@Configuration
@Import(LocalYtConfig.class)
public class TPLocalYtConfig {

    @Bean(YtReplicationConfig.META_CLUSTER_CLIENT)
    public YtClient metaClusterYtClient(@Named(MAIN_CLUSTER_CLIENT) YtClient ytClient) {
        return ytClient;
    }

    @Bean(YtReplicationConfig.REPLICA_CLUSTERS_CLIENT)
    public YtClient replicaClusterYtClient(@Named(MAIN_CLUSTER_CLIENT) YtClient ytClient) {
        return ytClient;
    }

    @Bean
    public KeyValueStorage keyValueStorage(@Named(MAIN_CLUSTER_CLIENT) YtClient ytClient) {
        return new KeyValueStorage(ytClient, ytClient);
    }
}
