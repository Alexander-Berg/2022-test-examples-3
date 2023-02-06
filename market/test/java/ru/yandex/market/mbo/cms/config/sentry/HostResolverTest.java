package ru.yandex.market.mbo.cms.config.sentry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author sergtru
 * @since 15.05.2018
 */
public class HostResolverTest {

    @Test
    public void testVlaTesting() {
        HostResolver resolver = HostResolver.forHost("vla1-5743-vla-market-test-mbo-cms-api-29625");
        assertNotNull(resolver);
        assertEquals("vla", resolver.getDataCenter());
        assertEquals("mbo-cms-api", resolver.getComponent());
        assertEquals("test", resolver.getEnvironment());
    }

    @Test
    public void testUnknownComponent() {
        HostResolver resolver = HostResolver.forHost("sas2-1265-sas-market-prod-unknown-30015");
        assertNotNull(resolver);
        assertEquals("unknown", resolver.getComponent());
    }

    @Test
    public void testInvalid() {
        HostResolver resolver = HostResolver.forHost("sas-unknown-30015");
        assertNull(resolver);
    }
}
