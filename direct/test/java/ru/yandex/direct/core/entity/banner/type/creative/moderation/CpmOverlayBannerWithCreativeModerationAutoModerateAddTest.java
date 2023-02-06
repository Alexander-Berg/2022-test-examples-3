package ru.yandex.direct.core.entity.banner.type.creative.moderation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class CpmOverlayBannerWithCreativeModerationAutoModerateAddTest extends BannerAdGroupInfoAddOperationTestBase {
    private CpmBanner banner;

    @Before
    public void before() throws Exception {
        adGroupInfo = steps.adGroupSteps().createActiveCpmVideoAdGroup();
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultOverlayCreative(adGroupInfo.getClientInfo(), creativeId);
        banner = clientCpmBanner(creativeId)
                .withAdGroupId(adGroupInfo.getAdGroupId());
    }

    @Test
    public void creativeStatusModerate_Yes() {
        Long id = prepareAndApplyValid(banner);

        CpmBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getCreativeStatusModerate(), equalTo(BannerCreativeStatusModerate.YES));
    }

    @Test
    public void bannerStatusModerate_Yes() {
        Long id = prepareAndApplyValid(banner);

        CpmBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getStatusModerate(), equalTo(BannerStatusModerate.YES));
    }
}
