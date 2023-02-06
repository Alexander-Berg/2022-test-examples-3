package ru.yandex.direct.ess.router.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.jooq.Name;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;
import org.jooq.impl.TableRecordImpl;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.ess.common.models.BaseLogicObject;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.DELETE;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.ess.router.utils.ColumnsChangeType.ALL;
import static ru.yandex.direct.ess.router.utils.ColumnsChangeType.ANY;
import static ru.yandex.direct.ess.router.utils.ColumnsChangeType.ANY_EXCEPT;
import static ru.yandex.direct.ess.router.utils.TableChangesHandlerTest.TestTable1.TEST_TABLE_1;
import static ru.yandex.direct.ess.router.utils.TableChangesHandlerTest.TestTable2.TEST_TABLE_2;

public class TableChangesHandlerTest {

    /**
     * Тест проверяет, что при отслеживании вставки в таблицу, вставки с нужной таблицей обработаются
     */
    @Test
    void testProcessChanges_Insert() {
        TableChangesHandler<TestLogicObject1> tableChangesHandler =
                new TableChangesHandler<>();
        tableChangesHandler.addTableChange(
                new TableChange.Builder<TestLogicObject1>()
                        .setTable(TEST_TABLE_1)
                        .setOperation(INSERT)
                        .setMapper(this::mapProceededChangToLogicObject)
                        .build());

        BinlogEvent.Row insertRow = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 1))
                .withBefore(Map.of())
                .withAfter(Map.of(
                        TEST_TABLE_1.id.getName(), 1,
                        TEST_TABLE_1.data.getName(), "testData",
                        TEST_TABLE_1.isSomeSign.getName(), true));

        BinlogEvent binlogEvent = new BinlogEvent()
                .withTable(TEST_TABLE_1.getName())
                .withOperation(INSERT)
                .withRows(List.of(insertRow));

        List<ProceededChange> expectedProceededChanges = singletonList(new ProceededChange.Builder()
                .setTableName(TEST_TABLE_1.getName())
                .setOperation(INSERT)
                .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 1))
                .setBefore(emptyMap())
                .setAfter(Map.of(
                        TEST_TABLE_1.id.getName(), 1,
                        TEST_TABLE_1.data.getName(), "testData",
                        TEST_TABLE_1.isSomeSign.getName(), true))
                .build()
        );

        List<TestLogicObject1> expected = proceededChangesToLogicObjects(expectedProceededChanges);

        List<TestLogicObject1> got = tableChangesHandler.processChanges(binlogEvent);
        assertThat(expected).containsExactly(got.toArray(new TestLogicObject1[0]));
    }

    /**
     * Тест проверяет, что при отслеживании вставки в таблицу строк с определенным значением поля
     * , вставки с нужной таблицей и значением поля обработаются, а остальные нет
     */
    @Test
    void testProcessChanges_InsertWithFilter() {
        TableChangesHandler<TestLogicObject1> tableChangesHandler =
                new TableChangesHandler<>();
        tableChangesHandler.addTableChange(
                new TableChange.Builder<TestLogicObject1>()
                        .setTable(TEST_TABLE_1)
                        .setOperation(INSERT)
                        .setValuesFilter(proceededChange -> ((Integer) proceededChange.getAfter(TEST_TABLE_1.id)) == 1)
                        .setMapper(this::mapProceededChangToLogicObject)
                        .build());

        BinlogEvent.Row insertRow1 = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(
                        TEST_TABLE_1.id.getName(), 1))
                .withBefore(Map.of())
                .withAfter(Map.of(
                        TEST_TABLE_1.id.getName(), 1,
                        TEST_TABLE_1.data.getName(), "testData",
                        TEST_TABLE_1.isSomeSign.getName(), true));

        BinlogEvent.Row insertRow2 = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(
                        TEST_TABLE_1.id.getName(), 2))
                .withBefore(Map.of())
                .withAfter(Map.of(
                        TEST_TABLE_1.id.getName(), 2,
                        TEST_TABLE_1.data.getName(), "testData",
                        TEST_TABLE_1.isSomeSign.getName(), true));

        BinlogEvent binlogEvent = new BinlogEvent()
                .withTable(TEST_TABLE_1.getName())
                .withOperation(INSERT)
                .withRows(List.of(insertRow1, insertRow2));

        List<ProceededChange> expectedProceededChanges = singletonList(new ProceededChange.Builder()
                .setTableName(TEST_TABLE_1.getName())
                .setOperation(INSERT)
                .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 1))
                .setBefore(emptyMap())
                .setAfter(Map.of(
                        TEST_TABLE_1.id.getName(), 1,
                        TEST_TABLE_1.data.getName(), "testData",
                        TEST_TABLE_1.isSomeSign.getName(), true))
                .build()
        );

        List<TestLogicObject1> expected = proceededChangesToLogicObjects(expectedProceededChanges);

        List<TestLogicObject1> got = tableChangesHandler.processChanges(binlogEvent);
        assertThat(expected).containsExactly(got.toArray(new TestLogicObject1[0]));
    }

    /**
     * Тест проверяет, что при отслеживании вставки в таблицу строк
     * , insert'ы в другие таблицы не обработаются
     */
    @Test()
    public void testProcessChanges_InsertIntoUntrackedTable() {
        TableChangesHandler<TestLogicObject1> tableChangesHandler = new TableChangesHandler<>();
        tableChangesHandler.addTableChange(new TableChange.Builder<TestLogicObject1>()
                .setTable(TEST_TABLE_1)
                .setOperation(INSERT)
                .setMapper(this::mapProceededChangToLogicObject)
                .build());

        BinlogEvent.Row insertRow = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(
                        TEST_TABLE_2.id.getName(), 1))
                .withBefore(Map.of())
                .withAfter(Map.of(
                        TEST_TABLE_2.id.getName(), 1
                ));

        BinlogEvent binlogEvent = new BinlogEvent()
                .withTable(TEST_TABLE_2.getName())
                .withOperation(INSERT)
                .withRows(List.of(insertRow));

        List<TestLogicObject1> expected = emptyList();

        List<TestLogicObject1>
                got = tableChangesHandler.processChanges(binlogEvent);

        assertThat(expected).containsExactly(got.toArray(new TestLogicObject1[0]));
    }

    /**
     * Тест проверяет, что при отслеживании определенной операции с таблицей
     * , другие операции не обработаются
     */
    @Test()
    public void testProcessChanges_UntrackedOperation() {
        TableChangesHandler<TestLogicObject1> tableChangesHandler = new TableChangesHandler<>();
        tableChangesHandler.addTableChange(new TableChange.Builder<TestLogicObject1>()
                .setTable(TEST_TABLE_1)
                .setOperation(INSERT)
                .setMapper(this::mapProceededChangToLogicObject)
                .build());

        BinlogEvent.Row deleteRow = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(
                        TEST_TABLE_2.id.getName(), 1))
                .withBefore(Map.of())
                .withAfter(Map.of(
                        TEST_TABLE_2.id.getName(), 1));

        BinlogEvent binlogEvent = new BinlogEvent()
                .withTable(TEST_TABLE_2.getName())
                .withOperation(DELETE)
                .withRows(List.of(deleteRow));

        List<TestLogicObject1> expected = emptyList();

        List<TestLogicObject1>
                got = tableChangesHandler.processChanges(binlogEvent);

        assertThat(expected).containsExactly(got.toArray(new TestLogicObject1[0]));
    }

    /**
     * Тест проверяет, что при отслеживании update'а, без указания определенного сталбца
     * , изменения любых столбцов обработаются
     */
    @Test
    void testProcessChanges_UpdateAnyColumns() {
        TableChangesHandler<TestLogicObject1> tableChangesHandler = new TableChangesHandler<>();
        tableChangesHandler.addTableChange(new TableChange.Builder<TestLogicObject1>()
                .setTable(TEST_TABLE_1)
                .setOperation(UPDATE)
                .setMapper(this::mapProceededChangToLogicObject)
                .build());

        BinlogEvent.Row updateRow = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 1))
                .withBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1"))
                .withAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2"));

        BinlogEvent binlogEvent = new BinlogEvent()
                .withTable(TEST_TABLE_1.getName())
                .withOperation(UPDATE)
                .withRows(List.of(updateRow));

        List<ProceededChange> expectedProceededChanges = singletonList(new ProceededChange.Builder()
                .setTableName(TEST_TABLE_1.getName())
                .setOperation(UPDATE)
                .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 1))
                .setBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1"))
                .setAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2"))
                .build()
        );

        List<TestLogicObject1> expected = proceededChangesToLogicObjects(expectedProceededChanges);

        List<TestLogicObject1> got = tableChangesHandler.processChanges(binlogEvent);
        assertThat(expected).containsExactly(got.toArray(new TestLogicObject1[0]));
    }

    /**
     * Тест проверяет, что при отслеживании update'а, если blob-поле превратилось в null,
     * изменение подхватится
     */
    @Test
    void testProcessChanges_UpdateBlobColumnToNull() {
        TableChangesHandler<TestLogicObject1> tableChangesHandler = new TableChangesHandler<>();
        tableChangesHandler.addTableChange(new TableChange.Builder<TestLogicObject1>()
                .setTable(TEST_TABLE_1)
                .setOperation(UPDATE)
                .setColumn(TEST_TABLE_1.data)
                .setMapper(this::mapProceededChangToLogicObject)
                .build());

        var updateRow = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 1))
                .withBefore(Map.of())
                .withAfter(singletonMap(TEST_TABLE_1.data.getName(), null));

        var binlogEvent = new BinlogEvent()
                .withTable(TEST_TABLE_1.getName())
                .withOperation(UPDATE)
                .withRows(List.of(updateRow));

        var expectedProceededChanges = singletonList(new ProceededChange.Builder()
                .setTableName(TEST_TABLE_1.getName())
                .setOperation(UPDATE)
                .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 1))
                .setBefore(Map.of())
                .setAfter(singletonMap(TEST_TABLE_1.data.getName(), null))
                .build()
        );

        var expected = proceededChangesToLogicObjects(expectedProceededChanges);

        var got = tableChangesHandler.processChanges(binlogEvent);
        assertThat(expected).containsExactly(got.toArray(new TestLogicObject1[0]));
    }

    /**
     * Тест проверяет, что при отслеживании update'а столбца с указанным пользовательским фильтром
     * , все подходящие под фильтр изменения стоблца обработаются, остальные - нет
     */
    @Test
    void testProcessChanges_UpdateWithFilter() {
        TableChangesHandler<TestLogicObject1> tableChangesHandler = new TableChangesHandler<>();
        Predicate<ProceededChange> filter = proceededChange -> {
            Boolean signBefore = proceededChange.getBefore(TEST_TABLE_1.isSomeSign);
            Boolean signAfter = proceededChange.getAfter(TEST_TABLE_1.isSomeSign);
            return !signBefore && signAfter;
        };
        tableChangesHandler.addTableChange(new TableChange.Builder<TestLogicObject1>()
                .setTable(TEST_TABLE_1)
                .setOperation(UPDATE)
                .setColumn(TEST_TABLE_1.isSomeSign)
                .setValuesFilter(filter)
                .setMapper(this::mapProceededChangToLogicObject)
                .build());

        BinlogEvent.Row updateRow1 = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(
                        TEST_TABLE_1.id.getName(), 1))
                .withBefore(Map.of(
                        TEST_TABLE_1.isSomeSign.getName(), true))
                .withAfter(Map.of(
                        TEST_TABLE_1.isSomeSign.getName(), false));

        BinlogEvent.Row updateRow2 = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(
                        TEST_TABLE_1.id.getName(), 2))
                .withBefore(Map.of(
                        TEST_TABLE_1.isSomeSign.getName(), false))
                .withAfter(Map.of(
                        TEST_TABLE_1.isSomeSign.getName(), true));


        BinlogEvent binlogEvent = new BinlogEvent()
                .withTable(TEST_TABLE_1.getName())
                .withOperation(UPDATE)
                .withRows(List.of(updateRow1, updateRow2));

        List<ProceededChange> expectedProceededChanges = singletonList(new ProceededChange.Builder()
                .setTableName(TEST_TABLE_1.getName())
                .setOperation(UPDATE)
                .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 2))
                .setBefore(Map.of(TEST_TABLE_1.isSomeSign.getName(), false))
                .setAfter(Map.of(TEST_TABLE_1.isSomeSign.getName(), true))
                .build()
        );

        List<TestLogicObject1> expected = proceededChangesToLogicObjects(expectedProceededChanges);

        List<TestLogicObject1> got = tableChangesHandler.processChanges(binlogEvent);
        assertThat(expected).containsExactly(got.toArray(new TestLogicObject1[0]));
    }

    /**
     * Тест проверяет, что при отслеживании update'а только одного столбца
     * , обновления других столбцов не обработаются
     */
    @Test
    void testProcessChanges_UpdateOneColumn() {
        TableChangesHandler<TestLogicObject1> tableChangesHandler = new TableChangesHandler<>();
        tableChangesHandler.addTableChange(new TableChange.Builder<TestLogicObject1>()
                .setTable(TEST_TABLE_1)
                .setOperation(UPDATE)
                .setColumn(TEST_TABLE_1.data)
                .setMapper(this::mapProceededChangToLogicObject)
                .build());

        BinlogEvent.Row updateDataRow = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 1))
                .withBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1"))
                .withAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2"));

        BinlogEvent.Row updateIsSomeSignRow = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 2))
                .withBefore(Map.of(TEST_TABLE_1.isSomeSign.getName(), false))
                .withAfter(Map.of(TEST_TABLE_1.isSomeSign.getName(), true));

        BinlogEvent binlogEvent = new BinlogEvent()
                .withTable(TEST_TABLE_1.getName())
                .withOperation(UPDATE)
                .withRows(List.of(updateDataRow, updateIsSomeSignRow));

        List<ProceededChange> expectedProceededChanges = singletonList(new ProceededChange.Builder()
                .setTableName(TEST_TABLE_1.getName())
                .setOperation(UPDATE)
                .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 1))
                .setBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1"))
                .setAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2"))
                .build()
        );

        List<TestLogicObject1> expected = proceededChangesToLogicObjects(expectedProceededChanges);

        List<TestLogicObject1> got = tableChangesHandler.processChanges(binlogEvent);
        assertThat(expected).containsExactly(got.toArray(new TestLogicObject1[0]));
    }


    /**
     * Тест проверяет, что отслеживание одинакового списка столбцов при разном типе изменения, типы изменения не
     * перезатрут друг друга - то есть будут отслеживаться изменения и по типу ALL, и по типу ANY
     */
    @Test
    void testProcessChanges_SeveralMappers() {
        TableChangesHandler<TestLogicObject1> tableChangesHandler = new TableChangesHandler<>();
        tableChangesHandler.addTableChange(new TableChange.Builder<TestLogicObject1>()
                .setTable(TEST_TABLE_1)
                .setOperation(UPDATE)
                .setColumns(ALL, List.of(TEST_TABLE_1.data, TEST_TABLE_1.isSomeSign))
                .setMapper(this::mapProceededChangToLogicObject)
                .build());

        tableChangesHandler.addTableChange(new TableChange.Builder<TestLogicObject1>().setTable(TEST_TABLE_1)
                .setOperation(UPDATE)
                .setColumns(ANY, List.of(TEST_TABLE_1.data, TEST_TABLE_1.isSomeSign))
                .setMapper(this::mapProceededChangToLogicObject)
                .build());

        // будем смотреть, что одно поле из двух не интересует
        // ANY_EXCEPT - 1
        tableChangesHandler.addTableChange(new TableChange.Builder<TestLogicObject1>()
                .setTable(TEST_TABLE_1)
                .setOperation(UPDATE)
                .setColumns(ANY_EXCEPT, List.of(TEST_TABLE_1.isSomeSign))
                .setMapper(this::mapProceededChangToLogicObject)
                .build());

        // будем смотреть, что одно поле из двух не интересует
        // ANY_EXCEPT - 2
        tableChangesHandler.addTableChange(new TableChange.Builder<TestLogicObject1>()
                .setTable(TEST_TABLE_1)
                .setOperation(UPDATE)
                .setColumns(ANY_EXCEPT, List.of(TEST_TABLE_1.data))
                .setMapper(this::mapProceededChangToLogicObject)
                .build());

        // будем смотреть, что два поля из двух не интересуют
        // ANY_EXCEPT - 3
        tableChangesHandler.addTableChange(new TableChange.Builder<TestLogicObject1>().setTable(TEST_TABLE_1)
                .setOperation(UPDATE)
                .setColumns(ANY_EXCEPT, List.of(TEST_TABLE_1.data, TEST_TABLE_1.isSomeSign))
                .setMapper(this::mapProceededChangToLogicObject)
                .build());

        BinlogEvent.Row updateDataRow = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 2))
                .withBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1"))
                .withAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2"));

        BinlogEvent.Row updateRow = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 1))
                .withBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1", TEST_TABLE_1.isSomeSign.getName(), false))
                .withAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2", TEST_TABLE_1.isSomeSign.getName(), true));


        BinlogEvent binlogEvent = new BinlogEvent()
                .withTable(TEST_TABLE_1.getName())
                .withOperation(UPDATE)
                .withRows(List.of(updateRow, updateDataRow));

        List<ProceededChange> expectedProceededChanges = List.of(
                // отработало ANY. Изменено любое поле из предложенных c id=1
                new ProceededChange.Builder()
                        .setTableName(TEST_TABLE_1.getName())
                        .setOperation(UPDATE)
                        .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 1))
                        .setBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1",
                                TEST_TABLE_1.isSomeSign.getName(), false))
                        .setAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2",
                                TEST_TABLE_1.isSomeSign.getName(), true))
                        .build(),
                // отработало ALL. Изменены оба поля c id=1
                new ProceededChange.Builder()
                        .setTableName(TEST_TABLE_1.getName())
                        .setOperation(UPDATE)
                        .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 1))
                        .setBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1",
                                TEST_TABLE_1.isSomeSign.getName(), false))
                        .setAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2",
                                TEST_TABLE_1.isSomeSign.getName(), true))
                        .build(),
                // отработало ANY_EXCEPT - 1. Изменено два поля c id=1, а исключить обратотку надо только если двух нет
                new ProceededChange.Builder()
                        .setTableName(TEST_TABLE_1.getName())
                        .setOperation(UPDATE)
                        .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 1))
                        .setBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1",
                                TEST_TABLE_1.isSomeSign.getName(), false))
                        .setAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2",
                                TEST_TABLE_1.isSomeSign.getName(), true))
                        .build(),
                // отработало ANY_EXCEPT - 2. Изменено два поля c id=1, а исключить обратотку надо только если двух нет
                new ProceededChange.Builder()
                        .setTableName(TEST_TABLE_1.getName())
                        .setOperation(UPDATE)
                        .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 1))
                        .setBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1",
                                TEST_TABLE_1.isSomeSign.getName(), false))
                        .setAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2",
                                TEST_TABLE_1.isSomeSign.getName(), true))
                        .build(),
                // отработало ANY. Изменено любое поле из предложенных c id=2
                new ProceededChange.Builder()
                        .setTableName(TEST_TABLE_1.getName())
                        .setOperation(UPDATE)
                        .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 2))
                        .setBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1"))
                        .setAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2"))
                        .build(),
                // отработало ANY_EXCEPT - 1. Изменено одно поле, но его нет в исключениях
                new ProceededChange.Builder()
                        .setTableName(TEST_TABLE_1.getName())
                        .setOperation(UPDATE)
                        .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 2))
                        .setBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1"))
                        .setAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2"))
                        .build()
        );
        // Из неотработанных (верное поведение):
        // ALL для  id=2
        // ANY_EXCEPT - 2 для id=2
        // ANY_EXCEPT - 3 для id=1 и id=2

        List<TestLogicObject1> expected = proceededChangesToLogicObjects(expectedProceededChanges);

        List<TestLogicObject1> got = tableChangesHandler.processChanges(binlogEvent);
        assertThat(got).containsExactly(expected.toArray(new TestLogicObject1[0]));
    }

    /**
     * Тест проверяет, что при отслеживании update'а только одного столбца, при изменении его на null
     * , такое изменение обработается
     */
    @Test
    void testProcessChanges_UpdateTrackedColumnToNull() {
        TableChangesHandler<TestLogicObject1> tableChangesHandler = new TableChangesHandler<>();
        tableChangesHandler.addTableChange(new TableChange.Builder<TestLogicObject1>()
                .setTable(TEST_TABLE_1)
                .setOperation(UPDATE)
                .setColumn(TEST_TABLE_1.data)
                .setMapper(this::mapProceededChangToLogicObject)
                .build());

        BinlogEvent.Row updateDataRow = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 1))
                .withBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1"))
                .withAfter(mapWithNullValue(TEST_TABLE_1.data.getName()));


        BinlogEvent binlogEvent = new BinlogEvent()
                .withTable(TEST_TABLE_1.getName())
                .withOperation(UPDATE)
                .withRows(singletonList(updateDataRow));

        Map<String, Object> expectedAfter = new HashMap<>();
        expectedAfter.put(TEST_TABLE_1.data.getName(), null);
        List<ProceededChange> expectedProceededChanges = singletonList(new ProceededChange.Builder()
                .setTableName(TEST_TABLE_1.getName())
                .setOperation(UPDATE)
                .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 1))
                .setBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1"))
                .setAfter(expectedAfter)
                .build()
        );

        List<TestLogicObject1> expected = proceededChangesToLogicObjects(expectedProceededChanges);

        List<TestLogicObject1> got = tableChangesHandler.processChanges(binlogEvent);
        assertThat(expected).containsExactly(got.toArray(new TestLogicObject1[0]));
    }

    /**
     * Тест проверяет, что при наличии нескольких отсалеживаний изменений столбцов таблицы, заданных как 2 разных
     * tableChange следующие кейсы верны:
     * - изменется любой из столбцов - изменение обрабатывается
     * - изменяются оба столбца одновременно - изменения обрабатываются для каждого из отслеживаний
     */
    @Test
    void testProcessChanges_UpdateSeveralColumns() {
        TableChangesHandler<TestLogicObject1> tableChangesHandler = new TableChangesHandler<>();
        tableChangesHandler.addTableChange(new TableChange.Builder<TestLogicObject1>()
                .setTable(TEST_TABLE_1)
                .setOperation(UPDATE)
                .setColumn(TEST_TABLE_1.data)
                .setMapper(this::mapProceededChangToLogicObject)
                .build());

        tableChangesHandler.addTableChange(new TableChange.Builder<TestLogicObject1>()
                .setTable(TEST_TABLE_1)
                .setOperation(UPDATE)
                .setColumn(TEST_TABLE_1.isSomeSign)
                .setMapper(this::mapProceededChangToLogicObject)
                .build());

        BinlogEvent.Row updateDataRow = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 1))
                .withBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1"))
                .withAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2"));

        BinlogEvent.Row updateIsSomeSignRow = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 2))
                .withBefore(Map.of(TEST_TABLE_1.isSomeSign.getName(), false))
                .withAfter(Map.of(TEST_TABLE_1.isSomeSign.getName(), true));

        BinlogEvent.Row updateDataAndIsSomeSignRow = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 3))
                .withBefore(Map.of(
                        TEST_TABLE_1.data.getName(), "testData1",
                        TEST_TABLE_1.isSomeSign.getName(), false))
                .withAfter(Map.of(
                        TEST_TABLE_1.data.getName(), "testData2",
                        TEST_TABLE_1.isSomeSign.getName(), true));

        BinlogEvent binlogEvent = new BinlogEvent()
                .withTable(TEST_TABLE_1.getName())
                .withOperation(UPDATE)
                .withRows(List.of(updateDataRow, updateIsSomeSignRow, updateDataAndIsSomeSignRow));

        List<ProceededChange> expectedProceededChanges = List.of(
                new ProceededChange.Builder()
                        .setTableName(TEST_TABLE_1.getName())
                        .setOperation(UPDATE)
                        .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 1))
                        .setBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1"))
                        .setAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2"))
                        .build(),

                new ProceededChange.Builder()
                        .setTableName(TEST_TABLE_1.getName())
                        .setOperation(UPDATE)
                        .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 2))
                        .setBefore(Map.of(TEST_TABLE_1.isSomeSign.getName(), false))
                        .setAfter(Map.of(TEST_TABLE_1.isSomeSign.getName(), true))
                        .build(),

                new ProceededChange.Builder()
                        .setTableName(TEST_TABLE_1.getName())
                        .setOperation(UPDATE)
                        .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 3))
                        .setBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1",
                                TEST_TABLE_1.isSomeSign.getName(), false))
                        .setAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2",
                                TEST_TABLE_1.isSomeSign.getName(), true))
                        .build(),

                new ProceededChange.Builder()
                        .setTableName(TEST_TABLE_1.getName())
                        .setOperation(UPDATE)
                        .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 3))
                        .setBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1",
                                TEST_TABLE_1.isSomeSign.getName(), false))
                        .setAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2",
                                TEST_TABLE_1.isSomeSign.getName(), true))
                        .build()
        );

        List<TestLogicObject1> expected = proceededChangesToLogicObjects(expectedProceededChanges);

        List<TestLogicObject1> got = tableChangesHandler.processChanges(binlogEvent);
        assertThat(expected).containsExactly(got.toArray(new TestLogicObject1[0]));
    }

    /**
     * Тест проверяет, что при наличии нескольких отсалеживаний изменений столбцов таблицы, заданных через setColumns
     * с типо ANY, следующие кейсы верны:
     * - изменется любой из столбцов - изменение обрабатывается
     * - изменяются оба столбца одновременно - изменения обрабатываются только один раз
     */
    @Test
    void testProcessChanges_UpdateSeveralColumns_UseOneSetter() {
        TableChangesHandler<TestLogicObject1> tableChangesHandler = new TableChangesHandler<>();
        tableChangesHandler.addTableChange(new TableChange.Builder<TestLogicObject1>()
                .setTable(TEST_TABLE_1)
                .setOperation(UPDATE)
                .setColumns(ANY, List.of(TEST_TABLE_1.data, TEST_TABLE_1.isSomeSign))
                .setMapper(this::mapProceededChangToLogicObject)
                .build());


        BinlogEvent.Row updateDataRow = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 1))
                .withBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1"))
                .withAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2"));

        BinlogEvent.Row updateIsSomeSignRow = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 2))
                .withBefore(Map.of(TEST_TABLE_1.isSomeSign.getName(), false))
                .withAfter(Map.of(TEST_TABLE_1.isSomeSign.getName(), true));

        BinlogEvent.Row updateDataAndIsSomeSignRow = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 3))
                .withBefore(Map.of(
                        TEST_TABLE_1.data.getName(), "testData1",
                        TEST_TABLE_1.isSomeSign.getName(), false))
                .withAfter(Map.of(
                        TEST_TABLE_1.data.getName(), "testData2",
                        TEST_TABLE_1.isSomeSign.getName(), true));

        BinlogEvent binlogEvent = new BinlogEvent()
                .withTable(TEST_TABLE_1.getName())
                .withOperation(UPDATE)
                .withRows(List.of(updateDataRow, updateIsSomeSignRow, updateDataAndIsSomeSignRow));

        List<ProceededChange> expectedProceededChanges = List.of(
                new ProceededChange.Builder()
                        .setTableName(TEST_TABLE_1.getName())
                        .setOperation(UPDATE)
                        .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 1))
                        .setBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1"))
                        .setAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2"))
                        .build(),

                new ProceededChange.Builder()
                        .setTableName(TEST_TABLE_1.getName())
                        .setOperation(UPDATE)
                        .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 2))
                        .setBefore(Map.of(TEST_TABLE_1.isSomeSign.getName(), false))
                        .setAfter(Map.of(TEST_TABLE_1.isSomeSign.getName(), true))
                        .build(),

                new ProceededChange.Builder()
                        .setTableName(TEST_TABLE_1.getName())
                        .setOperation(UPDATE)
                        .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 3))
                        .setBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1",
                                TEST_TABLE_1.isSomeSign.getName(), false))
                        .setAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2",
                                TEST_TABLE_1.isSomeSign.getName(), true))
                        .build()
        );

        List<TestLogicObject1> expected = proceededChangesToLogicObjects(expectedProceededChanges);

        List<TestLogicObject1> got = tableChangesHandler.processChanges(binlogEvent);
        assertThat(expected).containsExactly(got.toArray(new TestLogicObject1[0]));
    }

    /**
     * Тест проверяет, что при отслеживании одновременных изменений нескольких сталбцов
     * , такие изменения обработаются, а изменения столбцов по отдельности - нет
     */

    @Test
    void testProcessChanges_SeveralColumnsUpdatesInOneChange() {
        TableChangesHandler<TestLogicObject1> tableChangesHandler = new TableChangesHandler<>();
        tableChangesHandler.addTableChange(new TableChange.Builder<TestLogicObject1>()
                .setTable(TEST_TABLE_1)
                .setOperation(UPDATE)
                .setColumns(ALL, List.of(TEST_TABLE_1.data, TEST_TABLE_1.isSomeSign))
                .setMapper(this::mapProceededChangToLogicObject)
                .build());


        BinlogEvent.Row updateDataRow = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 1))
                .withBefore(Map.of(TEST_TABLE_1.data.getName(), "testData1"))
                .withAfter(Map.of(TEST_TABLE_1.data.getName(), "testData2"));

        BinlogEvent.Row updateIsSomeSignRow = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 2))
                .withBefore(Map.of(TEST_TABLE_1.isSomeSign.getName(), false))
                .withAfter(Map.of(TEST_TABLE_1.isSomeSign.getName(), true));

        BinlogEvent.Row updateDataRowAndIsSomeSignTheSame = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 3))
                .withBefore(Map.of(
                        TEST_TABLE_1.isSomeSign.getName(), true,
                        TEST_TABLE_1.data.getName(), "testData2"))
                .withAfter(Map.of(
                        TEST_TABLE_1.isSomeSign.getName(), true,
                        TEST_TABLE_1.data.getName(), "testData1"));

        BinlogEvent.Row updateIsSomeSignRowAndDataTheSame = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 4))
                .withBefore(Map.of(
                        TEST_TABLE_1.isSomeSign.getName(), false,
                        TEST_TABLE_1.data.getName(), "testData"))
                .withAfter(Map.of(
                        TEST_TABLE_1.isSomeSign.getName(), true,
                        TEST_TABLE_1.data.getName(), "testData"));

        BinlogEvent.Row updateBothColumns = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 5))
                .withBefore(Map.of(
                        TEST_TABLE_1.isSomeSign.getName(), false,
                        TEST_TABLE_1.data.getName(), "testData1"))
                .withAfter(Map.of(
                        TEST_TABLE_1.isSomeSign.getName(), true,
                        TEST_TABLE_1.data.getName(), "testData2"));

        BinlogEvent binlogEvent = new BinlogEvent()
                .withTable(TEST_TABLE_1.getName())
                .withOperation(UPDATE)
                .withRows(ImmutableList
                        .of(updateDataRow, updateIsSomeSignRow, updateDataRowAndIsSomeSignTheSame,
                                updateIsSomeSignRowAndDataTheSame, updateBothColumns));

        List<ProceededChange> expectedProceededChanges = singletonList(new ProceededChange.Builder()
                .setTableName(TEST_TABLE_1.getName())
                .setOperation(UPDATE)
                .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 5))
                .setBefore(Map.of(
                        TEST_TABLE_1.data.getName(), "testData1",
                        TEST_TABLE_1.isSomeSign.getName(), false))
                .setAfter(Map.of(
                        TEST_TABLE_1.data.getName(), "testData2",
                        TEST_TABLE_1.isSomeSign.getName(), true))
                .build()
        );
        List<TestLogicObject1> expected = proceededChangesToLogicObjects(expectedProceededChanges);

        List<TestLogicObject1> got = tableChangesHandler.processChanges(binlogEvent);
        assertThat(expected).containsExactly(got.toArray(new TestLogicObject1[0]));
    }

    /**
     * Тест проверяет, что при отслеживании удалений строк из таблицы, такие изменения обработаются
     */
    @Test
    void testProcessChanges_TrackDelete() {
        TableChangesHandler<TestLogicObject1> tableChangesHandler =
                new TableChangesHandler<>();
        tableChangesHandler.addTableChange(
                new TableChange.Builder<TestLogicObject1>()
                        .setTable(TEST_TABLE_1)
                        .setOperation(DELETE)
                        .setMapper(this::mapProceededChangToLogicObject)
                        .build());

        BinlogEvent.Row deleteRow = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 1))
                .withBefore(Map.of(
                        TEST_TABLE_1.id.getName(), 1,
                        TEST_TABLE_1.data.getName(), "testData",
                        TEST_TABLE_1.isSomeSign.getName(), true))
                .withAfter(Map.of());

        BinlogEvent binlogEvent = new BinlogEvent()
                .withTable(TEST_TABLE_1.getName())
                .withOperation(DELETE)
                .withRows(List.of(deleteRow));

        List<ProceededChange> expectedProceededChanges = singletonList(new ProceededChange.Builder()
                .setTableName(TEST_TABLE_1.getName())
                .setOperation(DELETE)
                .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 1))
                .setBefore(Map.of(
                        TEST_TABLE_1.id.getName(), 1,
                        TEST_TABLE_1.data.getName(), "testData",
                        TEST_TABLE_1.isSomeSign.getName(), true))
                .setAfter(emptyMap())
                .build()
        );
        List<TestLogicObject1> expected = proceededChangesToLogicObjects(expectedProceededChanges);

        List<TestLogicObject1> got = tableChangesHandler.processChanges(binlogEvent);
        assertThat(expected).containsExactly(got.toArray(new TestLogicObject1[0]));
    }

    /**
     * Тест проверяет, что при отслеживании удаления строк из таблицы с определенным значением поля
     * , удаления с нужной таблицей и значением поля обработаются, а остальные нет
     */
    @Test
    void testProcessChanges_DeleteWithFilter() {
        TableChangesHandler<TestLogicObject1> tableChangesHandler =
                new TableChangesHandler<>();
        tableChangesHandler.addTableChange(
                new TableChange.Builder<TestLogicObject1>()
                        .setTable(TEST_TABLE_1)
                        .setOperation(DELETE)
                        .setValuesFilter(proceededChange -> proceededChange.getBefore(TEST_TABLE_1.isSomeSign))
                        .setMapper(this::mapProceededChangToLogicObject)
                        .build());

        BinlogEvent.Row deleteRow1 = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 1))
                .withBefore(Map.of(
                        TEST_TABLE_1.id.getName(), 1,
                        TEST_TABLE_1.data.getName(), "testData",
                        TEST_TABLE_1.isSomeSign.getName(), true))
                .withAfter(Map.of());

        BinlogEvent.Row deleteRow2 = new BinlogEvent.Row()
                .withPrimaryKey(Map.of(TEST_TABLE_1.id.getName(), 1))
                .withBefore(Map.of(
                        TEST_TABLE_1.id.getName(), 2,
                        TEST_TABLE_1.data.getName(), "testData",
                        TEST_TABLE_1.isSomeSign.getName(), false))
                .withAfter(Map.of());

        BinlogEvent binlogEvent = new BinlogEvent()
                .withTable(TEST_TABLE_1.getName())
                .withOperation(DELETE)
                .withRows(List.of(deleteRow1, deleteRow2));

        List<ProceededChange> expectedProceededChanges = singletonList(new ProceededChange.Builder()
                .setTableName(TEST_TABLE_1.getName())
                .setOperation(DELETE)
                .setPrimaryKeys(Map.of(TEST_TABLE_1.id.getName(), 1))
                .setBefore(Map.of(
                        TEST_TABLE_1.id.getName(), 1,
                        TEST_TABLE_1.data.getName(), "testData",
                        TEST_TABLE_1.isSomeSign.getName(), true))
                .setAfter(emptyMap())
                .build()
        );
        List<TestLogicObject1> expected = proceededChangesToLogicObjects(expectedProceededChanges);

        List<TestLogicObject1> got = tableChangesHandler.processChanges(binlogEvent);
        assertThat(expected).containsExactly(got.toArray(new TestLogicObject1[0]));
    }

    private TestLogicObject1 mapProceededChangToLogicObject(ProceededChange changes) {
        return new TestLogicObject1(changes);
    }

    private List<TestLogicObject1> proceededChangesToLogicObjects(List<ProceededChange> changes) {
        return changes.stream().map(TestLogicObject1::new).collect(Collectors.toList());
    }

    private Map<String, Object> mapWithNullValue(String key) {
        var map = new HashMap<String, Object>();
        map.put(key, null);
        return map;
    }

    @SuppressWarnings("unused")
    static class TestTable1 extends TableImpl<TestTableRecord1> {
        static final TestTable1 TEST_TABLE_1 = new TestTable1();

        final TableField<TestTableRecord1, Integer> id =
                createField("id", SQLDataType.INTEGER, this, "");

        final TableField<TestTableRecord1, String> data =
                createField("data", org.jooq.impl.SQLDataType.CLOB, this, "");

        final TableField<TestTableRecord1, Boolean> isSomeSign =
                createField("is_some_sign", SQLDataType.BOOLEAN, this, "");

        TestTable1() {
            super(DSL.name("test_table"));
        }

        private TestTable1(Name alias, Table<TestTableRecord1> aliased) {
            super(alias, null, aliased, null, DSL.comment(""));
        }

        @Override
        public Class<? extends TestTableRecord1> getRecordType() {
            return TestTableRecord1.class;
        }

        @Override
        public TestTable1 as(String alias) {
            return new TestTable1(DSL.name(alias), this);
        }
    }

    public static class TestTableRecord1 extends TableRecordImpl<TestTableRecord1> {
        public TestTableRecord1() {
            super(TEST_TABLE_1);
        }
    }


    @SuppressWarnings("unused")
    static class TestTable2 extends TableImpl<TestTableRecord2> {
        static final TestTable2 TEST_TABLE_2 = new TestTable2();

        final TableField<TestTableRecord2, Long> id =
                createField("id", org.jooq.impl.SQLDataType.BIGINT, this, "");

        TestTable2() {
            super(DSL.name("test_table_2"));
        }

        private TestTable2(Name alias, Table<TestTableRecord2> aliased) {
            super(alias, null, aliased, null, DSL.comment(""));
        }

        @Override
        public Class<? extends TestTableRecord2> getRecordType() {
            return TestTableRecord2.class;
        }

        @Override
        public TestTable2 as(String alias) {
            return new TestTable2(DSL.name(alias), this);
        }
    }

    public static class TestTableRecord2 extends TableRecordImpl<TestTableRecord2> {
        public TestTableRecord2() {
            super(TEST_TABLE_2);
        }
    }

    class TestLogicObject1 extends BaseLogicObject {

        ProceededChange proceededChange;

        TestLogicObject1(ProceededChange proceededChange) {
            this.proceededChange = proceededChange;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TestLogicObject1 that = (TestLogicObject1) o;
            return Objects.equals(proceededChange, that.proceededChange);
        }

        @Override
        public int hashCode() {
            return Objects.hash(proceededChange);
        }

        @Override
        public String toString() {
            return "TestLogicObject1{" +
                    "proceededChange=" + proceededChange +
                    '}';
        }
    }
}
