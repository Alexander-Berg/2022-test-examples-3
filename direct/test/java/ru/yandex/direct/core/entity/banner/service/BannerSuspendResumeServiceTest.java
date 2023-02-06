package ru.yandex.direct.core.entity.banner.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesRepository;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.SelfStatus;
import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AdStatesEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AggregatedStatusAdData;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AggregatedStatusAdGroupData;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.NewPerformanceBannerInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.PerformanceBannerSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewPerformanceBanners.fullPerformanceBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerSuspendResumeServiceTest {

    private static final boolean RESUMED = true;
    private static final boolean SUSPENDED = false;

    @Autowired
    private Steps steps;

    @Autowired
    private PerformanceBannerSteps performanceBannerSteps;

    @Autowired
    private BannerSuspendResumeService bannerSuspendResumeService;

    @Autowired
    private BannerTypedRepository bannerRepository;

    @Autowired
    private AggregatedStatusesRepository aggregatedStatusesRepository;

    private ClientId clientId;
    private Long uid;
    private Integer shard;
    private ClientInfo clientInfo;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        uid = clientInfo.getUid();
        shard = clientInfo.getShard();
    }

    /**
     * Обновляет у баннера {@code statusShow} на {@code resume} через вызов {@link BannerSuspendResumeService}
     */
    private MassResult<Long> applyResume(Long bannerId, boolean resume) {
        List<ModelChanges<BannerWithSystemFields>> changes = singletonList(
                new ModelChanges<>(bannerId, BannerWithSystemFields.class).process(resume,
                        BannerWithSystemFields.STATUS_SHOW));

        return bannerSuspendResumeService.suspendResumeBanners(clientId, uid, changes, resume);
    }

    @Test
    public void resumeBanners_setAggregatedStatusIsObsolete() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        Long bannerId = createPerformanceBanner(adGroupInfo, SUSPENDED);

        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();
        createAggregatedStatuses(bannerId, adGroupId, campaignId);

        MassResult<Long> result = applyResume(bannerId, RESUMED);
        assumeThat(result, isSuccessful());

        BannerWithSystemFields actual = getBanner(bannerId);
        assertThat(actual.getStatusShow()).as("statusShow").isTrue();

        checkIsObsolete(bannerId, adGroupId, campaignId, true);
    }

    @Test
    public void suspendBanners_setAggregatedStatusIsObsolete() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        Long bannerId = createPerformanceBanner(adGroupInfo, RESUMED);

        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();
        createAggregatedStatuses(bannerId, adGroupId, campaignId);

        MassResult<Long> result = applyResume(bannerId, SUSPENDED);
        assumeThat(result, isSuccessful());

        BannerWithSystemFields actual = getBanner(bannerId);
        assertThat(actual.getStatusShow()).as("statusShow").isFalse();

        checkIsObsolete(bannerId, adGroupId, campaignId, true);
    }

    @Test
    public void suspendBanners_dontSetAggregatedStatusIsObsolete_whenNotChanged() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        Long bannerId = createPerformanceBanner(adGroupInfo, SUSPENDED);

        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();
        createAggregatedStatuses(bannerId, adGroupId, campaignId);

        MassResult<Long> result = applyResume(bannerId, SUSPENDED);
        assumeThat(result, isSuccessful());

        checkIsObsolete(bannerId, adGroupId, campaignId, false);
    }

    @Test
    public void resumeBanners_dontSetAggregatedStatusIsObsolete_whenAlreadyUpdated() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        Long bannerId = createPerformanceBanner(adGroupInfo, SUSPENDED);

        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();
        // устанавливаем время обновления статусов после времени начала операции
        // статусы в этом случае не должны сбрасываться
        LocalDateTime updateTime = LocalDateTime.now().plusHours(1);
        createAggregatedStatuses(bannerId, adGroupId, campaignId, updateTime);

        MassResult<Long> result = applyResume(bannerId, RESUMED);
        assumeThat(result, isSuccessful());

        checkIsObsolete(bannerId, adGroupId, campaignId, false);
    }

    private void createAggregatedStatuses(Long bannerId, Long adGroupId, Long campaignId) {
        LocalDateTime updateTime = LocalDateTime.now().minusSeconds(1);
        createAggregatedStatuses(bannerId, adGroupId, campaignId, updateTime);
    }

    private void createAggregatedStatuses(Long bannerId, Long adGroupId, Long campaignId, LocalDateTime updateTime) {
        AggregatedStatusAdData adStatus = new AggregatedStatusAdData(List.of(AdStatesEnum.SUSPENDED),
                new SelfStatus(GdSelfStatusEnum.PAUSE_OK, GdSelfStatusReason.SUSPENDED_BY_USER));
        aggregatedStatusesRepository.updateAds(shard, null, Map.of(bannerId, adStatus));

        AggregatedStatusAdGroupData adGroupStatus = new AggregatedStatusAdGroupData(
                null, null, GdSelfStatusEnum.STOP_OK, GdSelfStatusReason.SUSPENDED_BY_USER);
        aggregatedStatusesRepository.updateAdGroups(shard, null, Map.of(adGroupId, adGroupStatus));

        AggregatedStatusCampaignData campaignsStatus = new AggregatedStatusCampaignData(
                null, null, GdSelfStatusEnum.DRAFT, GdSelfStatusReason.DRAFT);
        aggregatedStatusesRepository.updateCampaigns(shard, null, Map.of(campaignId, campaignsStatus));

        aggregatedStatusesRepository.setAdStatusUpdateTime(shard, bannerId, updateTime);
        aggregatedStatusesRepository.setAdGroupStatusUpdateTime(shard, adGroupId, updateTime);
        aggregatedStatusesRepository.setCampaignStatusUpdateTime(shard, campaignId, updateTime);
    }

    private void checkIsObsolete(Long bannerId, Long adGroupId, Long campaignId, boolean isObsolete) {
        Map<Long, Boolean> adStatusesIsObsolete =
                aggregatedStatusesRepository.getAdStatusesIsObsolete(shard, singletonList(bannerId));
        Map<Long, Boolean> adGroupStatusesIsObsolete =
                aggregatedStatusesRepository.getAdGroupStatusesIsObsolete(shard, singletonList(adGroupId));
        Map<Long, Boolean> campaignStatusesIsObsolete =
                aggregatedStatusesRepository.getCampaignStatusesIsObsolete(shard, singletonList(campaignId));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(adStatusesIsObsolete.get(bannerId)).as("adStatusIsObsolete")
                    .isEqualTo(isObsolete);
            // не сбрасываем статус на объектах выше
            softly.assertThat(adGroupStatusesIsObsolete.get(adGroupId)).as("adGroupStatusIsObsolete")
                    .isEqualTo(false);
            softly.assertThat(campaignStatusesIsObsolete.get(campaignId)).as("campaignStatusIsObsolete")
                    .isEqualTo(false);
        });
    }

    private Long createPerformanceBanner(PerformanceAdGroupInfo adGroup, boolean statusShow) {
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(adGroup.getClientInfo());

        PerformanceBanner performanceBanner = fullPerformanceBanner(adGroup.getCampaignId(), adGroup.getAdGroupId(),
                creativeInfo.getCreativeId())
                .withStatusShow(statusShow)
                .withStatusActive(false);

        return performanceBannerSteps.createPerformanceBanner(new NewPerformanceBannerInfo()
                .withAdGroupInfo(adGroup)
                .withClientInfo(adGroup.getClientInfo())
                .withBanner(performanceBanner))
                .getBannerId();
    }

    private BannerWithSystemFields getBanner(Long bannerId) {
        return bannerRepository
                .getStrictlyFullyFilled(shard, singletonList(bannerId), BannerWithSystemFields.class).get(0);
    }
}
