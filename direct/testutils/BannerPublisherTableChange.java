package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_PUBLISHER;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class BannerPublisherTableChange extends BaseTableChange {
    public long bid;

    public static BinlogEvent createBannerPublisherEvent(List<BannerPublisherTableChange> changes,
                                                         Operation operation) {
        BinlogEvent binlogEvent =
                new BinlogEvent().withTable(BANNER_PUBLISHER.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = changes.stream()
                .map(change -> createBannerPublisherTableRow(change, operation))
                .collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createBannerPublisherTableRow(BannerPublisherTableChange change,
                                                                 Operation operation) {
        Map<String, Object> primaryKeys = Map.of(BANNER_PUBLISHER.BID.getName(), change.bid);
        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        fillChangedInRow(before, after, change.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public BannerPublisherTableChange withBid(long bid) {
        this.bid = bid;
        return this;
    }
}
