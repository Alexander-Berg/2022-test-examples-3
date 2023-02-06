package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldCpcVideoBanner;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.testing.info.CpcVideoBannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;
import ru.yandex.direct.web.entity.banner.model.WebBanner;
import ru.yandex.direct.web.entity.banner.model.WebBannerTurbolanding;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpcVideoBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpcVideoForCpcVideoBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebAdGroup;
import static ru.yandex.direct.web.testing.data.TestBanners.webCpcVideoBanner;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerUpdateAdGroupUpdateCpcVideoBannerTest extends TextAdGroupControllerTestBase {

    private CreativeInfo creativeInfo1;
    private CreativeInfo creativeInfo2;

    @Before
    public void before() {
        super.before();

        Creative creative1 = defaultCpcVideoForCpcVideoBanner(campaignInfo.getClientId(), null);
        creativeInfo1 = steps.creativeSteps().createCreative(creative1, campaignInfo.getClientInfo());

        Creative creative2 = defaultCpcVideoForCpcVideoBanner(campaignInfo.getClientId(), null);
        creativeInfo2 = steps.creativeSteps().createCreative(creative2, campaignInfo.getClientInfo());
    }

    @Test
    public void update_AdGroupWithUpdatedBanner_AdGroupIsUpdated() {
        CpcVideoBannerInfo bannerInfo = steps.bannerSteps().createActiveCpcVideoBanner(
                activeCpcVideoBanner(null, null, creativeInfo1.getCreativeId()), campaignInfo);
        WebTextAdGroup requestAdGroup = updateAdGroupAndUpdateBanner(bannerInfo, creativeInfo2.getCreativeId());

        List<AdGroup> actualAdGroups = findAdGroups();
        AdGroup actualUpdatedAdGroup = actualAdGroups.get(0);
        assertThat("группа не обновлена", actualUpdatedAdGroup.getName(), equalTo(requestAdGroup.getName()));
    }

    @Test
    public void update_AdGroupWithUpdatedBanner_BannerIsUpdated() {
        CpcVideoBannerInfo bannerInfo = steps.bannerSteps().createActiveCpcVideoBanner(
                activeCpcVideoBanner(null, null, creativeInfo1.getCreativeId())
                        .withTurboLandingId(bannerTurboLandings.get(0).getId())
                        .withTurboLandingStatusModerate(bannerTurboLandings.get(0).getStatusModerate()),
                campaignInfo);

        updateAdGroupAndUpdateBanner(bannerInfo, creativeInfo2.getCreativeId());

        List<OldBanner> actualBanners = findBanners();
        assertThat("должен быть один добавленный баннер", actualBanners, hasSize(1));

        OldBanner addedBanner = actualBanners.get(0);
        assertThat("данные баннера отличаются от ожидаемых",
                ((OldCpcVideoBanner) addedBanner).getCreativeId(),
                equalTo(creativeInfo2.getCreativeId()));
        assertThat("турболендинг баннера должен быть обновлен",
                ((OldCpcVideoBanner) addedBanner).getTurboLandingId(),
                equalTo(bannerTurboLandings.get(1).getId()));
        assertThat("статус модерации турболендинга баннера должен быть обновлен",
                ((OldCpcVideoBanner) addedBanner).getTurboLandingStatusModerate(),
                equalTo(OldBannerTurboLandingStatusModerate.READY));
    }

    @Test
    public void update_AdGroupWithUpdatedDraftBanner_BannerIsUpdatedAndReadyForModeration() {
        CpcVideoBannerInfo bannerInfo = steps.bannerSteps().createActiveCpcVideoBanner(
                activeCpcVideoBanner(null, null, creativeInfo1.getCreativeId())
                        .withStatusModerate(OldBannerStatusModerate.NEW)
                        .withCreativeStatusModerate(OldBannerCreativeStatusModerate.NEW),
                campaignInfo);

        List<OldBanner> actualBanners = findBanners();
        OldBanner addedBanner = actualBanners.get(0);
        assumeThat("кампания промодерирована",
                campaignInfo.getCampaign().getStatusModerate(),
                equalTo(StatusModerate.YES));
        assumeThat("баннер был создан как черновик",
                addedBanner.getStatusModerate(), equalTo(OldBannerStatusModerate.NEW));
        assumeThat("креатив баннера был создан как черновик",
                ((OldCpcVideoBanner) addedBanner).getCreativeStatusModerate(),
                equalTo(OldBannerCreativeStatusModerate.NEW));

        updateAdGroupAndUpdateBanner(bannerInfo, creativeInfo1.getCreativeId());

        actualBanners = findBanners();
        addedBanner = actualBanners.get(0);
        assertThat("должен быть один добавленный баннер", actualBanners, hasSize(1));

        assertThat("данные баннера отличаются от ожидаемых",
                ((OldCpcVideoBanner) addedBanner).getCreativeId(),
                equalTo(creativeInfo1.getCreativeId()));
        assumeThat("статус модерации баннера отличаются от ожидаемого",
                addedBanner.getStatusModerate(), equalTo(OldBannerStatusModerate.READY));
        assertThat("статус модерации креатива баннера отличаются от ожидаемого",
                ((OldCpcVideoBanner) addedBanner).getCreativeStatusModerate(),
                equalTo(OldBannerCreativeStatusModerate.READY));
    }

    private WebTextAdGroup updateAdGroupAndUpdateBanner(CpcVideoBannerInfo bannerInfo, long creativeId) {
        WebTextAdGroup adGroupWithBanners = randomNameWebAdGroup(bannerInfo.getAdGroupId(), null);
        WebBanner addedBanner = webCpcVideoBanner(bannerInfo.getBannerId(), creativeId);

        adGroupWithBanners.withBanners(singletonList(addedBanner
                .withTurbolanding(new WebBannerTurbolanding().withId(bannerTurboLandings.get(1).getId()))));

        updateAndCheckResult(singletonList(adGroupWithBanners));

        return adGroupWithBanners;
    }
}
