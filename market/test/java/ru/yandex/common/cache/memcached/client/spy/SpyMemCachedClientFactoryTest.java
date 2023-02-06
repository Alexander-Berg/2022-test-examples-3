package ru.yandex.common.cache.memcached.client.spy;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SpyMemCachedClientFactoryTest {
    @Test
    public void createAddresses() {
        List<InetSocketAddress> addresses = SpyMemCachedClientFactory.createAddresses(Arrays.asList(
                "localhost:21211",
                "localhost:21212"
        ));

        assertThat(addresses).containsExactlyInAnyOrder(
             new InetSocketAddress("localhost", 21211),
             new InetSocketAddress("localhost", 21212)
        );
    }

}
