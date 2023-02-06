package ru.yandex.market.antifraud.orders.cache;

import java.time.Duration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.test.annotations.DaoLayerTest;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Order;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
@DaoLayerTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PgCacheTest {

    @Autowired
    private NamedParameterJdbcOperations jdbcTemplate;

    @Test
    public void putAndGet() {
        String value = "test_string";
        Cache cache = new PgCache("putAndGet", jdbcTemplate, Duration.ofMinutes(10L));
        cache.put("key", value);
        Cache.ValueWrapper cached = cache.get("key");
        assertThat(cached).isNotNull();
        assertThat(cached.get()).isEqualTo(value);
    }

    @Test
    public void putAndGetClass() {
        String value = "test_string";
        Cache cache = new PgCache("putAndGet", jdbcTemplate, Duration.ofMinutes(10L));
        cache.put("key", value);
        String cached = cache.get("key", String.class);
        assertThat(cached).isEqualTo(value);
    }

    @Test
    public void putAndGetNull() {
        Cache cache = new PgCache("putAndGetNull", jdbcTemplate, Duration.ofMinutes(10L));
        cache.put("key", null);
        Cache.ValueWrapper cached = cache.get("key");
        assertThat(cached).isNotNull();
        assertThat(cached.get()).isNull();
    }

    @Test
    public void putAndGetClassNull() {
        Cache cache = new PgCache("putAndGetClassNull", jdbcTemplate, Duration.ofMinutes(10L));
        cache.put("key", null);
        String cached = cache.get("key", String.class);
        assertThat(cached).isNull();
    }

    @Test
    public void getEmpty() {
        Cache cache = new PgCache("getEmpty", jdbcTemplate, Duration.ofMinutes(10L));
        Cache.ValueWrapper cached = cache.get("key");
        assertThat(cached).isNull();
    }

    @Test
    public void getEmptyClass() {
        Cache cache = new PgCache("getEmptyClass", jdbcTemplate, Duration.ofMinutes(10L));
        String cached = cache.get("key", String.class);
        assertThat(cached).isNull();
    }

    @Test
    public void putAndGetOrder() {
        Order value = Order.newBuilder()
                .setKeyUid(Uid.newBuilder()
                        .setType(UidType.PUID)
                        .setIntValue(123L)
                        .build())
                .setId(1L)
                .build();
        Cache cache = new PgCache("putAndGetOrder", jdbcTemplate, Duration.ofMinutes(10L));
        cache.put("key", value);
        Order cached = cache.get("key", Order.class);
        assertThat(cached).isEqualTo(value);
    }

    @Test
    public void getCallable() {
        Cache cache = new PgCache("getCallable", jdbcTemplate, Duration.ofMinutes(10L));
        String value1 = cache.get("key", () -> "value1");
        assertThat(value1).isEqualTo("value1");
        String value2 = cache.get("key", () -> "value2");
        assertThat(value2).isEqualTo("value1");
    }

    @Test
    public void putIfAbsent() {
        Cache cache = new PgCache("putIfAbsent", jdbcTemplate, Duration.ofMinutes(10L));
        cache.putIfAbsent("key", "value1");
        cache.putIfAbsent("key", "value2");
        String value = cache.get("key", String.class);
        assertThat(value).isEqualTo("value1");
    }

    @Test
    public void testEvict() {
        Cache cache = new PgCache("testEvict", jdbcTemplate, Duration.ofMinutes(10L));
        cache.put("key", "value1");
        assertThat(cache.get("key", String.class)).isNotNull();
        cache.evict("key");
        assertThat(cache.get("key", String.class)).isNull();
    }

    @Test
    public void testClear() {
        Cache cache = new PgCache("testClear", jdbcTemplate, Duration.ofMinutes(10L));
        cache.put("key1", "value1");
        assertThat(cache.get("key1", String.class)).isNotNull();
        cache.put("key2", "value2");
        assertThat(cache.get("key2", String.class)).isNotNull();
        cache.clear();
        assertThat(cache.get("key1", String.class)).isNull();
        assertThat(cache.get("key2", String.class)).isNull();
    }

}