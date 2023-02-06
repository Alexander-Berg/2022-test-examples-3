package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldImageCreativeBanner;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.testing.info.ImageCreativeBannerInfo;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;
import ru.yandex.direct.web.entity.banner.model.WebBanner;
import ru.yandex.direct.web.entity.banner.model.WebBannerTurbolanding;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageCreativeBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebAdGroup;
import static ru.yandex.direct.web.testing.data.TestBanners.webImageCreativeBanner;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerUpdateAdGroupUpdateImageCreativeBannerTest extends TextAdGroupControllerTestBase {

    @Test
    public void update_AdGroupWithUpdatedBanner_AdGroupIsUpdated() {
        long creativeId1 = steps.creativeSteps().addDefaultCanvasCreative(campaignInfo.getClientInfo()).getCreativeId();
        long creativeId2 = steps.creativeSteps().addDefaultCanvasCreative(campaignInfo.getClientInfo()).getCreativeId();

        ImageCreativeBannerInfo bannerInfo = steps.bannerSteps()
                .createImageCreativeBanner(activeImageCreativeBanner(null, null, creativeId1), campaignInfo);

        WebTextAdGroup requestAdGroup = updateAdGroupAndUpdateBanner(bannerInfo, creativeId2);

        List<AdGroup> actualAdGroups = findAdGroups();
        AdGroup actualUpdatedAdGroup = actualAdGroups.get(0);
        assertThat("группа не обновлена", actualUpdatedAdGroup.getName(), equalTo(requestAdGroup.getName()));
    }

    @Test
    public void update_AdGroupWithUpdatedBanner_BannerIsUpdated() {
        long creativeId1 = steps.creativeSteps().addDefaultCanvasCreative(campaignInfo.getClientInfo()).getCreativeId();
        long creativeId2 = steps.creativeSteps().addDefaultCanvasCreative(campaignInfo.getClientInfo()).getCreativeId();

        ImageCreativeBannerInfo bannerInfo = steps.bannerSteps()
                .createImageCreativeBanner(
                        activeImageCreativeBanner(null, null, creativeId1)
                                .withTurboLandingId(bannerTurboLandings.get(0).getId())
                                .withTurboLandingStatusModerate(bannerTurboLandings.get(0).getStatusModerate()),
                        campaignInfo);

        updateAdGroupAndUpdateBanner(bannerInfo, creativeId2);

        List<OldBanner> actualBanners = findBanners();
        assertThat("должен быть один добавленный баннер", actualBanners, hasSize(1));

        OldBanner addedBanner = actualBanners.get(0);
        assertThat("данные баннера отличаются от ожидаемых",
                ((OldImageCreativeBanner) addedBanner).getCreativeId(),
                equalTo(creativeId2));
        assertThat("турболендинг баннера должен быть обновлен",
                ((OldImageCreativeBanner) addedBanner).getTurboLandingId(),
                equalTo(bannerTurboLandings.get(1).getId()));
        assertThat("статус турболендинга баннера должен быть обновлен",
                ((OldImageCreativeBanner) addedBanner).getTurboLandingStatusModerate(),
                equalTo(OldBannerTurboLandingStatusModerate.READY));
    }

    @Test
    public void update_AdGroupWithUpdatedDraftBanner_BannerIsUpdatedAndReadyForModeration() {
        long creativeId1 = steps.creativeSteps().addDefaultCanvasCreative(campaignInfo.getClientInfo()).getCreativeId();

        ImageCreativeBannerInfo bannerInfo = steps.bannerSteps()
                .createImageCreativeBanner(activeImageCreativeBanner(null, null, creativeId1)
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
                ((OldImageCreativeBanner) addedBanner).getCreativeStatusModerate(),
                equalTo(OldBannerCreativeStatusModerate.NEW));

        updateAdGroupAndUpdateBanner(bannerInfo, creativeId1);

        actualBanners = findBanners();
        addedBanner = actualBanners.get(0);
        assertThat("должен быть один добавленный баннер", actualBanners, hasSize(1));

        assertThat("данные баннера отличаются от ожидаемых",
                ((OldImageCreativeBanner) addedBanner).getCreativeId(),
                equalTo(creativeId1));
        assumeThat("статус модерации баннера отличаются от ожидаемого",
                addedBanner.getStatusModerate(), equalTo(OldBannerStatusModerate.READY));
        assertThat("статус модерации креатива баннера отличаются от ожидаемого",
                ((OldImageCreativeBanner) addedBanner).getCreativeStatusModerate(),
                equalTo(OldBannerCreativeStatusModerate.READY));
    }

    private WebTextAdGroup updateAdGroupAndUpdateBanner(ImageCreativeBannerInfo bannerInfo, long creativeId) {
        WebTextAdGroup adGroupWithBanners = randomNameWebAdGroup(bannerInfo.getAdGroupId(), null);
        WebBanner addedBanner = webImageCreativeBanner(bannerInfo.getBannerId(), creativeId)
                .withTurbolanding(new WebBannerTurbolanding().withId(bannerTurboLandings.get(1).getId()));

        adGroupWithBanners.withBanners(singletonList(addedBanner));

        updateAndCheckResult(singletonList(adGroupWithBanners));

        return adGroupWithBanners;
    }
}
