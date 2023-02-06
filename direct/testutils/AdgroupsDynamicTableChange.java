package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.binlog.model.Operation.DELETE;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.dbschema.ppc.Tables.ADGROUPS_DYNAMIC;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class AdgroupsDynamicTableChange extends BaseTableChange {
    public long pid;

    public static BinlogEvent createAdgroupsDynamicEvent(List<AdgroupsDynamicTableChange> tableChanges,
                                                         Operation operation) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(ADGROUPS_DYNAMIC.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = tableChanges.stream()
                .map(tableChange -> createAdgroupsDynamicTableRow(tableChange, operation)).collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createAdgroupsDynamicTableRow(AdgroupsDynamicTableChange tableChange,
                                                                 Operation operation) {
        Map<String, Object> primaryKeys = Map.of(ADGROUPS_DYNAMIC.PID.getName(), tableChange.pid);

        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        if (!operation.equals(INSERT)) {
            before.put(ADGROUPS_DYNAMIC.PID.getName(), tableChange.pid);
        }
        if (!operation.equals(DELETE)) {
            after.put(ADGROUPS_DYNAMIC.PID.getName(), tableChange.pid);
        }
        fillChangedInRow(before, after, tableChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public AdgroupsDynamicTableChange withPid(long pid) {
        this.pid = pid;
        return this;
    }
}
