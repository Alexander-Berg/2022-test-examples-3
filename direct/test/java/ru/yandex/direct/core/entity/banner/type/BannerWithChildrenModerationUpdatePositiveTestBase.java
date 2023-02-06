package ru.yandex.direct.core.entity.banner.type;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;

import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;

public abstract class BannerWithChildrenModerationUpdatePositiveTestBase<T extends Banner> extends BannerOldBannerInfoUpdateOperationTestBase<OldBanner> {

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public BannerStatusModerate bannerStatusModerate;

    @Parameterized.Parameter(2)
    public BannerStatusPostModerate bannerStatusPostModerate;

    @Parameterized.Parameter(3)
    public Class<? extends T> bannerClass;

    @Parameterized.Parameter(4)
    public ModerationMode moderationMode;

    @Parameterized.Parameter(5)
    public BannerWithChildrenModelChangesFunction<T> modelChangesGetter;

    @Autowired
    public DslContextProvider dslContextProvider;

    protected ClientInfo defaultClient;

    protected abstract AbstractBannerInfo<? extends OldBanner> createBanner();

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        defaultClient = steps.clientSteps().createDefaultClient();
    }

    @Override
    protected ModerationMode getModerationMode() {
        return moderationMode;
    }

    @Test
    public void updateBanner_childrenModerationTest() {
        bannerInfo = createBanner();
        var bannerId = bannerInfo.getBannerId();

        var modelChanges = modelChangesGetter.getModelChanges(steps, defaultClient, bannerClass, bannerId);

        setBannerStatusModerate(bannerId);
        setBannerStatusPostModerate(bannerId);
        setBannerChildrenModerationStatus(bannerId);
        var operation = createOperation(modelChanges);
        var id = operation.prepareAndApply().get(0).getResult();

        checkBanner(id);
        additionalChecks();
    }

    protected abstract void checkBanner(Long bannerId);

    protected void additionalChecks() {
    }

    protected static <T extends Banner> BannerWithChildrenModelChangesFunction<T> emptyModelChanges() {
        return (steps, clientInfo, bannerClass, bannerId) -> new ModelChanges<>(bannerId, bannerClass)
                .castModelUp(BannerWithSystemFields.class);
    }

    private void setBannerStatusModerate(Long bannerId) {
        dslContextProvider.ppc(defaultClient.getShard())
                .update(BANNERS)
                .set(BANNERS.STATUS_MODERATE, BannerStatusModerate.toSource(bannerStatusModerate))
                .where(BANNERS.BID.eq(bannerId))
                .execute();
    }

    private void setBannerStatusPostModerate(Long bannerId) {
        dslContextProvider.ppc(defaultClient.getShard())
                .update(BANNERS)
                .set(BANNERS.STATUS_POST_MODERATE, BannerStatusPostModerate.toSource(bannerStatusPostModerate))
                .where(BANNERS.BID.eq(bannerId))
                .execute();
    }

    protected abstract void setBannerChildrenModerationStatus(Long bannerId);
}
