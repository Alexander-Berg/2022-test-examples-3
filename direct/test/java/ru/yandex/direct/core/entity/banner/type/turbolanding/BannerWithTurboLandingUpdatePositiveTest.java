package ru.yandex.direct.core.entity.banner.type.turbolanding;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithTurboLanding;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.TextBanner.TURBO_LANDING_ID;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTurboLandingUpdatePositiveTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithTurboLanding> {

    @Test
    public void validTurboLandingIdForTextBanner() {
        bannerInfo = steps.bannerSteps().createActiveTextBanner();
        var newTurboLanding = steps.turboLandingSteps()
                .createDefaultBannerTurboLanding(bannerInfo.getClientId());
        ModelChanges<TextBanner> modelChanges = new ModelChanges<>(bannerInfo.getBannerId(),
                TextBanner.class)
                .process(newTurboLanding.getId(), TURBO_LANDING_ID);

        Long id = prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertThat(actualBanner.getTurboLandingId(), equalTo(newTurboLanding.getId()));
    }
}
