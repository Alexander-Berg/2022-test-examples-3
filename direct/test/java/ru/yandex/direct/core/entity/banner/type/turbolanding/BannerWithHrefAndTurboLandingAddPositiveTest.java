package ru.yandex.direct.core.entity.banner.type.turbolanding;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLanding;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.result.MassResult;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewCpcVideoBanners.clientCpcVideoBanner;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithHrefAndTurboLandingAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    @Test
    public void cpcVideoBannerWithOnlyHref() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        CreativeInfo creativeInfo = steps.creativeSteps()
                .addDefaultCpcVideoCreative(adGroupInfo.getClientInfo(), steps.creativeSteps().getNextCreativeId());
        var banner = clientCpcVideoBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withHref("https://www.yandex.ru")
                .withTurboLandingId(null);
        MassResult<Long> result = createOperation(banner, false).prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void cpcVideoBannerWithOnlyTurbo() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();

        CreativeInfo creativeInfo = steps.creativeSteps()
                .addDefaultCpcVideoCreative(adGroupInfo.getClientInfo(), steps.creativeSteps().getNextCreativeId());

        OldBannerTurboLanding turboLanding = steps.turboLandingSteps()
                .createDefaultBannerTurboLanding(adGroupInfo.getClientId());

        var banner = clientCpcVideoBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withHref(null)
                .withTurboLandingId(turboLanding.getId());

        MassResult<Long> result = createOperation(banner, false).prepareAndApply();
        assertThat(result, isFullySuccessful());
    }
}
