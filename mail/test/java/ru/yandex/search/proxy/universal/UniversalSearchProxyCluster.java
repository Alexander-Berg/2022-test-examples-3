package ru.yandex.search.proxy.universal;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpHost;

import ru.yandex.client.producer.ProducerClientConfigBuilder;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.config.DnsConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.parser.searchmap.SearchMapConfigBuilder;
import ru.yandex.search.proxy.UpstreamConfigBuilder;
import ru.yandex.search.proxy.UpstreamsConfigBuilder;
import ru.yandex.search.request.config.SearchBackendRequestConfigBuilder;

public class UniversalSearchProxyCluster
    implements GenericAutoCloseable<IOException>
{
    public static final int TIMEOUT = 4000;
    public static final int FAKE_PORT1 = 8088;
    public static final int FAKE_PORT2 = 531;
    public static final HttpHost FAKE_HOST1 =
        new HttpHost("nobody.will.ever.register.this", FAKE_PORT1);
    public static final HttpHost FAKE_HOST2 =
        new HttpHost("other", FAKE_PORT2);
    public static final String SERVICE = "my_service";
    public static final String GOOD_FAKE = "fake.host.here";
    public static final String GOOD_FAKE2 = "another.fake.host.here";
    public static final String GOOD_FAKE3 = "third.fake.host.here";

    private final StaticServer backend;
    private final StaticServer producer;
    private final UniversalSearchProxy<?> proxy;
    private final GenericAutoCloseableChain<IOException> chain;

    public UniversalSearchProxyCluster() throws Exception {
        this(true, true);
    }

    public UniversalSearchProxyCluster(
        final boolean localityShuffle) throws Exception
    {
        this(true, searchMapSuffix(true), localityShuffle);
    }

    public UniversalSearchProxyCluster(
        final boolean useProducer,
        final boolean useFakeHost)
        throws Exception
    {
        this(useProducer, searchMapSuffix(useFakeHost), false);
    }

    public UniversalSearchProxyCluster(
        final boolean useProducer,
        final String searchMapSuffix,
        final boolean localityShuffle)
        throws Exception
    {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            backend = new StaticServer(Configs.baseConfig("Backend"));
            chain.get().add(backend);
            producer = new StaticServer(Configs.baseConfig("Producer"));
            chain.get().add(producer);

            UniversalSearchProxyConfigBuilder builder =
                new UniversalSearchProxyConfigBuilder();
            builder.port(0);
            builder.connections(10);
            builder.dnsConfig(
                new DnsConfigBuilder()
                    .dnsHostsMapping(
                        Map.of(
                            GOOD_FAKE, "localhost",
                            GOOD_FAKE2, "localhost",
                            GOOD_FAKE3, "localhost")));

            StringBuilder searchMap =
                new StringBuilder(searchMapRule(backend.host()));
            searchMap.append(searchMapSuffix);
            builder.searchMapConfig(
                new SearchMapConfigBuilder().content(new String(searchMap)));

            builder.searchConfig(Configs.targetConfig());
            builder.indexerConfig(Configs.targetConfig());
            UpstreamConfigBuilder upstreamConfigBuilder =
                new UpstreamConfigBuilder()
                    .connections(10)
                    .timeout(TIMEOUT)
                    .requestConfig(
                        new SearchBackendRequestConfigBuilder()
                            .localityShuffle(localityShuffle));

            builder.upstreamsConfig(
                new UpstreamsConfigBuilder()
                    .asterisk(upstreamConfigBuilder));
            if (useProducer) {
                builder.producerClientConfig(
                    new ProducerClientConfigBuilder()
                        .connections(10)
                        .timeout(TIMEOUT)
                        .host(producer.host()));
            }

            proxy = new UniversalSearchProxy<>(builder.build());
            chain.get().add(proxy);
            this.chain = chain.release();
        }
    }

    public static String searchMapRule(final HttpHost host) {
        return SERVICE + " shards:0-65533,host:" + host.getHostName()
            + ",search_port_ng:" + host.getPort() + '\n';
    }

    private static String searchMapSuffix(final boolean useFakeHost) {
        if (useFakeHost) {
            return searchMapRule(FAKE_HOST1);
        } else {
            return "";
        }
    }

    public StaticServer backend() {
        return backend;
    }

    public StaticServer producer() {
        return producer;
    }

    public UniversalSearchProxy<?> proxy() {
        return proxy;
    }

    public void start() throws IOException {
        backend.start();
        producer.start();
        proxy.start();
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }
}

