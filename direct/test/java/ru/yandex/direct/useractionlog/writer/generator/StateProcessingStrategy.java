package ru.yandex.direct.useractionlog.writer.generator;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;

import ru.yandex.direct.binlog.reader.EnrichedDeleteRow;
import ru.yandex.direct.binlog.reader.EnrichedInsertRow;
import ru.yandex.direct.binlog.reader.EnrichedRow;
import ru.yandex.direct.binlog.reader.EnrichedUpdateRow;
import ru.yandex.direct.binlog.reader.MySQLSimpleRowIndexed;
import ru.yandex.direct.binlogclickhouse.schema.FieldValueList;
import ru.yandex.direct.useractionlog.ClientId;
import ru.yandex.direct.useractionlog.dict.DictDataCategory;
import ru.yandex.direct.useractionlog.dict.DictRequestsFiller;
import ru.yandex.direct.useractionlog.dict.DictResponsesAccessor;
import ru.yandex.direct.useractionlog.dict.FreshDictValuesFiller;
import ru.yandex.direct.useractionlog.schema.ActionLogRecord;
import ru.yandex.direct.useractionlog.schema.ObjectPath;
import ru.yandex.direct.useractionlog.schema.Operation;
import ru.yandex.direct.useractionlog.schema.RecordSource;

/**
 * TODO
 */
@ParametersAreNonnullByDefault
public class StateProcessingStrategy implements RowProcessingStrategy {
    private final DictDataCategory dictDataCategory;
    private final String idField;
    private final String rootTableName;
    private final String valueField;
    private final RecordSource recordSource;

    private StateProcessingStrategy(DictDataCategory dictDataCategory, String rootTableName, String idField,
                                    String valueField, RecordSource recordSource) {
        this.dictDataCategory = dictDataCategory;
        this.idField = idField;
        this.rootTableName = rootTableName;
        this.valueField = valueField;
        this.recordSource = recordSource;
    }

    private boolean isInitRecord(EnrichedRow row) {
        return row.getTableName().equals(rootTableName) && row instanceof EnrichedInsertRow;
    }

    @Override
    public void fillDictRequests(EnrichedRow row, DictRequestsFiller dictRequests) {
        if (!isInitRecord(row)) {
            dictRequests.require(dictDataCategory, extractId(row));
        }
    }

    @Override
    public void fillFreshDictValues(EnrichedRow row, DictResponsesAccessor dictData,
                                    FreshDictValuesFiller freshDictValues) {
        String data;
        if (isInitRecord(row)) {
            data = "";
        } else {
            SortedMap<String, String> result = deserialize((String) dictData.get(dictDataCategory, extractId(row)));
            updateData(row, result);
            data = serialize(result);
        }
        freshDictValues.add(dictDataCategory, extractId(row), data);
    }

    private void updateData(EnrichedRow row, SortedMap<String, String> result) {
        if (row instanceof EnrichedDeleteRow) {
            result.remove(row.getTableName());
        } else {
            MySQLSimpleRowIndexed tuple;
            if (row instanceof EnrichedInsertRow) {
                tuple = ((EnrichedInsertRow) row).getFields();
            } else if (row instanceof EnrichedUpdateRow) {
                tuple = ((EnrichedUpdateRow) row).getFields().getAfter();
            } else {
                throw new IllegalStateException("Can't handle " + row.getClass().getCanonicalName());
            }
            result.put(row.getTableName(), tuple.getByName(valueField).getValueAsString());
        }
    }

    private String serialize(SortedMap<String, String> data) {
        return data.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(" "));
    }

    private SortedMap<String, String> deserialize(String str) {
        try {
            return Stream.of(
                    str.split(" "))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(kv -> kv.split("="))
                    .collect(Collectors.toMap(
                            kv -> kv[0],
                            kv -> kv[1],
                            (o, n) -> {
                                throw new IllegalStateException(o + "->" + n);
                            },
                            TreeMap::new));
        } catch (RuntimeException e) {
            throw new IllegalStateException(str, e);
        }
    }

    @Nonnull
    @Override
    public List<ActionLogRecord> processEvent(EnrichedRow row, DictResponsesAccessor dictResponsesAccessor) {
        if (!row.getTableName().equals(rootTableName)) {
            SortedMap<String, String> value =
                    deserialize((String) dictResponsesAccessor.get(dictDataCategory, extractId(row)));
            ActionLogRecord.Builder builder = ActionLogRecord.builderFrom(row, recordSource)
                    .withPath(new ObjectPath.ClientPath(new ClientId(extractId(row))))
                    .withOldFields(FieldValueList.empty())
                    .withNewFields(FieldValueList.empty());
            builder.withOperation(Operation.UPDATE);
            if (row instanceof EnrichedInsertRow && value.isEmpty()) {
                builder.withOperation(Operation.INSERT);
            }
            updateData(row, value);
            if (row instanceof EnrichedDeleteRow && value.isEmpty()) {
                builder.withOperation(Operation.DELETE);
            } else {
                builder.withNewFields(FieldValueList.zip(
                        Arrays.asList(idField, valueField),
                        Arrays.asList(Long.toString(extractId(row)), serialize(value))));
            }
            return Collections.singletonList(builder.build());
        } else {
            return Collections.emptyList();
        }
    }

    private long extractId(EnrichedRow row) {
        return Util.fieldAsLong(Util.dataForGettingId(row), idField);
    }

    @Override
    public Collection<DictDataCategory> provides() {
        return ImmutableList.of(dictDataCategory);
    }

    @Override
    public DictFiller makePureDictFiller() {
        return this;
    }

    public static class Builder {
        private DictDataCategory dictDataCategory;
        private String rootTableName;
        private String idField;
        private String valueField;
        private RecordSource recordSource;

        public Builder setDictDataCategory(DictDataCategory dictDataCategory) {
            this.dictDataCategory = dictDataCategory;
            return this;
        }

        public Builder setRootTableName(String initTableName) {
            this.rootTableName = initTableName;
            return this;
        }

        public Builder setIdField(String idField) {
            this.idField = idField;
            return this;
        }

        public Builder setValueField(String valueField) {
            this.valueField = valueField;
            return this;
        }

        public Builder setRecordSource(RecordSource recordSource) {
            this.recordSource = recordSource;
            return this;
        }

        public StateProcessingStrategy build() {
            return new StateProcessingStrategy(
                    Objects.requireNonNull(dictDataCategory, "Forgotten dictDataCategory"),
                    Objects.requireNonNull(rootTableName, "Forgotten rootTableName"),
                    Objects.requireNonNull(idField, "Forgotten idField"),
                    Objects.requireNonNull(valueField, "Forgotten valueField"),
                    Objects.requireNonNull(recordSource, "Forgotten recordSource"));
        }
    }
}
