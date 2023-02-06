package ru.yandex.market.core.archive;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.archive.model.DatabaseModel;
import ru.yandex.market.core.archive.model.Key;
import ru.yandex.market.core.archive.model.KeyPart;
import ru.yandex.market.core.archive.model.Query;
import ru.yandex.market.core.archive.model.Relation;
import ru.yandex.market.core.archive.model.TableModel;
import ru.yandex.market.core.archive.type.KeyColumnType;
import ru.yandex.market.core.archive.type.QueryType;
import ru.yandex.market.core.archive.type.RelationRule;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.core.archive.ArchivingFunctionalTest.key;
import static ru.yandex.market.core.archive.ArchivingFunctionalTest.makeTableModel;

/**
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class QueryBuilderTest {

    @Test
    @DisplayName("Генерация запроса. Одна таблица, одна связь. Пустой pk у исходной таблицы")
    void testBuild01e() {
        oneTableOneRel(Key.of("TABLE02", Arrays.asList(new KeyPart("COLUMN", KeyColumnType.NUMBER)), true));
    }

    @Test
    @DisplayName("Генерация запроса. Одна таблица, одна связь. Не пустой pk у исходной таблицы")
    void testBuild01ne() {
        Key key02 = key("TABLE02", new KeyPart("PK02", KeyColumnType.NUMBER));
        oneTableOneRel(key02);
    }

    private void oneTableOneRel(Key key02) {
        Key key01 = key("TABLE01", new KeyPart("\"Pk01\"", KeyColumnType.NUMBER));
        TableModel model01 = makeTableModel("TABLE01", key01, Collections.emptyList());

        List<Relation> relations = Arrays.asList(
                Relation.of("TABLE01", "FK0102", "\"pK01\"", "FK", RelationRule.NO_ACTION)
        );
        TableModel model02 = makeTableModel("TABLE02", key02, relations);
        DatabaseModel model = new DatabaseModel(Arrays.asList(model01, model02));

        Map<String, Query> queries = QueryBuilder.createQueries(QueryType.SELECT, model);

        List<Query> expected = Arrays.asList(
                new Query("TABLE01", "select * from TABLE01 where \"Pk01\" in {list TABLE01.\"Pk01\"}"),
                new Query("TABLE02", "select * from TABLE02 where (FK in {list TABLE01.\"pK01\"})")
        );

        checkQueries(expected, queries);
    }

    @Test
    @DisplayName("Генерация запроса. Несколько таблиц, по одной связи")
    void testBuild02() {
        Key key01 = key("TABLE01", new KeyPart("PK1", KeyColumnType.NUMBER));
        TableModel model01 = makeTableModel("TABLE01", key01, Collections.emptyList());

        Key key02 = key("TABLE02", new KeyPart("PK2", KeyColumnType.NUMBER));
        TableModel model02 = makeTableModel("TABLE02", key02, Collections.emptyList());

        List<Relation> relations = Arrays.asList(
                Relation.of("TABLE01", "FK_NAME_1", "PK1", "FK1", RelationRule.NO_ACTION),
                Relation.of("TABLE02", "FK_NAME_2", "PK2", "FK2", RelationRule.NO_ACTION)
        );
        TableModel model03 = makeTableModel("TABLE03", key("TABLE03", new KeyPart("ID", KeyColumnType.NUMBER)), relations);

        DatabaseModel model = new DatabaseModel(Arrays.asList(model01, model02, model03));

        Map<String, Query> queries = QueryBuilder.createQueries(QueryType.SELECT, model);

        List<Query> expected = Arrays.asList(
                new Query("TABLE01", "select * from TABLE01 where PK1 in {list TABLE01.PK1}"),
                new Query("TABLE02", "select * from TABLE02 where PK2 in {list TABLE02.PK2}"),
                new Query("TABLE03", "select * from TABLE03 where (FK1 in {list TABLE01.PK1}) or (FK2 in {list TABLE02.PK2})")
        );

        checkQueries(expected, queries);
    }

    @Test
    @DisplayName("Генерация запроса. Одна таблица, одна связь. Тип fk - string")
    void testBuild03() {
        Key key01 = key("TABLE01", new KeyPart("PK01", KeyColumnType.STRING));
        TableModel model01 = makeTableModel("TABLE01", key01, Collections.emptyList());

        List<Relation> relations = Arrays.asList(
                Relation.of("TABLE01", "FK_NAME_1", "PK01", "FK", RelationRule.NO_ACTION)
        );
        TableModel model02 = makeTableModel("TABLE02", key("TABLE02", new KeyPart("ID", KeyColumnType.NUMBER)), relations);

        DatabaseModel model = new DatabaseModel(Arrays.asList(model01, model02));
        Map<String, Query> queries = QueryBuilder.createQueries(QueryType.SELECT, model);

        List<Query> expected = Arrays.asList(
                new Query("TABLE01", "select * from TABLE01 where PK01 in {list TABLE01.PK01}"),
                new Query("TABLE02", "select * from TABLE02 where (FK in {list TABLE01.PK01})")
        );

        checkQueries(expected, queries);
    }

    @Test
    @DisplayName("Генерация запроса. Запрос для entryPoint. Number pk")
    void testBuild05() {
        Key key = key("TEST_TABLE", new KeyPart("ID", KeyColumnType.NUMBER));
        TableModel modelTable = makeTableModel("TEST_TABLE", key, Collections.emptyList());

        DatabaseModel model = new DatabaseModel(Arrays.asList(modelTable));
        Map<String, Query> queries = QueryBuilder.createQueries(QueryType.SELECT, model);

        List<Query> expected = Arrays.asList(
                new Query("TEST_TABLE", "select * from TEST_TABLE where ID in {list TEST_TABLE.ID}")
        );

        checkQueries(expected, queries);
    }

    @Test
    @DisplayName("Генерация запроса. Запрос для entryPoint. String pk")
    void testBuild06() {
        Key key = key("TEST_TABLE", new KeyPart("ID", KeyColumnType.STRING));
        TableModel modelTable = makeTableModel("TEST_TABLE", key, Collections.emptyList());

        DatabaseModel model = new DatabaseModel(Arrays.asList(modelTable));
        Map<String, Query> queries = QueryBuilder.createQueries(QueryType.SELECT, model);

        List<Query> expected = Arrays.asList(
                new Query("TEST_TABLE", "select * from TEST_TABLE where ID in {list TEST_TABLE.ID}")
        );

        checkQueries(expected, queries);
    }

    @Test
    @DisplayName("Генерация запроса. Две таблицы. Разные типы столбцов у связей")
    void testBuild07() {
        Key key01 = key("TABLE01", new KeyPart("PK1", KeyColumnType.STRING));
        TableModel model01 = makeTableModel("TABLE01", key01, Collections.emptyList());

        Key key02 = key("TABLE02", new KeyPart("PK2", KeyColumnType.NUMBER));
        TableModel model02 = makeTableModel("TABLE02", key02, Collections.emptyList());

        List<Relation> relations = Arrays.asList(
                Relation.of("TABLE01", "FK_NAME_1", "PK1", "FK1", RelationRule.NO_ACTION),
                Relation.of("TABLE02", "FK_NAME_2", "PK2", "FK2", RelationRule.NO_ACTION)
        );
        TableModel model03 = makeTableModel("TABLE03", key("TABLE03", new KeyPart("ID", KeyColumnType.NUMBER)), relations);

        DatabaseModel model = new DatabaseModel(Arrays.asList(model01, model02, model03));

        Map<String, Query> queries = QueryBuilder.createQueries(QueryType.SELECT, model);

        List<Query> expected = Arrays.asList(
                new Query("TABLE01", "select * from TABLE01 where PK1 in {list TABLE01.PK1}"),
                new Query("TABLE02", "select * from TABLE02 where PK2 in {list TABLE02.PK2}"),
                new Query("TABLE03", "select * from TABLE03 where (FK1 in {list TABLE01.PK1}) or (FK2 in {list TABLE02.PK2})")
        );

        checkQueries(expected, queries);
    }

    @Test
    @DisplayName("Генерация запроса. Одна таблица, одна связь. Составной pk #0")
    void testBuild08_0() {
        Key key01 = key("TABLE01", new KeyPart("PK01_1", KeyColumnType.STRING), new KeyPart("PK01_2", KeyColumnType.NUMBER));
        TableModel model01 = makeTableModel("TABLE01", key01, Collections.emptyList());

        List<Relation> relations = Arrays.asList(
                Relation.of("TABLE01", "FK_NAME_1", "PK01_2", "FK", RelationRule.NO_ACTION)
        );
        TableModel model02 = makeTableModel("TABLE02", key("TABLE02", new KeyPart("ID", KeyColumnType.NUMBER)), relations);
        DatabaseModel model = new DatabaseModel(Arrays.asList(model01, model02));

        Map<String, Query> queries = QueryBuilder.createQueries(QueryType.SELECT, model);

        List<Query> expected = Arrays.asList(
                new Query("TABLE01", "select * from TABLE01 where {each (PK01_1, PK01_2) in (TABLE01.PK01_1, TABLE01.PK01_2)}"),
                new Query("TABLE02", "select * from TABLE02 where (FK in {list TABLE01.PK01_2})")
        );

        checkQueries(expected, queries);
    }

    @Test
    @DisplayName("Генерация запроса. Одна таблица, одна связь. Составной pk #1")
    void testBuild08_1() {
        Key key01 = key("TABLE01", new KeyPart("PK01_1", KeyColumnType.STRING), new KeyPart("PK01_2", KeyColumnType.NUMBER));
        TableModel model01 = makeTableModel("TABLE01", key01, Collections.emptyList());

        List<Relation> relations = Arrays.asList(
                Relation.of("TABLE01", "FK_NAME_1", "PK01_1", "FK", RelationRule.NO_ACTION)
        );
        TableModel model02 = makeTableModel("TABLE02", key("TABLE02", new KeyPart("ID", KeyColumnType.NUMBER)), relations);
        DatabaseModel model = new DatabaseModel(Arrays.asList(model01, model02));

        Map<String, Query> queries = QueryBuilder.createQueries(QueryType.SELECT, model);

        List<Query> expected = Arrays.asList(
                new Query("TABLE01", "select * from TABLE01 where {each (PK01_1, PK01_2) in (TABLE01.PK01_1, TABLE01.PK01_2)}"),
                new Query("TABLE02", "select * from TABLE02 where (FK in {list TABLE01.PK01_1})")
        );

        checkQueries(expected, queries);
    }

    @Test
    @DisplayName("Генерация запроса. Запрос для entryPoint. Составной pk")
    void testBuild09() {
        Key key = key("TEST_TABLE", new KeyPart("ID1", KeyColumnType.STRING), new KeyPart("ID2", KeyColumnType.NUMBER));
        TableModel modelTable = makeTableModel("TEST_TABLE", key, Collections.emptyList());
        DatabaseModel model = new DatabaseModel(Arrays.asList(modelTable));

        Map<String, Query> queries = QueryBuilder.createQueries(QueryType.SELECT, model);

        List<Query> expected = Arrays.asList(
                new Query("TEST_TABLE", "select * from TEST_TABLE where {each (ID1, ID2) in (TEST_TABLE.ID1, TEST_TABLE.ID2)}")
        );

        checkQueries(expected, queries);
    }

    @Test
    @DisplayName("Генерация запроса. Составной fk")
    void testBuild10() {
        Key key01 = key("TABLE01", new KeyPart("PK01_1", KeyColumnType.STRING), new KeyPart("PK01_2", KeyColumnType.NUMBER));
        TableModel model01 = makeTableModel("TABLE01", key01, Collections.emptyList());

        List<Relation> relations = Arrays.asList(
                new Relation.Builder("TABLE01", "FK_NAME_1", RelationRule.NO_ACTION)
                        .addColumns("PK01_1", "FK1")
                        .addColumns("PK01_2", "FK2")
                        .build()
        );
        TableModel model02 = makeTableModel("TABLE02", key("TABLE02", new KeyPart("ID", KeyColumnType.NUMBER)), relations);

        DatabaseModel model = new DatabaseModel(Arrays.asList(model01, model02));

        Map<String, Query> queries = QueryBuilder.createQueries(QueryType.SELECT, model);

        List<Query> expected = Arrays.asList(
                new Query("TABLE01", "select * from TABLE01 where {each (PK01_1, PK01_2) in (TABLE01.PK01_1, TABLE01.PK01_2)}"),
                new Query("TABLE02", "select * from TABLE02 where ({each (FK1, FK2) in (TABLE01.PK01_1, TABLE01.PK01_2)})")
        );

        checkQueries(expected, queries);
    }

    @Test
    @DisplayName("Генерация запроса. Одна таблица. Несколько связей")
    void testBuild11() {
        Key key01 = key("TABLE01", new KeyPart("PK1", KeyColumnType.NUMBER), new KeyPart("PK2", KeyColumnType.STRING));
        TableModel model01 = makeTableModel("TABLE01", key01, Collections.emptyList());

        List<Relation> relations = Arrays.asList(
                Relation.of("TABLE01", "FK_NAME_1", "PK1", "FK1", RelationRule.NO_ACTION),
                Relation.of("TABLE01", "FK_NAME_2", "PK2", "FK2", RelationRule.NO_ACTION)
        );
        TableModel model02 = makeTableModel("TABLE02", key("TABLE02", new KeyPart("ID", KeyColumnType.NUMBER)), relations);

        DatabaseModel model = new DatabaseModel(Arrays.asList(model01, model02));
        Map<String, Query> queries = QueryBuilder.createQueries(QueryType.SELECT, model);

        List<Query> expected = Arrays.asList(
                new Query("TABLE01", "select * from TABLE01 where {each (PK1, PK2) in (TABLE01.PK1, TABLE01.PK2)}"),
                new Query("TABLE02", "select * from TABLE02 where (FK1 in {list TABLE01.PK1}) or (FK2 in {list TABLE01.PK2})")
        );

        checkQueries(expected, queries);
    }

    @Test
    @DisplayName("Генерация запроса. Для таблиц с пользовательской связью")
    void testBuild14() {
        Key key01 = key("DATASOURCE", new KeyPart("ID", KeyColumnType.NUMBER));
        TableModel model01 = makeTableModel("DATASOURCE", key01, Collections.emptyList());

        List<Relation> relations = Arrays.asList(
                new Relation.Builder("DATASOURCE", "CUSTOM", RelationRule.NO_ACTION)
                        .addColumns("ID", "ENTITY_ID")
                        .setWhereClause("param_type_id in (select param_type_id from param_type where entity_name='datasource')")
                        .build()
        );
        TableModel model02 = makeTableModel("PARAM_VALUE", key("PARAM_VALUE", new KeyPart("ID", KeyColumnType.NUMBER)), relations);

        DatabaseModel model = new DatabaseModel(Arrays.asList(model01, model02));
        Map<String, Query> queries = QueryBuilder.createQueries(QueryType.SELECT, model);

        List<Query> expected = Arrays.asList(
                new Query("DATASOURCE", "select * from DATASOURCE where ID in {list DATASOURCE.ID}"),
                new Query("PARAM_VALUE", "select * from PARAM_VALUE where (ENTITY_ID in {list DATASOURCE.ID} " +
                        "and param_type_id in (select param_type_id from param_type where entity_name='datasource'))")
        );

        checkQueries(expected, queries);
    }

    @Test
    @DisplayName("Генерация запроса. Несколько таблиц со связью")
    void testBuild15() {
        Key key01 = key("TABLE01", new KeyPart("PK1", KeyColumnType.NUMBER), new KeyPart("PK2", KeyColumnType.STRING));
        TableModel model01 = makeTableModel("TABLE01", key01, Collections.emptyList());

        Key key02 = key("TABLE02", new KeyPart("ID", KeyColumnType.NUMBER));
        List<Relation> relations2 = Arrays.asList(
                Relation.of("TABLE01", "FK_NAME_1", "PK1", "FK1", RelationRule.NO_ACTION),
                Relation.of("TABLE01", "FK_NAME_2", "PK2", "FK2", RelationRule.NO_ACTION)
        );
        TableModel model02 = makeTableModel("TABLE02", key02, relations2);

        List<Relation> relations3 = Arrays.asList(
                Relation.of("TABLE02", "FK_NAME_3", "ID", "FK3", RelationRule.NO_ACTION)
        );
        TableModel model03 = makeTableModel("TABLE03", key("TABLE03", new KeyPart("ID", KeyColumnType.NUMBER)), relations3);

        DatabaseModel model = new DatabaseModel(Arrays.asList(model01, model02, model03));
        Map<String, Query> queries = QueryBuilder.createQueries(QueryType.SELECT, model);

        List<Query> expected = Arrays.asList(
                new Query("TABLE01", "select * from TABLE01 where {each (PK1, PK2) in (TABLE01.PK1, TABLE01.PK2)}"),
                new Query("TABLE02", "select * from TABLE02 where (FK1 in {list TABLE01.PK1}) or (FK2 in {list TABLE01.PK2})"),
                new Query("TABLE03", "select * from TABLE03 where (FK3 in {list TABLE02.ID})")
        );

        checkQueries(expected, queries);
    }

    @Test
    @DisplayName("Генерация запроса. Точка входа имеет цикл в себя")
    void testBuild16() {
        Key key01 = key("TABLE01", new KeyPart("ID", KeyColumnType.NUMBER));
        List<Relation> relations1 = Arrays.asList(
                Relation.of("TABLE01", "FK_NAME_1", "ID", "FK_SELF", RelationRule.NO_ACTION)
        );
        TableModel model01 = makeTableModel("TABLE01", key01, relations1);

        Key key02 = key("TABLE02", new KeyPart("ID", KeyColumnType.NUMBER));
        List<Relation> relations2 = Arrays.asList(
                Relation.of("TABLE01", "FK_NAME_2", "ID", "FK2", RelationRule.NO_ACTION)
        );
        TableModel model02 = makeTableModel("TABLE02", key02, relations2);

        DatabaseModel model = new DatabaseModel(Arrays.asList(model01, model02));
        Map<String, Query> queries = QueryBuilder.createQueries(QueryType.SELECT, model);

        List<Query> expected = Arrays.asList(
                new Query("TABLE01", "select * from TABLE01 where (FK_SELF in {list TABLE01.ID} or ID in {list TABLE01.ID})"),
                new Query("TABLE02", "select * from TABLE02 where (FK2 in {list TABLE01.ID})")
        );

        checkQueries(expected, queries);
    }

    @Test
    @DisplayName("Запрос на себя, с пк на пк")
    void testCyclePk() {
        Key partnerKey = key("PARTNER", new KeyPart("ID", KeyColumnType.NUMBER));
        final TableModel partner = makeTableModel("PARTNER", partnerKey, Collections.emptyList());

        final Key calendarOwnerKey = key("CALENDAR_OWNER_KEYS", new KeyPart("CALENDAR_OWNER_ID", KeyColumnType.NUMBER), new KeyPart("KEY_TYPE", KeyColumnType.NUMBER));
        Relation relation1 = new Relation.Builder("PARTNER", "custom_rel1", RelationRule.NO_ACTION)
                .addColumns("ID", "KEY_NUMERIC_VALUE")
                .setWhereClause("KEY_TYPE = 2")
                .build();
        Relation relation2 = new Relation.Builder("CALENDAR_OWNER_KEYS", "custom_rel2", RelationRule.NO_ACTION)
                .addColumns("CALENDAR_OWNER_ID", "CALENDAR_OWNER_ID")
                .build();
        final TableModel calendarOwnerKeys = makeTableModel("CALENDAR_OWNER_KEYS", calendarOwnerKey, Arrays.asList(relation1, relation2));

        final Key calendarsKey = key("CALENDARS", new KeyPart("ID", KeyColumnType.NUMBER));
        final TableModel calendars = makeTableModel("CALENDARS", calendarsKey, Arrays.asList(
                Relation.of("CALENDAR_OWNER_KEYS", "custom_3", "CALENDAR_OWNER_ID", "OWNER_ID", RelationRule.NO_ACTION),
                Relation.of("PARTNER", "custom_4", "ID", "OWNER_ID", RelationRule.NO_ACTION)
        ));

        DatabaseModel model = new DatabaseModel(Arrays.asList(partner, calendarOwnerKeys, calendars));

        Map<String, Query> queries = QueryBuilder.createQueries(QueryType.SELECT, model);

        List<Query> expected = Arrays.asList(
                new Query("PARTNER", "select * from PARTNER where ID in {list PARTNER.ID}"),
                new Query("CALENDAR_OWNER_KEYS",
                        "select * from CALENDAR_OWNER_KEYS where (KEY_NUMERIC_VALUE in {list PARTNER.ID} and KEY_TYPE = 2) " +
                        "or (CALENDAR_OWNER_ID in {list CALENDAR_OWNER_KEYS.CALENDAR_OWNER_ID} " +
                        "or {each (CALENDAR_OWNER_ID, KEY_TYPE) in (CALENDAR_OWNER_KEYS.CALENDAR_OWNER_ID, CALENDAR_OWNER_KEYS.KEY_TYPE)})"),
                new Query("CALENDARS", "select * from CALENDARS where (OWNER_ID in {list CALENDAR_OWNER_KEYS.CALENDAR_OWNER_ID}) " +
                        "or (OWNER_ID in {list PARTNER.ID})")
        );

        checkQueries(expected, queries);
    }

    @Test
    @DisplayName("ParamValue")
    void testParamValue() {
        final Key partnerKey = key("PARTNER", new KeyPart("ID", KeyColumnType.NUMBER));
        TableModel partner = makeTableModel("PARTNER", partnerKey, Collections.emptyList());
        final Key datafeedKey = key("DATAFEED", new KeyPart("ID", KeyColumnType.NUMBER));
        TableModel datafeed = makeTableModel("DATAFEED", datafeedKey, Collections.emptyList());
        final Key campaignInfoKey = key("CAMPAIGN_INFO", new KeyPart("CAMPAIGN_ID", KeyColumnType.NUMBER));
        TableModel campaignInfo = makeTableModel("CAMPAIGN_INFO", campaignInfoKey, Collections.emptyList());

        Key paramValueKey = key("PARAM_VALUE", new KeyPart("PARAM_VALUE_ID", KeyColumnType.NUMBER));
        List<Relation> paramValueRel = Arrays.asList(
                Relation.of("PARTNER", "FK_NAME_1", "ID", "ENTITY_ID", RelationRule.NO_ACTION),
                Relation.of("CAMPAIGN_INFO", "FK_NAME_2", "CAMPAIGN_ID", "ENTITY_ID", RelationRule.NO_ACTION),
                Relation.of("DATAFEED", "FK_NAME_3", "ID", "ENTITY_ID", RelationRule.NO_ACTION)
        );
        TableModel paramValue = makeTableModel("PARAM_VALUE", paramValueKey, paramValueRel);

        final DatabaseModel model = new DatabaseModel(Arrays.asList(partner, campaignInfo, datafeed, paramValue));
        Map<String, Query> queries = QueryBuilder.createQueries(QueryType.SELECT, model);

        List<Query> expected = Arrays.asList(
                new Query("PARAM_VALUE", "select * from PARAM_VALUE where (ENTITY_ID in {list PARTNER.ID}) " +
                        "or (ENTITY_ID in {list CAMPAIGN_INFO.CAMPAIGN_ID}) " +
                        "or (ENTITY_ID in {list DATAFEED.ID})"),
                new Query("PARTNER", "select * from PARTNER where ID in {list PARTNER.ID}"),
                new Query("CAMPAIGN_INFO", "select * from CAMPAIGN_INFO where CAMPAIGN_ID in {list CAMPAIGN_INFO.CAMPAIGN_ID}"),
                new Query("DATAFEED", "select * from DATAFEED where ID in {list DATAFEED.ID}")
        );

        checkQueries(expected, queries);
    }

    @Test
    @DisplayName("Генерация запроса на удаление")
    void testBuildDelete() {
        Key key01 = key("TABLE01", new KeyPart("PK1", KeyColumnType.NUMBER), new KeyPart("PK2", KeyColumnType.STRING));
        TableModel model01 = makeTableModel("TABLE01", key01, Collections.emptyList());

        List<Relation> relations = Arrays.asList(
                Relation.of("TABLE01", "FK_NAME_1", "PK1", "FK1", RelationRule.NO_ACTION),
                Relation.of("TABLE01", "FK_NAME_2", "PK2", "FK2", RelationRule.NO_ACTION)
        );
        TableModel model02 = makeTableModel("TABLE02", key("TABLE02", new KeyPart("ID", KeyColumnType.NUMBER)), relations);

        DatabaseModel model = new DatabaseModel(Arrays.asList(model01, model02));
        Map<String, Query> queries = QueryBuilder.createQueries(QueryType.DELETE, model);

        List<Query> expected = Arrays.asList(
                new Query("TABLE01", "delete from TABLE01 where {each (PK1, PK2) in (TABLE01.PK1, TABLE01.PK2)}"),
                new Query("TABLE02", "delete from TABLE02 where ID in {list TABLE02.ID}")
        );

        checkQueries(expected, queries);
    }

    private void checkQueries(List<Query> expected, Map<String, Query> actual) {
        assertThat(actual).hasSameSizeAs(expected);
        for (Query expectedQuery : expected) {
            Query actualQuery = actual.get(expectedQuery.getTableName());
            assertThat(actualQuery).isNotNull();
            checkQuery(expectedQuery, actualQuery);
        }
    }

    private void checkQuery(Query expected, Query actual) {
        assertThat(actual.getTableName()).isEqualTo(expected.getTableName());
        assertThat(actual.getQuery()).isEqualTo(expected.getQuery());
    }

}
