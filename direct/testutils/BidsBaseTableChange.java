package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.binlog.model.Operation.DELETE;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_BASE;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class BidsBaseTableChange extends BaseTableChange {
    public long bidId;
    public long pid;

    public static BinlogEvent createBidsBaseEvent(List<BidsBaseTableChange> bidsBaseTableChanges,
                                                  Operation operation) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(BIDS_BASE.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = bidsBaseTableChanges.stream()
                .map(bidsBaseTableChange -> createBidsBaseTableRow(bidsBaseTableChange, operation)

                ).collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createBidsBaseTableRow(BidsBaseTableChange bidsBaseTableChange,
                                                          Operation operation) {
        Map<String, Object> primaryKeys = Map.of(
                BIDS_BASE.BID_ID.getName(), bidsBaseTableChange.bidId);
        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        if (!operation.equals(INSERT)) {
            before.put(BIDS_BASE.PID.getName(), bidsBaseTableChange.pid);
        }

        if (!operation.equals(DELETE)) {
            after.put(BIDS_BASE.PID.getName(), bidsBaseTableChange.pid);
        }
        fillChangedInRow(before, after, bidsBaseTableChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public BidsBaseTableChange withBidId(long bidId) {
        this.bidId = bidId;
        return this;
    }

    public BidsBaseTableChange withPid(long pid) {
        this.pid = pid;
        return this;
    }
}
