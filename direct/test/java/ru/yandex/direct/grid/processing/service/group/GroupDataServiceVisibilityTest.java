package ru.yandex.direct.grid.processing.service.group;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
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
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.repository.internal.AdGroupTagsRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.tag.repository.TagRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.core.entity.group.container.GdiAdGroupRegionsInfo;
import ru.yandex.direct.grid.core.entity.group.model.GdiContentPromotionGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiContentPromotionGroupType;
import ru.yandex.direct.grid.core.entity.group.model.GdiCpmGeoPinGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiCpmGeoproductGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiCpmIndoorGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiCpmOutdoorGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiCpmVideoGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupsWithTotals;
import ru.yandex.direct.grid.core.entity.group.service.GridAdGroupService;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.grid.model.campaign.GdCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.group.GdAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupWithTotals;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.service.campaign.CampaignInfoService;
import ru.yandex.direct.grid.processing.service.group.loader.CanBeDeletedAdGroupsDataLoader;
import ru.yandex.direct.grid.processing.service.group.type.GroupTypeFacade;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.feature.FeatureName.CPM_GEO_PIN_PRODUCT_ENABLED;
import static ru.yandex.direct.feature.FeatureName.SHOW_CPM_GEOPRODUCT_GROUPS_IN_GRID;
import static ru.yandex.direct.feature.FeatureName.SHOW_CPM_INDOOR_GROUPS_IN_GRID;
import static ru.yandex.direct.feature.FeatureName.SHOW_CPM_OUTDOOR_GROUPS_IN_GRID;
import static ru.yandex.direct.feature.FeatureName.SHOW_CPM_VIDEO_GROUPS_IN_GRID;
import static ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType.CPM_GEOPRODUCT;
import static ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType.CPM_GEO_PIN;
import static ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType.CPM_INDOOR;
import static ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType.CPM_OUTDOOR;
import static ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType.CPM_VIDEO;
import static ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType.DYNAMIC;
import static ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType.MOBILE_CONTENT;
import static ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType.TEXT;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.TEST_GROUP_IDS;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdiBaseGroup;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdiDynamicGroup;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdiGroupStatus;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdiMobileContentGroup;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdCampaign;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.toGdiCampaign;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;

@RunWith(Parameterized.class)
public class GroupDataServiceVisibilityTest {
    private static final GdCampaign CAMPAIGN = defaultGdCampaign();
    private static final Map<Long, GdCampaign> TEST_CAMPAIGNS = Collections.singletonMap(CAMPAIGN.getId(), CAMPAIGN);
    private static final Map<Long, GdiCampaign> TEST_INTERNAL_CAMPAIGNS = Collections.singletonMap(CAMPAIGN.getId(),
            toGdiCampaign(CAMPAIGN));
    private static final Set<Long> TEST_CAMPAIGNS_IDS = TEST_CAMPAIGNS.keySet();

    private static GridGraphQLContext gridGraphQLContext;
    private static GdClientInfo clientInfo;

    private GdAdGroupsContainer adGroupsContainer;

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
    private Set<GdAdGroupType> defaultTypes;

    @Parameterized.Parameters(name = "Включенные фичи: {0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {List.of(), Set.of()},
                {List.of(SHOW_CPM_VIDEO_GROUPS_IN_GRID), Set.of(CPM_VIDEO)},
                {List.of(SHOW_CPM_OUTDOOR_GROUPS_IN_GRID), Set.of(CPM_OUTDOOR)},
                {List.of(SHOW_CPM_INDOOR_GROUPS_IN_GRID), Set.of(CPM_INDOOR)},
                {List.of(SHOW_CPM_GEOPRODUCT_GROUPS_IN_GRID), Set.of(CPM_GEOPRODUCT)},
                {List.of(CPM_GEO_PIN_PRODUCT_ENABLED), Set.of(CPM_GEO_PIN)},
                {List.of(SHOW_CPM_VIDEO_GROUPS_IN_GRID, SHOW_CPM_INDOOR_GROUPS_IN_GRID,
                        SHOW_CPM_GEOPRODUCT_GROUPS_IN_GRID),
                        Set.of(CPM_VIDEO, CPM_INDOOR, CPM_GEOPRODUCT)},
        });
    }

    @Parameterized.Parameter
    public List<FeatureName> features;

    @Parameterized.Parameter(1)
    public Set<GdAdGroupType> extraExpectedTypes;


    @BeforeClass
    public static void beforeClass() {
        gridGraphQLContext = ContextHelper.buildDefaultContext();
        clientInfo = gridGraphQLContext.getQueriedClient();
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        List<GdiGroup> intGroups = Arrays.asList(
                defaultGdiBaseGroup()
                        .withCampaignId(CAMPAIGN.getId()),

                defaultGdiMobileContentGroup()
                        .withCampaignId(CAMPAIGN.getId()),

                defaultGdiDynamicGroup()
                        .withCampaignId(CAMPAIGN.getId()),

                new GdiCpmVideoGroup()
                        .withType(AdGroupType.CPM_VIDEO)
                        .withId(TEST_GROUP_IDS[3])
                        .withCampaignId(CAMPAIGN.getId())
                        .withRegionsInfo(new GdiAdGroupRegionsInfo().withRegionIds(emptyList()))
                        .withStatus(defaultGdiGroupStatus()),

                new GdiCpmOutdoorGroup()
                        .withType(AdGroupType.CPM_OUTDOOR)
                        .withId(TEST_GROUP_IDS[4])
                        .withCampaignId(CAMPAIGN.getId())
                        .withRegionsInfo(new GdiAdGroupRegionsInfo().withRegionIds(emptyList()))
                        .withStatus(defaultGdiGroupStatus()),

                new GdiCpmIndoorGroup()
                        .withType(AdGroupType.CPM_INDOOR)
                        .withId(TEST_GROUP_IDS[5])
                        .withCampaignId(CAMPAIGN.getId())
                        .withRegionsInfo(new GdiAdGroupRegionsInfo().withRegionIds(emptyList()))
                        .withStatus(defaultGdiGroupStatus()),

                new GdiCpmGeoproductGroup()
                        .withType(AdGroupType.CPM_GEOPRODUCT)
                        .withId(TEST_GROUP_IDS[6])
                        .withCampaignId(CAMPAIGN.getId())
                        .withRegionsInfo(new GdiAdGroupRegionsInfo().withRegionIds(emptyList()))
                        .withStatus(defaultGdiGroupStatus()),

                new GdiCpmGeoPinGroup()
                        .withType(AdGroupType.CPM_GEO_PIN)
                        .withId(TEST_GROUP_IDS[7])
                        .withCampaignId(CAMPAIGN.getId())
                        .withRegionsInfo(new GdiAdGroupRegionsInfo().withRegionIds(emptyList()))
                        .withStatus(defaultGdiGroupStatus()),

                new GdiContentPromotionGroup()
                        .withType(AdGroupType.CONTENT_PROMOTION)
                        .withContentPromotionGroupType(GdiContentPromotionGroupType.VIDEO)
                        .withId(TEST_GROUP_IDS[8])
                        .withCampaignId(CAMPAIGN.getId())
                        .withRegionsInfo(new GdiAdGroupRegionsInfo().withRegionIds(emptyList()))
                        .withStatus(defaultGdiGroupStatus()),

                new GdiContentPromotionGroup()
                        .withType(AdGroupType.CONTENT_PROMOTION)
                        .withContentPromotionGroupType(GdiContentPromotionGroupType.COLLECTION)
                        .withId(TEST_GROUP_IDS[9])
                        .withCampaignId(CAMPAIGN.getId())
                        .withRegionsInfo(new GdiAdGroupRegionsInfo().withRegionIds(emptyList()))
                        .withStatus(defaultGdiGroupStatus())
        );

        adGroupsContainer = new GdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter())
                .withStatRequirements(new GdStatRequirements());

        doReturn(new GdiGroupsWithTotals().withGdiGroups(intGroups))
                .when(gridAdGroupService)
                .getGroups(anyInt(), anyLong(), any(), any(), any(), any(), any(), any(), anyBoolean(), any(),
                        any());

        ClientId clientId = ClientId.fromLong(clientInfo.getId());
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

        defaultTypes = Set.of(TEXT, DYNAMIC, MOBILE_CONTENT);
    }


    @Test
    public void testGetGroups() {
        doReturn(listToSet(features, FeatureName::getName))
                .when(featureService).getEnabledForClientId(eq(ClientId.fromLong(clientInfo.getId())));

        GdAdGroupWithTotals adGroupWithTotals = groupDataService
                .getAdGroups(clientInfo, adGroupsContainer, gridGraphQLContext);
        List<GdAdGroup> result = adGroupWithTotals.getGdAdGroups();
        Set<GdAdGroupType> actualTypes = listToSet(result, GdAdGroup::getType);

        Set<GdAdGroupType> expectedTypes = ImmutableSet.<GdAdGroupType>builder()
                .addAll(defaultTypes)
                .addAll(extraExpectedTypes)
                .build();

        assertThat(actualTypes)
                .is(matchedBy(beanDiffer(expectedTypes)));
    }
}
