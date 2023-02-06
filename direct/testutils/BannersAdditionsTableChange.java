package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS_ADDITIONS;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class BannersAdditionsTableChange extends BaseTableChange {
    public long additionsItemId;

    public static BinlogEvent createBannersAdditionsEvent(List<BannersAdditionsTableChange> bannersAdditionsTableChanges,
                                                          Operation operation) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(BANNERS_ADDITIONS.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = bannersAdditionsTableChanges.stream()
                .map(bannersAdditionsTableChange -> createBannersAdditionsTableRow(bannersAdditionsTableChange,
                        operation)
                ).collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createBannersAdditionsTableRow(BannersAdditionsTableChange bannersAdditionsTableChange,
                                                                  Operation operation) {
        Map<String, Object> primaryKeys = Map.of(BANNERS_ADDITIONS.ADDITIONS_ITEM_ID.getName(),
                bannersAdditionsTableChange.additionsItemId);
        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        fillChangedInRow(before, after, bannersAdditionsTableChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public BannersAdditionsTableChange withAdditionsItemId(long additionsItemId) {
        this.additionsItemId = additionsItemId;
        return this;
    }
}
