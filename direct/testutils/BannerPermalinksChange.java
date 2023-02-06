package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_PERMALINKS;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class BannerPermalinksChange extends BaseTableChange {
    public Long bid;
    public Long permalinkId;
    public Long chainId = 0L;

    public static BinlogEvent createBannerPermalinksEvent(List<BannerPermalinksChange> bannerPermalinksChanges,
                                                          Operation operation) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(BANNER_PERMALINKS.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = bannerPermalinksChanges.stream()
                .map(bannerPermalinksChange -> createBannerPermalinksRow(bannerPermalinksChange, operation))
                .collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createBannerPermalinksRow(BannerPermalinksChange bannerPermalinksChange,
                                                             Operation operation) {
        Map<String, Object> primaryKeys = Map.of(
                BANNER_PERMALINKS.BID.getName(), bannerPermalinksChange.bid,
                BANNER_PERMALINKS.PERMALINK.getName(), bannerPermalinksChange.permalinkId,
                BANNER_PERMALINKS.CHAIN_ID.getName(), bannerPermalinksChange.chainId);

        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        fillChangedInRow(before, after, bannerPermalinksChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public BannerPermalinksChange withBid(Long bid) {
        this.bid = bid;
        return this;
    }

    public BannerPermalinksChange withPermalinkId(Long permalinkId) {
        this.permalinkId = permalinkId;
        return this;
    }

    public BannerPermalinksChange withChainId(Long chainId) {
        this.chainId = chainId;
        return this;
    }
}
