package ru.yandex.direct.grid.processing.service.group;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.aggregatedstatuses.AggregatedStatusesViewService;
import ru.yandex.direct.core.entity.adgroup.repository.internal.AdGroupTagsRepository;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AggregatedStatusAdGroupData;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.tag.repository.TagRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupFilter;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupsWithTotals;
import ru.yandex.direct.grid.core.entity.group.service.GridAdGroupService;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.grid.model.aggregatedstatuses.GdAdgroupAggregatedStatusInfo;
import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupWithTotals;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.service.campaign.CampaignInfoService;
import ru.yandex.direct.grid.processing.service.group.type.GroupTypeFacade;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.feature.FeatureName.SHOW_DNA_BY_DEFAULT;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdiBaseGroup;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaign;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.toGdiCampaign;
import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultLimitOffset;

public class GroupDataServiceWithFilterByAggrStatusTest {
    private static final LocalDate TEST_FROM = LocalDate.now();
    private static final LocalDate TEST_TO = TEST_FROM.plusDays(10);
    private static final GdCampaign CAMPAIGN = defaultGdCampaign();
    private static final Map<Long, GdiCampaign> TEST_INTERNAL_CAMPAIGNS = Collections.singletonMap(CAMPAIGN.getId(),
            toGdiCampaign(CAMPAIGN));
    private static final Map<Long, GdCampaign> TEST_CAMPAIGNS = Collections.singletonMap(CAMPAIGN.getId(), CAMPAIGN);
    private static final Set<Long> TEST_CAMPAIGNS_IDS = TEST_CAMPAIGNS.keySet();

    private static GridGraphQLContext gridGraphQLContext;
    private static GdClientInfo clientInfo;
    private static ClientId clientId;
    private static User operator;

    @Mock
    private CampaignInfoService campaignInfoService;

    @Mock
    private AggregatedStatusesViewService aggregatedStatusesViewService;

    @Mock
    private GridAdGroupService gridAdGroupService;

    @Mock
    private FeatureService featureService;

    @Mock
    private AdGroupTagsRepository adGroupTagsRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private GroupTypeFacade adGroupTypeFacade;

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
        MockitoAnnotations.openMocks(this);

        doReturn(TEST_CAMPAIGNS)
                .when(campaignInfoService)
                .getTruncatedCampaigns(clientId, TEST_CAMPAIGNS_IDS);

        doReturn(TEST_INTERNAL_CAMPAIGNS)
                .when(campaignInfoService)
                .getAllBaseCampaignsMap(clientId);

        doReturn(Set.of(SHOW_DNA_BY_DEFAULT.getName()))
                .when(featureService)
                .getEnabledForClientId(clientId);

        doReturn(emptyMap())
                .when(adGroupTagsRepository)
                .getAdGroupsTags(anyInt(), anyCollection());

        doReturn(emptyList())
                .when(tagRepository)
                .getCampaignTagsWithUseCount(anyInt(), anyCollection());
    }

    @Test
    public void hasOneSameReason() {
        final var internalFilter = new GdiGroupFilter()
                .withReasonsContainSome(Set.of(GdSelfStatusReason.SUSPENDED_BY_USER,
                        GdSelfStatusReason.ADGROUP_INCOMPLETE))
                .withCampaignIdIn(TEST_CAMPAIGNS_IDS);

        final var adGroupsContainer = new GdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withReasonsContainSome(internalFilter.getReasonsContainSome())
                        .withCampaignIdIn(internalFilter.getCampaignIdIn()))
                .withOrderBy(emptyList())
                .withStatRequirements(new GdStatRequirements()
                        .withFrom(TEST_FROM)
                        .withTo(TEST_TO))
                .withLimitOffset(getDefaultLimitOffset());

        List<GdiGroup> intGroups = List.of(
                defaultGdiBaseGroup()
                        .withAggregatedStatusInfo(new GdAdgroupAggregatedStatusInfo().withReasons(List.of(GdSelfStatusReason.SUSPENDED_BY_USER)))
                        .withCampaignId(CAMPAIGN.getId())
        );

        doReturn(new GdiGroupsWithTotals().withGdiGroups(intGroups))
                .when(gridAdGroupService)
                .getGroups(eq(clientInfo.getShard()), eq(clientInfo.getCountryRegionId()), eq(clientId), eq(operator),
                        eq(internalFilter), any(), any(), any(), eq(true), eq(emptySet()),
                        eq(gridGraphQLContext.getFetchedFieldsReslover().getAdGroup()));

        doReturn(Map.of(intGroups.get(0).getId(), new AggregatedStatusAdGroupData(List.of(), null,
                GdSelfStatusEnum.DRAFT, List.of(GdSelfStatusReason.SUSPENDED_BY_USER))))
                .when(aggregatedStatusesViewService)
                .getAdGroupStatusesByIds(anyInt(), anySet());

        GdAdGroupWithTotals adGroupWithTotals = groupDataService
                .getAdGroups(clientInfo, adGroupsContainer, gridGraphQLContext);

        assertThat(adGroupWithTotals.getGdAdGroups()).hasSize(1);
        assertThat(adGroupWithTotals.getGdAdGroups().get(0).getId()).isEqualTo(intGroups.get(0).getId());
    }

    @Test
    public void noHasSameReason() {
        final var internalFilter = new GdiGroupFilter()
                .withReasonsContainSome(Set.of(GdSelfStatusReason.ADGROUP_INCOMPLETE))
                .withCampaignIdIn(TEST_CAMPAIGNS_IDS);

        final var adGroupsContainer = new GdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withReasonsContainSome(internalFilter.getReasonsContainSome())
                        .withCampaignIdIn(internalFilter.getCampaignIdIn()))
                .withOrderBy(emptyList())
                .withStatRequirements(new GdStatRequirements()
                        .withFrom(TEST_FROM)
                        .withTo(TEST_TO))
                .withLimitOffset(getDefaultLimitOffset());

        List<GdiGroup> intGroups = List.of(
                defaultGdiBaseGroup()
                        .withAggregatedStatusInfo(new GdAdgroupAggregatedStatusInfo().withReasons(List.of(GdSelfStatusReason.SUSPENDED_BY_USER)))
                        .withCampaignId(CAMPAIGN.getId())
        );

        doReturn(new GdiGroupsWithTotals().withGdiGroups(intGroups))
                .when(gridAdGroupService)
                .getGroups(eq(clientInfo.getShard()), eq(clientInfo.getCountryRegionId()), eq(clientId), eq(operator),
                        eq(internalFilter), any(), any(), any(), eq(true), eq(emptySet()),
                        eq(gridGraphQLContext.getFetchedFieldsReslover().getAdGroup()));

        doReturn(Map.of(intGroups.get(0).getId(), new AggregatedStatusAdGroupData(List.of(), null,
                GdSelfStatusEnum.DRAFT, List.of(GdSelfStatusReason.SUSPENDED_BY_USER))))
                .when(aggregatedStatusesViewService)
                .getAdGroupStatusesByIds(anyInt(), anySet());

        GdAdGroupWithTotals adGroupWithTotals = groupDataService
                .getAdGroups(clientInfo, adGroupsContainer, gridGraphQLContext);

        assertThat(adGroupWithTotals.getGdAdGroups()).isEmpty();
    }

    @Test
    public void noHasReasonsFilter() {
        final var internalFilter = new GdiGroupFilter()
                .withCampaignIdIn(TEST_CAMPAIGNS_IDS);

        final var adGroupsContainer = new GdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withReasonsContainSome(internalFilter.getReasonsContainSome())
                        .withCampaignIdIn(internalFilter.getCampaignIdIn()))
                .withOrderBy(emptyList())
                .withStatRequirements(new GdStatRequirements()
                        .withFrom(TEST_FROM)
                        .withTo(TEST_TO))
                .withLimitOffset(getDefaultLimitOffset());

        List<GdiGroup> intGroups = List.of(
                defaultGdiBaseGroup()
                        .withAggregatedStatusInfo(new GdAdgroupAggregatedStatusInfo().withReasons(List.of(GdSelfStatusReason.SUSPENDED_BY_USER)))
                        .withCampaignId(CAMPAIGN.getId())
        );

        doReturn(new GdiGroupsWithTotals().withGdiGroups(intGroups))
                .when(gridAdGroupService)
                .getGroups(eq(clientInfo.getShard()), eq(clientInfo.getCountryRegionId()), eq(clientId), eq(operator),
                        eq(internalFilter), any(), any(), any(), eq(true), eq(emptySet()),
                        eq(gridGraphQLContext.getFetchedFieldsReslover().getAdGroup()));

        doReturn(Map.of(intGroups.get(0).getId(), new AggregatedStatusAdGroupData(List.of(), null,
                GdSelfStatusEnum.DRAFT, List.of(GdSelfStatusReason.SUSPENDED_BY_USER))))
                .when(aggregatedStatusesViewService)
                .getAdGroupStatusesByIds(anyInt(), anySet());

        GdAdGroupWithTotals adGroupWithTotals = groupDataService
                .getAdGroups(clientInfo, adGroupsContainer, gridGraphQLContext);

        assertThat(adGroupWithTotals.getGdAdGroups()).hasSize(1);
    }
}
