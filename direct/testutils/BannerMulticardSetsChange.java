package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_MULTICARD_SETS;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

@ParametersAreNonnullByDefault
public class BannerMulticardSetsChange extends BaseTableChange {

    public Long bid;

    public BannerMulticardSetsChange(Long bid) {
        this.bid = bid;
    }

    public static BinlogEvent createBannerMulticardSetsBinlogEvent(
            List<BannerMulticardSetsChange> bannerMulticardSetsChangeList,
            Operation operation
    ) {
        BinlogEvent event = new BinlogEvent()
                .withTable(BANNER_MULTICARD_SETS.getName())
                .withOperation(operation);
        var rows = bannerMulticardSetsChangeList.stream()
                .map(change -> createTableRow(change, operation))
                .collect(Collectors.toList());
        event.withRows(rows);
        return event;
    }

    private static BinlogEvent.Row createTableRow(BannerMulticardSetsChange change, Operation operation) {
        Map<String, Object> primaryKeys = Map.of(BANNER_MULTICARD_SETS.BID.getName(), change.bid);

        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();
        fillChangedInRow(before, after, change.getChangedColumns(), operation);

        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

}
