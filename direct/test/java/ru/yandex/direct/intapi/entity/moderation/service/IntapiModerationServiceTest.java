package ru.yandex.direct.intapi.entity.moderation.service;

import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.draftTextBanner;
import static ru.yandex.direct.core.testing.data.TestGroups.draftTextAdgroup;

@IntApiTest
@ParametersAreNonnullByDefault
@RunWith(SpringJUnit4ClassRunner.class)
public class IntapiModerationServiceTest {

    private static final Long DIRECT_MONITORING_AGENCY_ID = (long) RandomUtils.nextInt();
    private static final Long NOT_DIRECT_MONITORING_AGENCY_ID = DIRECT_MONITORING_AGENCY_ID + 1;

    @Autowired
    private IntapiModerationService intapiModerationService;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private Steps steps;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private BannerTypedRepository bannerTypedRepository;
    @Autowired
    private KeywordRepository keywordRepository;

    private int shard;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        ppcPropertiesSupport.set(PpcPropertyNames.BS_AGENCY_IDS_FOR_DIRECT_MONITORING.getName(),
                DIRECT_MONITORING_AGENCY_ID.toString());
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
    }

    @Test
    public void moderateCampaigns_EmptyCampaignIds_ResultIsEmpty() {
        assertThat(intapiModerationService.moderateCampaigns(Collections.emptyList())).isEmpty();
    }

    @Test
    public void moderateCampaigns_TextCampaignWithDirectMonitoringAgencyId_CampaignIsModerated() {
        CampaignInfo campaignInfo = createCampaign(true, DIRECT_MONITORING_AGENCY_ID);
        List<Long> campaignIds = List.of(campaignInfo.getCampaignId());

        List<Long> moderatedCampaignIds = intapiModerationService.moderateCampaigns(campaignIds);
        assertThat(moderatedCampaignIds).isEqualTo(campaignIds);

        checkStatuses(campaignInfo.getCampaignId(), true);
    }

    @Test
    public void moderateCampaigns_NotTextCampaignWithDirectMonitoringAgencyId_CampaignIsNotModerated() {
        CampaignInfo campaignInfo = createCampaign(false, DIRECT_MONITORING_AGENCY_ID);
        List<Long> campaignIds = List.of(campaignInfo.getCampaignId());

        List<Long> moderatedCampaignIds = intapiModerationService.moderateCampaigns(campaignIds);
        assertThat(moderatedCampaignIds).isEmpty();

        checkStatuses(campaignInfo.getCampaignId(), false);
    }

    @Test
    public void moderateCampaigns_TextCampaignWithNotDirectMonitoringAgencyId_CampaignIsNotModerated() {
        CampaignInfo campaignInfo = createCampaign(true, NOT_DIRECT_MONITORING_AGENCY_ID);
        List<Long> campaignIds = List.of(campaignInfo.getCampaignId());

        List<Long> moderatedCampaignIds = intapiModerationService.moderateCampaigns(campaignIds);
        assertThat(moderatedCampaignIds).isEmpty();

        checkStatuses(campaignInfo.getCampaignId(), false);
    }

    @Test
    public void moderateCampaigns_TextCampaignWithoutAgencyId_CampaignIsNotModerated() {
        CampaignInfo campaignInfo = createCampaign(true, 0L);
        List<Long> campaignIds = List.of(campaignInfo.getCampaignId());

        List<Long> moderatedCampaignIds = intapiModerationService.moderateCampaigns(campaignIds);
        assertThat(moderatedCampaignIds).isEmpty();

        checkStatuses(campaignInfo.getCampaignId(), false);
    }

    @Test
    public void moderateCampaigns_SeveralCampaigns_ResultContainsTextCampaignsWithDirectMonitoringAgencyIds() {
        Long textCampaignWithDirectMonitoringAgencyId = createCampaign(true, DIRECT_MONITORING_AGENCY_ID)
                .getCampaignId();
        Long textCampaignWithDirectMonitoringAgencyIdSecond = createCampaign(true, DIRECT_MONITORING_AGENCY_ID)
                .getCampaignId();
        Long notTextCampaignWithDirectMonitoringAgencyId = createCampaign(false, DIRECT_MONITORING_AGENCY_ID)
                .getCampaignId();
        Long textCampaignWithNotDirectMonitoringAgencyId = createCampaign(false, NOT_DIRECT_MONITORING_AGENCY_ID)
                .getCampaignId();
        List<Long> campaignIds = List.of(
                textCampaignWithDirectMonitoringAgencyId, textCampaignWithDirectMonitoringAgencyIdSecond,
                notTextCampaignWithDirectMonitoringAgencyId, textCampaignWithNotDirectMonitoringAgencyId);

        List<Long> moderatedCampaignIds = intapiModerationService.moderateCampaigns(campaignIds);
        assertThat(moderatedCampaignIds).isEqualTo(
                List.of(textCampaignWithDirectMonitoringAgencyId, textCampaignWithDirectMonitoringAgencyIdSecond));
    }

    private CampaignInfo createCampaign(boolean isTextCampaign, Long agencyId) {
        var campaign = isTextCampaign ? TestCampaigns.newTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                : TestCampaigns.newDynamicCampaign(clientInfo.getClientId(), clientInfo.getUid());
        campaign.withStatusModerate(ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate.NEW)
                .withAgencyId(agencyId);
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign);
        AdGroupInfo adGroupInfo = steps.adGroupSteps()
                .createAdGroup(draftTextAdgroup(campaignInfo.getCampaignId()), campaignInfo);
        steps.bannerSteps().createBanner(draftTextBanner(), adGroupInfo);
        steps.keywordSteps().createKeyword(adGroupInfo);
        return campaignInfo;
    }

    private void checkStatuses(long campaignId, boolean isModerated) {
        List<Long> campaignIds = List.of(campaignId);

        var expectedCampaignStatusModerate = isModerated ? CampaignStatusModerate.YES : CampaignStatusModerate.NEW;
        var expectedCampaignStatusPostModerate = isModerated ? CampaignStatusPostmoderate.ACCEPTED
                : CampaignStatusPostmoderate.NEW;
        var expectedAdGroupStatusModerate = isModerated ? StatusModerate.YES : StatusModerate.NEW;
        var expectedAdGroupStatusPostModerate = isModerated ? StatusPostModerate.YES : StatusPostModerate.READY;
        var expectedBannerStatusModerate = isModerated ?
                BannerStatusModerate.YES
                : BannerStatusModerate.NEW;
        var expectedBannerStatusPostModerate = isModerated
                ? BannerStatusPostModerate.YES
                : BannerStatusPostModerate.NEW;
        var expectedKeywordStatusModerate = isModerated ? ru.yandex.direct.core.entity.keyword.model.StatusModerate.YES
                : ru.yandex.direct.core.entity.keyword.model.StatusModerate.NEW;

        List<Campaign> campaignsAfterChange = campaignRepository.getCampaigns(shard, campaignIds);
        assertThat(campaignsAfterChange).hasSize(1);
        assertThat(campaignsAfterChange.get(0).getStatusModerate()).isEqualTo(expectedCampaignStatusModerate);
        assertThat(campaignsAfterChange.get(0).getStatusPostModerate()).isEqualTo(expectedCampaignStatusPostModerate);

        List<Long> adGroupIds = adGroupRepository.getAdGroupIdsByCampaignIds(shard, campaignIds).get(campaignId);
        List<AdGroup> adGroupsAfterChange = adGroupRepository.getAdGroups(shard, adGroupIds);
        assertThat(adGroupsAfterChange).hasSize(1);
        assertThat(adGroupsAfterChange.get(0).getStatusModerate()).isEqualTo(expectedAdGroupStatusModerate);
        assertThat(adGroupsAfterChange.get(0).getStatusPostModerate()).isEqualTo(expectedAdGroupStatusPostModerate);

        List<TextBanner> bannersAfterChange = bannerTypedRepository.getBannersByCampaignIdsAndClass(shard,
                campaignIds, TextBanner.class);
        assertThat(bannersAfterChange).hasSize(1);
        assertThat(bannersAfterChange.get(0).getStatusModerate()).isEqualTo(expectedBannerStatusModerate);
        assertThat(bannersAfterChange.get(0).getStatusPostModerate()).isEqualTo(expectedBannerStatusPostModerate);

        List<Keyword> keywordsAfterChange = keywordRepository
                .getKeywordsByCampaignId(shard, campaignId);
        assertThat(keywordsAfterChange).hasSize(1);
        assertThat(keywordsAfterChange.get(0).getStatusModerate()).isEqualTo(expectedKeywordStatusModerate);
    }
}
