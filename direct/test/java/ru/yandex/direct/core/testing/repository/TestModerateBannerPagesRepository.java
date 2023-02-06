package ru.yandex.direct.core.testing.repository;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.core.entity.banner.model.ModerateBannerPage;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplier;
import ru.yandex.direct.jooqmapperhelper.InsertHelper;

import static ru.yandex.direct.core.entity.banner.repository.ModerateBannerPagesRepository.createModerateBannerPageMapper;
import static ru.yandex.direct.dbschema.ppc.Tables.MODERATE_BANNER_PAGES;

@Repository
public class TestModerateBannerPagesRepository {

    private final DslContextProvider dslContextProvider;

    private final ShardHelper shardHelper;

    private final JooqMapperWithSupplier<ModerateBannerPage> moderateBannerPageMapper =
            createModerateBannerPageMapper();

    @Autowired
    public TestModerateBannerPagesRepository(DslContextProvider contextProvider, ShardHelper shardHelper) {
        this.dslContextProvider = contextProvider;
        this.shardHelper = shardHelper;
    }

    public void addModerateBannerPages(int shard, Collection<ModerateBannerPage> moderateBannerPages) {
        Iterator<Long> ids = shardHelper.generateModerateBannerPageIds(moderateBannerPages.size()).iterator();
        moderateBannerPages.forEach(moderateBannerPage -> {
            moderateBannerPage.setId(ids.next());
        });

        new InsertHelper<>(dslContextProvider.ppc(shard), MODERATE_BANNER_PAGES)
                .addAll(moderateBannerPageMapper, moderateBannerPages)
                .executeIfRecordsAdded();
    }

    public List<ModerateBannerPage> getModerateBannerPages(int shard, long bannerId) {
        return dslContextProvider.ppc(shard)
                .select(moderateBannerPageMapper.getFieldsToRead())
                .from(MODERATE_BANNER_PAGES)
                .where(MODERATE_BANNER_PAGES.BID.eq(bannerId))
                .orderBy(MODERATE_BANNER_PAGES.PAGE_ID)
                .fetch()
                .map(moderateBannerPageMapper::fromDb);
    }

    /**
     * Получить вердикт модерации баннера по bannerPageId.
     */
    public ModerateBannerPage getModerateBannerPage(int shard, Long bannerPageIds) {
        return dslContextProvider.ppc(shard)
                .select(moderateBannerPageMapper.getFieldsToRead())
                .from(MODERATE_BANNER_PAGES)
                .where(MODERATE_BANNER_PAGES.MODERATE_BANNER_PAGE_ID.eq(bannerPageIds))
                .fetchOne(moderateBannerPageMapper::fromDb);
    }
}
