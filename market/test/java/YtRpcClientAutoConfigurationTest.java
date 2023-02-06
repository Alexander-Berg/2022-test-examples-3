package ru.yandex.market.starter.yt;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import ru.yandex.inside.yt.kosher.impl.YtConfiguration;
import ru.yandex.market.starter.yt.config.YtConfigAutoConfiguration;
import ru.yandex.market.starter.yt.config.YtRpcClientAutoConfiguration;
import ru.yandex.market.starter.yt.configurer.rpc.YtRpcClientConfigurerAdapter;
import ru.yandex.market.starter.yt.factory.YtRpcClientFactory;
import ru.yandex.market.starter.properties.yt.rpc.YtClusterProperties;
import ru.yandex.market.starter.properties.yt.rpc.YtRpcProperties;
import ru.yandex.yt.ytclient.proxy.YtClient;
import ru.yandex.yt.ytclient.proxy.YtCluster;
import ru.yandex.yt.ytclient.proxy.internal.DiscoveryMethod;
import ru.yandex.yt.ytclient.rpc.RpcOptions;

import static org.assertj.core.api.Assertions.assertThat;

public class YtRpcClientAutoConfigurationTest {
    private static final String TOKEN = "token";
    private static final String API_HOST = "apiHost";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withPropertyValues(
            "mj.yt.apiHost=" + API_HOST,
            "mj.yt.token=" + TOKEN
        )
        .withConfiguration(AutoConfigurations.of(
            YtConfigAutoConfiguration.class,
            YtRpcClientAutoConfiguration.class
        ));

    @Test
    void rpcOptionsTest() {
        final int acknowledgementTimeoutSecs = 23432;
        final int channelPoolRebalanceIntervalSecs = 775;
        final int channelPoolSize = 346;
        final int clientCacheExpirationMillis = 645;
        final int clientCacheSize = 2389;
        final boolean defaultRequestAck = true;
        final int failoverTimeoutMillis = 982;
        final int globalTimeoutMillis = 374;
        final int maxBackoffTimeSecs = 7659;
        final int minBackoffTimeSecs = 3673;
        final int pingTimeoutMillis = 261;
        final String preferableDiscoveryMethod = "rpc";
        final int proxyUpdateTimeoutMillis = 843;
        final boolean trace = true;
        final boolean traceDebug = true;
        final boolean traceSampled = true;
        final boolean useClientCache = true;
        final int rpcClientSelectionTimeoutSecs = 730;
        final int streamingReadTimeoutMillis = 902;
        final int streamingWriteTimeoutMillis = 2567;
        final int streamingWindowSizeBytes = 520;
        contextRunner
            .withPropertyValues(
                "mj.yt.async=true",
                "mj.yt.protocol=rpc",
                "mj.yt.rpc.custom=true",
                "mj.yt.rpc.options.acknowledgementTimeoutSecs=" + acknowledgementTimeoutSecs,
                "mj.yt.rpc.options.channelPoolRebalanceIntervalSecs=" + channelPoolRebalanceIntervalSecs,
                "mj.yt.rpc.options.channelPoolSize=" + channelPoolSize,
                "mj.yt.rpc.options.clientCacheExpirationMillis=" + clientCacheExpirationMillis,
                "mj.yt.rpc.options.clientCacheSize=" + clientCacheSize,
                "mj.yt.rpc.options.defaultRequestAck=" + defaultRequestAck,
                "mj.yt.rpc.options.failoverTimeoutMillis=" + failoverTimeoutMillis,
                "mj.yt.rpc.options.globalTimeoutMillis=" + globalTimeoutMillis,
                "mj.yt.rpc.options.maxBackoffTimeSecs=" + maxBackoffTimeSecs,
                "mj.yt.rpc.options.minBackoffTimeSecs=" + minBackoffTimeSecs,
                "mj.yt.rpc.options.pingTimeoutMillis=" + pingTimeoutMillis,
                "mj.yt.rpc.options.preferableDiscoveryMethod=" + preferableDiscoveryMethod,
                "mj.yt.rpc.options.proxyUpdateTimeoutMillis=" + proxyUpdateTimeoutMillis,
                "mj.yt.rpc.options.trace=" + trace,
                "mj.yt.rpc.options.traceDebug=" + traceDebug,
                "mj.yt.rpc.options.traceSampled=" + traceSampled,
                "mj.yt.rpc.options.useClientCache=" + useClientCache,
                "mj.yt.rpc.options.rpcClientSelectionTimeoutSecs=" + rpcClientSelectionTimeoutSecs,
                "mj.yt.rpc.options.streamingReadTimeoutMillis=" + streamingReadTimeoutMillis,
                "mj.yt.rpc.options.streamingWriteTimeoutMillis=" + streamingWriteTimeoutMillis,
                "mj.yt.rpc.options.streamingWindowSizeBytes=" + streamingWindowSizeBytes
            ).run(context -> {
                    final RpcOptions rpcOptions = context.getBean(RpcOptions.class);
                    assertThat(rpcOptions.getAcknowledgementTimeout())
                        .isEqualTo(Duration.ofSeconds(acknowledgementTimeoutSecs));
                    assertThat(rpcOptions.getChannelPoolRebalanceInterval())
                        .isEqualTo(Duration.ofSeconds(channelPoolRebalanceIntervalSecs));
                    assertThat(rpcOptions.getChannelPoolSize()).isEqualTo(channelPoolSize);
                    assertThat(rpcOptions.getClientCacheExpiration())
                        .isEqualTo(Duration.ofMillis(clientCacheExpirationMillis));
                    assertThat(rpcOptions.getClientsCacheSize()).isEqualTo(clientCacheSize);
                    assertThat(rpcOptions.getDefaultRequestAck()).isEqualTo(defaultRequestAck);
                    assertThat(rpcOptions.getFailoverTimeout()).isEqualTo(Duration.ofMillis(failoverTimeoutMillis));
                    assertThat(rpcOptions.getGlobalTimeout()).isEqualTo(Duration.ofMillis(globalTimeoutMillis));
                    assertThat(rpcOptions.getMaxBackoffTime()).isEqualTo(Duration.ofSeconds(maxBackoffTimeSecs));
                    assertThat(rpcOptions.getMinBackoffTime()).isEqualTo(Duration.ofSeconds(minBackoffTimeSecs));
                    assertThat(rpcOptions.getPingTimeout()).isEqualTo(Duration.ofMillis(pingTimeoutMillis));
                    assertThat(rpcOptions.getPreferableDiscoveryMethod())
                        .isEqualTo(DiscoveryMethod.valueOf(preferableDiscoveryMethod.toUpperCase(Locale.ROOT)));
                    assertThat(rpcOptions.getProxyUpdateTimeout())
                        .isEqualTo(Duration.ofMillis(proxyUpdateTimeoutMillis));
                    assertThat(rpcOptions.getTrace()).isEqualTo(trace);
                    assertThat(rpcOptions.getTraceDebug()).isEqualTo(traceDebug);
                    assertThat(rpcOptions.getTraceSampled()).isEqualTo(traceSampled);
                    assertThat(rpcOptions.getUseClientsCache()).isEqualTo(useClientCache);
                    assertThat(rpcOptions.getRpcClientSelectionTimeout())
                        .isEqualTo(Duration.ofSeconds(rpcClientSelectionTimeoutSecs));
                    assertThat(rpcOptions.getStreamingReadTimeout())
                        .isEqualTo(Duration.ofMillis(streamingReadTimeoutMillis));
                    assertThat(rpcOptions.getStreamingWriteTimeout())
                        .isEqualTo(Duration.ofMillis(streamingWriteTimeoutMillis));
                    assertThat(rpcOptions.getStreamingWindowSize()).isEqualTo(streamingWindowSizeBytes);
                }
            );
    }

    @Test
    public void clustersInfo_ViaProperties_Context_Test() {
        final String name = "nameFromProps";
        contextRunner
            .withUserConfiguration(SimpleYtConfigConfiguration.class)
            .withPropertyValues(
                "mj.yt.async=true",
                "mj.yt.protocol=rpc",
                "mj.yt.rpc.custom=true",
                "mj.yt.rpc.clusters[0].name=" + name
            )
            .run(context -> {
                    final YtClient ytClient = context.getBean(YtClient.class);
                    final List<YtCluster> clusters = ytClient.getClusters();
                    assertThat(clusters.size()).isEqualTo(1);
                    final YtCluster ytCluster = clusters.get(0);
                    assertThat(ytCluster.getName()).isEqualTo(name);
                }
            );
    }

    @Test
    public void clustersInfo_ViaProperties_collectClustersInfoMethod_Test() {
        final String name = "arerada";
        final String balancerFqdn = "pitweof";
        final int port = 454;
        final List<String> addresses = List.of("dfsdf", "sdfsdds");
        final String proxyRole = "rewuio";

        final YtRpcProperties ytRpcProperties = new YtRpcProperties();
        final YtClusterProperties ytClusterProperties = new YtClusterProperties();
        ytRpcProperties.setClusters(Collections.singletonList(ytClusterProperties));

        ytClusterProperties.setName(name);
        ytClusterProperties.setBalancerFqdn(balancerFqdn);
        ytClusterProperties.setPort(port);
        ytClusterProperties.setAddresses(addresses);
        ytClusterProperties.setProxyRole(proxyRole);

        final List<YtCluster> clusters = YtRpcClientFactory.collectClustersInfo(
            new YtConfiguration(), ytRpcProperties, new YtRpcClientConfigurerAdapter() {

            }, Collections.emptyList()
        );

        assertThat(clusters.size()).isEqualTo(1);
        final YtCluster ytCluster = clusters.get(0);
        assertThat(ytCluster.getName()).isEqualTo(name);
    }

    @Test
    public void clustersInfo_FromYtConfig_Test() {
        contextRunner
            .withPropertyValues(
                "mj.yt.async=true",
                "mj.yt.protocol=rpc",
                "mj.yt.rpc.custom=true"
            ).run(context -> {
                    final YtClient ytClient = context.getBean(YtClient.class);
                    assertThat(ytClient.getClusters().size()).isEqualTo(1);
                    final YtCluster ytCluster = ytClient.getClusters().get(0);
                    assertThat(ytCluster.getName()).isEqualTo(API_HOST);
                }
            );
    }

    @Test
    public void clustersInfo_ViaJavaConfig_Test() {
        contextRunner
            .withUserConfiguration(
                AdditionalYtClustersConfiguration.class,
                SimpleYtConfigConfiguration.class
            )
            .withPropertyValues(
                "mj.yt.async=true",
                "mj.yt.protocol=rpc",
                "mj.yt.rpc.custom=true"
            )
            .run(context -> {
                    final YtClient ytClient = context.getBean(YtClient.class);
                    final Set<YtCluster> clusters = new HashSet<>(ytClient.getClusters());
                    assertThat(clusters.size()).isEqualTo(2);
                    assertThat(clusters.contains(AdditionalYtClustersConfiguration.javaConfigCluster1)).isTrue();
                    assertThat(clusters.contains(AdditionalYtClustersConfiguration.javaConfigCluster2)).isTrue();
                }
            );
    }

    private static class AdditionalYtClustersConfiguration {
        private static YtCluster javaConfigCluster1 = new YtCluster("javaConfig1");
        private static YtCluster javaConfigCluster2 = new YtCluster("javaConfig2");

        @Bean
        public YtCluster javaConfigCluster1() {
            return javaConfigCluster1;
        }

        @Bean
        public YtCluster javaConfigCluster2() {
            return javaConfigCluster2;
        }
    }

    private static class SimpleYtConfigConfiguration {

        @Bean
        public YtConfiguration ytConfiguration() {
            return new YtConfiguration();
        }
    }
}
