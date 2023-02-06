package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class CampOptionsTableChange extends BaseTableChange {
    long cid;

    public static BinlogEvent createCampOptionsEvent(List<CampOptionsTableChange> campOptionsTableChanges,
                                                     Operation operation) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(CAMP_OPTIONS.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = campOptionsTableChanges.stream()
                .map(campOptionsTableChange -> createCampOptionsTableRow(campOptionsTableChange, operation)).collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createCampOptionsTableRow(CampOptionsTableChange campOptionsTableChange,
                                                             Operation operation) {
        Map<String, Object> primaryKeys = Map.of(CAMP_OPTIONS.CID.getName(), campOptionsTableChange.cid);

        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();
        fillChangedInRow(before, after, campOptionsTableChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public CampOptionsTableChange withCid(long cid) {
        this.cid = cid;
        return this;
    }
}
