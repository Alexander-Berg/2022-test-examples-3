package ru.yandex.market.mbo.lightmapper.reflective;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import ru.yandex.market.mbo.lightmapper.GenericMapperRepositoryImpl;
import ru.yandex.market.mbo.lightmapper.config.BaseDbTestClass;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.Column;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.GeneratedValue;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.Id;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.Version;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Пример обратной ситуации, когда значение нужно получать из БД
 * (оно там как-то вычисляется/обновляется, триггером или иначе).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DbComputedColumnTest extends BaseDbTestClass {

    private GenericMapperRepositoryImpl<DbComputedItem, Integer> repository;

    @BeforeAll
    void initTable() {
        jdbcTemplate.execute("drop schema if exists test cascade");
        jdbcTemplate.execute("create schema if not exists test");
        jdbcTemplate.execute("create table if not exists test.test(" +
                "   id serial primary key," +
                "   name text not null," +
                "   key text generated always as ( name || '!!!' ) stored," +
                "   version int" +
                ")");

        repository = new GenericMapperRepositoryImpl<>(LightMapper.forClass(DbComputedItem.class), namedJdbcTemplate,
                transactionTemplate, "test.test");
    }

    @Test
    public void testComputedColumnIsStoredToDb() {
        DbComputedItem test = repository.insert(new DbComputedItem(0, "test"));

        assertThat(test.getKey()).isEqualTo("test!!!");
        assertThat(test.version).isEqualTo(1);
        assertThat(test.id).isGreaterThan(0);

        Map<String, Object> dbData = namedJdbcTemplate.queryForMap(
                "select * from test.test where id = :id", singletonMap("id", test.id));
        assertThat(dbData.get("key")).isEqualTo(test.getKey());

        DbComputedItem updated = repository.update(
                new DbComputedItem(test.id, "updated test", test.version, test.getKey()));
        assertThat(updated.getKey()).isEqualTo("updated test!!!");
        dbData = namedJdbcTemplate.queryForMap(
                "select * from test.test where id = :id", singletonMap("id", test.id));
        assertThat(dbData.get("key")).isEqualTo(updated.getKey());
        assertThat(updated.version).isGreaterThan(test.version);
    }

    public static class DbComputedItem {
        private final int id;
        private final String name;
        private final int version;
        private final String key;

        public DbComputedItem(@Id @GeneratedValue int id, String name) {
            this(id, name, 0, null);
        }

        public DbComputedItem(@Id @GeneratedValue int id, String name, @Version int version,
                              @GeneratedValue(update = true) @Column(write = false) String key) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.key = key;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getVersion() {
            return version;
        }

        public String getKey() {
            return key;
        }
    }
}
