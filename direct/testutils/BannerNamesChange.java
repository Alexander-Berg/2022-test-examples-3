package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_NAMES;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class BannerNamesChange extends BaseTableChange {

    public long bid;

    public static BinlogEvent createBannerNamesEvent(List<BannerNamesChange> bannerNamesChanges,
                                                     Operation operation) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(BANNER_NAMES.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = bannerNamesChanges.stream()
                .map(bannerNamesChange -> createNamesTableRow(bannerNamesChange,
                        operation)).collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createNamesTableRow(BannerNamesChange bannerNamesChange,
                                                       Operation operation) {
        Map<String, Object> primaryKeys = Map.of(BANNER_NAMES.BID.getName(), bannerNamesChange.bid);
        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        fillChangedInRow(before, after, bannerNamesChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public BannerNamesChange withBid(long bid) {
        this.bid = bid;
        return this;
    }
}
