package ru.yandex.common.cache.memcached.client;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

public class MemCachedClusterFactoryTest {
    @Test
    public void parseServers() {
        assertThat(MemCachedClusterFactory.parseServers("mbi1ft:11212"))
                .isEqualTo(Collections.singletonList("mbi1ft:11212"));
        assertThat(MemCachedClusterFactory.parseServers("mbi1ft:11212;"))
                .as("Redundant separators are ignored")
                .isEqualTo(Collections.singletonList("mbi1ft:11212"));
        assertThat(MemCachedClusterFactory.parseServers("mbi1ft:11212;mbi2ft:11212;"))
                .as("Redundant separators are ignored")
                .isEqualTo(Arrays.asList("mbi1ft:11212", "mbi2ft:11212"));
    }

    @Test
    public void parseServersLegacy() {
        assertThat(MemCachedClusterFactory.parseServersGlobal("mbi1ft:11212;mbi2ft:11212|gravicapa1ft:11212;|gravicapa2ft:11212;", Collections.emptyList()))
                .as("Only fist pack is taken")
                .isEqualTo(Arrays.asList("mbi1ft:11212", "mbi2ft:11212"));
    }

    @Test
    public void makeClusterClient() {
        // given
        MemCachedClient c = Mockito.mock(MemCachedClient.class);
        MemCachedClientFactory cf = Mockito.mock(MemCachedClientFactory.class);
        given(cf.newClient(any(), any())).willReturn(c);
        MemCachedClusterFactory f = new MemCachedClusterFactory();

        // when
        f.setClientFactory(cf);
        f.setClusterServers("mbi1ft:11212;mbi2ft:11212;");
        MemCachedClient result = f.makeClusterClient("default");

        // then
        assertThat(result).isSameAs(c);
        verify(cf).newClient("default", Arrays.asList("mbi1ft:11212", "mbi2ft:11212"));
    }

    @Test
    @Deprecated
    public void makeClusterClientLegacy() {
        // given
        MemCachedClient c = Mockito.mock(MemCachedClient.class);
        MemCachedClientFactory cf = Mockito.mock(MemCachedClientFactory.class);
        given(cf.newClient(any(), any())).willReturn(c);
        MemCachedClusterFactory f = new MemCachedClusterFactory();

        // when
        f.setLocalClientFactory(cf);
        f.setLocalServersList("mbi1ft:11212;");
        f.setGlobalServersLists("mbi1ft:11212;|gravicapa1ft:11212;|gravicapa2ft:11212;");
        MemCachedClient result = f.makeClusterClient(null);

        // then
        assertThat(result).isSameAs(c);
        verify(cf).newClient(null, Collections.singletonList("mbi1ft:11212"));
    }
}
