package ru.yandex.direct.utils;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.direct.utils.hostname.CachedHostnameResolver;
import ru.yandex.direct.utils.hostname.FallbackHostnameResolver;
import ru.yandex.direct.utils.hostname.HostnameResolver;
import ru.yandex.direct.utils.hostname.HostnameResolvingException;

public class HostnameTest {
    static class SeqHostnameResolver implements HostnameResolver {
        int seq = 0;

        @Override
        public String getHostname() {
            seq += 1;
            return "hostname" + seq;
        }
    }

    static class FailingHostnameResolver implements HostnameResolver {
        @Override
        public String getHostname() {
            throw new HostnameResolvingException("Failed", new RuntimeException());
        }
    }

    @Test
    public void testFallback() {
        HostnameResolver resolver = new FallbackHostnameResolver(new SeqHostnameResolver(), "localhost");
        Assert.assertEquals("hostname1", resolver.getHostname());
        Assert.assertEquals("hostname2", resolver.getHostname());
        // TODO: проверить что в логе пусто
    }

    @Test
    public void testFallbackToLocalhost() {
        HostnameResolver resolver = new FallbackHostnameResolver(new FailingHostnameResolver(), "localhost");
        Assert.assertEquals("localhost", resolver.getHostname());
        Assert.assertEquals("localhost", resolver.getHostname());
        // TODO: проверить, что в логе ровно один ворнинг
    }

    @Test
    public void testCached() {
        HostnameResolver resolver = new CachedHostnameResolver(new SeqHostnameResolver());
        Assert.assertEquals("hostname1", resolver.getHostname());
        Assert.assertEquals("hostname1", resolver.getHostname());
        Assert.assertEquals("hostname1", resolver.getHostname());
    }

    @Test
    public void testCachedError() {
        HostnameResolver resolver = new CachedHostnameResolver(new FailingHostnameResolver());

        HostnameResolvingException exc1 = null;
        try {
            resolver.getHostname();
        } catch (HostnameResolvingException exc) {
            exc1 = exc;
        }
        Assert.assertNotNull(exc1);
        Assert.assertEquals("Failed", exc1.getMessage());
        Assert.assertNotNull(exc1.getCause());

        HostnameResolvingException exc2 = null;
        try {
            resolver.getHostname();
        } catch (HostnameResolvingException exc) {
            exc2 = exc;
        }
        Assert.assertNotNull(exc2);
        Assert.assertEquals("Failed", exc2.getMessage());
        Assert.assertNull(exc2.getCause());
    }
}
