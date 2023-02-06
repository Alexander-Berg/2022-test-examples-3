package ru.yandex.market.mbo.lightmapper.reflective;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.lightmapper.GenericMapperRepositoryImpl;
import ru.yandex.market.mbo.lightmapper.config.BaseDbTestClass;
import ru.yandex.market.mbo.lightmapper.exceptions.ItemNotFoundException;
import ru.yandex.market.mbo.lightmapper.exceptions.SqlConcurrentModificationException;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.Embedded;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.GeneratedValue;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.Id;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.Jsonb;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.PgEnum;
import ru.yandex.market.mbo.lightmapper.reflective.annotations.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LightMapperTest extends BaseDbTestClass {

    private ItemRepository itemRepository;

    @BeforeAll
    void initTable() {
        jdbcTemplate.execute("drop schema if exists test cascade");
        jdbcTemplate.execute("create schema if not exists test");
        jdbcTemplate.execute("create type test.status as enum ('OK', 'NOT_OK')");
        jdbcTemplate.execute("create table if not exists " + ItemRepository.TABLE_NAME + "(" +
                "   id serial primary key," +
                "   long_name text," +
                "   strings jsonb," +
                "   mapping_id bigint," +
                "   mapping_changed timestamptz," +
                "   mapping_status test.status," +
                "   created_at timestamptz not null," +
                "   updated_at timestamptz not null," +
                "   version int not null " +
                ")");


        itemRepository = new ItemRepository(namedJdbcTemplate, transactionTemplate);
    }

    @Test
    public void testInsertRead() {
        Instant now = Instant.now();
        Item instance = new Item(0, "some name", Arrays.asList("Hello", "world!"),
                new Mapping(123L, now, MappingStatus.OK),
                new AuditInfo(now, now), 0);
        Item inserted = itemRepository.insert(instance);

        assertThat(itemRepository.totalCount()).isEqualTo(1);
        assertThat(inserted.getId()).isGreaterThan(0);

        Map<String, Object> data = namedJdbcTemplate.queryForMap(
                "select * from " + ItemRepository.TABLE_NAME, Collections.emptyMap());

        Timestamp nowTs = Timestamp.from(now.truncatedTo(ChronoUnit.MICROS));
        assertThat(data).contains(
                entry("id", inserted.getId()),
                entry("long_name", "some name"),
                entry("mapping_id", 123L),
                entry("mapping_changed", nowTs),
                entry("created_at", nowTs),
                entry("updated_at", nowTs),
                entry("version", 1)
        );

        assertThat(data.get("strings").toString()).isEqualTo("[\"Hello\", \"world!\"]");
    }

    @Test
    public void testGetValues() throws SQLException {
        LightMapper<Item> mapper = LightMapper.forClass(Item.class);
        long testTime = 1642929183000L;
        Instant time = Instant.ofEpochMilli(testTime);
        Item item = new Item(42, "name",
                Arrays.asList("a", "b"),
                new Mapping(100, time, MappingStatus.OK),
                new AuditInfo(time, time),
                1);

        Map<String, Object> insertValues = mapper.getInsert().getValues(item);
        PGobject strings = new PGobject();
        strings.setType("json");
        strings.setValue("[\"a\",\"b\"]");
        assertThat(insertValues).contains(
                entry("long_name", "name"),
                entry("created_at", new Timestamp(testTime)),
                entry("mapping_id", 100L),
                entry("strings", strings) // complex types are converted
        );

        assertThat(insertValues).doesNotContainKey("id"); // Insert with generated key

        Map<String, Object> updateValues = mapper.getUpdate().getValues(item);
        assertThat(updateValues).contains(
                entry("id", 42),
                entry("long_name", "name"),
                entry("created_at", new Timestamp(testTime)),
                entry("mapping_id", 100L),
                entry("strings", strings), // complex types are converted
                entry("version", 2),
                entry("old_version", 1) // @Version fields
        );
    }

    @Test
    public void testFindById() {
        Item inserted = insertItem();
        Item selected = itemRepository.findById(inserted.getId());
        assertThat(selected).isEqualToComparingFieldByFieldRecursively(inserted);
    }

    @Test
    public void testUpdate() {
        Item inserted = insertItem();

        Item changed = inserted.withLongName("new name").withStrings(Arrays.asList("new", "world!"));
        Item updated = itemRepository.update(changed);

        Map<String, Object> data = namedJdbcTemplate.queryForMap(
                "select * from " + ItemRepository.TABLE_NAME + " where id = :id",
                Collections.singletonMap("id", changed.getId()));

        assertThat(updated).isEqualToIgnoringGivenFields(changed, "version");
        assertThat(updated.getVersion()).isGreaterThan(changed.getVersion());
        assertThat(data.get("long_name")).isEqualTo("new name");
        assertThat(data.get("strings").toString()).isEqualTo("[\"new\", \"world!\"]");
    }

    @Test
    public void testNullEmbeddedLoadSave() {
        Instant now = Instant.now();
        Item instance = new Item(0, "some name", Arrays.asList("Hello", "world!"), null,
                new AuditInfo(now, now), 0);
        Item inserted = itemRepository.insert(instance);
        Item loaded = itemRepository.findById(inserted.getId());
        assertThat(loaded.getMapping()).isNull();
        assertThat(loaded).isEqualToComparingFieldByFieldRecursively(inserted);
    }

    @Test
    public void testOptimisticLocking() {
        Item inserted = insertItem();
        Item changed = inserted.withLongName("updated!");
        Item updated = itemRepository.update(changed);

        Item changedTwice = changed.withLongName("updated twice!");
        Assertions.assertThatThrownBy(() -> itemRepository.update(changedTwice))
                .isInstanceOf(SqlConcurrentModificationException.class);
    }

    @Test
    public void testCompositeId() {

    }

    private Item insertItem() {
        return insertItem(123L);
    }

    private Item insertItem(long mappingId) {
        Instant now = Instant.now();
        Item instance = new Item(0, "some name", Arrays.asList("Hello", "world!"),
                new Mapping(mappingId, now, MappingStatus.OK),
                new AuditInfo(now, now), 0);
        return itemRepository.insert(instance);
    }

    @Test
    public void testDelete() {
        Item item1 = insertItem(1);
        Item item2 = insertItem(2);
        assertThat(itemRepository.totalCount()).isEqualTo(2);

        itemRepository.delete(item1);
        assertThat(itemRepository.totalCount()).isEqualTo(1);
        assertThat(itemRepository.findById(item2.getId())).isEqualToComparingFieldByFieldRecursively(item2);
        Assertions.assertThatThrownBy(() -> itemRepository.findById(item1.getId()))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test
    public void testDeleteOptimisticLocking() {
        Item item1 = insertItem(1);
        assertThat(itemRepository.totalCount()).isEqualTo(1);

        Item changed = item1.withLongName("changed");
        itemRepository.update(changed);

        Assertions.assertThatThrownBy(() -> itemRepository.delete(item1))
                .isInstanceOf(SqlConcurrentModificationException.class);
    }

    public static class ItemRepository extends GenericMapperRepositoryImpl<Item, Integer> {
        public static final String TABLE_NAME = "test.immutable_test";

        public ItemRepository(NamedParameterJdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
            super(LightMapper.forClass(Item.class), jdbcTemplate, transactionTemplate, TABLE_NAME);
        }
    }

    public static class Item {
        @Id
        @GeneratedValue
        private final int id;

        // Бывают и просто поля с данными :)
        private final String longName;

        @Jsonb
        private final List<String> strings;

        @Embedded(prefix = "mapping_")
        private final Mapping mapping;

        @Embedded
        private final AuditInfo auditInfo;

        @Version
        private final int version;

        public Item(int id, String longName, List<String> strings, Mapping mapping, AuditInfo auditInfo, int version) {
            this.id = id;
            this.longName = longName;
            this.strings = strings;
            this.mapping = mapping;
            this.auditInfo = auditInfo;
            this.version = version;
        }

        public int getId() {
            return id;
        }

        public List<String> getStrings() {
            return strings;
        }

        public Mapping getMapping() {
            return mapping;
        }

        public AuditInfo getAuditInfo() {
            return auditInfo;
        }

        public int getVersion() {
            return version;
        }

        public String getLongName() {
            return longName;
        }

        public Item withLongName(String longName) {
            return new Item(id, longName, strings, mapping, auditInfo.update(), version);
        }

        public Item withStrings(List<String> strings) {
            return new Item(id, longName, strings, mapping, auditInfo.update(), version);
        }

        public Item withMapping(Mapping mapping) {
            return new Item(id, longName, strings, mapping, auditInfo.update(), version);
        }
    }

    public enum MappingStatus {
        OK, NOT_OK
    }

    public static class Mapping {
        private final long id;
        private final Instant changed;

        @PgEnum("test.status")
        private final MappingStatus status;

        public Mapping(long id, Instant changed, MappingStatus status) {
            this.id = id;
            this.changed = changed;
            this.status = status;
        }

        public long getId() {
            return id;
        }

        public Instant getChanged() {
            return changed;
        }

        public MappingStatus getStatus() {
            return status;
        }
    }

    public static class AuditInfo {
        private final Instant createdAt;
        private final Instant updatedAt;

        public AuditInfo() {
            this(Instant.now(), Instant.now());
        }

        public AuditInfo(Instant createdAt, Instant updatedAt) {
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public AuditInfo update() {
            return new AuditInfo(createdAt, Instant.now());
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }
    }
}
