package ru.yandex.direct.binlogbroker.replicatetoyt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yt.ytclient.tables.ColumnSchema;
import ru.yandex.yt.ytclient.tables.TableSchema;

/**
 * Хранит в себе исходные данные одной строки, предоставляет методы для конверсии в различные представления:
 * {@link BinlogEvent.Row}, {@link YTreeNode}, lookup filter
 */
@ParametersAreNonnullByDefault
public class TestRow {

    private final TableSchema schema;
    private final Map<String, Object> values;

    /**
     * Создает строку данных
     *
     * @param schema схема данных в YT представлении
     * @param values - данные в виде мапы имя_колонки->значение
     */
    public TestRow(TableSchema schema, Map<String, Object> values) {
        this.schema = schema;
        this.values = values;
    }

    /**
     * Преобразует данные строки в {@link YTreeNode}, с которым удобнее иметь дело с YT
     *
     * @return YTreeNode
     */
    public YTreeNode toTreeNode() {
        final YTreeBuilder builder = YTree.mapBuilder();
        for (Map.Entry<String, Object> me : values.entrySet()) {
            Object value = me.getValue();
            if (value instanceof LocalDate) {
                value = ((LocalDate) value).format(DateTimeFormatter.ISO_LOCAL_DATE);
            } else if (value instanceof LocalDateTime) {
                value = ((LocalDateTime) value).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else if (value instanceof BigInteger || value instanceof BigDecimal) {
                value = value.toString();
            }
            builder.key(me.getKey()).value(value);
        }
        return builder.buildMap();
    }

    /**
     * Строит BinlogEvent.Row на основе данных строки
     *
     * @return BinlogEvent.Row
     */
    public BinlogEvent.Row toBinlogEventRow() {
        Map<String, Object> primaryKey = new HashMap<>();
        Map<String, Object> after = new HashMap<>();
        final int keyColumnsCount = schema.getKeyColumnsCount();

        for (Map.Entry<String, Object> me : values.entrySet()) {
            final String columnName = me.getKey();
            if (!YtReplicator.SOURCE_COLUMN_NAME.equals(columnName)) {
                if (schema.findColumn(columnName) < keyColumnsCount) {
                    primaryKey.put(columnName, me.getValue());
                } else {
                    after.put(columnName, me.getValue());
                }
            }
        }
        return new BinlogEvent.Row()
                .withRowIndex(0)
                .withPrimaryKey(primaryKey)
                .withAfter(after)
                .withBefore(Map.of())
                .validate();
    }

    /**
     * Строит список данных, который удобно передать в {@link ru.yandex.yt.ytclient.proxy.LookupRowsRequest},
     * чтобы найти строку в YT по ее ключу
     *
     * @return список значений.
     */
    public List<Object> getLookupFilter() {
        List<Object> result = new ArrayList<>();
        for (ColumnSchema columnSchema : schema.toLookup().getColumns()) {
            result.add(values.get(columnSchema.getName()));
        }
        return result;
    }

    /**
     * Строит новый TestRow instance, в который входят колонки из текущей строки, выбранные предикатом columnFilter
     *
     * @param columnFilter фильтр колонок
     * @return new TestRow instance with reduced column set
     */
    public TestRow mutate(Predicate<String> columnFilter) {
        return mutate((Map.Entry<String, Object> me) -> columnFilter.test(me.getKey()) ? me : null);
    }

    /**
     * Строит новый TestRow instance на основе текущей строки и mutator.
     *
     * @param mutator - мутатор пар имя_колонки->значение, если возвращает null, пара будет отсутствовать
     *                в новой TestRow
     * @return new TestRow instance with mutated data set
     */
    public TestRow mutate(Function<Map.Entry<String, Object>, Map.Entry<String, Object>> mutator) {
        final Map<String, Object> mutatedValues = values.entrySet().stream().map(mutator).filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new TestRow(schema, mutatedValues);
    }

    /**
     * @param column имя колонки
     * @return значение колонки
     */
    public Object getValue(String column) {
        return values.get(column);
    }

    public Map<String, Object> getValues() {
        return new HashMap<>(values);
    }

    /**
     * Строит новый TestRow instance на основе текущей строки, в которой для заданной колонки установлено
     * заданное значение.
     *
     * @param column имя колонки
     * @param value  значение
     * @return new TestRow instance with mutated data set
     */
    public TestRow setValue(String column, Object value) {
        final HashMap<String, Object> mutatedValues = new HashMap<>(values);
        mutatedValues.put(column, value);
        return new TestRow(schema, mutatedValues);
    }

    /**
     * Возращает новый TestRow instance, который хранит только primary key
     *
     * @return new TestRow instance with primary key only
     */
    public TestRow primaryKey() {
        return mutate((String column) ->
                YtReplicator.SOURCE_COLUMN_NAME.equals(column)
                        || schema.findColumn(column) < schema.getKeyColumnsCount());
    }

    /**
     * Строит новый TestRow instance на основе текущей строки и mutator.
     * Все отсутствующие колонки в новом TestRow получают значения из текущего.
     *
     * @param mutator мутатор пар имя_колонки->значение
     * @return new TestRow instance with mutated data set
     */
    public TestRow update(Function<TestRow, TestRow> mutator) {
        final TestRow updatedRow = mutator.apply(this);
        for (Map.Entry<String, Object> me : values.entrySet()) {
            updatedRow.values.putIfAbsent(me.getKey(), me.getValue());
        }
        return updatedRow;
    }
}
