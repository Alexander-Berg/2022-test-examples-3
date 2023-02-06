package ru.yandex.market.antifraud.orders.storage.entity;

import java.time.Instant;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
public class CacheEntityTest {

    @Test
    public void testSerialization(){
        CacheEntity cacheEntity = CacheEntity.builder()
                .id(1L)
                .cacheName("cache")
                .className("class")
                .entryKey("key")
                .entryValue("value".getBytes())
                .expiresAt(Instant.now())
                .build();
        String json = AntifraudJsonUtil.toJson(cacheEntity);
        assertThat(AntifraudJsonUtil.fromJson(json, CacheEntity.class)).isEqualTo(cacheEntity);
    }

}