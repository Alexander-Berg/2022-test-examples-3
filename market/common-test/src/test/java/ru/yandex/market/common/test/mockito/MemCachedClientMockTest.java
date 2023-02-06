package ru.yandex.market.common.test.mockito;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MemCachedClientMockTest {
    static final Date E = null; // expiry
    ConcurrentMap<String, Object> cache = new ConcurrentHashMap<>();
    MemCachedClientMock mock = new MemCachedClientMock(cache);

    @Before
    public void setUp() {
        cache.put("k1", 1);
        cache.put("k2", 2);
    }

    @Test
    public void get() {
        // when-then
        assertThat(mock.get("k1")).isEqualTo(1);
        assertThat(mock.get("k2")).isEqualTo(2);
        assertThat(mock.get("k3")).isNull();
    }

    @Test
    public void set() {
        // when
        mock.set("k1", 11, E); // обновление
        mock.set("k3", 3, E); // добавление

        // then
        assertThat(cache)
                .containsEntry("k1", 11)
                .containsEntry("k2", 2)
                .containsEntry("k3", 3);
    }

    @Test
    public void delete() {
        // when
        mock.delete("k2");

        // when-then
        assertThat(cache)
                .containsEntry("k1", 1)
                .doesNotContainKey("k2");
    }

    @Test
    public void add() {
        // when
        mock.add("k1", 11, E); // попытка обновления
        mock.add("k3", 3, E); // попытка добавления

        // when
        assertThat(cache)
                .as("должно сетить только если ключа нет")
                .containsEntry("k1", 1)
                .containsEntry("k3", 3);
    }

    @Test
    public void incr() {
        // given
        cache.put("k3", 3L); // long
        cache.put("k4", "4"); // не число

        // when
        Object k1 = mock.incr("k1", 10);
        Object k3 = mock.incr("k3", 10);
        Object k4 = mock.incr("k4", 10);

        // then
        assertThat(k1).as("не работет для int").isEqualTo(-1L);
        assertThat(k3).as("работает только для long").isEqualTo(13L);
        assertThat(k4).as("не работает для строк").isEqualTo(-1L);
        assertThat(cache)
                .containsEntry("k1", 1)
                .containsEntry("k3", 13L)
                .containsEntry("k4", "4");
    }
}
