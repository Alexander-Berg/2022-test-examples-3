package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_BUTTONS;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class BannerButtonsChange extends BaseTableChange {
    public long bid;

    public static BinlogEvent createBannerButtonsEvent(List<BannerButtonsChange> bannerButtonsChanges,
                                                       Operation operation) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(BANNER_BUTTONS.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = bannerButtonsChanges.stream()
                .map(bannerButtonsChange -> createButtonsTableRow(bannerButtonsChange,
                        operation)).collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createButtonsTableRow(BannerButtonsChange bannerButtonsChange,
                                                         Operation operation) {
        Map<String, Object> primaryKeys = Map.of(BANNER_BUTTONS.BID.getName(), bannerButtonsChange.bid);
        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        fillChangedInRow(before, after, bannerButtonsChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public BannerButtonsChange withBid(long bid) {
        this.bid = bid;
        return this;
    }
}
