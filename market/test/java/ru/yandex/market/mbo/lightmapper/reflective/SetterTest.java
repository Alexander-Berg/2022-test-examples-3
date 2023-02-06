package ru.yandex.market.mbo.lightmapper.reflective;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import ru.yandex.market.mbo.lightmapper.GenericMapperRepositoryImpl;
import ru.yandex.market.mbo.lightmapper.config.BaseDbTestClass;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.Column;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.GeneratedValue;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.Id;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.Version;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Пример, как использовать с частично-mutable классами. Важно разметить методы через @Column.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SetterTest extends BaseDbTestClass {

    private GenericMapperRepositoryImpl<CopiedItem, Integer> repository;

    @BeforeAll
    void initTable() {
        jdbcTemplate.execute("drop schema if exists test cascade");
        jdbcTemplate.execute("create schema if not exists test");
        jdbcTemplate.execute("create table if not exists test.test(" +
                "   id serial primary key," +
                "   name text not null," +
                "   version int," +
                "   data text" +
                ")");

        repository = new GenericMapperRepositoryImpl<>(LightMapper.forClass(CopiedItem.class), namedJdbcTemplate,
                transactionTemplate, "test.test");
    }

    @Test
    public void testComputedColumnIsStoredToDb() {
        CopiedItem test = repository.insert(new CopiedItem(0, "test"));

        assertThat(test.version).isEqualTo(1);
        assertThat(test.id).isGreaterThan(0);

        test.data = "updated";

        CopiedItem updated = repository.update(test);
        assertThat(updated.version).isGreaterThan(test.version);
        assertThat(updated.data).isEqualTo("updated");

        assertThat(updated).isNotSameAs(test); // NOTE: Object is still copied.

        CopiedItem selected = repository.findById(test.id);
        assertThat(selected.data).isEqualTo("updated"); // No data if selected
    }

    public static class CopiedItem {
        private final int id;
        private final String name;
        @Version
        private int version;
        private String data;

        public CopiedItem(@Id @GeneratedValue int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Column
        public int getVersion() {
            return version;
        }

        public CopiedItem setVersion(int version) {
            this.version = version;
            return this;
        }

        @Column
        public String getData() {
            return data;
        }

        public CopiedItem setData(String data) {
            this.data = data;
            return this;
        }
    }
}
