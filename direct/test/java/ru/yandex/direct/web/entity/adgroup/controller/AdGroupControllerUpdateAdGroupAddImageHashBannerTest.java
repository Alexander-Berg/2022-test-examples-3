package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldImageHashBanner;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerImageFormatRepository;
import ru.yandex.direct.dbschema.ppc.enums.BannerImagesFormatsImageType;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;
import ru.yandex.direct.web.entity.banner.model.WebBanner;
import ru.yandex.direct.web.entity.banner.model.WebBannerTurbolanding;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebAdGroup;
import static ru.yandex.direct.web.testing.data.TestBanners.randomTitleWebTextBanner;
import static ru.yandex.direct.web.testing.data.TestBanners.webImageHashBanner;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerUpdateAdGroupAddImageHashBannerTest extends TextAdGroupControllerTestBase {

    @Autowired
    private TestBannerImageFormatRepository testBannerImageFormatRepository;

    @Test
    public void update_AdGroupWithAddedBanner_AdGroupIsUpdated() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        String imageHash = createImage();

        WebTextAdGroup requestAdGroup = updateAdGroupAndAddBanner(adGroupInfo, imageHash);

        List<AdGroup> actualAdGroups = findAdGroups();
        AdGroup actualUpdatedAdGroup = actualAdGroups.get(0);
        assertThat("группа не обновлена", actualUpdatedAdGroup.getName(), equalTo(requestAdGroup.getName()));
    }

    @Test
    public void update_AdGroupWithAddedBanner_BannerIsAdded() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        String imageHash = createImage();

        updateAdGroupAndAddBanner(adGroupInfo, imageHash);

        List<OldBanner> actualBanners = findBanners();
        assertThat("должен быть один добавленный баннер", actualBanners, hasSize(1));

        OldBanner addedBanner = actualBanners.get(0);
        assertThat("данные баннера отличаются от ожидаемых",
                ((OldImageHashBanner) addedBanner).getImage().getImageHash(),
                equalTo(imageHash));
        assertThat("баннер должен быть добавлен в соответствующую группу",
                addedBanner.getAdGroupId(), equalTo(adGroupInfo.getAdGroupId()));
        assertThat("к баннеру должен быть добавлен турболендинг",
                ((OldImageHashBanner) addedBanner).getTurboLandingId(),
                equalTo(bannerTurboLandings.get(0).getId()));
        assertThat("к баннеру должен быть добавлен статус модерации турболендинга",
                ((OldImageHashBanner) addedBanner).getTurboLandingStatusModerate(),
                equalTo(OldBannerTurboLandingStatusModerate.READY));
    }

    @Test
    public void update_AdGroupWithBannerWithoutImage_AddImage() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner(adGroupInfo);
        String imageHash = "abc124";
        steps.bannerSteps().addImageToImagePool(shard, campaignInfo.getClientId(), imageHash);
        testBannerImageFormatRepository.create(shard, imageHash, BannerImagesFormatsImageType.regular);

        WebTextAdGroup adGroupWithBanners = randomNameWebAdGroup(adGroupInfo.getAdGroupId(), null)
                .withBanners(singletonList(randomTitleWebTextBanner(bannerInfo.getBannerId())
                        .withImageHash(imageHash)));

        updateAndCheckResult(singletonList(adGroupWithBanners));
    }

    private WebTextAdGroup updateAdGroupAndAddBanner(AdGroupInfo adGroupInfo, String imageHash) {
        WebTextAdGroup adGroupWithBanners = randomNameWebAdGroup(adGroupInfo.getAdGroupId(), null);
        WebBanner addedBanner = webImageHashBanner(null, imageHash)
                .withTurbolanding(new WebBannerTurbolanding().withId(bannerTurboLandings.get(0).getId()));

        adGroupWithBanners.withBanners(singletonList(addedBanner));

        updateAndCheckResult(singletonList(adGroupWithBanners));

        return adGroupWithBanners;
    }

    private String createImage() {
        BannerImageFormat imageFormat = steps.bannerSteps()
                .createImageAdImageFormat(campaignInfo.getClientInfo());
        return imageFormat.getImageHash();
    }
}
