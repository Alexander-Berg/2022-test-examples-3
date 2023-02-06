package ru.yandex.market.mbo.lightmapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import ru.yandex.market.mbo.lightmapper.config.BaseDbTestClass;
import ru.yandex.market.mbo.lightmapper.data.LightDataItem;
import ru.yandex.market.mbo.lightmapper.data.SimpleLightDataItem;
import ru.yandex.market.mbo.lightmapper.exceptions.SqlConcurrentModificationException;
import ru.yandex.market.mbo.lightmapper.reflective.LightMapper;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JdbcMapperHelperTest extends BaseDbTestClass {
    private static final String TABLE_NAME = "test.data_item";

    private GenericMapper<LightDataItem> mapper;
    private GenericMapper<SimpleLightDataItem> mapperWithoutGeneratedIdAndVersion;
    private JdbcMapperHelper jdbc;

    @BeforeAll
    void initTable() {
        jdbcTemplate.execute("create schema if not exists test");
        jdbcTemplate.execute("create table if not exists " + TABLE_NAME + "(" +
                "   id serial primary key ," +
                "   name text not null," +
                "   version timestamptz not null default now()" +
                ")");

        mapper = LightMapper.forClass(LightDataItem.class, TABLE_NAME);
        mapperWithoutGeneratedIdAndVersion = LightMapper.forClass(SimpleLightDataItem.class, TABLE_NAME);
        jdbc = new JdbcMapperHelper(namedJdbcTemplate, transactionTemplate);
    }

    @Test
    void insert() {
        LightDataItem item = new LightDataItem("test");
        LightDataItem saved = jdbc.insert(mapper, item);

        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getVersion()).isNotNull();

        assertThat(jdbc.find(mapper))
                .isNotEmpty()
                .extracting("id")
                .containsExactly(saved.getId());
    }

    @Test
    void insertBatch() {
        List<LightDataItem> items = Arrays.asList(
                new LightDataItem("test1"),
                new LightDataItem("test2")
        );
        List<LightDataItem> saved = jdbc.insert(mapper, items);

        assertThat(saved).extracting(LightDataItem::getId).allMatch(id -> id > 0);

        assertThat(jdbc.find(mapper))
                .hasSize(2)
                .extracting("id")
                .containsExactlyInAnyOrderElementsOf(saved.stream().map(LightDataItem::getId).collect(toList()));
    }

    @Test
    void testUpdate() throws InterruptedException {
        LightDataItem item = new LightDataItem("test");
        LightDataItem saved = jdbc.insert(mapper, item);
        Instant currentVersion = saved.getVersion();

        Thread.sleep(100); // So that version is updated for sure

        saved = jdbc.update(mapper, saved.withName("updated"));
        Instant updatedVersion = saved.getVersion();
        assertThat(updatedVersion).isAfter(currentVersion);

        // TODO: findById
        LightDataItem updated = jdbc.findOne(mapper, "where id = :id", ImmutableMap.of("id", saved.getId()));
        assertThat(updated.getName()).isEqualTo("updated");
        assertThat(updated.getVersion()).isEqualTo(updatedVersion);
    }

    @Test
    void testUpdateBatch() {
        LightDataItem item1 = new LightDataItem("test1");
        LightDataItem item2 = new LightDataItem("test2");
        List<LightDataItem> updated = jdbc.insert(mapper, Arrays.asList(item1, item2));

        List<LightDataItem> changed = Arrays.asList(
                updated.get(0).withName("test1-updated"),
                updated.get(1).withName("test2-updated")
        );
        jdbc.update(mapper, changed);

        List<LightDataItem> items = jdbc.find(mapper);
        assertThat(items)
                .hasSize(2)
                .extracting(LightDataItem::getName)
                .containsExactlyInAnyOrder("test1-updated", "test2-updated");
    }

    @Test
    void testConcurrentUpdates() {
        LightDataItem item = new LightDataItem("test");
        LightDataItem saved = jdbc.insert(mapper, item);

        // Make it outdated
        LightDataItem changed = saved
                .withVersion(saved.getVersion().minus(1, ChronoUnit.MINUTES))
                .withName("updated");

        Assertions.assertThatThrownBy(() -> jdbc.update(mapper, changed))
                .isInstanceOf(SqlConcurrentModificationException.class)
                .hasMessageContaining("record might be missing or version mistmatch");

        LightDataItem fresh = jdbc.findOneBy(mapper, ImmutableMap.of("id", saved.getId()));
        jdbc.update(mapper, fresh.withName("updated"));

        LightDataItem latest = jdbc.findOneBy(mapper, ImmutableMap.of("id", saved.getId()));
        assertThat(latest.getName()).isEqualTo("updated");
    }

    @Test
    public void testInsertOrIgnoreAll() {
        int id = 100;

        SimpleLightDataItem initialItem = new SimpleLightDataItem(id, "initial");
        jdbc.insertOrIgnoreAll(mapperWithoutGeneratedIdAndVersion, null, Collections.singleton(initialItem));
        SimpleLightDataItem saved = jdbc.findOneBy(mapperWithoutGeneratedIdAndVersion, ImmutableMap.of("id", id));
        assertThat(saved).isEqualTo(initialItem);

        SimpleLightDataItem updatedItem = new SimpleLightDataItem(id, "updated");
        jdbc.insertOrIgnoreAll(mapperWithoutGeneratedIdAndVersion, null, Collections.singleton(updatedItem));
        SimpleLightDataItem afterUpdate =
                jdbc.findOneBy(mapperWithoutGeneratedIdAndVersion, ImmutableMap.of("id", id));
        assertThat(afterUpdate).isEqualTo(initialItem);
        assertThat(afterUpdate).isNotEqualTo(updatedItem);

        List<SimpleLightDataItem> all = jdbc.find(mapperWithoutGeneratedIdAndVersion);
        assertThat(all).containsExactlyInAnyOrder(initialItem);
        assertThat(all).doesNotContain(updatedItem);
    }

    @Test
    public void testInsertOrIgnoreAllPartlyUpdate() {
        int id = 100;
        int otherId = 200;

        SimpleLightDataItem existingItem = new SimpleLightDataItem(id, "existing");
        jdbc.insertOrIgnoreAll(mapperWithoutGeneratedIdAndVersion, null, Collections.singleton(existingItem));

        SimpleLightDataItem updateForExisting = new SimpleLightDataItem(id, "update_for_existing");
        SimpleLightDataItem newOtherItem = new SimpleLightDataItem(otherId, "new_other");
        jdbc.insertOrIgnoreAll(mapperWithoutGeneratedIdAndVersion, null,
                ImmutableList.of(updateForExisting, newOtherItem));

        List<SimpleLightDataItem> all = jdbc.find(mapperWithoutGeneratedIdAndVersion);
        assertThat(all).containsExactlyInAnyOrder(existingItem, newOtherItem);
        assertThat(all).doesNotContain(updateForExisting);
    }

    @Test
    public void testInsertOrWithAutogeneratedId() {
        // It's not contract, just observation.
        // InsertOr... methods doesn't work with autogenerated id as expected

        LightDataItem item = new LightDataItem("test");
        LightDataItem saved = jdbc.insert(mapper, item); // saving and getting id
        assertThat(saved.getId()).isNotZero();

        // should rewrite to same
        jdbc.insertOrUpdate(mapper, Collections.singletonList(saved));

        // data not changed, should do nothing
        jdbc.insertOrUpdateAllIfDifferent(mapper, Collections.singletonList(saved));

        // inserting already existing id, should do nothing
        jdbc.insertOrIgnoreAll(mapper, Collections.singletonList(saved));

        // But in reality every InsertOr call creates new record with new id
        List<SimpleLightDataItem> all = jdbc.find(mapperWithoutGeneratedIdAndVersion);
        assertThat(all).hasSize(4);
        assertThat(all).map(SimpleLightDataItem::getName).containsOnly("test");
        Set<Integer> uniqueIds = all.stream()
                .map(SimpleLightDataItem::getId)
                .collect(Collectors.toSet());
        assertThat(uniqueIds).hasSize(4);
        assertThat(uniqueIds).allMatch(id -> id > 0);
    }
}
