package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_TURBOLANDINGS;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class BannerTurboLandingsChange extends BaseTableChange {
    public long bid;

    public static BinlogEvent createBannerTurboLandingsEvent(List<BannerTurboLandingsChange> changes,
                                                             Operation operation) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(BANNER_TURBOLANDINGS.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = changes.stream()
                .map(change -> createBannerTurboLandingParamsTableRow(change, operation))
                .collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createBannerTurboLandingParamsTableRow(BannerTurboLandingsChange change,
                                                                          Operation operation) {
        Map<String, Object> primaryKeys = Map.of(BANNER_TURBOLANDINGS.BID.getName(), change.bid);
        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        fillChangedInRow(before, after, change.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public BannerTurboLandingsChange withBid(long bid) {
        this.bid = bid;
        return this;
    }
}
