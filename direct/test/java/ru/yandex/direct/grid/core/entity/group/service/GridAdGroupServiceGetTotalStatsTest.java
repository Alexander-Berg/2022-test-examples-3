package ru.yandex.direct.grid.core.entity.group.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.adgroup.service.geotree.AdGroupGeoTreeProviderFactory;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.service.AdGroupAdditionalTargetingService;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.service.ClientGeoService;
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.configuration.GridCoreTest;
import ru.yandex.direct.grid.core.entity.banner.repository.GridImageRepository;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.AdGroupFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupFilter;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupsWithTotals;
import ru.yandex.direct.grid.core.entity.group.repository.GridAdGroupYtRepository;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.entity.recommendation.service.GridRecommendationService;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.regions.GeoTreeFactory;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

@GridCoreTest
@RunWith(SpringRunner.class)
public class GridAdGroupServiceGetTotalStatsTest {
    private static final AdGroupFetchedFieldsResolver AD_GROUP_FETCHED_FIELDS_RESOLVER =
            FetchedFieldsResolverCoreUtil.buildAdGroupFetchedFieldsResolver(true);

    @Autowired
    private Steps steps;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private MinusKeywordsPackRepository minusKeywordsPackRepository;
    @Autowired
    private AdGroupService adGroupService;
    @Autowired
    private ClientGeoService clientGeoService;
    @Autowired
    private GeoTreeFactory geoTreeFactory;
    @Autowired
    private AdGroupGeoTreeProviderFactory geoTreeProviderFactory;
    @Autowired
    private BidModifierService bidModifierService;
    @Autowired
    private GridImageRepository gridImageRepository;
    @Autowired
    private BannerTypedRepository bannerTypedRepository;
    @Autowired
    private BannerService bannerService;
    @Autowired
    private AdGroupAdditionalTargetingService adGroupAdditionalTargetingService;
    @Autowired
    private FeatureService featureService;
    @Autowired
    private CryptaSegmentRepository cryptaSegmentRepository;
    @Autowired
    private RelevanceMatchRepository relevanceMatchRepository;
    @Autowired
    private MetrikaGoalsService metrikaGoalsService;

    private GridAdGroupYtRepository gridAdGroupYtRepository;
    private GridAdGroupService gridAdGroupService;

    private ClientInfo clientInfo;
    private UserInfo userInfo;
    private ClientId clientId;
    private int shard;
    private AdGroupInfo adGroupActiveInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        userInfo = clientInfo.getChiefUserInfo();

        adGroupActiveInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        steps.bannerSteps().createActiveTextBanner(adGroupActiveInfo);

        gridAdGroupYtRepository = mock(GridAdGroupYtRepository.class);
        GridRecommendationService gridRecommendationService = mock(GridRecommendationService.class);

        gridAdGroupService = new GridAdGroupService(gridAdGroupYtRepository, adGroupRepository, campaignRepository,
                minusKeywordsPackRepository, adGroupService, bidModifierService, clientGeoService,
                geoTreeFactory, gridRecommendationService, gridImageRepository, bannerTypedRepository, bannerService,
                adGroupAdditionalTargetingService, geoTreeProviderFactory, featureService, cryptaSegmentRepository,
                relevanceMatchRepository, metrikaGoalsService);
    }

    /**
     * Из БД получили список групп с общей статистикой -> итоговая статистика возвращается
     */
    @Test
    public void getGroups_ReceivedGroupsWithTotalStats_GetTotalStats() {
        List<GdiGroup> gdiGroups = singletonList(new GdiGroup()
                .withId(adGroupActiveInfo.getAdGroupId())
                .withCampaignId(adGroupActiveInfo.getCampaignId())
                .withType(adGroupActiveInfo.getAdGroupType()));
        GdiGroupsWithTotals gdiGroupsWithTotals = new GdiGroupsWithTotals()
                .withGdiGroups(gdiGroups)
                .withTotalStats(new GdiEntityStats());

        doAnswer((Answer<GdiGroupsWithTotals>) invocation -> gdiGroupsWithTotals)
                .when(gridAdGroupYtRepository)
                .getGroups(anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean());

        GdiGroupsWithTotals receivedGroupsWithTotals = getClientGroups(new GdiGroupFilter());

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(receivedGroupsWithTotals.getGdiGroups())
                .as("должна вернуться одна группа")
                .hasSize(1);
        soft.assertThat(receivedGroupsWithTotals.getTotalStats())
                .as("должна вернуться итоговая статистика")
                .isNotNull();
        soft.assertAll();
    }

    /**
     * Из БД получили пустой список групп с общей статистикой -> итоговая статистика возвращается
     */
    @Test
    public void getGroups_ReceivedEmptyListWithTotalStats_GetTotalStats() {
        GdiGroupsWithTotals gdiGroupsWithTotals = new GdiGroupsWithTotals()
                .withGdiGroups(Collections.emptyList())
                .withTotalStats(new GdiEntityStats());

        doAnswer((Answer<GdiGroupsWithTotals>) invocation -> gdiGroupsWithTotals)
                .when(gridAdGroupYtRepository)
                .getGroups(anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean());

        GdiGroupsWithTotals receivedGroupsWithTotals = getClientGroups(new GdiGroupFilter());

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(receivedGroupsWithTotals.getGdiGroups())
                .as("вернулся пустой список групп")
                .isEmpty();
        soft.assertThat(receivedGroupsWithTotals.getTotalStats())
                .as("должна вернуться итоговая статистика")
                .isNotNull();
        soft.assertAll();
    }

    /**
     * Из БД получили список групп без общей статистики -> итоговая статистика не возвращается
     */
    @Test
    public void getGroups_ReceivedGroupsWithoutTotalStats_DoNotGetTotalStats() {
        List<GdiGroup> gdiGroups = singletonList(new GdiGroup()
                .withId(adGroupActiveInfo.getAdGroupId())
                .withCampaignId(adGroupActiveInfo.getCampaignId())
                .withType(adGroupActiveInfo.getAdGroupType()));
        GdiGroupsWithTotals gdiGroupsWithTotals = new GdiGroupsWithTotals()
                .withGdiGroups(gdiGroups);

        doAnswer((Answer<GdiGroupsWithTotals>) invocation -> gdiGroupsWithTotals)
                .when(gridAdGroupYtRepository)
                .getGroups(anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean());

        GdiGroupsWithTotals receivedGroupsWithTotals = getClientGroups(new GdiGroupFilter());

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(receivedGroupsWithTotals.getGdiGroups())
                .as("должна вернуться одна группа")
                .hasSize(1);
        soft.assertThat(receivedGroupsWithTotals.getTotalStats())
                .as("нет итоговой статистики")
                .isNull();
        soft.assertAll();
    }

    private GdiGroupsWithTotals getClientGroups(GdiGroupFilter filter) {
        GdStatRequirements statRequirements = new GdStatRequirements()
                .withFrom(LocalDate.now())
                .withTo(LocalDate.now());
        return gridAdGroupService.getGroups(shard, clientInfo.getClient().getCountryRegionId(), clientId,
                userInfo.getUser(), filter, Collections.emptySet(), Collections.emptyList(), statRequirements,
                false, Collections.emptySet(), AD_GROUP_FETCHED_FIELDS_RESOLVER);
    }
}
