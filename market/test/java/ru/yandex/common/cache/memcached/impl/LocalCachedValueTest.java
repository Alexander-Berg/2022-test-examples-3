package ru.yandex.common.cache.memcached.impl;

import java.time.Duration;
import java.util.Date;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LocalCachedValueTest {
    @Test
    public void getExpiryNull() {
        LocalCachedValue v = LocalCachedValue.of(null, null);
        assertNull(v.getExpiry());
        assertFalse(v.hasExpired());
    }

    @Test
    public void getExpiryZero() {
        long now = System.currentTimeMillis();
        LocalCachedValue v = LocalCachedValue.of(null, new Date(0L));
        assertThat(v.getExpiry().getTime())
                .as("zero expiration means maximum expiration")
                .isGreaterThanOrEqualTo(now + LocalCachedValue.MAX_EXPIRY);
    }

    @Test
    public void getExpiryRelative() {
        long now = System.currentTimeMillis();
        LocalCachedValue v = LocalCachedValue.of(null, new Date(10_000L));
        assertThat(v.getExpiry().getTime()).isGreaterThanOrEqualTo(now + 10_000L);
        assertFalse(v.hasExpired());
    }

    @Test
    public void getExpiryRelativeMax() {
        long now = System.currentTimeMillis();
        LocalCachedValue v = LocalCachedValue.of(null, new Date(LocalCachedValue.MAX_EXPIRY));
        assertThat(v.getExpiry().getTime()).isGreaterThanOrEqualTo(now + LocalCachedValue.MAX_EXPIRY);
        assertFalse(v.hasExpired());
    }

    @Test
    public void getExpiryAbsoluteMin() {
        long absolute = LocalCachedValue.MAX_EXPIRY + 1L;
        LocalCachedValue v = LocalCachedValue.of(null, new Date(absolute));
        assertThat(v.getExpiry().getTime()).isEqualTo(absolute);
        assertTrue(v.hasExpired());
    }

    @Test
    public void getExpiryAbsolute() {
        long absolute = System.currentTimeMillis() - 1L;
        LocalCachedValue v = LocalCachedValue.of(null, new Date(absolute));
        assertThat(v.getExpiry().getTime()).isEqualTo(absolute);
        assertTrue(v.hasExpired());
    }

    @Test
    public void getExpiryAbsoluteFuture() {
        long absolute = System.currentTimeMillis() + Duration.ofHours(1L).toMillis();
        LocalCachedValue v = LocalCachedValue.of(null, new Date(absolute));
        assertThat(v.getExpiry().getTime()).isEqualTo(absolute);
        assertFalse(v.hasExpired());
    }
}
