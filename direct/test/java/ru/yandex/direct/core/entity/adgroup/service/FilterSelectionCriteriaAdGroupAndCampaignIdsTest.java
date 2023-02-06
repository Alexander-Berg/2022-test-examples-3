package ru.yandex.direct.core.entity.adgroup.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria;
import ru.yandex.direct.core.entity.campaign.container.AffectedCampaignIdsContainer;
import ru.yandex.direct.core.entity.campaign.model.CampaignForAccessCheck;
import ru.yandex.direct.core.entity.campaign.model.CampaignForAccessCheckDefaultImpl;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignAccessCheckRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignForAccessCheckRepositoryAdapter;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessCheckerFactory;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.core.AllowedTypesCampaignAccessibilityChecker;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.campaign.model.CampaignSourceUtils.ALL_CAMPAIGN_SOURCES;
import static ru.yandex.direct.core.entity.campaign.model.CampaignTypeKinds.VCARDS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FilterSelectionCriteriaAdGroupAndCampaignIdsTest {

    private static final long OPERATOR_UID = 1;
    private static final ClientId CLIENT_ID = ClientId.fromLong(2);

    private static final Long FIRST_CAMPAIGN_ID = 1L;
    private static final Long SECOND_CAMPAIGN_ID = 2L;
    private static final Long[] CAMPAIGN_IDS = new Long[]{FIRST_CAMPAIGN_ID, SECOND_CAMPAIGN_ID};

    private static final Long FIRST_ADGROUP_ID = 10L;
    private static final Long SECOND_ADGROUP_ID = 11L;
    private static final Long THIRD_ADGROUP_ID = 12L;
    private static final Long FOURTH_ADGROUP_ID = 13L;
    private static final Long[] ADGROUP_IDS =
            new Long[]{FIRST_ADGROUP_ID, SECOND_ADGROUP_ID, THIRD_ADGROUP_ID, FOURTH_ADGROUP_ID};

    public CampaignRepository campaignRepository;
    public CampaignAccessCheckRepository campaignAccessCheckRepository;
    public RbacService rbacService;
    public ShardHelper shardHelper;
    public FeatureService featureService;
    private AdGroupService service;
    public CampaignSubObjectAccessCheckerFactory checkerFactory;
    public RequestCampaignAccessibilityCheckerProvider requestAccessibleCampaignTypes;

    @Before
    public void setUp() {
        rbacService = mock(RbacService.class);
        campaignRepository = mock(CampaignRepository.class);
        campaignAccessCheckRepository = mock(CampaignAccessCheckRepository.class);
        shardHelper = mock(ShardHelper.class);
        featureService = mock(FeatureService.class);

        requestAccessibleCampaignTypes = new RequestCampaignAccessibilityCheckerProvider();
        checkerFactory = new CampaignSubObjectAccessCheckerFactory(shardHelper, rbacService,
                campaignAccessCheckRepository,
                new AffectedCampaignIdsContainer(), requestAccessibleCampaignTypes, featureService);
        service = new AdGroupService(campaignRepository, shardHelper, checkerFactory);

        when(rbacService.getUidRole(anyLong())).thenReturn(RbacRole.CLIENT);
        requestAccessibleCampaignTypes.setCustom(new AllowedTypesCampaignAccessibilityChecker(VCARDS, VCARDS,
                ALL_CAMPAIGN_SOURCES, ALL_CAMPAIGN_SOURCES));
    }

    @Test
    public void filterOutNonVisibleAdGroups() {
        CampaignForAccessCheck visible = new CampaignForAccessCheckDefaultImpl()
                .withId(FIRST_CAMPAIGN_ID)
                .withType(CampaignType.TEXT)
                .withArchived(false);
        CampaignForAccessCheck notVisible = new CampaignForAccessCheckDefaultImpl()
                .withId(SECOND_CAMPAIGN_ID)
                .withType(CampaignType.TEXT)
                .withArchived(false);
        Map<Long, CampaignForAccessCheck> campaignsByAdGroup = new HashMap<>();
        campaignsByAdGroup.put(FIRST_ADGROUP_ID, visible);
        campaignsByAdGroup.put(SECOND_ADGROUP_ID, visible);
        campaignsByAdGroup.put(THIRD_ADGROUP_ID, notVisible);
        campaignsByAdGroup.put(FOURTH_ADGROUP_ID, notVisible);

        when(rbacService.getVisibleCampaigns(anyLong(), anyCollection())).thenReturn(singleton(FIRST_CAMPAIGN_ID));
        when(campaignAccessCheckRepository.getCampaignsForAccessCheckByAdGroupIds(anyInt(),
                any(CampaignForAccessCheckRepositoryAdapter.class), anyCollection()))
                .thenReturn(campaignsByAdGroup);

        AdGroupsSelectionCriteria selectionCriteria = new AdGroupsSelectionCriteria().withAdGroupIds(ADGROUP_IDS);
        service.filterSelectionCriteriaAdGroupAndCampaignIds(OPERATOR_UID, CLIENT_ID, selectionCriteria);
        assertThat(selectionCriteria.getAdGroupIds(), containsInAnyOrder(FIRST_ADGROUP_ID, SECOND_ADGROUP_ID));
    }

    @Test
    public void filterOutNotSupportedAdGroups() {
        CampaignForAccessCheck supported = new CampaignForAccessCheckDefaultImpl()
                .withId(FIRST_CAMPAIGN_ID)
                .withType(CampaignType.TEXT)
                .withArchived(false);
        CampaignForAccessCheck unsupported = new CampaignForAccessCheckDefaultImpl()
                .withId(SECOND_CAMPAIGN_ID)
                .withType(CampaignType.CPM_DEALS)
                .withArchived(false);
        Map<Long, CampaignForAccessCheck> campaignsByAdGroup = new HashMap<>();
        campaignsByAdGroup.put(FIRST_ADGROUP_ID, unsupported);
        campaignsByAdGroup.put(SECOND_ADGROUP_ID, unsupported);
        campaignsByAdGroup.put(THIRD_ADGROUP_ID, supported);
        campaignsByAdGroup.put(FOURTH_ADGROUP_ID, supported);
        Set<Long> visibleCampaigns = new HashSet<>();
        visibleCampaigns.addAll(Arrays.asList(CAMPAIGN_IDS));

        when(rbacService.getVisibleCampaigns(anyLong(), anyCollection())).thenReturn(visibleCampaigns);
        when(campaignAccessCheckRepository.getCampaignsForAccessCheckByAdGroupIds(anyInt(),
                any(CampaignForAccessCheckRepositoryAdapter.class), anyCollection()))
                .thenReturn(campaignsByAdGroup);

        AdGroupsSelectionCriteria selectionCriteria = new AdGroupsSelectionCriteria().withAdGroupIds(ADGROUP_IDS);
        service.filterSelectionCriteriaAdGroupAndCampaignIds(OPERATOR_UID, CLIENT_ID, selectionCriteria);
        assertThat(selectionCriteria.getAdGroupIds(), containsInAnyOrder(THIRD_ADGROUP_ID, FOURTH_ADGROUP_ID));
    }

    @Test
    public void filterOutNonVisibleCampaigns() {
        Map<Long, CampaignForAccessCheck> campaigns = new HashMap<>();
        CampaignForAccessCheck visible = new CampaignForAccessCheckDefaultImpl()
                .withId(FIRST_CAMPAIGN_ID)
                .withType(CampaignType.TEXT)
                .withArchived(false);
        CampaignForAccessCheck notVisible = new CampaignForAccessCheckDefaultImpl()
                .withId(SECOND_CAMPAIGN_ID)
                .withType(CampaignType.CPM_DEALS)
                .withArchived(false);
        campaigns.put(FIRST_CAMPAIGN_ID, visible);
        campaigns.put(SECOND_CAMPAIGN_ID, notVisible);

        when(rbacService.getVisibleCampaigns(anyLong(), anyCollection())).thenReturn(singleton(FIRST_CAMPAIGN_ID));
        when(campaignAccessCheckRepository.getCampaignsForAccessCheckByCampaignIds(
                anyInt(), any(CampaignForAccessCheckRepositoryAdapter.class), anyCollection())).thenReturn(campaigns);

        AdGroupsSelectionCriteria selectionCriteria = new AdGroupsSelectionCriteria().withCampaignIds(CAMPAIGN_IDS);
        service.filterSelectionCriteriaAdGroupAndCampaignIds(OPERATOR_UID, CLIENT_ID, selectionCriteria);
        assertThat(selectionCriteria.getCampaignIds(), contains(FIRST_CAMPAIGN_ID));
    }

    @Test
    public void filterOutNotSupportedCampaigns() {
        Map<Long, CampaignForAccessCheck> campaigns = new HashMap<>();
        CampaignForAccessCheck supported = new CampaignForAccessCheckDefaultImpl()
                .withId(FIRST_CAMPAIGN_ID)
                .withType(CampaignType.TEXT)
                .withArchived(false);
        CampaignForAccessCheck notSupported = new CampaignForAccessCheckDefaultImpl()
                .withId(SECOND_CAMPAIGN_ID)
                .withType(CampaignType.CPM_DEALS)
                .withArchived(false);
        campaigns.put(FIRST_CAMPAIGN_ID, notSupported);
        campaigns.put(SECOND_CAMPAIGN_ID, supported);

        when(rbacService.getVisibleCampaigns(anyLong(), anyCollection())).thenReturn(campaigns.keySet());
        when(campaignAccessCheckRepository.getCampaignsForAccessCheckByCampaignIds(
                anyInt(), any(CampaignForAccessCheckRepositoryAdapter.class), anyCollection())).thenReturn(campaigns);

        AdGroupsSelectionCriteria selectionCriteria = new AdGroupsSelectionCriteria().withCampaignIds(CAMPAIGN_IDS);
        service.filterSelectionCriteriaAdGroupAndCampaignIds(OPERATOR_UID, CLIENT_ID, selectionCriteria);
        assertThat(selectionCriteria.getCampaignIds(), contains(SECOND_CAMPAIGN_ID));
    }

}
