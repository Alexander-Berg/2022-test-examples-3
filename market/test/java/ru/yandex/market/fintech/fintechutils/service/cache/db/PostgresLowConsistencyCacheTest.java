package ru.yandex.market.fintech.fintechutils.service.cache.db;

import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.fintech.fintechutils.AbstractFunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PostgresLowConsistencyCacheTest extends AbstractFunctionalTest {

    private static final String TABLE_NAME = "fintech_utils.test_cache_table";

    private PostgresLowConsistencyCache<Long, CachedEntity> cache;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cache = new TestCache(jdbcTemplate);
        jdbcTemplate.getJdbcTemplate().execute("" +
                "create table if not exists " + TABLE_NAME + "(" +
                "key bigint primary key," +
                "value1 text," +
                "value2 double precision" +
                ")");
        jdbcTemplate.getJdbcOperations().execute("insert into " + TABLE_NAME + " values (0, 'abc', 3.14)");
    }

    @Test
    void testCache() {
        assertNull(cache.get(0L));
        cache.refreshCache();

        CachedEntity cached = cache.get(0L);
        assertNotNull(cached);
        assertEquals(0L, cached.getCacheKey());
        assertEquals("abc", cached.getCacheField1());
        assertEquals(3.14, cached.getCacheFiled2());

        jdbcTemplate.getJdbcOperations().execute("insert into " + TABLE_NAME + " values (3, 'lmn', '5.5')");

        assertNull(cache.get(3L));
        cache.refreshCache();

        cached = cache.get(3L);
        assertNotNull(cached);
        assertEquals(3L, cached.getCacheKey());
        assertEquals("lmn", cached.getCacheField1());
        assertEquals(5.5, cached.getCacheFiled2());
    }


    private static class TestCache extends PostgresLowConsistencyCache<Long, CachedEntity> {
        protected TestCache(NamedParameterJdbcTemplate jdbcTemplate) {
            super(
                    TABLE_NAME,
                    jdbcTemplate,
                    (rs, i) -> new CachedEntity(rs.getLong("key"), rs.getString("value1"), rs.getDouble("value2")),
                    CachedEntity::getCacheKey
            );
        }
    }

    @Value
    private static class CachedEntity {
        private long cacheKey;
        private String cacheField1;
        private double cacheFiled2;

    }

}
