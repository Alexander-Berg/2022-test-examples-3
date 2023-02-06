package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS_PERFORMANCE;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class BannersPerformanceTableChange extends BaseTableChange {
    public long bannerCreativeId;
    public long bid;

    public static BinlogEvent createBannersPerformanceEvent(
            List<BannersPerformanceTableChange> bannersPerformanceTableChanges,
            Operation operation
    ) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(BANNERS_PERFORMANCE.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = bannersPerformanceTableChanges.stream()
                .map(bannersPerformanceTableChange ->
                        createBannersPerformanceTableRow(bannersPerformanceTableChange, operation))
                .collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createBannersPerformanceTableRow(
            BannersPerformanceTableChange bannersPerformanceTableChange,
            Operation operation
    ) {
        Map<String, Object> primaryKeys = Map.of(
                BANNERS_PERFORMANCE.BANNER_CREATIVE_ID.getName(), bannersPerformanceTableChange.bannerCreativeId);
        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        after.put(BANNERS.BID.getName(), bannersPerformanceTableChange.bid);

        fillChangedInRow(before, after, bannersPerformanceTableChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public BannersPerformanceTableChange withBannerCreativeId(long bannerCreativeId) {
        this.bannerCreativeId = bannerCreativeId;
        return this;
    }

    public BannersPerformanceTableChange withBid(long bid) {
        this.bid = bid;
        return this;
    }
}
