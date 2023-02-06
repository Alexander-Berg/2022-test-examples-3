package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_MEASURERS;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class BannerMeasurersTableChange extends BaseTableChange {
    public long bid;

    public static BinlogEvent createBannersMeasurersEvent(
            List<BannerMeasurersTableChange> bannerMeasurersTableChanges,
            Operation operation
    ) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(BANNER_MEASURERS.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = bannerMeasurersTableChanges.stream()
                .map(bannersPerformanceTableChange ->
                        createBannerMeasurersTableRow(bannersPerformanceTableChange, operation))
                .collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createBannerMeasurersTableRow(
            BannerMeasurersTableChange bannerMeasurersTableChange,
            Operation operation
    ) {
        Map<String, Object> primaryKeys = Map.of(BANNER_MEASURERS.BID.getName(), bannerMeasurersTableChange.bid);
        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        fillChangedInRow(before, after, bannerMeasurersTableChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public BannerMeasurersTableChange withBid(long bid) {
        this.bid = bid;
        return this;
    }
}
