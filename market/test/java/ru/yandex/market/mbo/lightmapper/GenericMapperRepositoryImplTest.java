package ru.yandex.market.mbo.lightmapper;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import ru.yandex.market.mbo.lightmapper.config.BaseDbTestClass;
import ru.yandex.market.mbo.lightmapper.data.DataItem;
import ru.yandex.market.mbo.lightmapper.data.DataItemRepository;
import ru.yandex.market.mbo.lightmapper.exceptions.SqlConcurrentModificationException;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@SuppressWarnings("checkstyle:magicnumber")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GenericMapperRepositoryImplTest extends BaseDbTestClass {
    private DataItemRepository itemRepository;
    private SimpleMeterRegistry registry;

    @BeforeAll
    void initTable() {
        jdbcTemplate.execute("create schema if not exists test");
        jdbcTemplate.execute("create table if not exists " + DataItemRepository.TABLE_NAME + "(" +
                "   id serial primary key ," +
                "   name text not null," +
                "   version timestamptz not null default now()" +
                ")");

        itemRepository = new DataItemRepository(namedJdbcTemplate, transactionTemplate);
    }

    @BeforeEach
    void setupMeter() {
        registry = new SimpleMeterRegistry();
        Metrics.globalRegistry.add(registry);
        Metrics.globalRegistry.add(new PrometheusMeterRegistry(PrometheusConfig.DEFAULT));
    }

    @AfterEach
    void tearDownMeter() {
        Metrics.globalRegistry.remove(registry);
        DataItemRepository.setMetricsContext(null);
    }

    @Test
    void insert() {
        DataItem item = new DataItem("test");
        itemRepository.insert(item);

        assertThat(item.getId()).isGreaterThan(0);
        assertThat(item.getVersion()).isNotNull();

        assertThat(itemRepository.findAll())
                .isNotEmpty()
                .extracting("id")
                .containsExactly(item.getId());

        assertCounter("repository.statements.inserts", 1.0);
        assertCounter("repository.rows.inserts", 1.0);
        assertCounter("repository.statements.selects", 1.0);
        assertCounter("repository.rows.selects", 1.0);
    }

    @Test
    void insertWithContext() {
        DataItemRepository.setMetricsContext("context");
        DataItem item = new DataItem("test");
        itemRepository.insert(item);

        assertThat(item.getId()).isGreaterThan(0);
        assertThat(item.getVersion()).isNotNull();

        assertThat(itemRepository.findAll())
                .isNotEmpty()
                .extracting("id")
                .containsExactly(item.getId());

        assertCounter("repository.statements.inserts", 1.0);
        assertCounter("repository.rows.inserts", 1.0);
        assertCounter("repository.statements.selects", 1.0);
        assertCounter("repository.rows.selects", 1.0);
        assertCounterContext("context", "repository.statements.inserts", 1.0);
        assertCounterContext("context", "repository.rows.inserts", 1.0);
        assertCounterContext("context", "repository.statements.selects", 1.0);
        assertCounterContext("context", "repository.rows.selects", 1.0);
    }

    @Test
    void insertBatch() {
        List<DataItem> items = Arrays.asList(
                new DataItem("test1"),
                new DataItem("test2")
        );
        itemRepository.insertBatch(items);

        assertThat(items).extracting(DataItem::getId).allMatch(id -> id > 0);

        assertThat(itemRepository.findAll())
                .hasSize(2)
                .extracting("id")
                .containsExactlyInAnyOrderElementsOf(items.stream().map(DataItem::getId).collect(toList()));

        assertCounter("repository.statements.inserts", 1.0);
        assertCounter("repository.rows.inserts", 2.0);
        assertCounter("repository.statements.selects", 1.0);
        assertCounter("repository.rows.selects", 2.0);
    }

    @Test
    void testUpdate() throws InterruptedException {
        DataItem item = new DataItem("test");
        itemRepository.insert(item);
        Instant currentVersion = item.getVersion();

        Thread.sleep(100); // So that version is updated for sure
        item.setName("updated");

        itemRepository.update(item);
        Instant updatedVersion = item.getVersion();
        assertThat(updatedVersion).isAfter(currentVersion);

        DataItem updated = itemRepository.findById(item.getId());
        assertThat(updated.getName()).isEqualTo("updated");
        assertThat(updated.getVersion()).isEqualTo(updatedVersion);
    }

    @Test
    void testUpdateBatch() {
        DataItem item1 = new DataItem("test1");
        DataItem item2 = new DataItem("test2");
        itemRepository.insertBatch(item1, item2);

        item1.setName("test1-updated");
        item2.setName("test2-updated");
        itemRepository.updateBatch(item1, item2);

        List<DataItem> items = itemRepository.findAll();
        assertThat(items)
                .hasSize(2)
                .extracting(DataItem::getName)
                .containsExactlyInAnyOrder("test1-updated", "test2-updated");

        assertCounter("repository.statements.inserts", 1.0);
        assertCounter("repository.rows.inserts", 2.0);
        assertCounter("repository.statements.updates", 1.0);
        assertCounter("repository.rows.updates", 2.0);
        assertCounter("repository.statements.selects", 1.0);
        assertCounter("repository.rows.selects", 2.0);
        assertCounter("repository.concurrent-modifications", 0.0);
    }

    @Test
    public void testGetValues() throws SQLException, InterruptedException {
        GenericMapper<DataItem> mapper = itemRepository.getGenericMapper();
        DataItem item = new DataItem("name");

        Map<String, Object> insertValues = mapper.getInsert().getValues(item);
        assertThat(insertValues).contains(entry("name", "name"));
        assertThat(insertValues).doesNotContainKey("id"); // Insert with generated key

        itemRepository.insert(item);

        Instant version = item.getVersion();
        Thread.sleep(10);

        Map<String, Object> updateValues = mapper.getUpdate().getValues(item);
        assertThat(updateValues).contains(
                entry("id", item.getId()),
                entry("name", "name"),
                entry("old_version", Timestamp.from(version)) // @Version fields
        );

        assertThat((Timestamp) updateValues.get("version")).isAfter(Timestamp.from(version));
    }

    private void assertCounter(String name, double count) {
        assertThat(registry.get(name).tags("repository", "test.data_item").counter().count()).isEqualTo(count);
    }

    private void assertCounterContext(String context, String name, double count) {
        assertThat(registry.get(name).tags("repository", "test.data_item", "context", context).counter().count())
                .isEqualTo(count);
    }

    @Test
    void testConcurrentUpdates() {
        DataItem item = new DataItem("test");
        itemRepository.insert(item);

        // Make it outdated
        item.setVersion(item.getVersion().minus(1, ChronoUnit.MINUTES));
        item.setName("updated");

        Assertions.assertThatThrownBy(() -> itemRepository.update(item))
                .isInstanceOf(SqlConcurrentModificationException.class)
                .hasMessageContaining("record might be missing or version mistmatch");

        assertCounter("repository.concurrent-modifications", 1.0);

        DataItem fresh = itemRepository.findById(item.getId());
        fresh.setName("updated");
        itemRepository.update(fresh);

        DataItem updated = itemRepository.findById(item.getId());
        assertThat(updated.getName()).isEqualTo("updated");
    }
}
