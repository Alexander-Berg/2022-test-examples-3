package ru.yandex.direct.web.entity.inventori.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.inventori.service.CampaignInfoCollector;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.web.configuration.DirectWebTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.draftTextBanner;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;

@DirectWebTest
@RunWith(SpringRunner.class)
public class CampaignInfoCollectorFilteringAdGroupsTest {

    @Autowired
    private Steps steps;
    @Autowired
    private CampaignInfoCollector collector;

    @Test
    public void getActiveAdGroupsWithActiveBanners_OneAdGroup_ActiveBanner() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        var bannerId = steps.bannerSteps().createDefaultBanner(adGroupInfo).getBannerId();
        var result = collector.getActiveAdGroupsWithActiveBanners(
                adGroupInfo.getShard(), List.of(adGroupInfo.getAdGroup()), false, Collections.emptySet());

        assertThat(result).isEqualTo(Map.of(adGroupInfo.getAdGroupId(), Set.of(bannerId)));
    }

    @Test
    public void getActiveAdGroupsWithActiveBanners_RejectedAdGroup_ActiveBanner() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(
                defaultTextAdGroup(null).withStatusPostModerate(StatusPostModerate.REJECTED));
        steps.bannerSteps().createDefaultBanner(adGroupInfo);
        var result = collector.getActiveAdGroupsWithActiveBanners(
                adGroupInfo.getShard(), List.of(adGroupInfo.getAdGroup()), false, Collections.emptySet());

        assertThat(result).isEmpty();
    }

    @Test
    public void getActiveAdGroupsWithActiveBanners_AdGroupWithoutBanners() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        var result = collector.getActiveAdGroupsWithActiveBanners(
                adGroupInfo.getShard(), List.of(adGroupInfo.getAdGroup()), false, Collections.emptySet());

        assertThat(result).isEmpty();
    }

    @Test
    public void getActiveAdGroupsWithActiveBanners_OneAdGroup_NotActiveBanner() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        steps.bannerSteps().createBanner(draftTextBanner(), adGroupInfo);
        var result = collector.getActiveAdGroupsWithActiveBanners(
                adGroupInfo.getShard(), List.of(adGroupInfo.getAdGroup()), false, Collections.emptySet());

        assertThat(result).isEmpty();
    }

    @Test
    public void getActiveAdGroupsWithActiveBanners_OneAdGroup_NotActiveBannerForced() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        TextBannerInfo textBanner = steps.bannerSteps().createBanner(draftTextBanner(), adGroupInfo);
        var result = collector.getActiveAdGroupsWithActiveBanners(
                adGroupInfo.getShard(), List.of(adGroupInfo.getAdGroup()), false,
                Set.of(textBanner.getCampaignId()));
        // если кампания в cidsToForceIncludeGroups, то неактивные баннеры тоже отправлять.
        // в этот список они попадают только в тех местах, где это требуется.
        assertThat(result).isEqualTo(Map.of(adGroupInfo.getAdGroupId(), Set.of(textBanner.getBannerId())));
    }

    @Test
    public void getActiveAdGroupsWithActiveBanners_OneAdGroup_ActiveAndNotActiveBanner() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        var bannerId = steps.bannerSteps().createDefaultBanner(adGroupInfo).getBannerId();
        steps.bannerSteps().createBanner(draftTextBanner(), adGroupInfo);
        var result = collector.getActiveAdGroupsWithActiveBanners(
                adGroupInfo.getShard(), List.of(adGroupInfo.getAdGroup()), false, Collections.emptySet());

        assertThat(result).isEqualTo(Map.of(adGroupInfo.getAdGroupId(), Set.of(bannerId)));
    }

    @Test
    public void getActiveAdGroupsWithActiveBanners_OneAdGroup_ThreeNotActiveBannersWithDifferentStatuses() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        // Если нет хотя бы одного баннера, полностью удовлетворяющего условиям, то группа отфильтровывается.
        steps.bannerSteps().createBanner(
                defaultTextBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId()).withStatusModerate(
                        OldBannerStatusModerate.NEW),
                adGroupInfo);
        steps.bannerSteps().createBanner(
                defaultTextBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId()).withStatusShow(false),
                adGroupInfo);
        steps.bannerSteps().createBanner(
                defaultTextBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId()).withStatusArchived(true),
                adGroupInfo);
        var result = collector.getActiveAdGroupsWithActiveBanners(
                adGroupInfo.getShard(), List.of(adGroupInfo.getAdGroup()), false, Collections.emptySet());

        assertThat(result).isEmpty();
    }

    @Test
    public void getActiveAdGroupsWithActiveBanners_OneAdGroup_FourNotActiveBannersWithDifferentStatuses_IncludeNotModerated() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        var bannerId = steps.bannerSteps().createBanner(
                defaultTextBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId()).withStatusModerate(
                        OldBannerStatusModerate.NEW),
                adGroupInfo).getBannerId();
        steps.bannerSteps().createBanner(
                defaultTextBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId()).withStatusShow(false),
                adGroupInfo);
        steps.bannerSteps().createBanner(
                defaultTextBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId()).withStatusArchived(true),
                adGroupInfo);
        steps.bannerSteps().createBanner(
                defaultTextBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                        .withStatusModerate(OldBannerStatusModerate.NO)
                        .withStatusPostModerate(OldBannerStatusPostModerate.REJECTED),
                adGroupInfo);
        var result = collector.getActiveAdGroupsWithActiveBanners(
                adGroupInfo.getShard(), List.of(adGroupInfo.getAdGroup()), true, Collections.emptySet());

        assertThat(result).isEqualTo(Map.of(adGroupInfo.getAdGroupId(), Set.of(bannerId)));
    }

    @Test
    public void getActiveAdGroupsWithActiveBanners_OneAdGroup_RejectedBanners() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        steps.bannerSteps().createBanner(draftTextBanner()
                .withStatusModerate(OldBannerStatusModerate.NO)
                .withStatusPostModerate(OldBannerStatusPostModerate.REJECTED), adGroupInfo);
        var result = collector.getActiveAdGroupsWithActiveBanners(
                adGroupInfo.getShard(), List.of(adGroupInfo.getAdGroup()), true, Collections.emptySet());

        assertThat(result).isEmpty();
    }


    @Test
    public void getActiveAdGroupsWithActiveBanners_ThreeAdGroups_ActiveAndNotActiveBanners() {
        // группа с двумя активными баннерами - оба баннера содержатся в результате
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        var bannerId = steps.bannerSteps().createDefaultBanner(adGroupInfo).getBannerId();
        var secondBannerId = steps.bannerSteps().createDefaultBanner(adGroupInfo).getBannerId();

        // группа с активным и не активным баннером - в результате содержится только активный
        AdGroupInfo secondAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        var thirdBannerId = steps.bannerSteps().createDefaultBanner(secondAdGroupInfo).getBannerId();
        steps.bannerSteps().createBanner(draftTextBanner(), secondAdGroupInfo);

        // группа с двумя неактивными баннерами - группа не содержится в результате
        AdGroupInfo thirdAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        steps.bannerSteps().createBanner(draftTextBanner(), thirdAdGroupInfo);
        steps.bannerSteps().createBanner(draftTextBanner(), thirdAdGroupInfo);

        var result = collector.getActiveAdGroupsWithActiveBanners(
                adGroupInfo.getShard(),
                List.of(adGroupInfo.getAdGroup(), secondAdGroupInfo.getAdGroup(), thirdAdGroupInfo.getAdGroup()),
                false, Collections.emptySet());

        assertThat(result).isEqualTo(Map.of(
                adGroupInfo.getAdGroupId(), Set.of(bannerId, secondBannerId),
                secondAdGroupInfo.getAdGroupId(), Set.of(thirdBannerId)));
    }
}
