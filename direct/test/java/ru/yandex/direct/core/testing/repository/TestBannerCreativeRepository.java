package ru.yandex.direct.core.testing.repository;

import java.util.Collection;
import java.util.List;

import org.jooq.Record1;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreative;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithCreative;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.dbschema.ppc.enums.BannersPerformanceStatusmoderate;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.read.JooqReaderWithSupplier;
import ru.yandex.direct.jooqmapper.read.JooqReaderWithSupplierBuilder;

import static ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate.NEW;
import static ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate.toSource;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS_PERFORMANCE;
import static ru.yandex.direct.jooqmapper.read.ReaderBuilders.fromField;
import static ru.yandex.direct.utils.CommonUtils.nvl;

public class TestBannerCreativeRepository {

    private static final JooqReaderWithSupplier<OldBannerCreative> READER =
            JooqReaderWithSupplierBuilder.builder(OldBannerCreative::new)
                    .readProperty(OldBannerCreative.ID, fromField(BANNERS_PERFORMANCE.BANNER_CREATIVE_ID))
                    .readProperty(OldBannerCreative.CREATIVE_ID, fromField(BANNERS_PERFORMANCE.CREATIVE_ID))
                    .readProperty(OldBannerCreative.BANNER_ID, fromField(BANNERS_PERFORMANCE.BID))
                    .readProperty(OldBannerCreative.AD_GROUP_ID, fromField(BANNERS_PERFORMANCE.PID))
                    .readProperty(OldBannerCreative.CAMPAIGN_ID, fromField(BANNERS_PERFORMANCE.CID))
                    .readProperty(OldBannerCreative.STATUS_MODERATE, fromField(BANNERS_PERFORMANCE.STATUS_MODERATE)
                            .by(OldBannerCreativeStatusModerate::fromSource))
                    .readProperty(OldBannerCreative.EXTRACTED_TEXT, fromField(BANNERS_PERFORMANCE.EXTRACTED_TEXT))
                    .build();

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ShardHelper shardHelper;

    /**
     * Удаляет креативы по списку ID
     *
     * @param shard       Шард
     * @param creativeIds Список ID креативов
     */
    public void deleteByCreativeIds(int shard, Long... creativeIds) {
        dslContextProvider.ppc(shard).deleteFrom(BANNERS_PERFORMANCE)
                .where(BANNERS_PERFORMANCE.CREATIVE_ID.in(creativeIds))
                .execute();
    }

    /**
     * Обновляет статус модерации для креативов переданных bannerIds
     *
     * @param shard     шард
     * @param bannerIds список id баннеров
     */
    public void updateStatusModerate(int shard, Collection<Long> bannerIds, OldBannerCreativeStatusModerate status) {
        if (bannerIds.isEmpty()) {
            return;
        }
        dslContextProvider.ppc(shard)
                .update(BANNERS_PERFORMANCE)
                .set(BANNERS_PERFORMANCE.STATUS_MODERATE, toSource(status))
                .where(BANNERS_PERFORMANCE.BID.in(bannerIds))
                .execute();
    }

    /**
     * Создать связку баннера с креативом
     *
     * @param bannerInfo Информация о баннере, включая ID баннера и ID кампании
     * @param creativeId ID креатива
     * @return ID созданной связки
     */
    public long linkBannerWithCreative(AbstractBannerInfo<? extends OldBannerWithCreative> bannerInfo, long creativeId) {
        int shard = bannerInfo.getShard();

        long bannerCreativeId = shardHelper.generateBannerCreativeIds(1).get(0);

        dslContextProvider.ppc(shard)
                .insertInto(BANNERS_PERFORMANCE,
                        BANNERS_PERFORMANCE.BANNER_CREATIVE_ID,
                        BANNERS_PERFORMANCE.CREATIVE_ID,
                        BANNERS_PERFORMANCE.BID,
                        BANNERS_PERFORMANCE.CID,
                        BANNERS_PERFORMANCE.PID,
                        BANNERS_PERFORMANCE.STATUS_MODERATE
                )
                .values(bannerCreativeId,
                        creativeId,
                        bannerInfo.getBannerId(),
                        bannerInfo.getCampaignId(),
                        bannerInfo.getAdGroupId(),
                        toSource(nvl(bannerInfo.getBanner().getCreativeStatusModerate(), NEW)))
                .execute();

        bannerInfo.getBanner().withCreativeId(creativeId);

        return bannerCreativeId;
    }

    /**
     * Возвращает список креативов для баннеров
     *
     * @param shard     Шард
     * @param bannerIds Список ID баннеров
     * @return Список креативов
     */
    public List<OldBannerCreative> getByBannerIds(int shard, Collection<Long> bannerIds) {
        return dslContextProvider.ppc(shard)
                .select(READER.getFieldsToRead())
                .from(BANNERS_PERFORMANCE)
                .where(BANNERS_PERFORMANCE.BID.in(bannerIds))
                .fetch()
                .map(READER::fromDb);
    }

    public BannersPerformanceStatusmoderate getBannerPerformanceStatus(int shard, long bannerId) {
        return dslContextProvider.ppc(shard)
                .select(BANNERS_PERFORMANCE.STATUS_MODERATE)
                .from(BANNERS_PERFORMANCE)
                .where(BANNERS_PERFORMANCE.BID.eq(bannerId))
                .fetchOne(Record1::value1);
    }
}
