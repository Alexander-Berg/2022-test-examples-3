package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_LOGOS;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class BannerLogosChange extends BaseTableChange {
    public long bid;

    public static BinlogEvent createBannerLogosEvent(List<BannerLogosChange> bannerLogosChanges,
                                                     Operation operation) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(BANNER_LOGOS.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = bannerLogosChanges.stream()
                .map(bannerLogosChange -> createLogosTableRow(bannerLogosChange,
                        operation)).collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createLogosTableRow(BannerLogosChange bannerLogosChange,
                                                       Operation operation) {
        Map<String, Object> primaryKeys = Map.of(BANNER_LOGOS.BID.getName(), bannerLogosChange.bid);
        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        fillChangedInRow(before, after, bannerLogosChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public BannerLogosChange withBid(long bid) {
        this.bid = bid;
        return this;
    }
}
