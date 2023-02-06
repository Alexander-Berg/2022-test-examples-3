package ru.yandex.direct.core.copyentity;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithBannersService;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignWithAdGroupsService;
import ru.yandex.direct.core.entity.campaign.service.CopyCampaignService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignCopyWithStatusesServiceOptionsTest {
    @Autowired
    private Steps steps;

    @Autowired
    private CopyOperationAssert asserts;

    @Autowired
    private CopyCampaignService copyCampaignService;

    @Autowired
    private CampaignWithAdGroupsService campaignWithAdGroupsService;

    @Autowired
    private AdGroupWithBannersService adGroupWithBannersService;

    private Long operatorUid;

    private ClientId clientIdFrom;

    private ClientId clientIdTo;

    private AdGroupInfo adGroupInfo;
    private Long campaignId;

    @Before
    public void setUp() {
        var clientInfoSuper = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        operatorUid = clientInfoSuper.getUid();

        ClientInfo clientInfoFrom = steps.clientSteps().createDefaultClient();
        clientIdFrom = clientInfoFrom.getClientId();
        steps.featureSteps().setCurrentClient(clientIdFrom);

        ClientInfo clientInfoTo = steps.clientSteps().createDefaultClientAnotherShard();
        clientIdTo = clientInfoTo.getClientId();

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientIdFrom, clientInfoFrom.getUid()).withEmail("test@yandex-team.ru"),
                clientInfoFrom);
        campaignId = campaignInfo.getCampaignId();

        asserts.init(clientIdFrom, clientIdTo, operatorUid);

        adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfo);

        createActiveBanner();
        createStoppedBanner();
        createArchivedBanner();
        createModeratedBanner();

        createActiveKeyword();
        createSuspendedKeyword();
    }

    @Test
    public void doNotCopyStoppedBanners() {
        var flags = new CopyCampaignFlags();

        var copyResult = copyCampaignService.copyCampaigns(
                clientIdFrom, clientIdTo, operatorUid, List.of(campaignId), flags);
        asserts.checkErrors(copyResult);

        Set<Long> copiedAdGroupIds = getCopiedAdGroupIds(copyResult);
        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientIdTo, operatorUid,
                copiedAdGroupIds);

        assertThat(copiedBannerIds).hasSize(2);
    }

    @Test
    public void copyStoppedBanners() {
        var flags = new CopyCampaignFlags.Builder().withCopyStopped(true).build();

        var copyResult = copyCampaignService.copyCampaigns(
                clientIdFrom, clientIdTo, operatorUid, List.of(campaignId), flags);
        asserts.checkErrors(copyResult);

        Set<Long> copiedAdGroupIds = getCopiedAdGroupIds(copyResult);
        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientIdTo, operatorUid,
                copiedAdGroupIds);

        assertThat(copiedBannerIds).hasSize(3);
    }

    @Test
    public void doNotCopyArchivedBanners() {
        var flags = new CopyCampaignFlags();

        var copyResult = copyCampaignService.copyCampaigns(
                clientIdFrom, clientIdTo, operatorUid, List.of(campaignId), flags);
        asserts.checkErrors(copyResult);

        Set<Long> copiedAdGroupIds = getCopiedAdGroupIds(copyResult);
        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientIdTo, operatorUid,
                copiedAdGroupIds);

        assertThat(copiedBannerIds).hasSize(2);
    }

    @Test
    public void copyArchivedBanners() {
        var flags = new CopyCampaignFlags.Builder().withCopyArchived(true).build();

        var copyResult = copyCampaignService.copyCampaigns(
                clientIdFrom, clientIdTo, operatorUid, List.of(campaignId), flags);
        asserts.checkErrors(copyResult);

        Set<Long> copiedAdGroupIds = getCopiedAdGroupIds(copyResult);
        var copiedBannerIds = adGroupWithBannersService.getChildEntityIdsByParentIds(clientIdTo, operatorUid,
                copiedAdGroupIds);

        assertThat(copiedBannerIds).hasSize(3);
    }

    private void createActiveBanner() {
        steps.bannerSteps().createBanner(activeTextBanner().withHref("http://banners.com/1")
                        .withDomain("banners.com").withReverseDomain("moc.srennab"),
                adGroupInfo);
    }

    private void createStoppedBanner() {
        steps.bannerSteps().createBanner(activeTextBanner().withHref("http://banners.com/2")
                        .withDomain("banners.com").withReverseDomain("moc.srennab")
                        .withStatusShow(false),
                adGroupInfo);
    }

    private void createArchivedBanner() {
        steps.bannerSteps().createBanner(activeTextBanner().withHref("http://banners.com/3")
                        .withDomain("banners.com").withReverseDomain("moc.srennab")
                        .withStatusArchived(true),
                adGroupInfo);
    }

    private void createModeratedBanner() {
        steps.bannerSteps().createBanner(activeTextBanner().withHref("http://banners.com/3")
                        .withDomain("banners.com").withReverseDomain("moc.srennab")
                        .withStatusModerate(OldBannerStatusModerate.YES),
                adGroupInfo);
    }


    private void createActiveKeyword() {
        steps.keywordSteps().createKeyword(adGroupInfo, defaultKeyword().withPhrase("phrase")
                .withPhraseBsId(BigInteger.valueOf(101L)));
    }

    private void createSuspendedKeyword() {
        steps.keywordSteps().createKeyword(adGroupInfo, defaultKeyword().withPhrase("phrase suspended")
                .withIsSuspended(true)
                .withPhraseBsId(BigInteger.valueOf(102L)));
    }

    @SuppressWarnings("unchecked")
    private Set<Long> getCopiedAdGroupIds(CopyResult copyResult) {
        var copiedCampaignIds = Set.copyOf(copyResult.getEntityMapping(BaseCampaign.class).values());
        return campaignWithAdGroupsService.getChildEntityIdsByParentIds(clientIdTo, operatorUid, copiedCampaignIds);
    }
}
