package ru.yandex.direct.libs.cachingutils;

import javax.annotation.Nonnull;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CacheSimpleTest {

    @Test
    public void testValueReturnedCorrectly() {
        var cache = new TwoSizedCacheForTest();
        assertThat(cache.getValue("chupakabra"), is("chupakabra"));
    }

    private static class TwoSizedCacheForTest extends DirectLoadingCache<String, String> {
        TwoSizedCacheForTest() {
            super(2);
        }

        @Override
        protected String getValue(String key) {
            return key;
        }

        @Override
        @Nonnull
        protected String solomonSensorName() {
            return "zaglushka";
        }
    }
}
