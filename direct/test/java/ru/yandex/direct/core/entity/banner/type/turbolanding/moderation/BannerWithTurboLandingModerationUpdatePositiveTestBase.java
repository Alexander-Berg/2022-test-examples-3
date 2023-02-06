package ru.yandex.direct.core.entity.banner.type.turbolanding.moderation;

import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.BannerWithTurboLanding;
import ru.yandex.direct.core.entity.banner.model.BannerWithTurboLandingModeration;
import ru.yandex.direct.core.entity.banner.type.BannerWithChildrenModelChangesFunction;
import ru.yandex.direct.core.entity.banner.type.BannerWithChildrenModerationUpdatePositiveTestBase;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_TURBOLANDINGS;

public abstract class BannerWithTurboLandingModerationUpdatePositiveTestBase
        extends BannerWithChildrenModerationUpdatePositiveTestBase<BannerWithTurboLanding> {

    @Parameterized.Parameter(6)
    public BannerTurboLandingStatusModerate turboLandingStatusModerate;

    @Parameterized.Parameter(7)
    public BannerTurboLandingStatusModerate expectedTurboLandingStatusModerate;

    @Parameterized.Parameter(8)
    public BannerStatusModerate expectedBannerStatusModerate;

    @Override
    protected void checkBanner(Long bannerId) {
        BannerWithTurboLandingModeration actualBanner = getBanner(bannerId);

        assertSoftly(softly -> {
            softly.assertThat(actualBanner.getId()).isEqualTo(bannerId);
            softly.assertThat(actualBanner.getTurboLandingStatusModerate())
                    .isEqualTo(expectedTurboLandingStatusModerate);
            softly.assertThat(actualBanner.getStatusModerate()).isEqualTo(expectedBannerStatusModerate);
        });
    }

    protected static ModelChanges<BannerWithSystemFields> getModelChanges(
            Class<? extends BannerWithTurboLanding> bannerClass, Long bannerId, Long turboLandingId) {
        return new ModelChanges<>(bannerId, bannerClass)
                .process(turboLandingId, BannerWithTurboLanding.TURBO_LANDING_ID)
                .castModelUp(BannerWithSystemFields.class);
    }

    protected static BannerWithChildrenModelChangesFunction<BannerWithTurboLanding> modelChangesWithNewTurboLanding() {
        return (steps, clientInfo, bannerClass, bannerId) -> {
            Long turboLandingId = steps.turboLandingSteps()
                    .createDefaultBannerTurboLanding(clientInfo.getClientId()).getId();
            return getModelChanges(bannerClass, bannerId, turboLandingId);
        };
    }

    protected static BannerWithChildrenModelChangesFunction<BannerWithTurboLanding> modelChangesWithDeleteTurboLanding() {
        return (steps, clientInfo, bannerClass, bannerId) ->
                getModelChanges(bannerClass, bannerId, null);
    }

    @Override
    protected void setBannerChildrenModerationStatus(Long bannerId) {
        dslContextProvider.ppc(defaultClient.getShard())
                .update(BANNER_TURBOLANDINGS)
                .set(BANNER_TURBOLANDINGS.STATUS_MODERATE,
                        BannerTurboLandingStatusModerate.toSource(turboLandingStatusModerate))
                .where(BANNER_TURBOLANDINGS.BID.eq(bannerId))
                .execute();
    }
}
