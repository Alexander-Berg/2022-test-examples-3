package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS_INTERNAL;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class CampaignsInternalTableChange extends BaseTableChange {
    long cid;

    public CampaignsInternalTableChange withCid(long cid) {
        this.cid = cid;
        return this;
    }

    public static BinlogEvent createCampaignsInternalEvent(List<CampaignsInternalTableChange> tableChanges, Operation operation) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(CAMPAIGNS_INTERNAL.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = tableChanges.stream()
                .map(tableChange -> createCampaignsInternalTableRow(tableChange, operation)).collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createCampaignsInternalTableRow(CampaignsInternalTableChange tableChange, Operation operation) {
        Map<String, Object> primaryKeys = Map.of(CAMPAIGNS_INTERNAL.CID.getName(), tableChange.cid);

        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();
        fillChangedInRow(before, after, tableChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }
}
