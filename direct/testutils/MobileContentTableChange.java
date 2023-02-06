package ru.yandex.direct.ess.router.testutils;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.MOBILE_CONTENT;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class MobileContentTableChange extends BaseTableChange {
    BigInteger mobileContentId;

    public MobileContentTableChange withMobileContentId(BigInteger mobileContentId) {
        this.mobileContentId = mobileContentId;
        return this;
    }

    public static BinlogEvent createMobileContentEvent(List<MobileContentTableChange> changes,
                                                       Operation operation) {
        BinlogEvent binlogEvent =
                new BinlogEvent().withTable(MOBILE_CONTENT.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = changes.stream()
                .map(change -> createMobileContentTableRow(change, operation))
                .collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }


    private static BinlogEvent.Row createMobileContentTableRow(MobileContentTableChange change,
                                                               Operation operation) {
        Map<String, Object> primaryKeys = Map.of(MOBILE_CONTENT.MOBILE_CONTENT_ID.getName(), change.mobileContentId);

        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();
        fillChangedInRow(before, after, change.getChangedColumns(), operation);

        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }
}
