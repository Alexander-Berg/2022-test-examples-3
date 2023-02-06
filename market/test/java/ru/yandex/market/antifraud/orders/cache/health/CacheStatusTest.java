package ru.yandex.market.antifraud.orders.cache.health;

import java.time.Instant;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author dzvyagin
 */
public class CacheStatusTest {

    @Test
    public void serializationTest() {
        CacheStatus s = CacheStatus.builder()
                .host("host")
                .state(CacheState.FLAPPING)
                .timestamp(Instant.now())
                .putLatencyNs(123L)
                .getLatencyNs(22L)
                .lastUpdate(Instant.now().minusSeconds(2))
                .build();
        String json = AntifraudJsonUtil.toJson(s);
        assertThat(s).isEqualTo(AntifraudJsonUtil.fromJson(json, CacheStatus.class));
    }

}
