package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_MULTICARDS;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

@ParametersAreNonnullByDefault
public class BannerMulticardsChange extends BaseTableChange {

    public Long multicardId;

    public BannerMulticardsChange(Long multicardId) {
        this.multicardId = multicardId;
    }

    public static BinlogEvent createBannerMulticardsBinlogEvent(List<BannerMulticardsChange> bannerMulticardsChangeList,
                                                Operation operation) {
        BinlogEvent event = new BinlogEvent()
                .withTable(BANNER_MULTICARDS.getName())
                .withOperation(operation);
        var rows = bannerMulticardsChangeList.stream()
                .map(change -> createTableRow(change, operation))
                .collect(Collectors.toList());
        event.withRows(rows);
        return event;
    }

    private static BinlogEvent.Row createTableRow(BannerMulticardsChange change, Operation operation) {
        Map<String, Object> primaryKeys = Map.of(BANNER_MULTICARDS.MULTICARD_ID.getName(), change.multicardId);

        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();
        fillChangedInRow(before, after, change.getChangedColumns(), operation);

        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

}
