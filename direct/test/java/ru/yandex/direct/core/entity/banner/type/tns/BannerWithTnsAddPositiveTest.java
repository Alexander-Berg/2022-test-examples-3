package ru.yandex.direct.core.entity.banner.type.tns;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTnsAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    private static final String CORRECT_TNS_ID = "abc";

    @Test
    public void validTnsIdForCpmBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCanvasCreative(adGroupInfo.getClientInfo());

        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withTnsId(CORRECT_TNS_ID);

        Long id = prepareAndApplyValid(banner);
        CpmBanner actualBanner = getBanner(id, CpmBanner.class);
        assertThat(actualBanner.getTnsId(), equalTo(CORRECT_TNS_ID));
    }
}
