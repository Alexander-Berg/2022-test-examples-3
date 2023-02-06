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
 * Тест показывает пример, когда хотим рассчитать что-то в коде, но при этом иметь возможность сохранять это в БД.
 * Например, чтобы хранить там значение для индексации или для ключа уникальности.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CodeComputedColumnTest extends BaseDbTestClass {

    private GenericMapperRepositoryImpl<ComputedItem, Integer> repository;

    @BeforeAll
    void initTable() {
        jdbcTemplate.execute("drop schema if exists test cascade");
        jdbcTemplate.execute("create schema if not exists test");
        jdbcTemplate.execute("create table if not exists test.test(" +
                "   id serial primary key," +
                "   name text not null," +
                "   key text not null," +
                "   version int" +
                ")");

        repository = new GenericMapperRepositoryImpl<>(LightMapper.forClass(ComputedItem.class), namedJdbcTemplate,
                transactionTemplate, "test.test");
    }

    @Test
    public void testComputedColumnIsStoredToDb() {
        ComputedItem test = repository.insert(new ComputedItem(0, "test", 0));

        assertThat(test.getKey()).isEqualTo("test!!!");
        assertThat(test.version).isEqualTo(1);
        assertThat(test.id).isGreaterThan(0);

        Map<String, Object> dbData = namedJdbcTemplate.queryForMap(
                "select * from test.test where id = :id", singletonMap("id", test.id));
        assertThat(dbData.get("key")).isEqualTo(test.getKey());

        ComputedItem updated = repository.update(new ComputedItem(test.id, "updated test", test.version));
        dbData = namedJdbcTemplate.queryForMap(
                "select * from test.test where id = :id", singletonMap("id", test.id));
        assertThat(dbData.get("key")).isEqualTo(updated.getKey());
        assertThat(updated.version).isGreaterThan(test.version);
    }

    public static class ComputedItem {
        private final int id;
        private final String name;
        private final int version;

        public ComputedItem(@Id @GeneratedValue int id, String name, @Version int version) {
            this.id = id;
            this.name = name;
            this.version = version;
        }

        @Column(read = false)
        public String getKey() {
            // NOTE: Can't used generated values, because they are changed upon insert.
            return name + "!!!";
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
    }
}
