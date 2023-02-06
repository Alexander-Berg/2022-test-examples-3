package ru.yandex.market.core.archive;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.archive.model.DatabaseModel;
import ru.yandex.market.core.archive.model.Key;
import ru.yandex.market.core.archive.model.KeyPart;
import ru.yandex.market.core.archive.model.KeyValues;
import ru.yandex.market.core.archive.model.Relation;
import ru.yandex.market.core.archive.model.RelationConfig;
import ru.yandex.market.core.archive.type.KeyColumnType;
import ru.yandex.market.core.archive.type.RelationRule;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchivingServiceTest extends ArchivingFunctionalTest {
    @Autowired
    private ArchivingService archivingService;

    @Autowired
    private DatabaseModelService databaseModelService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("Невалидная точка входа: нет данных")
    @DbUnitDataSet
    void testModelNullEntryPoint() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch01.single_table", new RelationConfig());
        Key key = key("sch01.single_table", new KeyPart("id", KeyColumnType.NUMBER));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("id", 1L)
                .build();

        archive(model, keyValues);
    }

    @Test
    @DisplayName("Проверка архивации в стрим")
    @DbUnitDataSet(before = "csv/02.archive.two_tables.before.csv", after = "csv/02.archive.two_tables.after.csv")
    void testArchive02Stream() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch03.first_table", new RelationConfig());
        Key key = key("sch03.first_table", new KeyPart("id", KeyColumnType.NUMBER));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("id", 2L)
                .build();

        String actual = archive(model, keyValues);
        checkArchiveResult("02.archive.result.xml", actual);
    }

    @Test
    @DisplayName("Проверка архивации в clob")
    @DbUnitDataSet(before = "csv/03.archive.clob_value.before.csv", after = "csv/03.archive.clob_value.after.csv")
    void testArchive03Clob() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch08.table01", new RelationConfig());
        Key key = key("sch08.table01", new KeyPart("id", KeyColumnType.NUMBER));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("id", 2L)
                .build();

        String actual = archive(model, keyValues);
        checkArchiveResult("03.archive.clob_value.result.xml", actual);
    }

    @Test
    @DisplayName("Архивация двух связанных таблиц со строковым fk")
    @DbUnitDataSet(before = "csv/04.archive.before.csv", after = "csv/04.archive.after.csv")
    void testArchive04() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch10.table01", new RelationConfig());
        Key key = key("sch10.table01", new KeyPart("id", KeyColumnType.STRING));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("id", "MY_ID")
                .build();

        String actual = archive(model, keyValues);
        checkArchiveResult("04.archive.result.xml", actual);
    }

    @Test
    @DisplayName("Архивация глубокого графа")
    @DbUnitDataSet(before = "csv/05.archive.long.before.csv", after = "csv/05.archive.long.after.csv")
    void testArchive05() throws Exception {
        testArchive05Common(new RelationConfig(), "05.archive.long.result.xml");
    }

    @Test
    @DisplayName("Кастомная связь дублирует fk (не точка входа)")
    @DbUnitDataSet(before = "csv/05.archive.long.before.csv")
    void testCustomDuplicateFkNotEntryPoint() throws Exception {
        RelationConfig relationConfig = new RelationConfig(List.of(Pair.of(
                "sch05.table03",
                Relation.of("sch05.table02", "custom_rel", "id", "table02_id", RelationRule.NO_ACTION)
        )), Set.of());

        testArchive05Common(relationConfig, "05.archive.long.result.tmp.xml"); // TODO batalin: fix MBI-30704
    }

    @Test
    @DisplayName("Кастомная связь дублирует fk (точка входа)")
    @DbUnitDataSet(before = "csv/05.archive.long.before.csv")
    void testCustomDuplicateFkEntryPoint() throws Exception {
        RelationConfig relationConfig = new RelationConfig(List.of(Pair.of(
                "sch05.table02",
                Relation.of("sch05.table01", "custom_rel", "id", "table01_id", RelationRule.NO_ACTION)
        )), Set.of());

        testArchive05Common(relationConfig, "05.archive.long.result.xml");
    }

    private void testArchive05Common(final RelationConfig config, final String expectedXml) throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch05.table01", config);
        Key key = key("sch05.table01", new KeyPart("id", KeyColumnType.NUMBER));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("id", 4L)
                .build();

        String actual = archive(model, keyValues);
        checkArchiveResult(expectedXml, actual);
    }

    @Test
    @DisplayName("Архивация графа с составным pk")
    @DbUnitDataSet(before = "csv/07.archive.composite_pk.before.csv", after = "csv/07.archive.composite_pk.after.csv")
    void testArchive06() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch12.table01", new RelationConfig());
        Key key = key("sch12.table01", new KeyPart("id1", KeyColumnType.NUMBER), new KeyPart("id2",
                KeyColumnType.STRING));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("id1", 4L)
                .addValue("id2", "SECOND")
                .build();

        String actual = archive(model, keyValues);
        checkArchiveResult("06.archive.composite_pk.result.xml", actual);
    }

    @Test
    @DisplayName("Архивация графа с составным fk")
    @DbUnitDataSet(before = "csv/08.archive.composite_fk.before.csv", after = "csv/08.archive.composite_fk.after.csv")
    void testArchive08() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch13.table01", new RelationConfig());
        Key key = key("sch13.table01", new KeyPart("id1", KeyColumnType.STRING), new KeyPart("id2", KeyColumnType.NUMBER));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("id1", "AB")
                .addValue("id2", 4L)
                .build();

        String actual = archive(model, keyValues);
        checkArchiveResult("08.archive.composite_fk.xml", actual);
    }

    @Test
    @DisplayName("Архивация графа, в котором данные без связи (нет данных в таблице)")
    @DbUnitDataSet(before = "csv/09.archive.empty_table.before.csv", after = "csv/09.archive.empty_table.after.csv")
    void testArchive09() throws Exception {
        DatabaseModel model = databaseModelService.getDatabaseModel("sch05.table01", new RelationConfig());
        Key key = key("sch05.table01", new KeyPart("id", KeyColumnType.NUMBER));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("id", 4L)
                .build();

        String actual = archive(model, keyValues);
        checkArchiveResult("09.archive.empty_table.result.xml", actual);
    }

    @Test
    @DisplayName("Архивация графа, в есть пользовательская связь")
    @DbUnitDataSet(before = "csv/10.archive.param_value.before.csv", after = "csv/10.archive.param_value.after.csv")
    void testArchive10() throws Exception {
        Relation customRel = new Relation.Builder("sch17.table01", "custom", RelationRule.NO_ACTION)
                .addColumns("id", "entity_id")
                .setWhereClause("param_type_id in (select param_type_id from sch17.param_type where entity_name='DATASOURCE')")
                .build();
        RelationConfig relationConfig = new RelationConfig(List.of(Pair.of("sch17.param_value", customRel)), Set.of());

        DatabaseModel model = databaseModelService.getDatabaseModel("sch17.table01", relationConfig);
        Key key = key("sch17.table01", new KeyPart("id", KeyColumnType.NUMBER));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("id", 2L)
                .build();

        String actual = archive(model, keyValues);
        checkArchiveResult("10.archive.param_value.result.xml", actual);
    }

    @Test
    @DisplayName("Архивация графа. BLOB")
    @DbUnitDataSet
    void testArchive11() throws Exception {
        byte[] blobData = {2, 5, 11, 10, 120, -30, -1};
        jdbcTemplate.update("insert into sch19.table01 (id, blob_data) values (1, ?)", new Object[]{blobData});

        RelationConfig relationConfig = new RelationConfig();
        DatabaseModel model = databaseModelService.getDatabaseModel("sch19.table01", relationConfig);
        Key key = key("sch19.table01", new KeyPart("id", KeyColumnType.NUMBER));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("id", 1L)
                .build();

        String actual = archive(model, keyValues);
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root><table name=\"sch19.table01\">" +
                "<row><column name=\"id\" type=\"int4\">1</column>" +
                "<column name=\"blob_data\" type=\"bytea\">AgULCnji/w==</column></row></table></root>";

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Архивация графа. Цикл на себя (в центре)")
    @DbUnitDataSet(before = "csv/12.archive.cyclic_self.before.csv", after = "csv/12.archive.cyclic_self.after.csv")
    void testArchive12() throws Exception {
        RelationConfig relationConfig = new RelationConfig();

        DatabaseModel model = databaseModelService.getDatabaseModel("sch16.table01", relationConfig);
        Key key = key("sch16.table01", new KeyPart("id", KeyColumnType.NUMBER));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("id", 2L)
                .build();

        String actual = archive(model, keyValues);
        checkArchiveResult("12.archive.cyclic_self.result.xml", actual);
    }

    @Test
    @DisplayName("Архивация графа. Цикл на себя (в начале)")
    @DbUnitDataSet(before = "csv/13.archive.cyclic_self.before.csv", after = "csv/13.archive.cyclic_self.after.csv")
    void testArchive13() throws Exception {
        RelationConfig relationConfig = new RelationConfig();

        DatabaseModel model = databaseModelService.getDatabaseModel("sch14.table01", relationConfig);
        Key key = key("sch14.table01", new KeyPart("id", KeyColumnType.NUMBER));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("id", 2L)
                .build();

        String actual = archive(model, keyValues);
        checkArchiveResult("13.archive.cyclic_self.result.xml", actual);
    }

    @Test
    @Disabled("В базе нет больших циклов (есть только на себя). Поэтому сейчас этот функционал не нужен. " +
            "Чтобы удалять такие циклы, надо либо отключать проверку констрейнтов, либо занулять столбец (рвать цикл). " +
            "Пока что этот функционал не нужен. Архивирвоать такое умеем, удалять не умеем")
    @DisplayName("Архивация графа. Цикл")
    @DbUnitDataSet(before = "csv/14.archive.cyclic.before.csv", after = "csv/14.archive.cyclic.after.csv")
    void testArchive14() throws Exception {
        jdbcTemplate.update("update sch20.table02 set table03_id=1 where id=1");
        jdbcTemplate.update("update sch20.table02 set table03_id=2 where id=3");
        jdbcTemplate.update("update sch20.table02 set table03_id=4 where id=4");

        RelationConfig relationConfig = new RelationConfig();

        DatabaseModel model = databaseModelService.getDatabaseModel("sch20.table01", relationConfig);
        Key key = key("sch20.table01", new KeyPart("id", KeyColumnType.NUMBER));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("id", 2L)
                .build();

        String actual = archive(model, keyValues);
        checkArchiveResult("14.archive.cyclic.result.xml", actual);
    }

    @Test
    @DisplayName("Архивация графа. FK на уникальный столбец")
    @DbUnitDataSet(before = "csv/15.archive.unique.before.csv", after = "csv/15.archive.unique.after.csv")
    void testArchive15() throws Exception {
        RelationConfig relationConfig = new RelationConfig();

        DatabaseModel model = databaseModelService.getDatabaseModel("sch21.table01", relationConfig);
        Key key = key("sch21.table01", new KeyPart("id", KeyColumnType.NUMBER));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("id", 2L)
                .build();

        String actual = archive(model, keyValues);
        checkArchiveResult("15.archive.unique_key.result.xml", actual);
    }

    @Test
    @DisplayName("Цикл на свой ключ")
    @DbUnitDataSet(before = "csv/23.archive.cycle.before.csv")
    void testArchive23() throws Exception {
        Relation relation1 = new Relation.Builder("sch23.partner", "custom_rel1", RelationRule.NO_ACTION)
                .addColumns("id", "key_numeric_value")
                .setWhereClause("key_type = 2")
                .build();

        Relation relation2 = new Relation.Builder("sch23.calendar_owner_keys", "custom_rel2", RelationRule.NO_ACTION)
                .addColumns("calendar_owner_id", "calendar_owner_id")
                .build();

        Relation relation3 = Relation.of("sch23.calendar_owner_keys", "custom_3", "calendar_owner_id", "owner_id", RelationRule.NO_ACTION);

        Relation relation4 = Relation.of("sch23.partner", "custom_4", "id", "owner_id", RelationRule.NO_ACTION);

        RelationConfig relationConfig = new RelationConfig(List.of(
            Pair.of("sch23.calendar_owner_keys", relation1),
            Pair.of("sch23.calendar_owner_keys", relation2),
            Pair.of("sch23.calendars", relation3),
            Pair.of("sch23.calendars", relation4)
        ), Set.of());

        DatabaseModel model = databaseModelService.getDatabaseModel("sch23.partner", relationConfig);
        Key key = key("sch23.partner", new KeyPart("id", KeyColumnType.NUMBER));
        KeyValues keyValues = new KeyValues.Builder(key)
                .addValue("id", 2L)
                .build();

        String actual = archive(model, keyValues);
        checkArchiveResult("23.archive.cycle.result.xml", actual);
    }

    private String archive(DatabaseModel model, KeyValues keyValues) {
        String[] result = new String[1];
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull final TransactionStatus status) {
                try {
                    archivingService.archive(model, keyValues, data -> result[0] = data.getData());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return result[0];
    }

    private void checkArchiveResult(String expectedFile, String actualXml) {
        try (InputStream stream = getClass().getResourceAsStream("xml/" + expectedFile)) {
            final Diff diff = DiffBuilder.compare(Input.fromStream(stream))
                    .withTest(Input.fromString(actualXml))
                    .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
                    .ignoreWhitespace()
                    .ignoreComments()
                    .checkForSimilar()
                    .build();

            assertFalse(diff.toString(), diff.hasDifferences());
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
