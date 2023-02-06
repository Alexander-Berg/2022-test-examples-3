package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpcVideoBanner;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;
import ru.yandex.direct.web.entity.banner.model.WebBanner;
import ru.yandex.direct.web.entity.banner.model.WebBannerTurbolanding;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpcVideoForCpcVideoBanner;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebAdGroup;
import static ru.yandex.direct.web.testing.data.TestBanners.webCpcVideoBanner;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerUpdateAdGroupAddCpcVideoBannerTest extends TextAdGroupControllerTestBase {

    private CreativeInfo creativeInfo;

    @Before
    public void before() {
        super.before();

        Creative creative = defaultCpcVideoForCpcVideoBanner(campaignInfo.getClientId(), null);
        creativeInfo = steps.creativeSteps().createCreative(creative, campaignInfo.getClientInfo());
    }

    @Test
    public void update_AdGroupWithAddedBanner_AdGroupIsUpdated() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        WebTextAdGroup requestAdGroup = updateAdGroupAndAddBanner(adGroupInfo, creativeInfo.getCreativeId());

        List<AdGroup> actualAdGroups = findAdGroups();
        AdGroup actualUpdatedAdGroup = actualAdGroups.get(0);
        assertThat("группа не обновлена", actualUpdatedAdGroup.getName(), equalTo(requestAdGroup.getName()));
    }

    @Test
    public void update_AdGroupWithAddedBanner_BannerIsAdded() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        updateAdGroupAndAddBanner(adGroupInfo, creativeInfo.getCreativeId());

        List<OldBanner> actualBanners = findBanners();
        assertThat("должен быть один добавленный баннер", actualBanners, hasSize(1));

        OldBanner addedBanner = actualBanners.get(0);
        assertThat("данные баннера отличаются от ожидаемых",
                ((OldCpcVideoBanner) addedBanner).getCreativeId(),
                equalTo(creativeInfo.getCreativeId()));
        assertThat("баннер должен быть добавлен в соответствующую группу",
                addedBanner.getAdGroupId(), equalTo(adGroupInfo.getAdGroupId()));
        assertThat("к баннеру должен быть добавлен турболендинг",
                ((OldCpcVideoBanner) addedBanner).getTurboLandingId(),
                equalTo(bannerTurboLandings.get(0).getId()));
        assertThat("к баннеру должен быть добавлен статус модерации турболендинга",
                ((OldCpcVideoBanner) addedBanner).getTurboLandingStatusModerate(),
                equalTo(OldBannerTurboLandingStatusModerate.READY));
    }

    private WebTextAdGroup updateAdGroupAndAddBanner(AdGroupInfo adGroupInfo, long creativeId) {
        WebTextAdGroup adGroupWithBanners = randomNameWebAdGroup(adGroupInfo.getAdGroupId(), null);
        WebBanner addedBanner = webCpcVideoBanner(null, creativeId)
                .withTurbolanding(new WebBannerTurbolanding().withId(bannerTurboLandings.get(0).getId()));
        adGroupWithBanners.withBanners(singletonList(addedBanner));

        updateAndCheckResult(singletonList(adGroupWithBanners));

        return adGroupWithBanners;
    }
}

