package ru.yandex.market.antifraud.orders.cache;

import java.time.Duration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.test.annotations.DaoLayerTest;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author dzvyagin
 */
@DaoLayerTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PgCacheManagerTest {

    @Autowired
    private NamedParameterJdbcOperations jdbcTemplate;

    @Test
    public void clearOldEntries() throws Exception{
        PgCacheManager cacheManager = new PgCacheManager(jdbcTemplate, Duration.ofSeconds(2));
        Cache cache = cacheManager.getCache("clearOldEntries");
        cache.put("key", "value");
        assertThat(cache.get("key", String.class)).isNotNull();
        Thread.sleep(3_000);
        cacheManager.clearOldEntries();
        assertThat(cache.get("key", String.class)).isNull();
    }

}