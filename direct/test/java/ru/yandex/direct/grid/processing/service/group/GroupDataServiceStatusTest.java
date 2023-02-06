package ru.yandex.direct.grid.processing.service.group;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.aggregatedstatuses.AggregatedStatusesViewService;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupStates;
import ru.yandex.direct.core.entity.adgroup.repository.internal.AdGroupTagsRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.tag.repository.TagRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupFilter;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupModerationStatus;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupPrimaryStatus;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupsWithTotals;
import ru.yandex.direct.grid.core.entity.group.service.GridAdGroupService;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.group.GdAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupModerationStatus;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupPrimaryStatus;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupPrimaryStatusDesc;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupStatus;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupWithTotals;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.model.group.GdDynamicAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdMobileContentAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdTextAdGroup;
import ru.yandex.direct.grid.processing.service.campaign.CampaignInfoService;
import ru.yandex.direct.grid.processing.service.group.type.GroupTypeFacade;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdAdGroupStatus;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdBaseGroup;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdDynamicGroup;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdMobileContentGroup;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdiBaseGroup;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdiDynamicGroup;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdiMobileContentGroup;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaign;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.toGdiCampaign;
import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultLimitOffset;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(Parameterized.class)
public class GroupDataServiceStatusTest {

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

    @Mock
    private GridAdGroupService gridAdGroupService;

    @Mock
    private GroupTypeFacade groupTypeFacade;

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

    @Parameterized.Parameter()
    public String testCaseDescription;

    @Parameterized.Parameter(1)
    public GdiGroupModerationStatus internalStatusModerate;

    @Parameterized.Parameter(2)
    public AdGroupStates internalAdGroupStates;

    @Parameterized.Parameter(3)
    public GdiGroupPrimaryStatus internalPrimaryStatus;

    @Parameterized.Parameter(4)
    public GdAdGroupStatus expectedStatus;

    @Parameterized.Parameters(name = "case = {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                        {"Draft - черновик группы",
                                GdiGroupModerationStatus.DRAFT,
                                new AdGroupStates()
                                        .withShowing(true)
                                        .withActive(false)
                                        .withArchived(false)
                                        .withBsEverSynced(false)
                                        .withHasDraftAds(false),
                                GdiGroupPrimaryStatus.DRAFT,
                                defaultGdAdGroupStatus()
                                        .withShowing(true)
                                        .withActive(false)
                                        .withArchived(false)
                                        .withBsEverSynced(false)
                                        .withPrimaryStatus(GdAdGroupPrimaryStatus.DRAFT)
                                        .withPrimaryStatusDesc(null)
                                        .withModerationStatus(GdAdGroupModerationStatus.DRAFT)
                        },
                        {"Moderation, Showing - отправлена на модерацию",
                                GdiGroupModerationStatus.MODERATION,
                                new AdGroupStates()
                                        .withShowing(true)
                                        .withActive(false)
                                        .withArchived(false)
                                        .withBsEverSynced(false)
                                        .withHasDraftAds(false),
                                GdiGroupPrimaryStatus.MODERATION,
                                defaultGdAdGroupStatus()
                                        .withShowing(true)
                                        .withActive(false)
                                        .withArchived(false)
                                        .withBsEverSynced(false)
                                        .withPrimaryStatus(GdAdGroupPrimaryStatus.MODERATION)
                                        .withPrimaryStatusDesc(null)
                                        .withModerationStatus(GdAdGroupModerationStatus.MODERATION)
                        },
                        {"Preaccepted, Showing -  активна, предварительно принята на модерации",
                                GdiGroupModerationStatus.PREACCEPTED,
                                new AdGroupStates()
                                        .withShowing(true)
                                        .withActive(false)
                                        .withArchived(false)
                                        .withBsEverSynced(false)
                                        .withHasDraftAds(false),
                                GdiGroupPrimaryStatus.ACTIVE,
                                defaultGdAdGroupStatus()
                                        .withShowing(true)
                                        .withActive(false)
                                        .withArchived(false)
                                        .withBsEverSynced(false)
                                        .withPrimaryStatus(GdAdGroupPrimaryStatus.ACTIVE)
                                        .withPrimaryStatusDesc(null)
                                        .withModerationStatus(GdAdGroupModerationStatus.PREACCEPTED)
                        },
                        {"Accepted, Showing -  активна, принята на модерации",
                                GdiGroupModerationStatus.ACCEPTED,
                                new AdGroupStates()
                                        .withShowing(true)
                                        .withActive(false)
                                        .withArchived(false)
                                        .withBsEverSynced(false)
                                        .withHasDraftAds(false),
                                GdiGroupPrimaryStatus.ACTIVE,
                                defaultGdAdGroupStatus()
                                        .withShowing(true)
                                        .withActive(false)
                                        .withArchived(false)
                                        .withBsEverSynced(false)
                                        .withPrimaryStatus(GdAdGroupPrimaryStatus.ACTIVE)
                                        .withPrimaryStatusDesc(null)
                                        .withModerationStatus(GdAdGroupModerationStatus.ACCEPTED)
                        },
                        {"Accepted, Active -  принята на модерации, идут показы",
                                GdiGroupModerationStatus.ACCEPTED,
                                new AdGroupStates()
                                        .withShowing(true)
                                        .withActive(true)
                                        .withArchived(false)
                                        .withBsEverSynced(false)
                                        .withHasDraftAds(false),
                                GdiGroupPrimaryStatus.ACTIVE,
                                defaultGdAdGroupStatus()
                                        .withShowing(true)
                                        .withActive(true)
                                        .withArchived(false)
                                        .withBsEverSynced(false)
                                        .withPrimaryStatus(GdAdGroupPrimaryStatus.ACTIVE)
                                        .withPrimaryStatusDesc(GdAdGroupPrimaryStatusDesc.SHOWING)
                                        .withModerationStatus(GdAdGroupModerationStatus.ACCEPTED)
                        },
                        {"Accepted, Showing -  принята на модерации, синхронизированна, идут показы",
                                GdiGroupModerationStatus.ACCEPTED,
                                new AdGroupStates()
                                        .withShowing(true)
                                        .withActive(true)
                                        .withArchived(false)
                                        .withBsEverSynced(true)
                                        .withHasDraftAds(false),
                                GdiGroupPrimaryStatus.ACTIVE,
                                defaultGdAdGroupStatus()
                                        .withShowing(true)
                                        .withActive(true)
                                        .withArchived(false)
                                        .withBsEverSynced(true)
                                        .withPrimaryStatus(GdAdGroupPrimaryStatus.ACTIVE)
                                        .withPrimaryStatusDesc(GdAdGroupPrimaryStatusDesc.SHOWING)
                                        .withModerationStatus(GdAdGroupModerationStatus.ACCEPTED)
                        },
                        {"Accepted, Stopped -  принята на модерации и остановлена",
                                GdiGroupModerationStatus.ACCEPTED,
                                new AdGroupStates()
                                        .withShowing(false)
                                        .withActive(false)
                                        .withArchived(false)
                                        .withBsEverSynced(false)
                                        .withHasDraftAds(false),
                                GdiGroupPrimaryStatus.STOPPED,
                                defaultGdAdGroupStatus()
                                        .withShowing(false)
                                        .withActive(false)
                                        .withArchived(false)
                                        .withBsEverSynced(false)
                                        .withPrimaryStatus(GdAdGroupPrimaryStatus.STOPPED)
                                        .withPrimaryStatusDesc(null)
                                        .withModerationStatus(GdAdGroupModerationStatus.ACCEPTED)
                        },
                        {"Rejected, Showing -  отклонена на модерации, активна",
                                GdiGroupModerationStatus.REJECTED,
                                new AdGroupStates()
                                        .withShowing(true)
                                        .withActive(false)
                                        .withArchived(false)
                                        .withBsEverSynced(false)
                                        .withHasDraftAds(false),
                                GdiGroupPrimaryStatus.REJECTED,
                                defaultGdAdGroupStatus()
                                        .withShowing(true)
                                        .withActive(false)
                                        .withArchived(false)
                                        .withBsEverSynced(false)
                                        .withPrimaryStatus(GdAdGroupPrimaryStatus.REJECTED)
                                        .withPrimaryStatusDesc(null)
                                        .withModerationStatus(GdAdGroupModerationStatus.REJECTED)
                        },
                        {"Rejected, Prev.Version Showing -  отклонена на модерации, идут показы предыдущей версии",
                                GdiGroupModerationStatus.REJECTED,
                                new AdGroupStates()
                                        .withShowing(true)
                                        .withActive(true)
                                        .withArchived(false)
                                        .withBsEverSynced(true)
                                        .withHasDraftAds(false),
                                GdiGroupPrimaryStatus.REJECTED,
                                defaultGdAdGroupStatus()
                                        .withShowing(true)
                                        .withActive(true)
                                        .withArchived(false)
                                        .withBsEverSynced(true)
                                        .withPrimaryStatus(GdAdGroupPrimaryStatus.REJECTED)
                                        .withPrimaryStatusDesc(GdAdGroupPrimaryStatusDesc.PREVIOUS_VERSION_SHOWING)
                                        .withModerationStatus(GdAdGroupModerationStatus.REJECTED)
                        },
                        {"Rejected, Stopped -  отклонена на модерации, остановлена",
                                GdiGroupModerationStatus.REJECTED,
                                new AdGroupStates()
                                        .withShowing(false)
                                        .withActive(false)
                                        .withArchived(false)
                                        .withBsEverSynced(false)
                                        .withHasDraftAds(false),
                                GdiGroupPrimaryStatus.STOPPED,
                                defaultGdAdGroupStatus()
                                        .withShowing(false)
                                        .withActive(false)
                                        .withArchived(false)
                                        .withBsEverSynced(false)
                                        .withPrimaryStatus(GdAdGroupPrimaryStatus.STOPPED)
                                        .withPrimaryStatusDesc(null)
                                        .withModerationStatus(GdAdGroupModerationStatus.REJECTED)
                        }
                }
        );
    }

    @BeforeClass
    public static void beforeClass() {
        gridGraphQLContext = ContextHelper.buildDefaultContext();
        clientInfo = gridGraphQLContext.getQueriedClient();
        clientId = ClientId.fromLong(clientInfo.getId());
        operator = gridGraphQLContext.getOperator();
    }

    @Before
    public void initTestData() {
        MockitoAnnotations.initMocks(this);

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

        doReturn(emptyMap())
                .when(aggregatedStatusesViewService)
                .getAdGroupStatusesByIds(anyInt(), anySet());
    }


    @Test
    public void testBaseGroupStatus() {
        GdiGroup gdiGroup = getGroupWithUpdatedStatuses(
                defaultGdiBaseGroup()
                        .withCampaignId(CAMPAIGN.getId())
        );
        doReturn(new GdiGroupsWithTotals().withGdiGroups(Collections.singletonList(gdiGroup)))
                .when(gridAdGroupService)
                .getGroups(eq(clientInfo.getShard()), eq(clientInfo.getCountryRegionId()), eq(clientId), eq(operator),
                        eq(internalFilter), any(), any(), eq(STAT_REQUIREMENTS), eq(false), eq(emptySet()), any());

        GdAdGroupWithTotals adGroupWithTotals = groupDataService
                .getAdGroups(clientInfo, adGroupsContainer, gridGraphQLContext);
        List<GdAdGroup> result = adGroupWithTotals.getGdAdGroups();

        GdTextAdGroup gdTextAdGroup = defaultGdBaseGroup()
                .withCampaignId(CAMPAIGN.getId())
                .withCampaign(CAMPAIGN)
                .withStatus(expectedStatus);
        gdTextAdGroup.getAccess().setStatus(expectedStatus);
        assertThat(result)
                .is(matchedBy(beanDiffer(singletonList(gdTextAdGroup))
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    @Test
    public void testDynamicGroupStatus() {
        GdiGroup gdiGroup = getGroupWithUpdatedStatuses(
                defaultGdiDynamicGroup()
                        .withCampaignId(CAMPAIGN.getId())
        );
        doReturn(new GdiGroupsWithTotals().withGdiGroups(Collections.singletonList(gdiGroup)))
                .when(gridAdGroupService)
                .getGroups(eq(clientInfo.getShard()), eq(clientInfo.getCountryRegionId()), eq(clientId), eq(operator),
                        eq(internalFilter), any(), any(), eq(STAT_REQUIREMENTS), eq(false), eq(emptySet()), any());

        GdAdGroupWithTotals adGroupWithTotals = groupDataService
                .getAdGroups(clientInfo, adGroupsContainer, gridGraphQLContext);
        List<GdAdGroup> result = adGroupWithTotals.getGdAdGroups();

        GdDynamicAdGroup gdDynamicAdGroup = defaultGdDynamicGroup()
                .withCampaignId(CAMPAIGN.getId())
                .withCampaign(CAMPAIGN)
                .withStatus(expectedStatus);
        gdDynamicAdGroup.getAccess().setStatus(expectedStatus);
        assertThat(result)
                .is(matchedBy(beanDiffer(singletonList(gdDynamicAdGroup))
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    @Test
    public void testMobileContentGroupStatus() {
        GdiGroup gdiGroup = getGroupWithUpdatedStatuses(
                defaultGdiMobileContentGroup()
                        .withCampaignId(CAMPAIGN.getId())
        );
        doReturn(new GdiGroupsWithTotals().withGdiGroups(Collections.singletonList(gdiGroup)))
                .when(gridAdGroupService)
                .getGroups(eq(clientInfo.getShard()), eq(clientInfo.getCountryRegionId()), eq(clientId), eq(operator),
                        eq(internalFilter), any(), any(), eq(STAT_REQUIREMENTS), eq(false), eq(emptySet()), any());

        GdAdGroupWithTotals adGroupWithTotals = groupDataService
                .getAdGroups(clientInfo, adGroupsContainer, gridGraphQLContext);
        List<GdAdGroup> result = adGroupWithTotals.getGdAdGroups();

        GdMobileContentAdGroup gdMobileContentAdGroup = defaultGdMobileContentGroup()
                .withCampaignId(CAMPAIGN.getId())
                .withCampaign(CAMPAIGN)
                .withStatus(expectedStatus);
        gdMobileContentAdGroup.getAccess().setStatus(expectedStatus);
        assertThat(result)
                .is(matchedBy(beanDiffer(singletonList(gdMobileContentAdGroup))
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }


    private GdiGroup getGroupWithUpdatedStatuses(GdiGroup group) {
        group.getStatus().withModerationStatus(internalStatusModerate);
        group.getStatus().withStateFlags(internalAdGroupStates);
        group.getStatus().withPrimaryStatus(internalPrimaryStatus);
        return group;
    }

}
