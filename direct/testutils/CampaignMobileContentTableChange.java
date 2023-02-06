package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS_MOBILE_CONTENT;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class CampaignMobileContentTableChange extends BaseTableChange {
    long cid;

    public CampaignMobileContentTableChange withCid(long cid) {
        this.cid = cid;
        return this;
    }

    public static BinlogEvent createCampaignsMobileContentEvent(List<CampaignMobileContentTableChange> changes,
                                                                Operation operation) {
        BinlogEvent binlogEvent =
                new BinlogEvent().withTable(CAMPAIGNS_MOBILE_CONTENT.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = changes.stream()
                .map(change -> createCampaignsMobileContentTableRow(change, operation))
                .collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }


    private static BinlogEvent.Row createCampaignsMobileContentTableRow(CampaignMobileContentTableChange change,
                                                                        Operation operation) {
        Map<String, Object> primaryKeys = Map.of(CAMPAIGNS_MOBILE_CONTENT.CID.getName(), change.cid);

        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();
        fillChangedInRow(before, after, change.getChangedColumns(), operation);

        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }
}
