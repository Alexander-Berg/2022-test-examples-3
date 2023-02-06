package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldImageHashBanner;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.ImageHashBannerInfo;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;
import ru.yandex.direct.web.entity.banner.model.WebBanner;
import ru.yandex.direct.web.entity.banner.model.WebBannerTurbolanding;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeImageHashBanner;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebAdGroup;
import static ru.yandex.direct.web.testing.data.TestBanners.webImageHashBanner;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerUpdateAdGroupUpdateImageHashBannerTest extends TextAdGroupControllerTestBase {

    @Test
    public void update_AdGroupWithUpdatedBanner_AdGroupIsUpdated() {
        ImageHashBannerInfo bannerInfo = createImageHashBanner();
        String imageHash2 = createImage();

        WebTextAdGroup requestAdGroup = updateAdGroupAndUpdateBanner(bannerInfo, imageHash2);

        List<AdGroup> actualAdGroups = findAdGroups();
        AdGroup actualUpdatedAdGroup = actualAdGroups.get(0);
        assertThat("группа не обновлена", actualUpdatedAdGroup.getName(), equalTo(requestAdGroup.getName()));
    }

    @Test
    public void update_AdGroupWithUpdatedBanner_BannerIsUpdated() {
        ImageHashBannerInfo bannerInfo = createImageHashBanner();
        String imageHash2 = createImage();

        updateAdGroupAndUpdateBanner(bannerInfo, imageHash2);

        List<OldBanner> actualBanners = findBanners();
        assertThat("должен быть один добавленный баннер", actualBanners, hasSize(1));

        OldBanner addedBanner = actualBanners.get(0);
        assertThat("данные баннера отличаются от ожидаемых",
                ((OldImageHashBanner) addedBanner).getImage().getImageHash(),
                equalTo(imageHash2));
        assertThat("турболендинг баннера должен быть обновлен",
                ((OldImageHashBanner) addedBanner).getTurboLandingId(),
                equalTo(bannerTurboLandings.get(1).getId()));
        assertThat("статус модерации турболендинга баннера должен быть обновлен",
                ((OldImageHashBanner) addedBanner).getTurboLandingStatusModerate(),
                equalTo(OldBannerTurboLandingStatusModerate.READY));
    }

    private WebTextAdGroup updateAdGroupAndUpdateBanner(ImageHashBannerInfo bannerInfo, String imageHash) {
        WebTextAdGroup adGroupWithBanners = randomNameWebAdGroup(bannerInfo.getAdGroupId(), null);
        WebBanner addedBanner = webImageHashBanner(bannerInfo.getBannerId(), imageHash)
                .withTurbolanding(new WebBannerTurbolanding().withId(bannerTurboLandings.get(1).getId()));

        adGroupWithBanners.withBanners(singletonList(addedBanner));

        updateAndCheckResult(singletonList(adGroupWithBanners));

        return adGroupWithBanners;
    }

    private ImageHashBannerInfo createImageHashBanner() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        OldImageHashBanner banner = activeImageHashBanner(adGroupInfo.getCampaignId(),
                adGroupInfo.getAdGroupId())
                .withTurboLandingId(bannerTurboLandings.get(0).getId())
                .withTurboLandingStatusModerate(bannerTurboLandings.get(0).getStatusModerate());
//                .withDomain("www.ya.ru");
        ImageHashBannerInfo bannerInfo = steps.bannerSteps()
                .createActiveImageHashBanner(banner, adGroupInfo);
        steps.bannerSteps().createImage(bannerInfo);
        return bannerInfo;
    }

    private String createImage() {
        BannerImageFormat imageFormat = steps.bannerSteps()
                .createImageAdImageFormat(campaignInfo.getClientInfo());
        return imageFormat.getImageHash();
    }
}
