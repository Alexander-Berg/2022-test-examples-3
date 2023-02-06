package ru.yandex.direct.core.entity.banner.type.turbolandingparams;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithTurboLandingParams;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTurboLandingParamsAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    @Test
    public void turboLandingParamsNotNull_Ok() {
        String hrefParams = "abc";
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        var banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTurboLandingHrefParams(hrefParams);

        Long id = prepareAndApplyValid(banner);

        BannerWithTurboLandingParams actualBanner = getBanner(id);
        assertThat(actualBanner.getTurboLandingHrefParams(), equalTo(hrefParams));
    }

    @Test
    public void turboLandingParamsNull_Ok() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        var banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTurboLandingHrefParams(null);

        Long id = prepareAndApplyValid(banner);

        BannerWithTurboLandingParams actualBanner = getBanner(id);
        assertThat(actualBanner.getTurboLandingHrefParams(), nullValue());
    }

}
