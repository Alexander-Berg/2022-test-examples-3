package ru.yandex.direct.grid.processing.service.group;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dataloader.DataLoader;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.aggregatedstatuses.AggregatedStatusesViewService;
import ru.yandex.direct.core.entity.adgroup.repository.internal.AdGroupTagsRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.tag.repository.TagRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupFilter;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupsWithTotals;
import ru.yandex.direct.grid.core.entity.group.service.GridAdGroupService;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupWithTotals;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.service.campaign.CampaignInfoService;
import ru.yandex.direct.grid.processing.service.group.loader.CanBeDeletedAdGroupsDataLoader;
import ru.yandex.direct.grid.processing.service.group.type.GroupTypeFacade;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdiBaseGroup;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaign;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.toGdiCampaign;
import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultLimitOffset;

@GridProcessingTest
@RunWith(Parameterized.class)
public class GroupDataServiceGetTotalStatsTest {

    private static final GdCampaign CAMPAIGN = defaultGdCampaign();
    private static final Map<Long, GdiCampaign> TEST_INTERNAL_CAMPAIGNS =
            Collections.singletonMap(CAMPAIGN.getId(), toGdiCampaign(CAMPAIGN));

    private static GridGraphQLContext gridGraphQLContext;
    private static GdClientInfo clientInfo;
    private static ClientId clientId;
    private static User operator;

    @Mock
    private CanBeDeletedAdGroupsDataLoader canBeDeletedAdGroupsDataLoader;
    @Mock
    private DataLoader<Long, Boolean> dataLoaderMock;
    @Mock
    private GridAdGroupService gridAdGroupService;
    @Mock
    private CampaignInfoService campaignInfoService;
    @Mock
    private AggregatedStatusesViewService aggregatedStatusesViewService;
    @Mock
    private FeatureService featureService;
    @Mock
    private AdGroupTagsRepository adGroupTagsRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private GroupTypeFacade adGroupTypeFacade;

    private GdiGroupFilter internalFilter;
    private GdAdGroupsContainer adGroupsContainer;

    @InjectMocks
    private GroupDataService groupDataService;

    @Parameterized.Parameter()
    public String description;

    @Parameterized.Parameter(1)
    public List<GdiGroup> gdiGroups;

    @Parameterized.Parameter(2)
    public Boolean withTotalStats;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"Запросом из БД вернулась итоговая статистика со списком групп -> получаем итоговую статистику",
                        singletonList(defaultGdiBaseGroup().withCampaignId(CAMPAIGN.getId())), true},

                {"Запросом из БД вернулась итоговая статистика с пустым списоком групп -> получаем итоговую статистику",
                        emptyList(), true},

                {"Запросом из БД вернулся список групп без итоговой статистики -> итоговую статистику не получаем",
                        singletonList(defaultGdiBaseGroup().withCampaignId(CAMPAIGN.getId())), false},
        };
    }

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

        internalFilter = new GdiGroupFilter()
                .withCampaignIdIn(Set.of(CAMPAIGN.getId()));

        adGroupsContainer = new GdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter()
                        .withCampaignIdIn(internalFilter.getCampaignIdIn()))
                .withOrderBy(emptyList())
                .withStatRequirements(new GdStatRequirements()
                        .withFrom(LocalDate.now())
                        .withTo(LocalDate.now().plusDays(10)))
                .withLimitOffset(getDefaultLimitOffset());

        doReturn(dataLoaderMock)
                .when(canBeDeletedAdGroupsDataLoader).get();
        doReturn(emptyMap())
                .when(aggregatedStatusesViewService)
                .getAdGroupStatusesByIds(anyInt(), anySet());
        doReturn(Collections.singletonMap(CAMPAIGN.getId(), CAMPAIGN))
                .when(campaignInfoService)
                .getTruncatedCampaigns(clientId, Set.of(CAMPAIGN.getId()));
        doReturn(TEST_INTERNAL_CAMPAIGNS)
                .when(campaignInfoService)
                .getAllBaseCampaignsMap(clientId);
    }

    @Test
    public void getGroups() {
        doReturnGdiGroups(gdiGroups, withTotalStats);

        GdAdGroupWithTotals groupsWithTotals = groupDataService
                .getAdGroups(clientInfo, adGroupsContainer, gridGraphQLContext);

        assertThat("итоговая статистика (тоталы)", groupsWithTotals.getTotalStats(),
                withTotalStats ? notNullValue() : nullValue());
    }

    private void doReturnGdiGroups(List<GdiGroup> groups, boolean withTotalStats) {
        GdiGroupsWithTotals gdiGroupsWithTotals = new GdiGroupsWithTotals()
                .withGdiGroups(groups)
                .withTotalStats(!withTotalStats ? null : new GdiEntityStats()
                        .withCost(BigDecimal.TEN)
                        .withShows(BigDecimal.TEN)
                        .withClicks(BigDecimal.TEN)
                        .withGoals(BigDecimal.TEN));

        doReturn(gdiGroupsWithTotals)
                .when(gridAdGroupService)
                .getGroups(eq(clientInfo.getShard()), eq(clientInfo.getCountryRegionId()), eq(clientId), eq(operator),
                        eq(internalFilter), any(), any(), any(), eq(false), eq(emptySet()),
                        eq(gridGraphQLContext.getFetchedFieldsReslover().getAdGroup()));
    }
}
