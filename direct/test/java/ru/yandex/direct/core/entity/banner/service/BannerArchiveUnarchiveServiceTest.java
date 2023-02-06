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
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
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
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerArchiveUnarchiveServiceTest {

    private static final boolean ARCHIVED = true;
    private static final boolean UNARCHIVED = false;

    @Autowired
    private PerformanceBannerSteps performanceBannerSteps;

    @Autowired
    private Steps steps;

    @Autowired
    private BannerArchiveUnarchiveService bannerArchiveUnarchiveService;

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
     * Обновляет у баннера {@code statusArch} на {@code archived} через вызов {@link BannerArchiveUnarchiveService}
     */
    private MassResult<Long> applyArchive(Long bannerId, boolean archived) {
        List<ModelChanges<BannerWithSystemFields>> changes = singletonList(
                new ModelChanges<>(bannerId, BannerWithSystemFields.class)
                        .process(archived, BannerWithSystemFields.STATUS_ARCHIVED));

        return bannerArchiveUnarchiveService.archiveUnarchiveBanners(clientId, uid, changes, archived);
    }

    @Test
    public void archiveUnarchiveBanners_archiveSuccess_forPerformanceBanners() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        Long bannerId = createInactivePerformanceBanner(adGroupInfo);

        MassResult<Long> result = applyArchive(bannerId, ARCHIVED);

        assertThat(result.getErrorCount()).as("result error count").isZero();
        BannerWithSystemFields actual = getBanner(bannerId);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getStatusArchived()).as("statusArchived").isTrue();
            // не трогаем статусы модерации при архивации
            softly.assertThat(actual.getStatusModerate()).as("statusModerate").isEqualTo(BannerStatusModerate.YES);
            softly.assertThat(actual.getStatusPostModerate()).as("statusPostModerate").isEqualTo(BannerStatusPostModerate.YES);
        });
    }

    @Test
    public void archiveUnarchiveBanners_unarchiveKeepModerationStatus_forPerformanceBanners() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        Long bannerId = createInactivePerformanceBanner(adGroupInfo);
        // нас интересует случай разархивации при наличии условий показа
        steps.performanceFilterSteps().addDefaultBidsPerformance(adGroupInfo);

        MassResult<Long> result = applyArchive(bannerId, ARCHIVED);
        assumeThat(result, isSuccessful());

        result = applyArchive(bannerId, UNARCHIVED);

        assertThat(result).as("result error count").is(matchedBy(isSuccessful()));
        assertThat(result.getErrorCount()).as("result error count").isZero();
        BannerWithSystemFields actual = getBanner(bannerId);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getStatusArchived()).as("statusArchived").isFalse();
            // для смарт-объявлений сохраняем статусы модерации при разархивации
            softly.assertThat(actual.getStatusModerate()).as("statusModerate").isEqualTo(BannerStatusModerate.YES);
            softly.assertThat(actual.getStatusPostModerate()).as("statusPostModerate").isEqualTo(BannerStatusPostModerate.YES);
        });
    }

    @Test
    public void archiveBanners_setAggregatedStatusIsObsolete() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        Long bannerId = createInactivePerformanceBanner(adGroupInfo);

        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();
        createAggregatedStatuses(bannerId, adGroupId, campaignId);

        MassResult<Long> result = applyArchive(bannerId, ARCHIVED);
        assumeThat(result, isSuccessful());

        checkIsObsolete(bannerId, adGroupId, campaignId);
    }

    @Test
    public void unarchiveBanners_setAggregatedStatusIsObsolete() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        Long bannerId = createInactivePerformanceBanner(adGroupInfo);
        steps.performanceFilterSteps().addDefaultBidsPerformance(adGroupInfo);

        MassResult<Long> result = applyArchive(bannerId, ARCHIVED);
        assumeThat(result, isSuccessful());

        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();
        createAggregatedStatuses(bannerId, adGroupId, campaignId);

        result = applyArchive(bannerId, UNARCHIVED);
        assumeThat(result, isSuccessful());

        checkIsObsolete(bannerId, adGroupId, campaignId);
    }

    /**
     * Создаём неактивное смарт-объявление
     */
    private Long createInactivePerformanceBanner(PerformanceAdGroupInfo adGroupInfo) {
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(adGroupInfo.getClientInfo());

        PerformanceBanner performanceBanner = fullPerformanceBanner(adGroupInfo.getCampaignId(),
                adGroupInfo.getAdGroupId(),
                creativeInfo.getCreativeId())
                .withStatusShow(false)
                .withStatusActive(false);

        return performanceBannerSteps.createPerformanceBanner(new NewPerformanceBannerInfo()
                .withAdGroupInfo(adGroupInfo)
                .withClientInfo(adGroupInfo.getClientInfo())
                .withBanner(performanceBanner))
                .getBannerId();
    }

    private void createAggregatedStatuses(Long bannerId, Long adGroupId, Long campaignId) {
        AggregatedStatusAdData adStatus = new AggregatedStatusAdData(List.of(AdStatesEnum.SUSPENDED),
                new SelfStatus(GdSelfStatusEnum.PAUSE_OK, GdSelfStatusReason.SUSPENDED_BY_USER));
        aggregatedStatusesRepository.updateAds(shard, null, Map.of(bannerId, adStatus));

        AggregatedStatusAdGroupData adGroupStatus = new AggregatedStatusAdGroupData(
                null, null, GdSelfStatusEnum.STOP_OK, GdSelfStatusReason.SUSPENDED_BY_USER);
        aggregatedStatusesRepository.updateAdGroups(shard, null, Map.of(adGroupId, adGroupStatus));

        AggregatedStatusCampaignData campaignsStatus = new AggregatedStatusCampaignData(
                null, null, GdSelfStatusEnum.DRAFT, GdSelfStatusReason.DRAFT);
        aggregatedStatusesRepository.updateCampaigns(shard, null, Map.of(campaignId, campaignsStatus));

        LocalDateTime updateTime = LocalDateTime.now().minusSeconds(1);
        aggregatedStatusesRepository.setAdStatusUpdateTime(shard, bannerId, updateTime);
        aggregatedStatusesRepository.setAdGroupStatusUpdateTime(shard, adGroupId, updateTime);
        aggregatedStatusesRepository.setCampaignStatusUpdateTime(shard, campaignId, updateTime);
    }

    private void checkIsObsolete(Long bannerId, Long adGroupId, Long campaignId) {
        Map<Long, Boolean> adStatusesIsObsolete =
                aggregatedStatusesRepository.getAdStatusesIsObsolete(shard, singletonList(bannerId));
        Map<Long, Boolean> adGroupStatusesIsObsolete =
                aggregatedStatusesRepository.getAdGroupStatusesIsObsolete(shard, singletonList(adGroupId));
        Map<Long, Boolean> campaignStatusesIsObsolete =
                aggregatedStatusesRepository.getCampaignStatusesIsObsolete(shard, singletonList(campaignId));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(adStatusesIsObsolete.get(bannerId)).as("adStatusIsObsolete").isTrue();
            // не сбрасываем статус на объектах выше
            softly.assertThat(adGroupStatusesIsObsolete.get(adGroupId)).as("adGroupStatusIsObsolete").isFalse();
            softly.assertThat(campaignStatusesIsObsolete.get(campaignId)).as("campaignStatusIsObsolete").isFalse();
        });
    }

    private BannerWithSystemFields getBanner(Long bannerId) {
        return bannerRepository
                .getStrictlyFullyFilled(shard, singletonList(bannerId), BannerWithSystemFields.class).get(0);
    }
}
