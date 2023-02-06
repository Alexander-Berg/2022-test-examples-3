package ru.yandex.direct.core.entity.banner.type.creative.moderation;

import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithCreative;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.type.BannerWithChildrenModerationUpdatePositiveTestBase;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS_PERFORMANCE;

public abstract class BannerWithCreativeModerationUpdatePositiveTestBase
        extends BannerWithChildrenModerationUpdatePositiveTestBase<BannerWithCreative> {

    @Parameterized.Parameter(6)
    public BannerCreativeStatusModerate creativeStatusModerate;

    @Parameterized.Parameter(7)
    public BannerCreativeStatusModerate expectedCreativeStatusModerate;

    @Override
    protected void checkBanner(Long bannerId) {
        BannerWithCreative actualBanner = getBanner(bannerId);
        assertThat(actualBanner.getCreativeStatusModerate(), equalTo(expectedCreativeStatusModerate));
    }

    protected static ModelChanges<BannerWithSystemFields> getModelChanges(
            Class<? extends BannerWithCreative> bannerClass, Long bannerId, Long creativeId) {
        return new ModelChanges<>(bannerId, bannerClass)
                .process(creativeId, BannerWithCreative.CREATIVE_ID)
                .castModelUp(BannerWithSystemFields.class);
    }

    @Override
    protected void setBannerChildrenModerationStatus(Long bannerId) {
        dslContextProvider.ppc(defaultClient.getShard())
                .update(BANNERS_PERFORMANCE)
                .set(BANNERS_PERFORMANCE.STATUS_MODERATE,
                        BannerCreativeStatusModerate.toSource(creativeStatusModerate))
                .where(BANNERS_PERFORMANCE.BID.eq(bannerId))
                .execute();
    }
}
