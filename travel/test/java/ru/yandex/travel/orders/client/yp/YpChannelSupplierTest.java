package ru.yandex.travel.orders.client.yp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.netty.util.concurrent.ImmediateEventExecutor;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.orders.client.ChannelConsumer;
import ru.yandex.yp.YpRawClient;
import ru.yandex.yp.model.YpSelectedObjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class YpChannelSupplierTest {
    private YpChannelSupplier supplier;
    private TestConsumer consumer;

    private YpRawClient sasYp = mock(YpRawClient.class, RETURNS_DEEP_STUBS);
    private YpRawClient manYp = mock(YpRawClient.class, RETURNS_DEEP_STUBS);
    private YpRawClient vlaYp = mock(YpRawClient.class, RETURNS_DEEP_STUBS);

    private LocalYpCache cache = new MockedLocalCache();
    private Map<String, List<Endpoint>> cacheBackend = new HashMap<>();


    @Before
    public void init() {
        var clients = Map.of("SAS", sasYp, "MAN", manYp, "VLA", vlaYp);
        supplier = new YpChannelSupplier(clients, cache, "test-endpoint", ImmediateEventExecutor.INSTANCE);
        consumer = new TestConsumer();
        supplier.subscribe(consumer);
    }

    private void initLocalCache(String location, String... targets) {
        cacheBackend.put(location, Arrays.stream(targets).map(Endpoint::fromTarget).collect(Collectors.toList()));
    }

    private void mockDiscovery(YpRawClient client, String... targets) {
        List<Object> eps = Arrays.stream(targets).map(Endpoint::fromTarget).collect(Collectors.toList());
        when(client.objectService().selectObjects(any(), any())).thenReturn(CompletableFuture.completedFuture(new YpSelectedObjects(eps, 0L)));
    }

    private void mockDiscoveryException(YpRawClient client) {
        when(client.objectService().selectObjects(any(), any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("YP in unavailable")));
    }


    @Test
    public void testRegularDiscovery() {
        mockDiscovery(sasYp, "sas1:80", "sas2:80");
        mockDiscovery(manYp, "man1:80", "man2:80");
        mockDiscovery(vlaYp, "vla1:80", "vla2:80");
        supplier.refresh();
        assertThat(consumer.knownLabels).contains("sas1:80", "sas2:80", "man1:80", "man2:80", "vla1:80", "vla2:80");
        assertThat(cache.get("SAS")).extracting(Endpoint::getTarget).contains("sas1:80", "sas2:80");
        assertThat(cache.get("VLA")).extracting(Endpoint::getTarget).contains("vla1:80", "vla2:80");
        assertThat(cache.get("MAN")).extracting(Endpoint::getTarget).contains("man1:80", "man2:80");
        assertThat(cacheBackend.get("SAS")).extracting(Endpoint::getTarget).contains("sas1:80", "sas2:80");
        assertThat(cacheBackend.get("VLA")).extracting(Endpoint::getTarget).contains("vla1:80", "vla2:80");
        assertThat(cacheBackend.get("MAN")).extracting(Endpoint::getTarget).contains("man1:80", "man2:80");
    }

    @Test
    public void testDiscoveryOverridesCacheValues() {
        initLocalCache("SAS", "sas1:80");
        initLocalCache("MAN", "man1:80");
        initLocalCache("VLA", "vla1:80");
        mockDiscovery(sasYp, "sas2:80");
        mockDiscovery(manYp, "man2:80");
        mockDiscovery(vlaYp, "vla2:80");
        cache.load();
        assertThat(cache.get("SAS")).extracting(Endpoint::getTarget).contains("sas1:80");
        assertThat(cache.get("VLA")).extracting(Endpoint::getTarget).contains("vla1:80");
        assertThat(cache.get("MAN")).extracting(Endpoint::getTarget).contains("man1:80");
        supplier.refresh();
        assertThat(consumer.knownLabels).contains("sas2:80", "man2:80", "vla2:80");
        assertThat(cache.get("SAS")).extracting(Endpoint::getTarget).contains("sas2:80");
        assertThat(cache.get("VLA")).extracting(Endpoint::getTarget).contains("vla2:80");
        assertThat(cache.get("MAN")).extracting(Endpoint::getTarget).contains("man2:80");
        assertThat(cacheBackend.get("SAS")).extracting(Endpoint::getTarget).contains("sas2:80");
        assertThat(cacheBackend.get("VLA")).extracting(Endpoint::getTarget).contains("vla2:80");
        assertThat(cacheBackend.get("MAN")).extracting(Endpoint::getTarget).contains("man2:80");
    }

    @Test
    public void testLoosingChannel() {
        mockDiscovery(sasYp, "sas1:80", "sas2:80");
        mockDiscovery(manYp, "man1:80", "man2:80");
        mockDiscovery(vlaYp, "vla1:80", "vla2:80");
        supplier.refresh();
        assertThat(consumer.knownLabels).contains("sas1:80", "sas2:80", "man1:80", "man2:80", "vla1:80", "vla2:80");
        mockDiscovery(manYp, "man3:80");
        supplier.refresh();
        assertThat(consumer.knownLabels).contains("sas1:80", "sas2:80", "man3:80", "vla1:80", "vla2:80");
        assertThat(consumer.knownLabels).doesNotContain("man1:80", "man2:80");
    }

    @Test
    public void testDiscoveryCrashesCacheRemains() {
        initLocalCache("SAS", "sas1:80");
        initLocalCache("MAN", "man1:80");
        initLocalCache("VLA", "vla1:80");
        cache.load();
        mockDiscovery(sasYp, "sas2:80");
        mockDiscoveryException(manYp);
        mockDiscovery(vlaYp, "vla2:80");
        supplier.refresh();
        assertThat(consumer.knownLabels).contains("sas2:80", "man1:80", "vla2:80");
    }

    static class TestConsumer implements ChannelConsumer {
        Set<String> knownLabels = new HashSet<>();

        @Override
        public void onChannelDiscovered(String channelLabel) {
            knownLabels.add(channelLabel);
        }

        @Override
        public void onChannelLost(String channelLabel) {
            knownLabels.remove(channelLabel);
        }
    }

    class MockedLocalCache extends LocalYpCache {
        public MockedLocalCache() {
            super(null);
        }

        @Override
        public synchronized void load() {
            cachedMap = new ConcurrentHashMap<>(cacheBackend);
        }

        @Override
        public synchronized void save() {
            cacheBackend = new HashMap<>(cachedMap);
        }
    }
}
