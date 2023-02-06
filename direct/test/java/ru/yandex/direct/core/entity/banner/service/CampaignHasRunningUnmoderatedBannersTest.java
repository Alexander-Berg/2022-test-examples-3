package ru.yandex.direct.core.entity.banner.service;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusPhoneFlagModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.organization.model.OrganizationStatusPublish.PUBLISHED;
import static ru.yandex.direct.core.entity.organization.model.OrganizationStatusPublish.UNPUBLISHED;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignHasRunningUnmoderatedBannersTest {

    @Autowired
    private BannerService bannerService;

    @Autowired
    private Steps steps;

    private CampaignInfo campaignInfo;

    @Before
    public void before() {
        campaignInfo = steps.campaignSteps().createActiveTextCampaign();
    }

    @Test
    public void test_GroupNotModerated_BannerNotActive() {
        AdGroupInfo adGroupInfo = createAdGroup(StatusPostModerate.NO, StatusModerate.NO);
        OldTextBanner banner = activeTextBanner()
                .withStatusActive(false);
        steps.bannerSteps().createBanner(banner, adGroupInfo);
        assertResult(false);
    }

    @Test
    public void test_GroupNotModerated_BannerActive() {
        AdGroupInfo adGroupInfo = createAdGroup(StatusPostModerate.NO, StatusModerate.NO);
        OldTextBanner banner = activeTextBanner();
        steps.bannerSteps().createBanner(banner, adGroupInfo);
        assertResult(true);
    }

    @Test
    public void test_GroupModerated_BannerActive() {
        AdGroupInfo adGroupInfo = createAdGroup(StatusPostModerate.YES, StatusModerate.YES);
        OldTextBanner banner = activeTextBanner();
        steps.bannerSteps().createBanner(banner, adGroupInfo);
        assertResult(false);
    }

    @Test
    public void test_GroupModerated_BannerNotModerated() {
        AdGroupInfo adGroupInfo = createAdGroup(StatusPostModerate.YES, StatusModerate.YES);
        OldTextBanner banner = activeTextBanner()
                .withStatusModerate(OldBannerStatusModerate.NO)
                .withStatusPostModerate(OldBannerStatusPostModerate.NO);
        steps.bannerSteps().createBanner(banner, adGroupInfo);
        assertResult(true);
    }

    @Test
    public void test_GroupModerated_BannerWith_NoHref_NoModeratedVcard_NoManualPublishedOrganization() {
        AdGroupInfo adGroupInfo = createAdGroup(StatusPostModerate.YES, StatusModerate.YES);
        OldTextBanner banner = activeTextBanner()
                .withHref(null)
                .withPhoneFlag(StatusPhoneFlagModerate.NO)
                .withPermalinkId(null);
        steps.bannerSteps().createBanner(banner, adGroupInfo);
        assertResult(true);
    }

    @Test
    public void test_GroupModerated_BannerWith_Href_NoModeratedVcard_NoManualPublishedOrganization() {
        AdGroupInfo adGroupInfo = createAdGroup(StatusPostModerate.YES, StatusModerate.YES);
        OldTextBanner banner = activeTextBanner()
                .withHref("https://ya.ru")
                .withPhoneFlag(StatusPhoneFlagModerate.NO)
                .withPermalinkId(null);
        steps.bannerSteps().createBanner(banner, adGroupInfo);
        assertResult(false);
    }

    @Test
    public void test_GroupModerated_BannerWith_NoHref_ModeratedVcard_NoManualPublishedOrganization() {
        AdGroupInfo adGroupInfo = createAdGroup(StatusPostModerate.YES, StatusModerate.YES);
        OldTextBanner banner = activeTextBanner()
                .withHref(null)
                .withPhoneFlag(StatusPhoneFlagModerate.YES)
                .withPermalinkId(null);
        steps.bannerSteps().createBanner(banner, adGroupInfo);
        assertResult(false);
    }

    @Test
    public void test_GroupModerated_BannerWith_NoHref_NoModeratedVcard_ManualPublishedOrganization() {
        long permalinkId = RandomNumberUtils.nextPositiveLong();
        AdGroupInfo adGroupInfo = createAdGroup(StatusPostModerate.YES, StatusModerate.YES);
        OldTextBanner banner = activeTextBanner()
                .withHref(null)
                .withPhoneFlag(StatusPhoneFlagModerate.NO)
                .withPermalinkId(permalinkId);
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo);
        steps.organizationSteps()
                .createClientOrganization(campaignInfo.getClientId(), permalinkId, PUBLISHED);
        steps.organizationSteps()
                .linkOrganizationToBanner(campaignInfo.getClientId(), permalinkId, bannerInfo.getBannerId());
        assertResult(false);
    }

    @Test
    public void test_GroupModerated_BannerWith_NoHref_NoModeratedVcard_ManualUnublishedOrganization() {
        long permalinkId = RandomNumberUtils.nextPositiveLong();
        AdGroupInfo adGroupInfo = createAdGroup(StatusPostModerate.YES, StatusModerate.YES);
        OldTextBanner banner = activeTextBanner()
                .withHref(null)
                .withPhoneFlag(StatusPhoneFlagModerate.NO)
                .withPermalinkId(permalinkId);
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo);
        steps.organizationSteps()
                .createClientOrganization(campaignInfo.getClientId(), permalinkId, UNPUBLISHED);
        steps.organizationSteps()
                .linkOrganizationToBanner(campaignInfo.getClientId(), permalinkId, bannerInfo.getBannerId());
        assertResult(true);
    }

    private AdGroupInfo createAdGroup(StatusPostModerate statusPostModerate, StatusModerate statusModerate) {
        TextAdGroup textAdGroup = defaultTextAdGroup(campaignInfo.getCampaignId())
                .withStatusPostModerate(statusPostModerate)
                .withStatusModerate(statusModerate);

        return steps.adGroupSteps().createAdGroup(textAdGroup, campaignInfo);
    }

    private void assertResult(boolean hasRunningUnmoderatedBanners) {
        Map<Long, Boolean> result = bannerService
                .getHasRunningUnmoderatedBannersByCampaignId(campaignInfo.getClientId(),
                        List.of(campaignInfo.getCampaignId()));
        var expectedResult = Map.of(campaignInfo.getCampaignId(), hasRunningUnmoderatedBanners);
        assertThat(result).is(matchedBy(beanDiffer(expectedResult)));
    }
}
