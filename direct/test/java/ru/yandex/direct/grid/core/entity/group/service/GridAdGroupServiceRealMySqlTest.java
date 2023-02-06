package ru.yandex.direct.grid.core.entity.group.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
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
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.configuration.GridCoreTest;
import ru.yandex.direct.grid.core.entity.banner.repository.GridImageRepository;
import ru.yandex.direct.grid.core.entity.fetchedfieldresolver.AdGroupFetchedFieldsResolver;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupFilter;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupsWithTotals;
import ru.yandex.direct.grid.core.entity.group.model.GdiMinusKeywordsPackInfo;
import ru.yandex.direct.grid.core.entity.group.repository.GridAdGroupYtRepository;
import ru.yandex.direct.grid.core.entity.recommendation.service.GridRecommendationService;
import ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.regions.GeoTreeFactory;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.libraryMinusKeywordsPack;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridCoreTest
@RunWith(SpringRunner.class)
public class GridAdGroupServiceRealMySqlTest {
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
    private TestMinusKeywordsPackRepository testMinusKeywordsPackRepository;
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
    private GridRecommendationService gridRecommendationService;
    private GridAdGroupService gridAdGroupService;

    private ClientInfo clientInfo;
    private UserInfo userInfo;
    private ClientId clientId;
    private int shard;
    private List<Long> libraryMinusKeywordsPacks;

    private AdGroupInfo adGroupInfo;

    private Long namedPackId1;
    private Long namedPackId2;
    private Long adGroupIdWithNamedPack1;
    private Long adGroupIdWithNamedPack2;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        userInfo = clientInfo.getChiefUserInfo();

        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        GdiGroup gdiGroup = new GdiGroup()
                .withId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withType(adGroupInfo.getAdGroupType());

        libraryMinusKeywordsPacks = steps.minusKeywordsPackSteps().createLibraryMinusKeywordsPacks(clientInfo, 2);

        namedPackId1 = steps.minusKeywordsPackSteps().createMinusKeywordsPack(libraryMinusKeywordsPack()
                .withName("pack one"), clientInfo).getMinusKeywordPackId();
        namedPackId2 = steps.minusKeywordsPackSteps().createMinusKeywordsPack(libraryMinusKeywordsPack()
                .withName("pack two"), clientInfo).getMinusKeywordPackId();
        adGroupIdWithNamedPack1 = steps.adGroupSteps().createActiveTextAdGroup(clientInfo).getAdGroupId();
        adGroupIdWithNamedPack2 = steps.adGroupSteps().createActiveTextAdGroup(clientInfo).getAdGroupId();
        linkToAdGroup(namedPackId1, adGroupIdWithNamedPack1);
        linkToAdGroup(namedPackId2, adGroupIdWithNamedPack2);

        gridAdGroupYtRepository = mock(GridAdGroupYtRepository.class);

        doAnswer((Answer<GdiGroupsWithTotals>) invocation -> {
            GdiGroupFilter filter = invocation.getArgument(1);
            // если фильтр groupIdIn задан и пуст, то вернуть пустой результат
            if (filter.getGroupIdIn() != null && filter.getGroupIdIn().isEmpty()) {
                return new GdiGroupsWithTotals().withGdiGroups(emptyList());
            }
            return new GdiGroupsWithTotals().withGdiGroups(singletonList(gdiGroup));
        }).when(gridAdGroupYtRepository)
                .getGroups(anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean());


        gridRecommendationService = mock(GridRecommendationService.class);
        gridAdGroupService =
                new GridAdGroupService(gridAdGroupYtRepository, adGroupRepository, campaignRepository,
                        minusKeywordsPackRepository, adGroupService, bidModifierService, clientGeoService,
                        geoTreeFactory, gridRecommendationService, gridImageRepository, bannerTypedRepository,
                        bannerService, adGroupAdditionalTargetingService, geoTreeProviderFactory, featureService,
                        cryptaSegmentRepository, relevanceMatchRepository, metrikaGoalsService);
    }

    @Test
    public void adGroupsLibraryMinusKeywordsPacksFetched() {

        libraryMinusKeywordsPacks.forEach(packId -> linkToAdGroup(packId, adGroupInfo.getAdGroupId()));

        Map<Long, MinusKeywordsPack> minusKeywordsPacks =
                minusKeywordsPackRepository.getMinusKeywordsPacks(shard, clientId, libraryMinusKeywordsPacks);

        List<GdiGroup> result = getClientGroups(new GdiGroupFilter());
        assertThat("должна вернуться одна группа", result, hasSize(1));
        assertThat(result.get(0).getLibraryMinusKeywordsPacks(),
                containsInAnyOrder(mapList(minusKeywordsPacks.values(), this::toGdiMinusKeywordPack).toArray()));
    }

    @Test
    public void adGroupFetchedByLibraryMwIdFilter() {
        libraryMinusKeywordsPacks.forEach(packId -> linkToAdGroup(packId, adGroupInfo.getAdGroupId()));

        Map<Long, MinusKeywordsPack> minusKeywordsPacks =
                minusKeywordsPackRepository.getMinusKeywordsPacks(shard, clientId, libraryMinusKeywordsPacks);

        List<GdiGroup> result =
                getClientGroups(new GdiGroupFilter().withLibraryMwIdIn(singleton(libraryMinusKeywordsPacks.get(1))));
        assertThat("должна вернуться одна группа", result, hasSize(1));
        assertThat(result.get(0).getLibraryMinusKeywordsPacks(),
                containsInAnyOrder(mapList(minusKeywordsPacks.values(), this::toGdiMinusKeywordPack).toArray()));
    }

    @Test
    public void adGroupNotFetchedBecauseOfLibraryMwIdFilter() {
        linkToAdGroup(libraryMinusKeywordsPacks.get(0), adGroupInfo.getAdGroupId());

        List<GdiGroup> result =
                getClientGroups(new GdiGroupFilter().withLibraryMwIdIn(singleton(libraryMinusKeywordsPacks.get(1))));
        assertThat("не должно вернуться групп, так как они отсеялись по фильтру", result, hasSize(0));
    }

    private GdiMinusKeywordsPackInfo toGdiMinusKeywordPack(MinusKeywordsPack minusKeywordsPack) {
        return new GdiMinusKeywordsPackInfo()
                .withId(minusKeywordsPack.getId())
                .withName(minusKeywordsPack.getName());
    }

    private List<GdiGroup> getClientGroups(GdiGroupFilter filter) {
        GdStatRequirements statRequirements = new GdStatRequirements()
                .withFrom(LocalDate.now())
                .withTo(LocalDate.now());
        GdiGroupsWithTotals gdiGroupsWithTotals = gridAdGroupService
                .getGroups(shard, clientInfo.getClient().getCountryRegionId(), clientId, userInfo.getUser(),
                        filter, Collections.emptySet(), Collections.emptyList(), statRequirements,
                        false, Collections.emptySet(), AD_GROUP_FETCHED_FIELDS_RESOLVER);
        return gdiGroupsWithTotals.getGdiGroups();
    }

    @Test
    public void updateFilterIfMwFilterSpecified_FilterLibraryMwIdInIsEmpty_EmptyGroupIdInFilter() {
        GdiGroupFilter filter = new GdiGroupFilter()
                .withGroupIdIn(singleton(1L))
                .withLibraryMwIdIn(emptySet());
        gridAdGroupService.updateFilterIfMwFilterSpecified(shard, clientId, filter);

        assertThat(filter.getGroupIdIn(), hasSize(0));
    }

    @Test
    public void updateFilterIfMwFilterSpecified_FilterLibraryMwIdInIsNull_GroupIdInFilterIsNotChanged() {
        GdiGroupFilter filter = new GdiGroupFilter()
                .withGroupIdIn(singleton(1L))
                .withLibraryMwIdIn(null);
        gridAdGroupService.updateFilterIfMwFilterSpecified(shard, clientId, filter);

        assertThat(filter.getGroupIdIn(), equalTo(singleton(1L)));
    }

    @Test
    public void updateFilterIfMwFilterSpecified_MysqlAdGroupIdsIsEmptyAndFilterGroupIdInIsSpecified_EmptyGroupIdInFilter() {
        Long libMwId = libraryMinusKeywordsPacks.get(0);
        GdiGroupFilter filter = new GdiGroupFilter()
                .withGroupIdIn(singleton(1L))
                .withLibraryMwIdIn(singleton(libMwId));

        gridAdGroupService.updateFilterIfMwFilterSpecified(shard, clientId, filter);

        assertThat(filter.getGroupIdIn(), hasSize(0));
    }

    @Test
    public void updateFilterIfMwFilterSpecified_MysqlAdGroupIdsIsEmptyAndFilterGroupIdInIsNotSpecified_EmptyGroupIdInFilter() {
        Long libMwId = libraryMinusKeywordsPacks.get(0);
        GdiGroupFilter filter = new GdiGroupFilter()
                .withLibraryMwIdIn(singleton(libMwId));

        gridAdGroupService.updateFilterIfMwFilterSpecified(shard, clientId, filter);

        assertThat(filter.getGroupIdIn(), hasSize(0));
    }

    @Test
    public void updateFilterIfMwFilterSpecified_MysqlAdGroupIdsIsNotEmptyAndFilterGroupIdInIsSpecified_GroupIdInFilterIsSpecified() {
        Long libMwId = libraryMinusKeywordsPacks.get(0);

        Long adGroupIdWithoutLibMw = createAdGroup();
        Long adGroupIdForLink1 = createAdGroup();
        Long adGroupIdForLink2 = createAdGroup();
        linkToAdGroup(libMwId, adGroupIdForLink1);
        linkToAdGroup(libMwId, adGroupIdForLink2);

        Set<Long> adGroupIdsFilter = new HashSet<>(asList(adGroupIdForLink1, adGroupIdWithoutLibMw));
        GdiGroupFilter filter = new GdiGroupFilter()
                .withGroupIdIn(adGroupIdsFilter)
                .withLibraryMwIdIn(singleton(libMwId));

        gridAdGroupService.updateFilterIfMwFilterSpecified(shard, clientId, filter);

        assertThat(filter.getGroupIdIn(), equalTo(singleton(adGroupIdForLink1)));
    }

    @Test
    public void updateFilterIfMwFilterSpecified_MysqlAdGroupIdsIsNotEmptyAndFilterGroupIdInIsNotSpecified_GroupIdInFilterIsSpecified() {
        Long libMwId = libraryMinusKeywordsPacks.get(0);
        Long adGroupIdForLink = adGroupInfo.getAdGroupId();
        linkToAdGroup(libMwId, adGroupIdForLink);

        GdiGroupFilter filter = new GdiGroupFilter()
                .withLibraryMwIdIn(singleton(libMwId));
        gridAdGroupService.updateFilterIfMwFilterSpecified(shard, clientId, filter);

        assertThat(filter.getGroupIdIn(), equalTo(singleton(adGroupIdForLink)));
    }

    @Test
    public void updateFilterIfMwFilterSpecified_FilteredByPackNameContains() {
        GdiGroupFilter filter = new GdiGroupFilter()
                .withGroupIdIn(ImmutableSet.of(adGroupIdWithNamedPack1, adGroupIdWithNamedPack2))
                .withMwPackNameContains("one");
        gridAdGroupService.updateFilterIfMwFilterSpecified(shard, clientId, filter);

        assertThat(filter.getGroupIdIn(), equalTo(singleton(adGroupIdWithNamedPack1)));
    }

    @Test
    public void updateFilterIfMwFilterSpecified_FilteredByPackNameNotContains() {
        GdiGroupFilter filter = new GdiGroupFilter()
                .withGroupIdIn(ImmutableSet.of(adGroupIdWithNamedPack1, adGroupIdWithNamedPack2))
                .withMwPackNameNotContains("one");
        gridAdGroupService.updateFilterIfMwFilterSpecified(shard, clientId, filter);

        assertThat(filter.getGroupIdIn(), equalTo(singleton(adGroupIdWithNamedPack2)));
    }

    @Test
    public void updateFilterIfMwFilterSpecified_NameContainsFilterIgnoresCase() {
        GdiGroupFilter filter = new GdiGroupFilter()
                .withGroupIdIn(ImmutableSet.of(adGroupIdWithNamedPack1, adGroupIdWithNamedPack2))
                .withMwPackNameContains("OnE");
        gridAdGroupService.updateFilterIfMwFilterSpecified(shard, clientId, filter);

        assertThat(filter.getGroupIdIn(), equalTo(singleton(adGroupIdWithNamedPack1)));
    }

    @Test
    public void updateFilterIfMwFilterSpecified_NameNotContainsFilterIgnoresCase() {
        GdiGroupFilter filter = new GdiGroupFilter()
                .withGroupIdIn(ImmutableSet.of(adGroupIdWithNamedPack1, adGroupIdWithNamedPack2))
                .withMwPackNameNotContains("TWo");
        gridAdGroupService.updateFilterIfMwFilterSpecified(shard, clientId, filter);

        assertThat(filter.getGroupIdIn(), equalTo(singleton(adGroupIdWithNamedPack1)));
    }

    private void linkToAdGroup(Long libMwId, Long adGroupId) {
        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToAdGroup(shard, libMwId, adGroupId);
    }

    private Long createAdGroup() {
        return steps.adGroupSteps().createActiveTextAdGroup(clientInfo).getAdGroupId();
    }
}
