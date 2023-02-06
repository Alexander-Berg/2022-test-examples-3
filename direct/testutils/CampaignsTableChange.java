package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsType;

import static ru.yandex.direct.binlog.model.Operation.DELETE;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class CampaignsTableChange extends BaseTableChange {
    long cid;
    long clientId;
    long rf, rfReset;
    CampaignsType type;

    public static BinlogEvent createCampaignEvent(List<CampaignsTableChange> campaignsTableChanges,
                                                  Operation operation) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(CAMPAIGNS.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = campaignsTableChanges.stream()
                .map(campaignsTableChange -> createCampaignTableRow(campaignsTableChange, operation)).collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createCampaignTableRow(CampaignsTableChange campaignsTableChange,
                                                          Operation operation) {
        Map<String, Object> primaryKeys = Map.of(CAMPAIGNS.CID.getName(), campaignsTableChange.cid);

        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        if (!operation.equals(INSERT)) {
            before.put(CAMPAIGNS.CLIENT_ID.getName(), campaignsTableChange.clientId);
            before.put(CAMPAIGNS.RF.getName(), campaignsTableChange.rf);
            before.put(CAMPAIGNS.RF_RESET.getName(), campaignsTableChange.rfReset);
            if (campaignsTableChange.type != null) {
                before.put(CAMPAIGNS.TYPE.getName(), campaignsTableChange.type.getLiteral());
            }
        }
        if (!operation.equals(DELETE)) {
            after.put(CAMPAIGNS.CLIENT_ID.getName(), campaignsTableChange.clientId);
            after.put(CAMPAIGNS.RF.getName(), campaignsTableChange.rf);
            after.put(CAMPAIGNS.RF_RESET.getName(), campaignsTableChange.rfReset);
            if (campaignsTableChange.type != null) {
                after.put(CAMPAIGNS.TYPE.getName(), campaignsTableChange.type.getLiteral());
            }
        }
        fillChangedInRow(before, after, campaignsTableChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public CampaignsTableChange withCid(long cid) {
        this.cid = cid;
        return this;
    }

    public CampaignsTableChange withClientId(long clientId) {
        this.clientId = clientId;
        return this;
    }

    public CampaignsTableChange withType(CampaignsType type) {
        this.type = type;
        return this;
    }

    public CampaignsTableChange withRf(Long rf) {
        this.rf = rf;
        return this;
    }

    public CampaignsTableChange withRfReset(Long rfReset) {
        this.rfReset = rfReset;
        return this;
    }
}
