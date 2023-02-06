package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS_MINUS_GEO;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class BannersMinusGeoTableChange extends BaseTableChange {
    public long bid;

    public static BinlogEvent createBannersMinusGeoEvent(List<BannersMinusGeoTableChange> bannersMinusGeoTableChanges,
                                                         Operation operation) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(BANNERS_MINUS_GEO.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = bannersMinusGeoTableChanges.stream()
                .map(bannersMinusGeoTableChange -> createBannersMinusGeoTableRow(bannersMinusGeoTableChange,
                        operation)
                ).collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createBannersMinusGeoTableRow(BannersMinusGeoTableChange bannersMinusGeoTableChange,
                                                                 Operation operation) {
        Map<String, Object> primaryKeys = Map.of(
                BANNERS_MINUS_GEO.BID.getName(), bannersMinusGeoTableChange.bid);
        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        fillChangedInRow(before, after, bannersMinusGeoTableChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public BannersMinusGeoTableChange withBid(long bid) {
        this.bid = bid;
        return this;
    }
}
