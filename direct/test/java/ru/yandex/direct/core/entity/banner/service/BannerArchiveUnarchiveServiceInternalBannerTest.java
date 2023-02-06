package ru.yandex.direct.core.entity.banner.service;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class BannerArchiveUnarchiveServiceInternalBannerTest {

    private static final boolean ARCHIVED = true;
    private static final boolean UNARCHIVED = false;

    @Autowired
    private Steps steps;

    @Autowired
    private BannerArchiveUnarchiveService bannerArchiveUnarchiveService;

    @Autowired
    private BannerTypedRepository bannerRepository;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private AdGroupRepository adGroupRepository;

    private ClientId clientId;
    private Long uid;
    private Integer shard;
    private AdGroupInfo adGroupInfo;

    @Before
    public void setUp() {
        ClientInfo clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveInternalAutobudgetCampaign(clientInfo);
        adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);

        clientId = clientInfo.getClientId();
        uid = clientInfo.getUid();
        shard = clientInfo.getShard();
    }


    @Test
    public void archiveUnarchiveBanners_ArchiveSuccess_ForInternalBanners() {
        Long bannerId = steps.bannerSteps().createStoppedInternalBanner(adGroupInfo).getBannerId();

        MassResult<Long> result = applyArchive(bannerId, ARCHIVED);

        assertThat(result.getErrorCount()).as("result error count").isZero();
        BannerWithSystemFields actual = getBanner(bannerId);
        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, List.of(actual.getCampaignId()));
        Campaign campaign = campaigns.get(0);
        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, List.of(actual.getAdGroupId()));
        AdGroup adGroup = adGroups.get(0);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getStatusArchived()).as("statusArchived").isTrue();
            // не трогаем статусы модерации при архивации
            softly.assertThat(actual.getStatusModerate()).as("statusModerate").isEqualTo(BannerStatusModerate.YES);
            softly.assertThat(actual.getStatusPostModerate()).as("statusPostModerate").isEqualTo(BannerStatusPostModerate.YES);

            softly.assertThat(campaign.getStatusModerate()).as("campaigns statusModerate").isEqualTo(CampaignStatusModerate.YES);
            softly.assertThat(campaign.getStatusPostModerate()).as("campaigns statusPostModerate").isEqualTo(CampaignStatusPostmoderate.YES);

            softly.assertThat(adGroup.getStatusModerate()).as("adGroups statusModerate").isEqualTo(StatusModerate.YES);
            softly.assertThat(adGroup.getStatusPostModerate()).as("adGroups statusPostModerate").isEqualTo(StatusPostModerate.YES);
        });
    }

    @Test
    public void archiveUnarchiveBanners_UnarchiveKeepModerationStatus_ForInternalBanners() {
        Long bannerId = steps.bannerSteps().createStoppedInternalBanner(adGroupInfo).getBannerId();

        // нас интересует случай разархивации при наличии условий показа
        steps.retargetingSteps().createDefaultRetargeting(adGroupInfo);

        MassResult<Long> result = applyArchive(bannerId, ARCHIVED);
        assumeThat(result, isSuccessful());

        result = applyArchive(bannerId, UNARCHIVED);

        assertThat(result).as("result error count").is(matchedBy(isSuccessful()));
        assertThat(result.getErrorCount()).as("result error count").isZero();

        BannerWithSystemFields actual = getBanner(bannerId);
        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, List.of(actual.getCampaignId()));
        Campaign campaign = campaigns.get(0);
        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, List.of(actual.getAdGroupId()));
        AdGroup adGroup = adGroups.get(0);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getStatusArchived()).as("statusArchived").isFalse();
            // сохраняем статусы модерации при разархивации
            softly.assertThat(actual.getStatusModerate()).as("statusModerate").isEqualTo(BannerStatusModerate.YES);
            softly.assertThat(actual.getStatusPostModerate()).as("statusPostModerate").isEqualTo(BannerStatusPostModerate.YES);

            softly.assertThat(campaign.getStatusModerate()).as("campaigns statusModerate").isEqualTo(CampaignStatusModerate.YES);
            softly.assertThat(campaign.getStatusPostModerate()).as("campaigns statusPostModerate").isEqualTo(CampaignStatusPostmoderate.YES);

            softly.assertThat(adGroup.getStatusModerate()).as("adGroups statusModerate").isEqualTo(StatusModerate.YES);
            softly.assertThat(adGroup.getStatusPostModerate()).as("adGroups statusPostModerate").isEqualTo(StatusPostModerate.YES);
        });
    }

    @Test
    public void archiveUnarchiveBanners_UnarchiveSuccess_WhenCampaignInDraft() {
        steps.campaignSteps().setStatusModerate(shard, adGroupInfo.getCampaignId(), CampaignStatusModerate.NEW);
        Long bannerId = steps.bannerSteps().createStoppedInternalBanner(adGroupInfo).getBannerId();

        // нас интересует случай разархивации при наличии условий показа
        steps.retargetingSteps().createDefaultRetargeting(adGroupInfo);

        MassResult<Long> result = applyArchive(bannerId, ARCHIVED);
        assumeThat(result, isSuccessful());
        result = applyArchive(bannerId, UNARCHIVED);

        assertThat(result.getErrorCount()).as("result error count").isZero();
        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, List.of(adGroupInfo.getCampaignId()));
        Campaign campaign = campaigns.get(0);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(campaign.getStatusModerate()).as("campaigns statusModerate").isEqualTo(CampaignStatusModerate.READY);
            softly.assertThat(campaign.getStatusPostModerate()).as("campaigns statusPostModerate").isEqualTo(CampaignStatusPostmoderate.YES);
        });
    }


    /**
     * Обновляет у баннера {@code statusArch} на {@code archived} через вызов {@link BannerArchiveUnarchiveService}
     */
    private MassResult<Long> applyArchive(Long bannerId, boolean archived) {
        List<ModelChanges<BannerWithSystemFields>> changes = singletonList(
                new ModelChanges<>(bannerId, BannerWithSystemFields.class)
                        .process(archived, BannerWithSystemFields.STATUS_ARCHIVED));

        return bannerArchiveUnarchiveService.archiveUnarchiveBanners(clientId, uid, changes, archived);
    }

    private BannerWithSystemFields getBanner(Long bannerId) {
        return bannerRepository
                .getStrictlyFullyFilled(shard, singletonList(bannerId), BannerWithSystemFields.class).get(0);
    }
}
