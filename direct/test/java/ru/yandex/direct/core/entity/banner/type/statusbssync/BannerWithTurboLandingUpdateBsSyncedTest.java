package ru.yandex.direct.core.entity.banner.type.statusbssync;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

import static ru.yandex.direct.core.entity.banner.model.BannerWithTurboLanding.TURBO_LANDING_ID;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTurboLandingUpdateBsSyncedTest extends BannerWithRelatedEntityUpdateBsSyncedTestBase<TextBanner> {

    @Override
    protected Long createPlainBanner() {
        return steps.bannerSteps().createBanner(activeTextBanner(), clientInfo).getBannerId();
    }

    @Override
    protected Long createBannerWithRelatedEntity() {
        var turboLanding = steps.turboLandingSteps().createDefaultBannerTurboLanding(clientInfo.getClientId());
        OldTextBanner banner = activeTextBanner()
                .withTurboLandingId(turboLanding.getId())
                .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.YES);

        return steps.bannerSteps().createBanner(banner, clientInfo).getBannerId();
    }

    @Override
    protected ModelChanges<TextBanner> getPlainModelChanges(Long bannerId) {
        return new ModelChanges<>(bannerId, TextBanner.class);
    }

    @Override
    protected void setNewRelatedEntity(ModelChanges<TextBanner> modelChanges) {
        var turboLanding = steps.turboLandingSteps().createDefaultBannerTurboLanding(clientInfo.getClientId());
        modelChanges.process(turboLanding.getId(), TURBO_LANDING_ID);
    }

    @Override
    protected void deleteRelatedEntity(ModelChanges<TextBanner> modelChanges) {
        modelChanges.process(null, TURBO_LANDING_ID);
    }
}
