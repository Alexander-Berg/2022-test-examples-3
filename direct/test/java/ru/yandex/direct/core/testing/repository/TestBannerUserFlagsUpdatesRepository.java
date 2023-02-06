package ru.yandex.direct.core.testing.repository;

import java.time.LocalDateTime;

import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.type.flags.BannerUserFlagsUpdateInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.tables.BannerUserFlagsUpdates.BANNER_USER_FLAGS_UPDATES;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

public class TestBannerUserFlagsUpdatesRepository {
    @Autowired
    private DslContextProvider dslContextProvider;

    public Pair<BannerUserFlagsUpdateInfo, LocalDateTime> get(int shard, Long bannerId) {
        return dslContextProvider.ppc(shard)
                .select(BANNER_USER_FLAGS_UPDATES.BID,
                        BANNER_USER_FLAGS_UPDATES.PID,
                        BANNER_USER_FLAGS_UPDATES.CID,
                        BANNER_USER_FLAGS_UPDATES.UPDATE_TIME)
                .from(BANNER_USER_FLAGS_UPDATES)
                .where(BANNER_USER_FLAGS_UPDATES.BID.eq(bannerId))
                .fetchOne(TestBannerUserFlagsUpdatesRepository::fromResult);
    }

    static private Pair<BannerUserFlagsUpdateInfo, LocalDateTime> fromResult(Record record) {
        return ifNotNull(record,
                r -> Pair.of(new BannerUserFlagsUpdateInfo(
                        r.getValue(BANNER_USER_FLAGS_UPDATES.BID),
                        r.getValue(BANNER_USER_FLAGS_UPDATES.PID),
                        r.getValue(BANNER_USER_FLAGS_UPDATES.CID)),
                        r.getValue(BANNER_USER_FLAGS_UPDATES.UPDATE_TIME)));
    }

}
