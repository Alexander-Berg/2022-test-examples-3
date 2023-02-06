package ru.yandex.direct.core.testing.repository;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_MODERATION_VERSIONS;

@Repository
public class TestBannerModerationVersionsRepository {

    private final DslContextProvider dslContextProvider;

    @Autowired
    public TestBannerModerationVersionsRepository(DslContextProvider contextProvider) {
        this.dslContextProvider = contextProvider;
    }

    public void addVersion(int shard, long bannerId, long version) {
        dslContextProvider.ppc(shard)
                .insertInto(BANNER_MODERATION_VERSIONS)
                .set(BANNER_MODERATION_VERSIONS.BID, bannerId)
                .set(BANNER_MODERATION_VERSIONS.VERSION, version)
                .execute();
    }

    public Map<Long, Long> getVersion(int shard, Collection<Long> bannerIds) {
        return dslContextProvider.ppc(shard)
                .select(BANNER_MODERATION_VERSIONS.BID, BANNER_MODERATION_VERSIONS.VERSION)
                .from(BANNER_MODERATION_VERSIONS)
                .where(BANNER_MODERATION_VERSIONS.BID.in(bannerIds))
                .fetchMap(BANNER_MODERATION_VERSIONS.BID, BANNER_MODERATION_VERSIONS.VERSION);
    }
}
