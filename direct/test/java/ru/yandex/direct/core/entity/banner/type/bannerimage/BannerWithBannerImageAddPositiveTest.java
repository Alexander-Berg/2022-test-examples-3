package ru.yandex.direct.core.entity.banner.type.bannerimage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.BannerSteps;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithBannerImageAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    @Autowired
    private BannerSteps bannerSteps;

    @Test
    public void validBannerImageTypeForTextBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        String imageHash = bannerSteps.createRegularImageFormat(adGroupInfo.getClientInfo()).getImageHash();

        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withImageHash(imageHash);

        Long id = prepareAndApplyValid(banner);
        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertThat(actualBanner.getImageHash(), equalTo(imageHash));
    }

}
