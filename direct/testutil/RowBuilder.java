package ru.yandex.direct.ytwrapper.dynamic.testutil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooq.Field;

import ru.yandex.yt.ytclient.tables.ColumnSchema;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.wire.UnversionedRow;
import ru.yandex.yt.ytclient.wire.UnversionedValue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Builder для удобного описания строк при создании {@link ru.yandex.yt.ytclient.wire.UnversionedRowset}
 * с помощью {@link RowsetBuilder}
 */
public class RowBuilder {
    private List<Object> values = new ArrayList<>();
    private List<ColumnSchema> columnSchemaList = new ArrayList<>();

    public static RowBuilder rowBuilder() {
        return new RowBuilder();
    }

    public List<ColumnSchema> getColumnSchemaList() {
        return columnSchemaList;
    }

    public UnversionedRow build(List<ColumnSchema> externalColumnSchema) {
        Map<String, Integer> extColIdxByName = new HashMap<>();
        for (int i = 0; i < externalColumnSchema.size(); i++) {
            extColIdxByName.put(externalColumnSchema.get(i).getName(), i);
        }
        List<UnversionedValue> unversionedValues = new ArrayList<>(externalColumnSchema.size());
        for (int i = 0; i < externalColumnSchema.size(); i++) {
            unversionedValues.add(null);
        }
        for (int i = 0; i < values.size(); i++) {
            ColumnSchema columnSchema = columnSchemaList.get(i);
            Object value = values.get(i);
            Integer extColIdx = extColIdxByName.get(columnSchema.getName());
            checkNotNull(extColIdx);

            ColumnValueType type = externalColumnSchema.get(extColIdx).getType();
            Object convertedValue = UnversionedValue.convertValueTo(value, type);
            unversionedValues.set(extColIdx, new UnversionedValue(extColIdx, type, false, convertedValue));
        }

        return new UnversionedRow(unversionedValues);
    }

    public RowBuilder withColValue(Field<?> field, Object value) {
        return withColValue(field.getName(), value);
    }

    public RowBuilder withColValue(String name, Object value) {
        if (value == null) {
            return this;
        }
        ColumnValueType detectedType = detectType(value);

        return withColValue(name, detectedType, value);
    }

    public RowBuilder withColValue(String name, ColumnValueType type, Object value) {
        if (value == null) {
            return this;
        }
        values.add(value);
        columnSchemaList.add(new ColumnSchema(name, type));
        return this;
    }

    private static ColumnValueType detectType(Object value) {
        checkNotNull(value);
        Class<?> valueType = value.getClass();
        if (String.class.isAssignableFrom(valueType)) {
            return ColumnValueType.STRING;
        } else if (Long.class.isAssignableFrom(valueType) || Integer.class.isAssignableFrom(valueType)) {
            return ColumnValueType.INT64;
        } else if (Boolean.class.isAssignableFrom(valueType)) {
            return ColumnValueType.BOOLEAN;
        } else if (Double.class.isAssignableFrom(valueType) || Float.class.isAssignableFrom(valueType)) {
            return ColumnValueType.DOUBLE;
        } else {
            throw new IllegalArgumentException(
                    String.format("Value type %s is not supported in this builder", valueType));
        }
    }
}
