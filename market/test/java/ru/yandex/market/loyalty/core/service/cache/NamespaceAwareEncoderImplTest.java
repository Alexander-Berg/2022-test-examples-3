package ru.yandex.market.loyalty.core.service.cache;

import org.junit.Test;

import ru.yandex.market.loyalty.core.config.caches.PromoBundleCache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NamespaceAwareEncoderImplTest {

    @Test
    public void encodeKey() {
        final NamespaceAwareEncoder encoder = createEncoder();
        assertEquals("promo_bundle_key1", encoder.encodeKey("key1"));
        assertEquals("promo_bundle_key2", encoder.encodeKey("promo_bundle_key2"));
    }

    @Test
    public void decodeKey() {
        final NamespaceAwareEncoder encoder = createEncoder();
        assertEquals("key1", encoder.decodeKey("key1"));
        assertEquals("key2", encoder.decodeKey("promo_bundle_key2"));
    }

    private NamespaceAwareEncoder createEncoder() {
        final NamespaceAwareEncoder encoder = NamespaceAwareEncoderImpl.of(PromoBundleCache.NAMESPACE);
        assertNotNull(encoder);
        assertEquals("promo_bundle", encoder.getNamespace());
        return encoder;
    }
}
