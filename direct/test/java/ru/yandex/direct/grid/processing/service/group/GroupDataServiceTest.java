package ru.yandex.direct.grid.processing.service.group;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang.math.RandomUtils;
import org.dataloader.DataLoader;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.aggregatedstatuses.AggregatedStatusesViewService;
import ru.yandex.direct.core.entity.adgroup.repository.internal.AdGroupTagsRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.tag.repository.TagRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.dbschema.ppcdict.enums.MediaFilesAvatarsHost;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupFilter;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupsWithTotals;
import ru.yandex.direct.grid.core.entity.group.service.GridAdGroupService;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.model.campaign.GdCampaignTruncated;
import ru.yandex.direct.grid.model.campaign.GdTextCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.group.GdAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupAccess;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupWithTotals;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.model.group.GdMobileContentAdGroupOsType;
import ru.yandex.direct.grid.processing.service.campaign.CampaignInfoService;
import ru.yandex.direct.grid.processing.service.group.loader.CanBeDeletedAdGroupsDataLoader;
import ru.yandex.direct.grid.processing.service.group.type.GroupTypeFacade;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdBaseGroup;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdDynamicGroup;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdMobileContentGroup;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdiBaseGroup;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdiDynamicGroup;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdiInternalGroup;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdiMobileContentGroup;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaign;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.toGdiCampaign;
import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultLimitOffset;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class GroupDataServiceTest {

    private static final GdCampaign CAMPAIGN = defaultGdCampaign();
    private static final Map<Long, GdCampaign> TEST_CAMPAIGNS = Collections.singletonMap(CAMPAIGN.getId(), CAMPAIGN);
    private static final Map<Long, GdiCampaign> TEST_INTERNAL_CAMPAIGNS = Collections.singletonMap(CAMPAIGN.getId(),
            toGdiCampaign(CAMPAIGN));
    private static final Set<Long> TEST_CAMPAIGNS_IDS = TEST_CAMPAIGNS.keySet();
    private static final LocalDate TEST_FROM = LocalDate.now();
    private static final LocalDate TEST_TO = TEST_FROM.plusDays(10);
    private static final GdStatRequirements STAT_REQUIREMENTS = new GdStatRequirements()
            .withFrom(TEST_FROM)
            .withTo(TEST_TO);

    private static GridGraphQLContext gridGraphQLContext;
    private static GdClientInfo clientInfo;
    private static ClientId clientId;
    private static User operator;

    private GdiGroupFilter internalFilter;
    private GdAdGroupsContainer adGroupsContainer;
    private GdAdGroupAccess gdAdGroupAccess;

    @Mock
    private CanBeDeletedAdGroupsDataLoader canBeDeletedAdGroupsDataLoader;
    @Mock
    private DataLoader<Long, Boolean> dataLoaderMock;

    @Mock
    private GridAdGroupService gridAdGroupService;

    @Mock
    private GroupTypeFacade adGroupTypeFacade;

    @Mock
    private CampaignInfoService campaignInfoService;

    @Mock
    private FeatureService featureService;

    @Mock
    private AdGroupTagsRepository adGroupTagsRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private AggregatedStatusesViewService aggregatedStatusesViewService;

    @InjectMocks
    private GroupDataService groupDataService;

    @BeforeClass
    public static void beforeClass() {
        gridGraphQLContext = ContextHelper.buildDefaultContext();
        clientInfo = gridGraphQLContext.getQueriedClient();
        clientId = ClientId.fromLong(clientInfo.getId());
        operator = gridGraphQLContext.getOperator();
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(dataLoaderMock)
                .when(canBeDeletedAdGroupsDataLoader).get();

        List<GdiGroup> intGroups = Arrays.asList(
                defaultGdiMobileContentGroup()
                        .withCampaignId(CAMPAIGN.getId()),
                defaultGdiDynamicGroup()
                        .withCampaignId(CAMPAIGN.getId()),
                defaultGdiBaseGroup()
                        .withCampaignId(CAMPAIGN.getId()));

        internalFilter = new GdiGroupFilter()
                .withCampaignIdIn(TEST_CAMPAIGNS_IDS);

        adGroupsContainer = new GdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withCampaignIdIn(internalFilter.getCampaignIdIn()))
                .withOrderBy(emptyList())
                .withStatRequirements(new GdStatRequirements()
                        .withFrom(TEST_FROM)
                        .withTo(TEST_TO))
                .withLimitOffset(getDefaultLimitOffset());

        doReturn(new GdiGroupsWithTotals().withGdiGroups(intGroups))
                .when(gridAdGroupService)
                .getGroups(eq(clientInfo.getShard()), eq(clientInfo.getCountryRegionId()), eq(clientId), eq(operator),
                        eq(internalFilter), any(), any(), eq(STAT_REQUIREMENTS), eq(false), eq(emptySet()),
                        eq(gridGraphQLContext.getFetchedFieldsReslover().getAdGroup()));

        gdAdGroupAccess = new GdAdGroupAccess()
                .withAdGroupId(RandomNumberUtils.nextPositiveLong());

        doReturn(emptyMap())
                .when(aggregatedStatusesViewService)
                .getAdGroupStatusesByIds(anyInt(), anySet());
    }


    @Test
    public void testGetGroups() {
        doReturn(TEST_CAMPAIGNS)
                .when(campaignInfoService)
                .getTruncatedCampaigns(clientId, TEST_CAMPAIGNS_IDS);

        doReturn(TEST_INTERNAL_CAMPAIGNS)
                .when(campaignInfoService)
                .getAllBaseCampaignsMap(clientId);

        doReturn(emptyMap())
                .when(adGroupTagsRepository)
                .getAdGroupsTags(anyInt(), anyCollection());

        doReturn(emptyList())
                .when(tagRepository)
                .getCampaignTagsWithUseCount(anyInt(), anyCollection());

        GdAdGroupWithTotals adGroupWithTotals = groupDataService
                .getAdGroups(clientInfo, adGroupsContainer, gridGraphQLContext);
        List<GdAdGroup> result = adGroupWithTotals.getGdAdGroups();

        verify(gridAdGroupService)
                .getGroups(eq(clientInfo.getShard()), eq(clientInfo.getCountryRegionId()), eq(clientId), eq(operator),
                        eq(internalFilter), any(), any(), eq(STAT_REQUIREMENTS), eq(false), eq(emptySet()),
                        eq(gridGraphQLContext.getFetchedFieldsReslover().getAdGroup()));

        List<GdAdGroup> expected = Arrays.asList(
                defaultGdMobileContentGroup()
                        .withOsType(GdMobileContentAdGroupOsType.ANDROID)
                        .withIndex(0)
                        .withIconHash("BFPewVw")
                        .withIconUrl("//" + MediaFilesAvatarsHost.avatars_mds_yandex_net.getLiteral()
                                + "/get-google-play-app-icon/BFPewVw/icon")
                        .withCampaignId(CAMPAIGN.getId())
                        .withCampaign(CAMPAIGN)
                        .withTags(emptyList()),
                defaultGdDynamicGroup()
                        .withIndex(1)
                        .withCampaignId(CAMPAIGN.getId())
                        .withCampaign(CAMPAIGN)
                        .withTags(emptyList()),
                defaultGdBaseGroup()
                        .withIndex(2)
                        .withCampaignId(CAMPAIGN.getId())
                        .withCampaign(CAMPAIGN)
                        .withTags(emptyList())
        );

        assertThat(result)
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void testGetGroups_WhenFetchedCampaignsIsEmpty() {
        doReturn(emptyMap())
                .when(campaignInfoService)
                .getTruncatedCampaigns(clientId, TEST_CAMPAIGNS_IDS);

        GdAdGroupWithTotals adGroupWithTotals = groupDataService
                .getAdGroups(clientInfo, adGroupsContainer, gridGraphQLContext);
        List<GdAdGroup> adGroups = adGroupWithTotals.getGdAdGroups();
        assertThat(adGroups).isEmpty();

        verify(campaignInfoService)
                .getTruncatedCampaigns(clientId, TEST_CAMPAIGNS_IDS);
        verifyZeroInteractions(adGroupTypeFacade);
    }

    @Test
    public void testGetGroups_WhenFetchedCampaignsDontContainProperCampaign() {
        doReturn(Collections.singletonMap(111L, new GdTextCampaign()))
                .when(campaignInfoService)
                .getTruncatedCampaigns(clientId, TEST_CAMPAIGNS_IDS);

        GdAdGroupWithTotals adGroupWithTotals = groupDataService
                .getAdGroups(clientInfo, adGroupsContainer, gridGraphQLContext);
        List<GdAdGroup> adGroups = adGroupWithTotals.getGdAdGroups();
        assertThat(adGroups).isEmpty();

        verify(campaignInfoService)
                .getTruncatedCampaigns(clientId, TEST_CAMPAIGNS_IDS);
        verifyZeroInteractions(adGroupTypeFacade);
    }

    @Test
    public void testGetCanBeDeletedAdGroup() {
        gdAdGroupAccess.setCanEdit(true);
        boolean expectedCanBeDeletedAdGroupValue = RandomUtils.nextBoolean();
        doReturn(CompletableFuture.completedFuture(expectedCanBeDeletedAdGroupValue))
                .when(dataLoaderMock).load(gdAdGroupAccess.getAdGroupId());

        CompletableFuture<Boolean> canBeDeletedAdGroup = groupDataService.getCanBeDeletedAdGroup(gdAdGroupAccess);
        assertThat(canBeDeletedAdGroup.join())
                .isEqualTo(expectedCanBeDeletedAdGroupValue);
        verify(dataLoaderMock).load(gdAdGroupAccess.getAdGroupId());
    }

    @Test
    public void testGetCanBeDeletedAdGroup_WhenCanEditIsFalse() {
        gdAdGroupAccess.setCanEdit(false);

        CompletableFuture<Boolean> canBeDeletedAdGroup = groupDataService.getCanBeDeletedAdGroup(gdAdGroupAccess);
        assertThat(canBeDeletedAdGroup.join())
                .isFalse();
        verifyZeroInteractions(canBeDeletedAdGroupsDataLoader);
    }

    @Test
    public void testGetCanBeDeletedAdGroupsCount() {
        gdAdGroupAccess.setCanEdit(true);
        boolean hasDeletableAdGroups = RandomUtils.nextBoolean();
        List<Long> adGroupIds = singletonList(gdAdGroupAccess.getAdGroupId());
        doReturn(CompletableFuture.completedFuture(singletonList(hasDeletableAdGroups)))
                .when(dataLoaderMock).loadMany(adGroupIds);

        CompletableFuture<Integer> canBeDeletedAdGroupsCount =
                groupDataService.getCanBeDeletedAdGroupsCount(singletonList(gdAdGroupAccess));
        assertThat(canBeDeletedAdGroupsCount.join())
                .isEqualTo(hasDeletableAdGroups ? 1 : 0);
        verify(dataLoaderMock).loadMany(adGroupIds);
    }

    @Test
    public void testGetCanBeDeletedAdGroupsCount_WhenCanEditIsFalse() {
        gdAdGroupAccess.setCanEdit(false);
        CompletableFuture<Integer> canBeDeletedAdGroupsCount =
                groupDataService.getCanBeDeletedAdGroupsCount(singletonList(gdAdGroupAccess));

        assertThat(canBeDeletedAdGroupsCount.join())
                .isZero();
        verifyZeroInteractions(canBeDeletedAdGroupsDataLoader);
    }

    @Test
    public void testGetCanBeDeletedAdGroupsCount_WhenAdGroupAccessListIsEmpty() {
        CompletableFuture<Integer> canBeDeletedAdGroupsCount =
                groupDataService.getCanBeDeletedAdGroupsCount(emptyList());

        assertThat(canBeDeletedAdGroupsCount.join())
                .isZero();
        verifyZeroInteractions(canBeDeletedAdGroupsDataLoader);
    }

    @Test
    public void testGetTruncatedInternalGroup() {
        var truncatedCampaign = (GdCampaignTruncated) CAMPAIGN;
        var group = defaultGdiInternalGroup().withCampaignId(truncatedCampaign.getId());

        doReturn(List.of(group))
                .when(gridAdGroupService)
                .getTruncatedAdGroupsFromMysql(eq(clientInfo.getShard()), eq(clientInfo.getCountryRegionId()),
                        eq(Set.of(group.getId())), anyMap());

        doReturn(TEST_INTERNAL_CAMPAIGNS)
                .when(campaignInfoService)
                .getAllBaseCampaignsMap(clientId);

        var actual = groupDataService.getTruncatedAdGroups(clientInfo.getShard(),
                clientInfo.getCountryRegionId(), clientId, operator, Set.of(group.getId()),
                Map.of(truncatedCampaign.getId(), truncatedCampaign));

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getId()).isEqualTo(group.getId());
    }

}
