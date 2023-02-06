package ru.yandex.direct.core.entity.banner.type.image;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.McBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.BannerSteps;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultMcBannerImageFormat;
import static ru.yandex.direct.core.testing.data.TestNewImageBanners.clientImageBannerWithImage;
import static ru.yandex.direct.core.testing.data.TestNewMcBanners.clientMcBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithImageAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    @Autowired
    private BannerSteps bannerSteps;

    @Test
    public void validBannerImageTypeForTextBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        String imageHash = bannerSteps.createImageAdImageFormat(adGroupInfo.getClientInfo()).getImageHash();

        var banner = clientImageBannerWithImage(imageHash)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId());

        Long id = prepareAndApplyValid(banner);
        ImageBanner actualBanner = getBanner(id, ImageBanner.class);
        assertThat(actualBanner.getImageHash(), equalTo(imageHash));
    }

    @Test
    public void validImageSizeForMcBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveMcBannerAdGroup();
        var imageHash = steps.bannerSteps()
                .createBannerImageFormat(adGroupInfo.getClientInfo(), defaultMcBannerImageFormat(null))
                .getImageHash();

        var banner = clientMcBanner(imageHash)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId());

        Long id = prepareAndApplyValid(banner);
        McBanner actualBanner = getBanner(id, McBanner.class);
        assertThat(actualBanner.getImageHash(), equalTo(imageHash));
    }

}
