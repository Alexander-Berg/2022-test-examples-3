package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.WIDGET_PARTNER_CAMPAIGNS;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class WidgetPartnerCampaignsTableChange extends BaseTableChange {

    long cid;

    public static BinlogEvent createWidgetPartnerCampaignsEvent(
            List<WidgetPartnerCampaignsTableChange> widgetPartnerCampaignsTableChanges, Operation operation) {
        BinlogEvent binlogEvent = new BinlogEvent()
                .withTable(WIDGET_PARTNER_CAMPAIGNS.getName())
                .withOperation(operation);
        List<BinlogEvent.Row> rows = widgetPartnerCampaignsTableChanges.stream()
                .map(change -> createWidgetPartnerCampaignsTableRow(change, operation)).collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createWidgetPartnerCampaignsTableRow(
            WidgetPartnerCampaignsTableChange widgetPartnerCampaignsTableChange, Operation operation) {
        Map<String, Object> primaryKeys =
                Map.of(WIDGET_PARTNER_CAMPAIGNS.CID.getName(), widgetPartnerCampaignsTableChange.cid);

        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        fillChangedInRow(before, after, widgetPartnerCampaignsTableChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public WidgetPartnerCampaignsTableChange withCid(long cid) {
        this.cid = cid;
        return this;
    }
}
