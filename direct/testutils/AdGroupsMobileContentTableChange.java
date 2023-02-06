package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.ADGROUPS_MOBILE_CONTENT;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class AdGroupsMobileContentTableChange extends BaseTableChange {
    public long pid;

    public AdGroupsMobileContentTableChange withPid(long pid) {
        this.pid = pid;
        return this;
    }

    public static BinlogEvent createAdGroupsMobileContentEvent(List<AdGroupsMobileContentTableChange> tableChanges,
                                                               Operation operation) {
        BinlogEvent binlogEvent =
                new BinlogEvent().withTable(ADGROUPS_MOBILE_CONTENT.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = tableChanges.stream()
                .map(tableChange -> createAdGroupsMobileContentTableRow(tableChange, operation))
                .collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createAdGroupsMobileContentTableRow(AdGroupsMobileContentTableChange tableChange,
                                                                       Operation operation) {
        Map<String, Object> primaryKeys = Map.of(ADGROUPS_MOBILE_CONTENT.PID.getName(), tableChange.pid);
        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        fillChangedInRow(before, after, tableChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }
}
