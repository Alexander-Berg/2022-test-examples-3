package ru.yandex.direct.core.entity.banner.type.creative;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewImageBanners.clientImageBannerWithCreative;

@CoreTest
@RunWith(SpringRunner.class)
public class NewImageBannerWithCreativePlayableAddTest extends BannerAdGroupInfoAddOperationTestBase {

    private static final Long CREATIVE_WIDTH = 500L;
    private static final Long CREATIVE_HEIGTH = 500L;

    @Test
    public void validHtml5CreativeForImageBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();

        var creativeId = steps.creativeSteps()
                .addDefaultHtml5CreativeWithSize(adGroupInfo.getClientInfo(), CREATIVE_WIDTH, CREATIVE_HEIGTH)
                .getCreativeId();
        var banner = clientImageBannerWithCreative(creativeId)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId());

        Long id = prepareAndApplyValid(banner);

        ImageBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getCreativeId(), equalTo(creativeId));
    }
}
