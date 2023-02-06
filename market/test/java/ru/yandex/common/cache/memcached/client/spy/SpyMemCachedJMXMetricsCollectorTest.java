package ru.yandex.common.cache.memcached.client.spy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.management.ObjectName;

import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedConnection;
import net.spy.memcached.MemcachedNode;
import org.junit.Test;

import ru.yandex.market.mbi.web.solomon.common.PullMetric;
import ru.yandex.market.mbi.web.solomon.metrics.JmxMetricsProvider;
import ru.yandex.monlib.metrics.labels.Label;
import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.monlib.metrics.registry.MetricId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SpyMemCachedJMXMetricsCollectorTest {
    @Test
    public void getSolomonFriendlyJmxAttributes() {
        Map<ObjectName, Set<String>> attrs = SpyMemCachedJMXMetricsCollector.getSolomonFriendlyJmxAttributes();
        assertThat(attrs).isNotEmpty();
        assertThat(attrs).doesNotContainValue(Collections.emptySet());
    }

    @Test
    public void toSolomonFriendly() {
        assertThat(SpyMemCachedJMXMetricsCollector.toSolomonFriendly(
                "[MEM] Reconnecting Nodes (ReconnectQueue)"
        )).isEqualTo("recon_queue");
        assertThat(SpyMemCachedJMXMetricsCollector.toSolomonFriendly(
                "[MEM] Average Time on wire for operations (Âµs)"
        )).isEqualTo("avg_time_on_wire");
    }

    @Test
    public void shouldValidatePrefix() {
        assertThatThrownBy(() -> new SpyMemCachedJMXMetricsCollector("невалидный-префикс!"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldIntegrateWithSolomon() throws IOException {
        // given
        try (SpyMemCachedJMXMetricsCollector collector = new SpyMemCachedJMXMetricsCollector(null)) {

            // when
            // MemcachedConnection регистрирует метрики прямо в конструкторе
            ConnectionFactoryBuilder connectionFactoryBuilder = new ConnectionFactoryBuilder();
            collector.enableFor(connectionFactoryBuilder);
            ConnectionFactory connectionFactory = connectionFactoryBuilder.build();

            List<InetSocketAddress> stubAddresses = Arrays.asList(
                    // берем пару адресов чтоб проверить работу фабрик очередей и сгенеренных имён
                    InetSocketAddress.createUnresolved("localhost", 12340),
                    InetSocketAddress.createUnresolved("localhost", 12341)
            );
            MemcachedConnection stubConnection = new MemcachedConnection(
                    connectionFactory.getReadBufSize(),
                    connectionFactory,
                    stubAddresses,
                    connectionFactory.getInitialObservers(),
                    connectionFactory.getFailureMode(),
                    connectionFactory.getOperationFactory()
            ) {
                @Override
                protected List<MemcachedNode> createConnections(
                        Collection<InetSocketAddress> addrs
                ) throws IOException {
                    // имитируем коннект, чтобы проверить как отработают фабрики очередей
                    SocketChannel ch = SocketChannel.open();
                    ch.configureBlocking(false);
                    for (SocketAddress sa : addrs) {
                        connectionFactory.createMemcachedNode(sa, ch, connectionFactory.getReadBufSize());
                    }
                    return Collections.emptyList(); // do nothing
                }

                @Override
                public void run() {
                    // do nothing
                }

                @Override
                public synchronized void start() {
                    // do nothing
                }
            };
            stubConnection.shutdown();

            Map<ObjectName, Set<String>> attrs = SpyMemCachedJMXMetricsCollector.getSolomonFriendlyJmxAttributes();
            JmxMetricsProvider provider = new JmxMetricsProvider(attrs);
            List<MetricId> metricIds = provider.getMetricsForPull().stream()
                    .map(PullMetric::createSolomonMetricId) // easier to debug
                    .collect(Collectors.toList());

            // then
            assertThat(metricIds)
                    .as("must contain at least one sensor for each metric")
                    .hasSizeGreaterThanOrEqualTo(attrs.values().stream().mapToInt(Collection::size).sum());
            // https://docs.yandex-team.ru/solomon/concepts/data-model#limits
            Pattern labelNamePattern = Pattern.compile("^[a-zA-Z][0-9a-zA-Z_]{0,31}$");
            List<Label> metricLabels = metricIds.stream()
                    .flatMap(mid -> mid.getLabels().stream())
                    .collect(Collectors.toList());
            for (Label label : metricLabels) {
                assertThat(label.getKey()).matches(labelNamePattern);
                assertThat(label.getValue()).matches(SpyMemCachedJMXMetricsCollector::isSolomonFriendlyLabelValue);
            }
            // избирательно проверим некоторые
            assertThat(metricLabels).contains(
                    Labels.allocator.alloc("domain", SpyMemCachedJMXMetricsCollector.JMX_DOMAIN),
                    Labels.allocator.alloc("name", "id0_avg_time_on_wire"),
                    Labels.allocator.alloc("name", "id0_read_op_queue_1_size"), // для второго адреса
                    Labels.allocator.alloc("name", "id0_recon_queue"),
                    Labels.allocator.alloc("name", "id0_write_op_queue_0_size") // для первого адреса
            );
        }
    }
}
