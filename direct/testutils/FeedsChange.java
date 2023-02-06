package ru.yandex.direct.ess.router.testutils;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.binlog.model.Operation.DELETE;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.dbschema.ppc.Tables.FEEDS;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class FeedsChange extends BaseTableChange {
    public BigInteger feedId;
    public long clientId;

    public static BinlogEvent createFeedsEvent(List<FeedsChange> feedsChanges,
                                               Operation operation) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(FEEDS.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = feedsChanges.stream()
                .map(feedsChange -> createFeedsTableRow(feedsChange, operation)).collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createFeedsTableRow(FeedsChange feedsChange,
                                                       Operation operation) {
        Map<String, Object> primaryKeys = Map.of(FEEDS.FEED_ID.getName(), feedsChange.feedId);
        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();
        if (!operation.equals(INSERT)) {
            before.put(FEEDS.CLIENT_ID.getName(), feedsChange.clientId);
        }
        if (!operation.equals(DELETE)) {
            after.put(FEEDS.CLIENT_ID.getName(), feedsChange.clientId);
        }
        fillChangedInRow(before, after, feedsChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public FeedsChange withFeedId(BigInteger feedId) {
        this.feedId = feedId;
        return this;
    }

    public FeedsChange withClientId(long clientId) {
        this.clientId = clientId;
        return this;
    }
}
