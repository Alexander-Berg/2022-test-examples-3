package ru.yandex.market.core.archive;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import ru.yandex.market.core.archive.model.DatabaseModel;
import ru.yandex.market.core.archive.model.Key;
import ru.yandex.market.core.archive.model.KeyPart;
import ru.yandex.market.core.archive.model.KeyValues;
import ru.yandex.market.core.archive.model.Query;
import ru.yandex.market.core.archive.model.Relation;
import ru.yandex.market.core.archive.model.TableModel;
import ru.yandex.market.core.archive.type.KeyColumnType;
import ru.yandex.market.core.archive.type.RelationRule;
import ru.yandex.market.mbi.util.db.jdbc.TNumberTbl;
import ru.yandex.market.mbi.util.db.jdbc.TStringTbl;

/**
 * Тесты для {@link QueryProcessor}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class QueryProcessorTest extends ArchivingFunctionalTest {

    @Autowired
    private QueryProcessor queryProcessor;

    @Test
    @DisplayName("Генерация запроса. Одна таблица, одна связь")
    void oneTableOneRel() {
        Key key01 = key("SHOPS_WEB.TABLE01", new KeyPart("PK01", KeyColumnType.NUMBER));
        TableModel model01 = makeTableModel("SHOPS_WEB.TABLE01", key01, Collections.emptyList());

        List<Relation> relations = List.of(
                Relation.of("SHOPS_WEB.TABLE01", "FK0102", "PK01", "FK", RelationRule.NO_ACTION)
        );
        Key key02 = key("SCHEMA.TABLE02", new KeyPart("PK02", KeyColumnType.NUMBER));
        TableModel model02 = makeTableModel("SCHEMA.TABLE02", key02, relations);
        DatabaseModel model = new DatabaseModel(List.of(model01, model02));

        List<Query> queries = List.of(
                new Query("SHOPS_WEB.TABLE01", "select * from SHOPS_WEB.TABLE01 where PK01 in {list TABLE01.PK01}"),
                new Query("SCHEMA.TABLE02", "select * from SCHEMA.TABLE02 where (FK in {list TABLE01.PK01})")
        );

        checkProcess(queries.get(0),
                model,
                "select * from SHOPS_WEB.TABLE01 where PK01 in (null)",
                List.of());

        checkProcess(queries.get(0),
                model,
                "select * from SHOPS_WEB.TABLE01 where PK01 in " +
                        "(select /*+ cardinality(t 1)*/ value(t) from table(cast(:param_0 as shops_web.t_number_tbl)) t)",
                List.of(new TNumberTbl(List.of(2L, 3L))),
                getValues(key01, 2L, 3L),
                getValues(key02, 5L));

        checkProcess(queries.get(1),
                model,
                "select * from SCHEMA.TABLE02 where (FK in " +
                        "(select /*+ cardinality(t 1)*/ value(t) from table(cast(:param_0 as shops_web.t_number_tbl)) t))",
                List.of(new TNumberTbl(List.of(2L, 3L))),
                getValues(key01, 2L, 3L),
                getValues(key02, 5L));
    }

    @Test
    @DisplayName("Генерация запроса. Запрос для entryPoint. String pk")
    void testBuild06() {
        Key key = key("SHOPS_WEB.TEST_TABLE", new KeyPart("ID", KeyColumnType.STRING));
        TableModel modelTable = makeTableModel("SHOPS_WEB.TEST_TABLE", key, Collections.emptyList());

        DatabaseModel model = new DatabaseModel(List.of(modelTable));

        List<Query> queries = List.of(
                new Query("SHOPS_WEB.TEST_TABLE", "select * from SHOPS_WEB.TEST_TABLE where ID in {list TEST_TABLE.ID}")
        );

        checkProcess(queries.get(0),
                model,
                "select * from SHOPS_WEB.TEST_TABLE where ID in " +
                        "(select /*+ cardinality(t 1)*/ value(t) from table(cast(:param_0 as shops_web.ntt_varchar2)) t)",
                List.of(new TStringTbl(List.of("str", "str2"))),
                getValues(key, "str", "str2"));
    }

    @Test
    @DisplayName("Генерация запроса. Составной fk")
    void testBuild10() {
        Key key01 = key("SHOPS_WEB.TABLE01", new KeyPart("PK01_1", KeyColumnType.STRING), new KeyPart("PK01_2", KeyColumnType.NUMBER));
        TableModel model01 = makeTableModel("SHOPS_WEB.TABLE01", key01, Collections.emptyList());

        List<Relation> relations = List.of(
                new Relation.Builder("SHOPS_WEB.TABLE01", "FK_NAME_1", RelationRule.NO_ACTION)
                        .addColumns("PK01_1", "FK1")
                        .addColumns("PK01_2", "FK2")
                        .build()
        );
        final Key key02 = key("SCHEMA.TABLE02", new KeyPart("ID", KeyColumnType.NUMBER));
        TableModel model02 = makeTableModel("SCHEMA.TABLE02", key02, relations);

        DatabaseModel model = new DatabaseModel(List.of(model01, model02));

        List<Query> queries = List.of(
                new Query("SHOPS_WEB.TABLE01", "select * from SHOPS_WEB.TABLE01 where {each (PK01_1, PK01_2) in (TABLE01.PK01_1, TABLE01.PK01_2)}"),
                new Query("SCHEMA.TABLE02", "select * from SCHEMA.TABLE02 where ({each (FK1, FK2) in (TABLE01.PK01_1, TABLE01.PK01_2)})")
        );

        checkProcess(queries.get(0),
                model,
                "select * from SHOPS_WEB.TABLE01 where (1=2)",
                List.of());

        checkProcess(queries.get(0),
                model,
                "select * from SHOPS_WEB.TABLE01 where ((PK01_1 = :param_0 and PK01_2 = :param_1) or (PK01_1 = :param_2 and PK01_2 = :param_3))",
                List.of("key1", 1L, "gg", 5L),
                getValues(key01, "key1", 1L, "gg", 5L),
                getValues(key02, 9L));

        checkProcess(queries.get(1),
                model,
                "select * from SCHEMA.TABLE02 where (((FK1 = :param_0 and FK2 = :param_1) or (FK1 = :param_2 and FK2 = :param_3)))",
                List.of("key1", 1L, "gg", 5L),
                getValues(key01, "key1", 1L, "gg", 5L),
                getValues(key02, 9L));
    }

    @Test
    @DisplayName("Запрос вместе с {each} и {list}")
    void testEachAndList() {
        Key key01 = key("SHOPS_WEB.TABLE01", new KeyPart("PK01_1", KeyColumnType.STRING),
                new KeyPart("PK01_2", KeyColumnType.NUMBER),
                new KeyPart("PK01_3", KeyColumnType.NUMBER));
        TableModel model01 = makeTableModel("SHOPS_WEB.TABLE01", key01, Collections.emptyList());

        List<Relation> relations = List.of(
                new Relation.Builder("SHOPS_WEB.TABLE01", "FK_NAME_1", RelationRule.NO_ACTION)
                        .addColumns("PK01_1", "FK1")
                        .addColumns("PK01_2", "FK2")
                        .build(),
                new Relation.Builder("SHOPS_WEB.TABLE01", "FK_NAME_2", RelationRule.NO_ACTION)
                        .addColumns("PK01_3", "FK3")
                        .build()
        );
        final Key key02 = key("SCHEMA.TABLE02", new KeyPart("ID", KeyColumnType.NUMBER));
        TableModel model02 = makeTableModel("SCHEMA.TABLE02", key02, relations);

        DatabaseModel model = new DatabaseModel(List.of(model01, model02));

        List<Query> queries = List.of(
                new Query("SCHEMA.TABLE02", "select * from SCHEMA.TABLE02 where ({each (FK1, FK2) in (TABLE01.PK01_1, TABLE01.PK01_2)}) or (FK3 in {list TABLE01.PK01_3})")
        );

        checkProcess(queries.get(0),
                model,
                "select * from SCHEMA.TABLE02 where (((FK1 = :param_1 and FK2 = :param_2) or (FK1 = :param_3 and FK2 = :param_4))) or (FK3 in (select /*+ cardinality(t 1)*/ value(t) from table(cast(:param_0 as shops_web.t_number_tbl)) t))",
                List.of(new TNumberTbl(List.of(31L, 32L)), "key1", 1L, "gg", 5L),
                getValues(key01, "key1", 1L, 31L, "gg", 5L, 32L),
                getValues(key02, 9L));
    }

    @Test
    @DisplayName("Несколько одинаковых {list}")
    void testSeveralList() {
        Key key01 = key("SHOPS_WEB.TABLE01", new KeyPart("PK01", KeyColumnType.NUMBER));
        TableModel model01 = makeTableModel("SHOPS_WEB.TABLE01", key01, Collections.emptyList());

        List<Relation> relations = List.of(
                new Relation.Builder("SHOPS_WEB.TABLE01", "FK_NAME_1", RelationRule.NO_ACTION)
                        .addColumns("PK01", "FK1")
                        .build()
        );
        final Key key02 = key("SCHEMA.TABLE02", new KeyPart("ID", KeyColumnType.NUMBER));
        TableModel model02 = makeTableModel("SCHEMA.TABLE02", key02, relations);

        DatabaseModel model = new DatabaseModel(List.of(model01, model02));

        List<Query> queries = List.of(
                new Query("SCHEMA.TABLE02", "select * from SCHEMA.TABLE02 where (FK1 in {list TABLE01.PK01}) or (FK1 in {list TABLE01.PK01})")
        );

        checkProcess(queries.get(0),
                model,
                "select * from SCHEMA.TABLE02 where (FK1 in (select /*+ cardinality(t 1)*/ value(t) from table(cast(:param_0 as shops_web.t_number_tbl)) t)) or (FK1 in (select /*+ cardinality(t 1)*/ value(t) from table(cast(:param_0 as shops_web.t_number_tbl)) t))",
                List.of(new TNumberTbl(List.of(31L, 32L))),
                getValues(key01, 31L, 32L),
                getValues(key02, 9L));
    }

    @Test
    @DisplayName("Несколько одинаковых {each}")
    void testSeveralEach() {
        Key key01 = key("SHOPS_WEB.TABLE01", new KeyPart("PK01", KeyColumnType.NUMBER));
        TableModel model01 = makeTableModel("SHOPS_WEB.TABLE01", key01, Collections.emptyList());

        List<Relation> relations = List.of(
                new Relation.Builder("SHOPS_WEB.TABLE01", "FK_NAME_1", RelationRule.NO_ACTION)
                        .addColumns("PK01", "FK1")
                        .build()
        );
        final Key key02 = key("SCHEMA.TABLE02", new KeyPart("ID", KeyColumnType.NUMBER));
        TableModel model02 = makeTableModel("SCHEMA.TABLE02", key02, relations);

        DatabaseModel model = new DatabaseModel(List.of(model01, model02));

        List<Query> queries = List.of(
                new Query("SCHEMA.TABLE02", "select * from SCHEMA.TABLE02 where {each (FK1) in (TABLE01.PK01)} or {each (FK1) in (TABLE01.PK01)}")
        );

        checkProcess(queries.get(0),
                model,
                "select * from SCHEMA.TABLE02 where ((FK1 = :param_0) or (FK1 = :param_1)) or ((FK1 = :param_0) or (FK1 = :param_1))",
                List.of(31L, 32L),
                getValues(key01, 31L, 32L),
                getValues(key02, 9L));
    }

    @Test
    @DisplayName("Запрос на столбец с типом <> {number, varchar}")
    void testNotGoodType() {
        Key key = key("SHOPS_WEB.TEST_TABLE", new KeyPart("ID", KeyColumnType.DATE));
        TableModel modelTable = makeTableModel("SHOPS_WEB.TEST_TABLE", key, Collections.emptyList());

        DatabaseModel model = new DatabaseModel(List.of(modelTable));

        List<Query> queries = List.of(
                new Query("SHOPS_WEB.TEST_TABLE", "select * from SHOPS_WEB.TEST_TABLE where ID in {list TEST_TABLE.ID}")
        );

        final Timestamp timstamp1 = Timestamp.valueOf(LocalDateTime.now().minusDays(1));
        final Timestamp timestamp2 = Timestamp.valueOf(LocalDateTime.now().minusYears(1));
        checkProcess(queries.get(0),
                model,
                "select * from SHOPS_WEB.TEST_TABLE where ID in (:param_0)",
                List.of(List.of(timstamp1, timestamp2)),
                getValues(key, timstamp1, timestamp2));
    }

    @Test
    @DisplayName("Запрос на удаление")
    void testBuildDelete() {
        Key key01 = key("SHOPS_WEB.TABLE01", new KeyPart("PK1", KeyColumnType.NUMBER), new KeyPart("PK2", KeyColumnType.STRING));
        TableModel model01 = makeTableModel("SHOPS_WEB.TABLE01", key01, Collections.emptyList());

        List<Relation> relations = List.of(
                Relation.of("SHOPS_WEB.TABLE01", "FK_NAME_1", "PK1", "FK1", RelationRule.NO_ACTION),
                Relation.of("SHOPS_WEB.TABLE01", "FK_NAME_2", "PK2", "FK2", RelationRule.NO_ACTION)
        );
        final Key key02 = key("SHOPS_WEB.TABLE02", new KeyPart("ID", KeyColumnType.NUMBER));
        TableModel model02 = makeTableModel("SHOPS_WEB.TABLE02", key02, relations);

        DatabaseModel model = new DatabaseModel(List.of(model01, model02));

        List<Query> queries = List.of(
                new Query("SHOPS_WEB.TABLE01", "delete from SHOPS_WEB.TABLE01 where {each (PK1, PK2) in (TABLE01.PK1, TABLE01.PK2)}"),
                new Query("SHOPS_WEB.TABLE02", "delete from SHOPS_WEB.TABLE02 where ID in {list TABLE02.ID}")
        );

        checkProcess(queries.get(0),
                model,
                "delete from SHOPS_WEB.TABLE01 where (1=2)",
                List.of());

        checkProcess(queries.get(0),
                model,
                "delete from SHOPS_WEB.TABLE01 where ((PK1 = :param_0 and PK2 = :param_1) or (PK1 = :param_2 and PK2 = :param_3))",
                List.of(1L, "key1", 5L, "gg"),
                getValues(key01, 1L, "key1", 5L, "gg"),
                getValues(key02, 9L, 11L));

        checkProcess(queries.get(1),
                model,
                "delete from SHOPS_WEB.TABLE02 where ID in (select /*+ cardinality(t 1)*/ value(t) from table(cast(:param_0 as shops_web.t_number_tbl)) t)",
                List.of(new TNumberTbl(List.of(9L, 11L))),
                getValues(key01, 1L, "key1", 5L, "gg"),
                getValues(key02, 9L, 11L));
    }

    private KeyValues getValues(Key key, Object... args) {
        final KeyValues.Builder builder = new KeyValues.Builder(key);
        for (int i = 0; i < args.length; i++) {
            if (i % key.getColumns().size() == 0 && i != 0) {
                builder.nextRow();
            }
            builder.addValue(key.getColumns().get(i % key.getColumns().size()).getColumn(), args[i]);
        }

        return builder.build();
    }

    private void checkProcess(Query query, DatabaseModel model, String expectedSql, List<Object> expectedParams, KeyValues... values) {
        final KeyValuesCache cache = new KeyValuesCache();
        Arrays.stream(values).forEach(cache::addValues);

        final Map<String, Object> expectedParamsMap = IntStream.range(0, expectedParams.size())
                .boxed()
                .collect(Collectors.toMap(e -> "param_" + e, expectedParams::get));

        final MapSqlParameterSource params = new MapSqlParameterSource();
        cache.transactionReading("table", getter -> {
            final String actualSql = queryProcessor.process(query, model, params, getter);
            Assertions.assertEquals(expectedSql, actualSql);
            Assertions.assertEquals(expectedParamsMap, params.getValues());
        });
    }
}
