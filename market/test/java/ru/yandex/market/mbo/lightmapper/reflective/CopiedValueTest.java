package ru.yandex.market.mbo.lightmapper.reflective;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import ru.yandex.market.mbo.lightmapper.GenericMapperRepositoryImpl;
import ru.yandex.market.mbo.lightmapper.config.BaseDbTestClass;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.Column;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.GeneratedValue;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.Id;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.Version;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Пример про то, как сделать значение, которое не сохраняется в БД, но при этом копируется при обновлении записи.
 * При выборке из БД оно будет пустым (можно использовать свой DefaultProvider).
 *
 * use-case: используем Котлин, immutable объекты и nullable коллекцию для хранения связей
 * коллекция сохраняется/обновляется отдельно, задача просто сохранять её при вставке значений.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CopiedValueTest extends BaseDbTestClass {

    private GenericMapperRepositoryImpl<CopiedItem, Integer> repository;

    @BeforeAll
    void initTable() {
        jdbcTemplate.execute("drop schema if exists test cascade");
        jdbcTemplate.execute("create schema if not exists test");
        jdbcTemplate.execute("create table if not exists test.test(" +
                "   id serial primary key," +
                "   name text not null," +
                "   version int" +
                ")");

        repository = new GenericMapperRepositoryImpl<>(LightMapper.forClass(CopiedItem.class), namedJdbcTemplate,
                transactionTemplate, "test.test");
    }

    @Test
    public void testComputedColumnIsStoredToDb() {
        CopiedItem test = repository.insert(new CopiedItem(0, "test", 0, singletonList(42)));

        assertThat(test.version).isEqualTo(1);
        assertThat(test.id).isGreaterThan(0);
        assertThat(test.someRelatedData).isEqualTo(singletonList(42)); // copied

        CopiedItem updated = repository
                .update(new CopiedItem(test.id, "updated test", test.version, singletonList(84)));
        assertThat(updated.version).isGreaterThan(test.version);
        assertThat(updated.someRelatedData).isEqualTo(singletonList(84));

        CopiedItem selected = repository.findById(test.id);
        assertThat(selected.someRelatedData).isNull(); // No data if selected
    }

    public static class CopiedItem {
        private final int id;
        private final String name;
        private final int version;
        private final List<Integer> someRelatedData;

        public CopiedItem(@Id @GeneratedValue int id, String name, @Version int version,
                          @Column(read = false, write = false) List<Integer> someRelatedData) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.someRelatedData = someRelatedData;
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

        public List<Integer> getSomeRelatedData() {
            return someRelatedData;
        }
    }
}
