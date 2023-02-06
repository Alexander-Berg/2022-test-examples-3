package ru.yandex.market.ydb.integration;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.yandex.ydb.table.result.ResultSetReader;
import com.yandex.ydb.table.transaction.TransactionMode;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ydb.integration.model.YdbField;
import ru.yandex.market.ydb.integration.query.QFrom;
import ru.yandex.market.ydb.integration.query.QSelect;
import ru.yandex.market.ydb.integration.query.QWhere;
import ru.yandex.market.ydb.integration.query.TableReadOperation;
import ru.yandex.market.ydb.integration.query.YdbInsert;
import ru.yandex.market.ydb.integration.query.YdbSelect;
import ru.yandex.market.ydb.integration.table.Primary;
import ru.yandex.market.ydb.integration.util.Quadruple;
import ru.yandex.market.ydb.integration.utils.Converters;
import ru.yandex.market.ydb.integration.utils.DaoUtils;
import ru.yandex.market.ydb.integration.utils.YdbUtils;

import static com.yandex.ydb.core.StatusCode.GENERIC_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.ydb.integration.YdbTemplate.inTransaction;
import static ru.yandex.market.ydb.integration.YdbTemplate.txRead;
import static ru.yandex.market.ydb.integration.YdbTemplate.txWrite;

public class YdbTemplateTest extends ServiceTestBase {

    @Autowired
    private YdbTemplate ydbTemplate;
    @Autowired
    private NumericYdbTableDescription numericTable;
    @Autowired
    private UUIDYdbTableDescription uuidTable;
    @Autowired
    private StringHashYdbTableDescription stringHashTable;
    @Autowired
    private StringYdbTableDescription stringTable;

    @BeforeEach
    void configure() {
        ydbTemplate.createTable(numericTable.toCreate());
        ydbTemplate.createTable(uuidTable.toCreate());
        ydbTemplate.createTable(stringHashTable.toCreate());
        ydbTemplate.createTable(stringTable.toCreate());
    }

    @AfterEach
    void clean() {
        ydbTemplate.dropTable(numericTable.tableName());
        ydbTemplate.dropTable(uuidTable.tableName());
        ydbTemplate.dropTable(stringHashTable.tableName());
        ydbTemplate.dropTable(stringTable.tableName());
    }

    @Test
    void shouldReadNumericTableByRange() {
        List<Long> series = LongStream.range(1, 10000)
                .mapToObj(i -> ThreadLocalRandom.current().nextLong(i * 100, (i + 1) * 100))
                .distinct()
                .sorted()
                .collect(Collectors.toUnmodifiableList());

        YdbInsert.Builder b = YdbInsert.insert(numericTable, numericTable.id, numericTable.name);
        for (Long id : series) {
            b.row(id, "some name");
        }
        ydbTemplate.update(b.toQuery(), txWrite());


        List<Long> ids = ydbTemplate.readTable(txRead(), numericTable,
                YdbUtils.range(numericTable.id, series.get(3), series.get(5002)),
                (collector, rdr) -> collector.yield(rdr.getColumn(numericTable.id.name()).getInt64()));

        assertThat(ids, hasSize(5000));
    }

    @Test
    void shouldReadStringTableByRange() {
        List<String> series = LongStream.range(1, 10000)
                .mapToObj(i ->
                        DigestUtils.md5Hex("" +
                                ThreadLocalRandom.current().nextLong(i * 100, (i + 1) * 100)))
                .distinct()
                .sorted()
                .collect(Collectors.toUnmodifiableList());

        YdbInsert.Builder b = YdbInsert.insert(stringHashTable, stringHashTable.id, stringHashTable.name);
        for (String id : series) {
            b.row(id, "some name");
        }
        ydbTemplate.update(b.toQuery(), txWrite());

        YdbInsert.Builder bb = YdbInsert.insert(stringTable, stringTable.id, stringTable.name);
        for (String id : series) {
            bb.row(id, "some name");
        }
        ydbTemplate.update(bb.toQuery(), txWrite());


        List<String> ids = ydbTemplate.readTable(txRead(), stringHashTable,
                YdbUtils.range(stringHashTable.id, series.get(3), series.get(5002)),
                (collector, rdr) -> collector.yield(rdr.getColumn(stringHashTable.id.name()).getUtf8()));

        assertThat(ids, hasSize(5000));


        List<String> stringIds = ydbTemplate.readTable(txRead(), stringTable,
                YdbUtils.range(stringTable.id, series.get(3), series.get(5002)),
                (collector, rdr) -> collector.yield(
                    rdr.getColumn(stringTable.id.name()).getString(StandardCharsets.UTF_8)
                ));

        assertThat(stringIds, hasSize(5000));
    }

    @Test
    @Disabled
    void shouldReadStringTableByName() {
        List<String> series = LongStream.range(1, 200000)
                .mapToObj(i ->
                        DigestUtils.md5Hex("" +
                                ThreadLocalRandom.current().nextLong(i * 100, (i + 1) * 100)))
                .distinct()
                .sorted()
                .collect(Collectors.toUnmodifiableList());

        YdbInsert.Builder b = YdbInsert.insert(stringHashTable, stringHashTable.id, stringHashTable.name);
        for (String id : series) {
            b.row(id, "some name");
        }
        ydbTemplate.update(b.toQuery(), txWrite());
        YdbSelect.Builder sb = YdbSelect.select(
                QSelect.of(stringHashTable.id, stringHashTable.name)
                        .from(QFrom.table(stringHashTable)))
                .where(QWhere.and(stringHashTable.getName().eq("some name")));

        List<String> ids = ydbTemplate.scanQuery(txRead(), sb.toQuery(),
                (collector, rdr) -> collector.yield(rdr.getColumn(stringHashTable.id.alias()).getUtf8()));

        assertThat(ids, hasSize(199999));
    }

    @Test
    void shouldReadUUIDTableByRange() {
        List<UUID> series = LongStream.range(1, 10000)
                .mapToObj(i -> UUID.randomUUID())
                .distinct()
                .sorted()
                .collect(Collectors.toUnmodifiableList());

        YdbInsert.Builder b = YdbInsert.insert(uuidTable, uuidTable.uid, uuidTable.name);
        for (UUID id : series) {
            b.row(id, "some name");
        }
        ydbTemplate.update(b.toQuery(), txWrite());


        List<UUID> ids = ydbTemplate.readTable(txRead(), uuidTable,
                YdbUtils.range(uuidTable.uid, series.get(3), series.get(1002)),
                (collector, rdr) -> collector.yield(
                        UUID.fromString(rdr.getColumn(uuidTable.uid.name()).getUtf8())));

        assertThat(ids, hasSize(1000));
    }

    @Test
    void shouldReadTablesByRange() {
        List<Long> numericSeries = LongStream.range(1, 10000)
                .mapToObj(i -> ThreadLocalRandom.current().nextLong(i * 100, (i + 1) * 100))
                .distinct()
                .sorted()
                .collect(Collectors.toUnmodifiableList());

        List<UUID> uidSeries = LongStream.range(1, 10000)
                .mapToObj(i -> UUID.randomUUID())
                .distinct()
                .sorted()
                .collect(Collectors.toUnmodifiableList());

        List<String> stringSeries = LongStream.range(1, 10000)
                .mapToObj(i ->
                        DigestUtils.md5Hex("" +
                                ThreadLocalRandom.current().nextLong(i * 100, (i + 1) * 100)))
                .distinct()
                .sorted()
                .collect(Collectors.toUnmodifiableList());

        YdbInsert.Builder numericBuilder = YdbInsert.insert(numericTable, numericTable.id, numericTable.name);
        for (Long id : numericSeries) {
            numericBuilder.row(id, "some name");
        }
        ydbTemplate.update(numericBuilder.toQuery(), txWrite());

        YdbInsert.Builder uidBuilder = YdbInsert.insert(uuidTable, uuidTable.uid, uuidTable.name);
        for (UUID id : uidSeries) {
            uidBuilder.row(id, "some name");
        }
        ydbTemplate.update(uidBuilder.toQuery(), txWrite());

        YdbInsert.Builder strBuilder = YdbInsert.insert(stringHashTable, stringHashTable.id, stringHashTable.name);
        for (String id : stringSeries) {
            strBuilder.row(id, "some name");
        }
        ydbTemplate.update(strBuilder.toQuery(), txWrite());

        YdbInsert.Builder strBytesBuilder = YdbInsert.insert(stringTable, stringTable.id, stringTable.name);
        for (String id : stringSeries) {
            strBytesBuilder.row(id, "some name");
        }
        ydbTemplate.update(strBytesBuilder.toQuery(), txWrite());


        List<Quadruple<Long, UUID, String, String>> result = ydbTemplate.readTables(txRead(), List.of(
                TableReadOperation.readFrom(numericTable, YdbUtils.range(numericTable.getId(),
                        numericSeries.get(1),
                        numericSeries.get(1000)
                ), (collector, rdr) -> collector.yield(rdr.getColumn(numericTable.id.name()).getInt64())),
                TableReadOperation.readFrom(uuidTable, YdbUtils.range(uuidTable.getUid(),
                        uidSeries.get(1),
                        uidSeries.get(1000)
                ), (collector, rdr) -> collector.yield(
                        UUID.fromString(rdr.getColumn(uuidTable.uid.name()).getUtf8())
                )),
                TableReadOperation.readFrom(stringHashTable, YdbUtils.range(stringHashTable.getId(),
                        stringSeries.get(1),
                        stringSeries.get(1000)
                ), (collector, rdr) -> collector.yield(
                        rdr.getColumn(stringHashTable.id.name()).getUtf8()
                )),
                TableReadOperation.readFrom(stringTable, YdbUtils.range(stringTable.getId(),
                        stringSeries.get(1),
                        stringSeries.get(1000)
                ), (collector, rdr) -> collector.yield(
                        rdr.getColumn(stringTable.id.name()).getString(StandardCharsets.UTF_8)
                ))
        ), (tuple, collector) -> {
            List<Long> d1 = tuple.resultFor(numericTable);
            List<UUID> d2 = tuple.resultFor(uuidTable);
            List<String> d3 = tuple.resultFor(stringHashTable);
            List<String> d4 = tuple.resultFor(stringTable);
            for (int i = 0; i < d1.size(); i++) {
                collector.yield(new Quadruple(d1.get(i), d2.get(i), d3.get(i), d4.get(i)));
            }
        });

        assertThat(result, hasSize(1000));
    }

    @Test
    void shouldInsertIntoTable() {
        ydbTemplate.update(YdbInsert.insert(stringHashTable, stringHashTable.id, stringHashTable.name)
                        .row("some id", "some name"),
                txWrite());
    }

    @Test
    void shouldInsertIntoBinaryStringTable() {
        ydbTemplate.update(YdbInsert.insert(stringTable, stringTable.id, stringTable.name)
                        .row("some id", "some name"),
                txWrite());
    }

    @Test
    void shouldInsertNullIntoTable() {
        ydbTemplate.update(YdbInsert.insert(stringHashTable, stringHashTable.id, stringHashTable.name)
                        .row("some another id", null),
                txWrite());
    }

    @Test
    void shouldInsertNullIntoBinaryStringTable() {
        ydbTemplate.update(YdbInsert.insert(stringTable, stringTable.id, stringTable.name)
                        .row("some another id", null),
                txWrite());
    }

    @Test
    void shouldInsertInTransaction() {
        ydbTemplate.transaction(TransactionMode.SERIALIZABLE_READ_WRITE, sessionCtx -> {
            assertThat(inTransaction(), is(true));
            ydbTemplate.update(YdbInsert.insert(stringHashTable, stringHashTable.id, stringHashTable.name)
                            .row("some another id", null),
                    sessionCtx);
            return null;
        });

        assertThat(ydbTemplate.selectFirst(YdbSelect.select(QSelect.of(stringHashTable.fields())
                        .from(QFrom.table(stringHashTable)).select())
                        .where(stringHashTable.id.eq("some another id"))
                        .toQuery(), txRead(),
                queryResult -> queryResult.getResultSet(0).next()).get(), is(true));
    }

    @Test
    void shouldRevertTransaction() {
        boolean hasError = false;
        try {
            assertThat(ydbTemplate.transaction(TransactionMode.SERIALIZABLE_READ_WRITE, sessionCtx -> {
                assertThat(inTransaction(), is(true));
                ydbTemplate.update(YdbInsert.insert(stringHashTable, stringHashTable.id, stringHashTable.name)
                                .row("some another id", null),
                        sessionCtx);
                return ydbTemplate.selectFirst(YdbSelect.select(QSelect.of(stringHashTable.fields())
                                .from(QFrom.table(stringHashTable)).select())
                                .where(stringHashTable.id.eq("some another id"))
                                .toQuery(), sessionCtx,
                        queryResult -> queryResult.getResultSet(0).next()).get();
            }), is(true));
        } catch (ApplicationYdbException e) {
            hasError = true;
            //Попытка чтения после изменений в той же транзакции вызывает ошибку
            assertThat(e.statusCode(), is(GENERIC_ERROR));
        }
        assertThat(hasError, is(true));

        assertThat(ydbTemplate.selectFirst(YdbSelect.select(QSelect.of(stringHashTable.fields())
                        .from(QFrom.table(stringHashTable)).select())
                        .where(stringHashTable.id.eq("some another id"))
                        .toQuery(), txRead(),
                queryResult -> queryResult.getResultSet(0).getRowCount()).get(), comparesEqualTo(0));

    }

    @Test
    void shouldSelectEntitiesByRegexp() {
        ydbTemplate.transaction(TransactionMode.SERIALIZABLE_READ_WRITE, sessionCtx -> {
            assertThat(inTransaction(), is(true));
            ydbTemplate.update(YdbInsert.insert(stringHashTable, stringHashTable.id, stringHashTable.name)
                            .row("1", "name").row("2", "anotherName").row("3", "wrong").row("4", "name2"),
                    sessionCtx);
            return null;
        });
        List<Map.Entry<String, String>> entries =
                ydbTemplate.selectList(YdbSelect.select(QSelect.of(stringHashTable.fields())
                                .from(QFrom.table(stringHashTable)).select())
                                .where(stringHashTable.name.regexp("name|another")), YdbTemplate.txRead(),
                        Converters.convertEachRowToList((collector, rdr) -> collector.yield(Map.entry(
                                DaoUtils.toString(rdr.getColumn(stringHashTable.getId().alias())),
                                DaoUtils.toString(rdr.getColumn(stringHashTable.getName().alias()))
                        ))));

        assertThat(entries.size(), equalTo(3));
    }

    @Test
    @DisplayName("Получение сущностей по пользовательскому YQL запросу")
    void canSelectEntitiesByCustomQuery() {
        ydbTemplate.transaction(TransactionMode.SERIALIZABLE_READ_WRITE, sessionCtx -> {
            assertThat(inTransaction(), is(true));
            ydbTemplate.update(
                YdbInsert.insert(stringHashTable, stringHashTable.id, stringHashTable.name)
                    .row("1", "name1").row("2", "anotherName").row("3", "anotherName").row("4", "name2"),
                sessionCtx
            );
            return null;
        });

        List<String> entryIds = ydbTemplate.selectList(
            "SELECT sht.id as sht_id, sht.name as sht_name "
                + "from `/local/string_hash_table` as sht "
                + "where sht.name = 'anotherName';",
            YdbTemplate.txRead(),
            queryResult -> {
                List<String> ids = new ArrayList<>();
                ResultSetReader resultSetReader = queryResult.getResultSet(0);

                while (resultSetReader.next()) {
                    ids.add(DaoUtils.toString(resultSetReader.getColumn(stringHashTable.getId().alias())));
                }
                return ids;
            }
        );

        assertThat(entryIds, hasSize(2));
        assertThat(entryIds, containsInAnyOrder(equalTo("2"), equalTo("3")));
    }

    @DatabaseModel(value = "num_hash_table", alias = "nht")
    public static class NumericYdbTableDescription extends YdbTableDescription {

        @Primary
        private final YdbField<Long> id = bigint("id");
        private final YdbField<String> name = text("name");

        public YdbField<Long> getId() {
            return id;
        }

        public YdbField<String> getName() {
            return name;
        }
    }

    @DatabaseModel(value = "uuid_hash_table", alias = "uht")
    public static class UUIDYdbTableDescription extends YdbTableDescription {

        @Primary
        private final YdbField<UUID> uid = uuid("uid");
        private final YdbField<String> name = text("name");


        public YdbField<UUID> getUid() {
            return uid;
        }

        public YdbField<String> getName() {
            return name;
        }
    }

    @DatabaseModel(value = "string_hash_table", alias = "sht")
    public static class StringHashYdbTableDescription extends YdbTableDescription {

        @Primary
        private final YdbField<String> id = text("id");
        private final YdbField<String> name = text("name");

        public YdbField<String> getId() {
            return id;
        }

        public YdbField<String> getName() {
            return name;
        }
    }

    @DatabaseModel(value = "string_table", alias = "sht")
    public static class StringYdbTableDescription extends YdbTableDescription {

        @Primary
        private final YdbField<String> id = string("id", StandardCharsets.UTF_8);
        private final YdbField<String> name = string("name", StandardCharsets.UTF_8);

        public YdbField<String> getId() {
            return id;
        }
    }
}
