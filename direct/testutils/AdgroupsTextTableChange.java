package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.binlog.model.Operation.DELETE;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.dbschema.ppc.Tables.ADGROUPS_TEXT;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

@ParametersAreNonnullByDefault
public class AdgroupsTextTableChange extends BaseTableChange {
    public long pid;
    @Nullable
    public Long feedId;

    public static BinlogEvent createAdgroupsTextEvent(List<AdgroupsTextTableChange> tableChanges,
                                                      Operation operation) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(ADGROUPS_TEXT.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = tableChanges.stream()
                .map(tableChange -> createAdgroupsTextTableRow(tableChange, operation)).collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createAdgroupsTextTableRow(AdgroupsTextTableChange tableChange,
                                                              Operation operation) {
        Map<String, Object> primaryKeys = Map.of(ADGROUPS_TEXT.PID.getName(), tableChange.pid);

        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        if (!operation.equals(INSERT)) {
            before.put(ADGROUPS_TEXT.PID.getName(), tableChange.pid);
        }
        if (!operation.equals(DELETE)) {
            after.put(ADGROUPS_TEXT.PID.getName(), tableChange.pid);
        }
        if (tableChange.feedId != null) {
            after.put(ADGROUPS_TEXT.FEED_ID.getName(), tableChange.feedId);
        }
        fillChangedInRow(before, after, tableChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public AdgroupsTextTableChange withPid(long pid) {
        this.pid = pid;
        return this;

    }

    public AdgroupsTextTableChange withFeedId(@Nullable Long feedId) {
        this.feedId = feedId;
        return this;
    }
}
