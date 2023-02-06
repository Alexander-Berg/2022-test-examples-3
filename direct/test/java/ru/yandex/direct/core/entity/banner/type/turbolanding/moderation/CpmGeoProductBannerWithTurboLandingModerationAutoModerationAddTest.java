package ru.yandex.direct.core.entity.banner.type.turbolanding.moderation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.common.db.PpcPropertyNames.CPM_GEOPRODUCT_AUTO_MODERATION;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class CpmGeoProductBannerWithTurboLandingModerationAutoModerationAddTest extends BannerAdGroupInfoAddOperationTestBase {
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Before
    public void before() throws Exception {
        ppcPropertiesSupport.set(CPM_GEOPRODUCT_AUTO_MODERATION.getName(), "true");
    }

    @After
    public void after() {
        ppcPropertiesSupport.remove(CPM_GEOPRODUCT_AUTO_MODERATION.getName());
    }

    @Test
    public void statusModerate() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmGeoproductAdGroup();
        Long creativeId = steps.creativeSteps()
                .addDefaultHtml5CreativeForGeoproduct(adGroupInfo.getClientInfo()).getCreativeId();
        var turboLanding = steps.turboLandingSteps().createDefaultBannerTurboLanding(adGroupInfo.getClientId());
        CpmBanner banner = clientCpmBanner(creativeId)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTurboLandingId(turboLanding.getId())
                .withHref(null);

        Long id = prepareAndApplyValid(banner);

        CpmBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getTurboLandingStatusModerate(), equalTo(BannerTurboLandingStatusModerate.YES));
        assertThat(actualBanner.getStatusModerate(), equalTo(BannerStatusModerate.YES));
    }
}
