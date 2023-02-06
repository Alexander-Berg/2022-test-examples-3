package ru.yandex.direct.core.copyentity;

import java.util.Set;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithBannersService;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupWithPerformanceFilterService;
import ru.yandex.direct.core.entity.banner.model.BannerWithCreative;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultPerformanceFilter;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.otherFilterConditions;

/**
 * Проверяем копирование групп СМАРТ/PERFORMANCE
 * Проверка остальных объектов группы СМАРТ/PERFORMANCE в {@link CopyOperationCommonAdGroupSmokeTest}
 */
@SuppressWarnings("rawtypes")
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CopyOperationSmartAdGroupSmokeTest {
    @Autowired
    private AdGroupService adgroupService;

    @Autowired
    private BannerService bannerService;

    @Autowired
    private AdGroupWithBannersService adGroupWithBannersService;

    @Autowired
    private PerformanceFilterService performanceFilterService;

    @Autowired
    private AdGroupWithPerformanceFilterService adGroupWithPerformanceFilterService;

    @Autowired
    private BannerTypedRepository newBannerTypedRepository;

    @Autowired
    private Steps steps;

    private Long uid;

    private ClientInfo clientInfoFrom;
    private ClientId clientIdFrom;
    private int shardFrom;
    private Client client;
    private ClientInfo clientInfoTo;

    private Long campaignIdFrom;
    private Long campaignIdDifferentClient;
    private Long adGroupIdFrom;

    @Autowired
    private CopyOperationFactory factory;

    @Before
    public void setUp() {
        var superInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        uid = superInfo.getUid();

        prepareClientData();
        prepareDifferentClientData();
    }

    @Test
    public void copyAdGroupSameCampaign() {
        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfoFrom, adGroupIdFrom, campaignIdFrom, uid);
        var xerox = factory.build(copyConfig);
        var copyResult = xerox.copy();

        checkErrors(copyResult.getMassResult());

        @SuppressWarnings("unchecked")
        Set<Long> copiedAdGroupIds = StreamEx.of(copyResult.getEntityMapping(AdGroup.class).values())
                .select(Long.class)
                .toSet();
        var copiedAdGroups = adgroupService.get(clientIdFrom, uid, copiedAdGroupIds);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(copiedAdGroups)
                .as("группы")
                .hasSize(1);

        var copiedBannerIds = adGroupWithBannersService
                .getChildEntityIdsByParentIds(clientIdFrom, uid, copiedAdGroupIds);
        var copiedBanners = bannerService.get(clientIdFrom, uid, copiedBannerIds);
        soft.assertThat(copiedBanners)
                .as("баннеры")
                .hasSize(1);

        var copiedBannerCreatives = newBannerTypedRepository.getSafely(shardFrom, copiedBannerIds,
                BannerWithCreative.class);
        soft.assertThat(copiedBannerCreatives)
                .as("баннер с креативом")
                .hasSize(1);
        soft.assertThat(copiedBannerCreatives.get(0).getCreativeId())
                .as("креатив")
                .isNotNull();

        var copiedPerformanceFilterIds = adGroupWithPerformanceFilterService
                .getChildEntityIdsByParentIds(clientIdFrom, uid, copiedAdGroupIds);
        var copiedPerformanceFilters = performanceFilterService.get(clientIdFrom, uid, copiedPerformanceFilterIds);
        soft.assertThat(copiedPerformanceFilters)
                .as("фильтр")
                .hasSize(1);

        soft.assertAll();
    }

    @Test
    public void copyAdGroupDifferentCampaignSameClient() {
        var campaignInfoTo = steps.campaignSteps().createActiveSmartCampaign(clientInfoFrom);
        Long campaignIdTo = campaignInfoTo.getCampaignId();

        CopyConfig copyConfig = CopyEntityTestUtils.adGroupCopyConfig(
                clientInfoFrom, adGroupIdFrom, campaignIdFrom, campaignIdTo, uid);
        var xerox = factory.build(copyConfig);
        var copyResult = xerox.copy();

        checkErrors(copyResult.getMassResult());

        @SuppressWarnings("unchecked")
        Set<Long> copiedAdGroupIds = StreamEx.of(copyResult.getEntityMapping(AdGroup.class).values())
                .select(Long.class)
                .toSet();
        var copiedAdGroups = adgroupService.get(clientIdFrom, uid, copiedAdGroupIds);

        SoftAssertions soft = new SoftAssertions();
        assertThat(copiedAdGroups)
                .as("группы")
                .hasSize(1);

        var copiedBannerIds = adGroupWithBannersService
                .getChildEntityIdsByParentIds(clientIdFrom, uid, copiedAdGroupIds);
        var copiedBanners = bannerService.get(clientIdFrom, uid, copiedBannerIds);
        soft.assertThat(copiedBanners)
                .as("баннеры")
                .hasSize(1);

        var copiedBannerCreatives = newBannerTypedRepository.getSafely(shardFrom, copiedBannerIds,
                BannerWithCreative.class);
        soft.assertThat(copiedBannerCreatives)
                .as("баннер с креативом")
                .hasSize(1);
        soft.assertThat(copiedBannerCreatives.get(0).getCreativeId())
                .as("креатив")
                .isNotNull();

        var copiedPerformanceFilterIds = adGroupWithPerformanceFilterService
                .getChildEntityIdsByParentIds(clientIdFrom, uid, copiedAdGroupIds);
        var copiedPerformanceFilters = performanceFilterService.get(clientIdFrom, uid, copiedPerformanceFilterIds);
        soft.assertThat(copiedPerformanceFilters)
                .as("фильтр")
                .hasSize(1);

        soft.assertAll();
    }

    @Test
    public void copyAdGroupDifferentClient() {
        CopyConfig copyConfig = CopyEntityTestUtils.adGroupBetweenClientsCopyConfig(
                clientInfoFrom, clientInfoTo, adGroupIdFrom, campaignIdFrom, campaignIdDifferentClient, uid);

        var xerox = factory.build(copyConfig);

        var copyResult = xerox.copy();
        assertThat(copyResult.getMassResult().getValidationResult().flattenErrors()).isNotEmpty();
    }

    private void checkErrors(MassResult massResult) {
        assertThat(massResult.getValidationResult().flattenErrors()).isEmpty();
    }

    private void prepareClientData() {
        clientInfoFrom = steps.clientSteps().createDefaultClient();

        client = clientInfoFrom.getClient();
        clientIdFrom = clientInfoFrom.getClientId();
        shardFrom = clientInfoFrom.getShard();

        var campaignInfo = steps.campaignSteps().createActiveSmartCampaign(clientInfoFrom);
        campaignIdFrom = campaignInfo.getCampaignId();

        var filterInfo = createPerformanceAdGroup(campaignInfo);
        var adGroupInfo = filterInfo.getAdGroupInfo();
        adGroupIdFrom = filterInfo.getAdGroupId();

        steps.bannerCreativeSteps().createPerformanceBannerCreative(adGroupInfo);
    }

    private void prepareDifferentClientData() {
        clientInfoTo = steps.clientSteps().createDefaultClient();
        clientInfoTo.getClientId();

        var campaignInfoDifferentClient = steps.campaignSteps().createActiveSmartCampaign(clientInfoTo);
        campaignIdDifferentClient = campaignInfoDifferentClient.getCampaignId();
    }

    private PerformanceFilterInfo createPerformanceAdGroup(CampaignInfo campaignInfo) {
        var adGroupInfoFrom = steps.adGroupSteps().addPerformanceAdGroup(new PerformanceAdGroupInfo()
                .withClientInfo(campaignInfo.getClientInfo())
                .withCampaignInfo(campaignInfo));
        var filterFrom = defaultPerformanceFilter(adGroupInfoFrom.getAdGroupId(),
                adGroupInfoFrom.getFeedId())
                .withConditions(otherFilterConditions());
        var performanceFilterInfo = new PerformanceFilterInfo()
                .withAdGroupInfo(adGroupInfoFrom)
                .withFilter(filterFrom);
        return steps.performanceFilterSteps().addPerformanceFilter(performanceFilterInfo);
    }
}
