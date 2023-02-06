package ru.yandex.direct.useractionlog.writer;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.shyiko.mysql.binlog.event.DeleteRowsEventData;
import com.github.shyiko.mysql.binlog.event.RowsQueryEventData;
import com.github.shyiko.mysql.binlog.event.TableMapEventData;
import com.github.shyiko.mysql.binlog.event.UpdateRowsEventData;
import com.github.shyiko.mysql.binlog.event.WriteRowsEventData;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.DataType;
import org.jooq.Key;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;

import ru.yandex.direct.binlog.reader.EnrichedEvent;
import ru.yandex.direct.binlog.reader.Transaction;
import ru.yandex.direct.clickhouse.ClickHouseUtils;
import ru.yandex.direct.mysql.BinlogEvent;
import ru.yandex.direct.mysql.BinlogEventData;
import ru.yandex.direct.mysql.BinlogEventType;
import ru.yandex.direct.mysql.MySQLUtils;
import ru.yandex.direct.mysql.schema.ColumnSchema;
import ru.yandex.direct.mysql.schema.KeyColumn;
import ru.yandex.direct.mysql.schema.KeySchema;
import ru.yandex.direct.mysql.schema.TableSchema;
import ru.yandex.direct.mysql.schema.TableType;
import ru.yandex.direct.utils.Checked;

@ParametersAreNonnullByDefault
public class BinlogFixtureGenerator {
    public static final String SERVER_UUID = "12345678-9abc-def0-1234-56789abcdef0";

    private BinlogFixtureGenerator() {
    }

    /**
     * Почти настоящая {@code TableSchema} из {@code org.jooq.Table}.
     */
    public static TableSchema makeTableSchema(Table table) {
        List<ColumnSchema> columnSchemas = new ArrayList<>();
        List<KeySchema> keySchemas = new ArrayList<>();
        for (TableField field : getTableFields(table)) {
            DataType dataType = field.getDataType();
            columnSchemas.add(new ColumnSchema(field.getName(),
                    dataType.getTypeName(),
                    dataType.getTypeName(),
                    dataType.defaultValue() == null ? "" : dataType.defaultValue().toString(),
                    dataType.nullable()));
        }
        for (Object key : table.getKeys()) {
            List<KeyColumn> keyColumns = new ArrayList<>();
            int counter = 0;
            for (Object keyField : ((Key) key).getFields()) {
                keyColumns.add(new KeyColumn(((TableField) keyField).getName(),
                        counter++,
                        ((TableField) keyField).getDataType().nullable()));
            }
            keySchemas.add(new KeySchema(((Key) key).getName(), "BTREE", key instanceof UniqueKey, keyColumns));
        }
        return new TableSchema(table.getName(), TableType.TABLE, "ignored", columnSchemas, keySchemas,
                Collections.emptyList());
    }

    /**
     * Список всех колонок в jooq-таблице
     */
    public static List<TableField> getTableFields(Table table) {
        return Arrays.stream(table.getClass().getDeclaredFields())
                .filter(attr -> TableField.class.isAssignableFrom(attr.getType()))
                .map(attr -> (TableField) Checked.get(() -> attr.get(table)))
                .collect(Collectors.toList());
    }

    /**
     * Создаёт макет binlog-события, в котором есть только вставка какой-то одной строчки.
     */
    public static EnrichedEvent createInsertEvent(LocalDateTime dateTime,
                                                  long serverEventId,
                                                  Table table,
                                                  ImmutableList<Pair<TableField, Serializable>> after) {
        TableSchema tableSchema = makeTableSchema(table);
        WriteRowsEventData data = new WriteRowsEventData();

        Map<String, Integer> columnIds = IntStream.range(0, tableSchema.getColumns().size())
                .mapToObj(i -> new AbstractMap.SimpleEntry<>(tableSchema.getColumns().get(i).getName(), i))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        data.setIncludedColumns(
                after.stream().map(pair -> columnIds.get(pair.getKey().getName())).collect(collectToBitSet()));

        data.setRows(Collections.singletonList(mapToSchemaChangesList(after, columnIds)));

        Duration timestamp = Duration.ofSeconds(dateTime.atZone(ZoneId.of("UTC")).toEpochSecond());
        TableMapEventData tableMap = new TableMapEventData();
        tableMap.setDatabase("ppc");
        tableMap.setTable(table.getName());
        tableMap.setColumnTypes(new byte[columnIds.size()]);
        tableMap.setColumnMetadata(new int[columnIds.size()]);
        List<TableField> tableFields = getTableFields(table);
        tableMap.setColumnNullability(
                IntStream.range(0, tableFields.size())
                        .filter(i -> tableFields.get(i).getDataType().nullable())
                        .boxed()
                        .collect(collectToBitSet()));
        BinlogEvent insertEvent = new BinlogEvent(timestamp.toMillis(),
                BinlogEventType.INSERT, new BinlogEventData.Insert("xxx:1", data, tableMap, tableSchema));

        RowsQueryEventData rowsQueryEventData = new RowsQueryEventData();
        rowsQueryEventData.setQuery("INSERT INTO " + MySQLUtils.quoteName(table.getName()) + " VALUES ()");
        return new EnrichedEvent(SERVER_UUID + ":" + serverEventId,
                new Transaction.Query(
                        new BinlogEvent(timestamp.toMillis(),
                                BinlogEventType.ROWS_QUERY,
                                new BinlogEventData.RowsQuery("xxx:1", rowsQueryEventData)),
                        Collections.singletonList(insertEvent)),
                insertEvent,
                0,
                0);
    }

    /**
     * Создаёт макет binlog-события, в котором есть только изменение какой-то одной строчки.
     */
    public static EnrichedEvent createUpdateEvent(LocalDateTime dateTime,
                                                  long serverEventId,
                                                  Table table,
                                                  ImmutableList<Pair<TableField, Serializable>> before,
                                                  ImmutableList<Pair<TableField, Serializable>> after) {
        TableSchema tableSchema = makeTableSchema(table);
        UpdateRowsEventData data = new UpdateRowsEventData();

        Map<String, Integer> columnIds = IntStream.range(0, tableSchema.getColumns().size())
                .mapToObj(i -> new AbstractMap.SimpleEntry<>(tableSchema.getColumns().get(i).getName(), i))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        data.setIncludedColumnsBeforeUpdate(
                before.stream().map(pair -> columnIds.get(pair.getKey().getName())).collect(collectToBitSet()));

        data.setIncludedColumns(
                after.stream().map(pair -> columnIds.get(pair.getKey().getName())).collect(collectToBitSet()));

        data.setRows(Collections.singletonList(new AbstractMap.SimpleEntry<>(
                mapToSchemaChangesList(before, columnIds),
                mapToSchemaChangesList(after, columnIds))));

        Duration timestamp = Duration.ofSeconds(dateTime.atZone(ZoneId.of("UTC")).toEpochSecond());
        TableMapEventData tableMap = new TableMapEventData();
        tableMap.setDatabase("ppc");
        tableMap.setTable(table.getName());
        tableMap.setColumnTypes(new byte[columnIds.size()]);
        tableMap.setColumnMetadata(new int[columnIds.size()]);
        List<TableField> tableFields = getTableFields(table);
        tableMap.setColumnNullability(
                IntStream.range(0, tableFields.size())
                        .filter(i -> tableFields.get(i).getDataType().nullable())
                        .boxed()
                        .collect(collectToBitSet()));
        BinlogEvent updateEvent = new BinlogEvent(timestamp.toMillis(),
                BinlogEventType.UPDATE, new BinlogEventData.Update("xxx:1", data, tableMap, tableSchema));

        RowsQueryEventData rowsQueryEventData = new RowsQueryEventData();
        rowsQueryEventData.setQuery("UPDATE " + MySQLUtils.quoteName(table.getName()) + " SET " +
                after.stream()
                        .map(e -> MySQLUtils.quoteName(e.getKey().getName()) + " = " + ClickHouseUtils
                                .quote(e.getValue() == null ? "NULL" : e.getValue().toString()))
                        .collect(Collectors.joining(", ")));
        return new EnrichedEvent(SERVER_UUID + ":" + serverEventId,
                new Transaction.Query(
                        new BinlogEvent(timestamp.toMillis(),
                                BinlogEventType.ROWS_QUERY,
                                new BinlogEventData.RowsQuery("xxx:1", rowsQueryEventData)),
                        Collections.singletonList(updateEvent)),
                updateEvent,
                0,
                0);
    }

    /**
     * Создаёт макет binlog-события, в котором есть только удаление какой-то одной строчки.
     */
    public static EnrichedEvent createDeleteEvent(LocalDateTime dateTime,
                                                  long serverEventId,
                                                  Table table,
                                                  ImmutableList<Pair<TableField, Serializable>> before) {
        TableSchema tableSchema = makeTableSchema(table);
        DeleteRowsEventData data = new DeleteRowsEventData();

        Map<String, Integer> columnIds = IntStream.range(0, tableSchema.getColumns().size())
                .mapToObj(i -> new AbstractMap.SimpleEntry<>(tableSchema.getColumns().get(i).getName(), i))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        data.setIncludedColumns(
                before.stream().map(pair -> columnIds.get(pair.getKey().getName())).collect(collectToBitSet()));

        data.setRows(Collections.singletonList(mapToSchemaChangesList(before, columnIds)));

        Duration timestamp = Duration.ofSeconds(dateTime.atZone(ZoneId.of("UTC")).toEpochSecond());
        TableMapEventData tableMap = new TableMapEventData();
        tableMap.setDatabase("ppc");
        tableMap.setTable(table.getName());
        tableMap.setColumnTypes(new byte[columnIds.size()]);
        tableMap.setColumnMetadata(new int[columnIds.size()]);
        List<TableField> tableFields = getTableFields(table);
        tableMap.setColumnNullability(
                IntStream.range(0, tableFields.size())
                        .filter(i -> tableFields.get(i).getDataType().nullable())
                        .boxed()
                        .collect(collectToBitSet()));
        BinlogEvent deleteEvent = new BinlogEvent(timestamp.toMillis(),
                BinlogEventType.DELETE, new BinlogEventData.Delete("xxx:1", data, tableMap, tableSchema));

        RowsQueryEventData rowsQueryEventData = new RowsQueryEventData();
        rowsQueryEventData.setQuery("DELETE FROM " + MySQLUtils.quoteName(table.getName()));
        return new EnrichedEvent(SERVER_UUID + ":" + serverEventId,
                new Transaction.Query(
                        new BinlogEvent(timestamp.toMillis(),
                                BinlogEventType.ROWS_QUERY,
                                new BinlogEventData.RowsQuery("xxx:1", rowsQueryEventData)),
                        Collections.singletonList(deleteEvent)),
                deleteEvent,
                0,
                0);
    }


    private static Serializable[] mapToSchemaChangesList(ImmutableList<Pair<TableField, Serializable>> fieldValues,
                                                         Map<String, Integer> columnIds) {
        return fieldValues.stream()
                .sorted(Comparator.comparingInt(e -> columnIds.get(e.getKey().getName())))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList())
                .toArray(new Serializable[0]);
    }

    private static Collector<Integer, ?, BitSet> collectToBitSet() {
        return Collector.of(BitSet::new,
                BitSet::set,
                (set1, set2) -> {
                    set1.or(set2);
                    return set1;
                },
                Collector.Characteristics.UNORDERED);
    }
}
