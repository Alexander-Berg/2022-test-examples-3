package ru.yandex.direct.ess.router.testutils;

import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.dbschema.ppc.Tables;

import static ru.yandex.direct.dbschema.ppc.Tables.MODERATE_BANNER_PAGES;

@ParametersAreNonnullByDefault
public final class ModerateBannerPagesTableChange extends ConvertibleToBinlogEventTableChange {

    private static final String TABLE_NAME = Tables.MODERATE_BANNER_PAGES.getName();

    public long moderateBannerPageId;
    public long bid;
    public long pid;
    public long version;

    public static BinlogEvent createEvent(List<ModerateBannerPagesTableChange> bannerImagesTableChanges,
                                          Operation operation) {
        return ConvertibleToBinlogEventTableChange.createEvent(bannerImagesTableChanges, operation, TABLE_NAME);
    }

    public ModerateBannerPagesTableChange withModerateBannerPageId(long moderateBannerPageId) {
        this.moderateBannerPageId = moderateBannerPageId;
        return this;
    }

    public ModerateBannerPagesTableChange withBid(long bid) {
        this.bid = bid;
        return this;
    }

    public ModerateBannerPagesTableChange withPid(long pid) {
        this.pid = pid;
        return this;
    }

    public ModerateBannerPagesTableChange withVersion(long version) {
        this.version = version;
        return this;
    }

    @Override
    protected Map<String, Object> getPrimaryKeys() {
        return Map.of(MODERATE_BANNER_PAGES.MODERATE_BANNER_PAGE_ID.getName(), moderateBannerPageId);
    }

    @Override
    protected void fillChangeToMap(Map<String, Object> dest) {
        dest.put(MODERATE_BANNER_PAGES.BID.getName(), bid);
        dest.put(MODERATE_BANNER_PAGES.PAGE_ID.getName(), pid);
        dest.put(MODERATE_BANNER_PAGES.VERSION.getName(), version);
    }

}
