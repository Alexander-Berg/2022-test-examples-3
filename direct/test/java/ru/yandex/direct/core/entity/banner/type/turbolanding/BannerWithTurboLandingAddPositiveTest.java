package ru.yandex.direct.core.entity.banner.type.turbolanding;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTurboLandingAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    @Test
    public void validTurboLandingForTextBanner() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        var turboLanding = steps.turboLandingSteps().createDefaultTurboLanding(adGroupInfo.getClientId());
        var banner = clientTextBanner()
                .withTurboLandingId(turboLanding.getId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        Long id = prepareAndApplyValid(banner);

        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getTurboLandingId(), equalTo(banner.getTurboLandingId()));
    }
}
