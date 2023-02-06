package ru.yandex.market.antifraud.orders.cache;

import java.time.Duration;

import org.junit.Test;
import org.springframework.cache.Cache;

import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Order;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
public class SpyMemCacheTest {


    private final Cache cache = SpyMemCache.builder()
            .cacheName("test_cache")
            .expirationPeriod(Duration.ofHours(1))
            .memcachedClient(new MemcachedClientStub())
            .build();

    @Test
    public void get(){
        String key = "test_get_key";
        String value = "test_get_value";

        cache.put(key, value);
        assertThat(cache.get(key, String.class)).isEqualTo(value);
    }

    @Test
    public void getNull(){
        String key = "test_getNull_key";

        assertThat(cache.get(key)).isNull();
        cache.put(key, null);
        assertThat(cache.get(key)).isNotNull();
        assertThat(cache.get(key).get()).isNull();
    }


    @Test
    public void getNullClass(){
        String key = "test_getNullClass_key";

        assertThat(cache.get(key, String.class)).isNull();
        cache.put(key, null);
        assertThat(cache.get(key, String.class)).isNull();
    }

    @Test
    public void getOrder(){
        String key = "test_getOrder_key";
        Order value = Order.newBuilder()
                .setKeyUid(Uid.newBuilder()
                        .setType(UidType.PUID)
                        .setIntValue(123L)
                        .build())
                .setId(1L)
                .build();
        cache.put(key, value);
        Order cached = cache.get(key, Order.class);
        assertThat(cached).isEqualTo(value);
    }

    @Test
    public void getCallable(){
        String key = "test_getCallable_key";
        cache.get(key, () -> "value1");
        assertThat(cache.get(key, String.class)).isEqualTo("value1");
        cache.get(key, () -> "value2");
        assertThat(cache.get(key, String.class)).isEqualTo("value1");
    }


    @Test
    public void putIfAbsent(){
        String key = "test_putIfAbsent_key";
        String value1 = "test_putIfAbsent_value_1";
        String value2 = "test_putIfAbsent_value_2";

        cache.putIfAbsent(key, value1);
        assertThat(cache.get(key, String.class)).isEqualTo(value1);

        cache.putIfAbsent(key, value2);
        assertThat(cache.get(key, String.class)).isEqualTo(value1);
    }


    @Test
    public void evict(){
        String key = "test_evict_key";
        String value = "test_evict_value";

        cache.putIfAbsent(key, value);
        assertThat(cache.get(key, String.class)).isEqualTo(value);

        cache.evict(key);
        assertThat(cache.get(key, String.class)).isNull();
    }


    @Test
    public void clear(){
        String key1 = "test_clear_key1";
        String value1 = "test_clear_value1";
        String key2 = "test_clear_key2";
        String value2 = "test_clear_value2";

        cache.put(key1, value1);
        cache.put(key2, value2);
        assertThat(cache.get(key1, String.class)).isEqualTo(value1);
        assertThat(cache.get(key2, String.class)).isEqualTo(value2);

        cache.clear();
        assertThat(cache.get(key1, String.class)).isNull();
        assertThat(cache.get(key2, String.class)).isNull();
    }

}