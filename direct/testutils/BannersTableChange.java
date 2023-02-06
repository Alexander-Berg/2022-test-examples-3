package ru.yandex.direct.ess.router.testutils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;

import static ru.yandex.direct.binlog.model.Operation.DELETE;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class BannersTableChange extends BaseTableChange {
    public long bid;
    public long pid;
    public long cid;
    public BannersBannerType bannerType;

    public static BinlogEvent createBannersEvent(List<BannersTableChange> bannersTableChanges,
                                                 Operation operation, LocalDateTime timestamp) {
        BinlogEvent binlogEvent = new BinlogEvent()
                .withTable(BANNERS.getName())
                .withOperation(operation)
                .withUtcTimestamp(timestamp);
        List<BinlogEvent.Row> rows = bannersTableChanges.stream()
                .map(bannersTableChange -> createBannerTableRow(bannersTableChange, operation)).collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    public static BinlogEvent createBannersEvent(List<BannersTableChange> bannersTableChanges, Operation operation) {
        return createBannersEvent(bannersTableChanges, operation, null);
    }

    private static BinlogEvent.Row createBannerTableRow(BannersTableChange bannersTableChange, Operation operation) {
        Map<String, Object> primaryKeys = Map.of(BANNERS.BID.getName(), bannersTableChange.bid);

        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        if (!operation.equals(INSERT)) {
            before.put(BANNERS.PID.getName(), bannersTableChange.pid);
            before.put(BANNERS.CID.getName(), bannersTableChange.cid);
            if (bannersTableChange.bannerType != null) {
                before.put(BANNERS.BANNER_TYPE.getName(), bannersTableChange.bannerType.getLiteral());
            }
        }
        if (!operation.equals(DELETE)) {
            after.put(BANNERS.PID.getName(), bannersTableChange.pid);
            after.put(BANNERS.CID.getName(), bannersTableChange.cid);
            if (bannersTableChange.bannerType != null) {
                after.put(BANNERS.BANNER_TYPE.getName(), bannersTableChange.bannerType.getLiteral());
            }
        }
        fillChangedInRow(before, after, bannersTableChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);

    }

    public BannersTableChange withBid(long bid) {
        this.bid = bid;
        return this;
    }

    public BannersTableChange withPid(long pid) {
        this.pid = pid;
        return this;
    }

    public BannersTableChange withCid(long cid) {
        this.cid = cid;
        return this;
    }

    public BannersTableChange withBannerType(BannersBannerType bannerType) {
        this.bannerType = bannerType;
        return this;
    }
}
