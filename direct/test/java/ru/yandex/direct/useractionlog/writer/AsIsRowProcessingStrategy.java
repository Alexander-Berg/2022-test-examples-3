package ru.yandex.direct.useractionlog.writer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;

import ru.yandex.direct.binlog.reader.EnrichedDeleteRow;
import ru.yandex.direct.binlog.reader.EnrichedInsertRow;
import ru.yandex.direct.binlog.reader.EnrichedRow;
import ru.yandex.direct.binlog.reader.EnrichedUpdateRow;
import ru.yandex.direct.binlogclickhouse.schema.FieldValueList;
import ru.yandex.direct.useractionlog.ClientId;
import ru.yandex.direct.useractionlog.dict.DictDataCategory;
import ru.yandex.direct.useractionlog.dict.DictResponsesAccessor;
import ru.yandex.direct.useractionlog.dict.FreshDictValuesFiller;
import ru.yandex.direct.useractionlog.schema.ActionLogRecord;
import ru.yandex.direct.useractionlog.schema.ObjectPath;
import ru.yandex.direct.useractionlog.schema.RecordSource;
import ru.yandex.direct.useractionlog.writer.generator.DictFiller;
import ru.yandex.direct.useractionlog.writer.generator.RowProcessingStrategy;

/**
 * Тестовая стратегия, которая переносит данные как есть.
 */
@ParametersAreNonnullByDefault
public class AsIsRowProcessingStrategy implements RowProcessingStrategy {
    private final RecordSource recordSource;

    public AsIsRowProcessingStrategy(RecordSource recordSource) {
        this.recordSource = recordSource;
    }

    @Nonnull
    @Override
    public List<ActionLogRecord> processEvent(EnrichedRow row, DictResponsesAccessor dictResponsesAccessor) {
        ActionLogRecord.Builder builder = ActionLogRecord.builderFrom(row, recordSource)
                .withPath(new ObjectPath.ClientPath(new ClientId(0)));
        if (row instanceof EnrichedInsertRow) {
            builder.withNewFields(FieldValueList.fromColumnDataList(((EnrichedInsertRow) row).getFields()));
        } else if (row instanceof EnrichedUpdateRow) {
            builder.withOldFields(FieldValueList.fromColumnDataList(((EnrichedUpdateRow) row).getFields().getBefore()));
            builder.withNewFields(FieldValueList.fromColumnDataList(((EnrichedUpdateRow) row).getFields().getAfter()));
        } else {
            builder.withOldFields(FieldValueList.fromColumnDataList(((EnrichedDeleteRow) row).getFields()));
        }
        return ImmutableList.of(builder.build());
    }

    @Override
    public void fillFreshDictValues(EnrichedRow row, DictResponsesAccessor dictData,
                                    FreshDictValuesFiller freshDictValues) {
        // no need
    }

    @Override
    public Collection<DictDataCategory> provides() {
        return Collections.emptyList();
    }

    @Override
    public DictFiller makePureDictFiller() {
        return this;
    }
}
