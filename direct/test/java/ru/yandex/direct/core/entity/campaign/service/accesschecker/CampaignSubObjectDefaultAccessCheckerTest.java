package ru.yandex.direct.core.entity.campaign.service.accesschecker;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import one.util.streamex.EntryStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.campaign.container.AffectedCampaignIdsContainer;
import ru.yandex.direct.core.entity.campaign.model.CampaignForAccessCheck;
import ru.yandex.direct.core.entity.campaign.model.CampaignForAccessCheckDefaultImpl;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.core.AllowedTypesCampaignAccessibilityChecker;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;

import static java.util.Collections.singleton;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.campaign.model.CampaignSourceUtils.API_DEFAULT_CAMPAIGN_SOURCES;
import static ru.yandex.direct.core.entity.campaign.model.CampaignTypeKinds.API5_EDIT;
import static ru.yandex.direct.core.entity.campaign.model.CampaignTypeKinds.API5_VISIBLE;


@RunWith(MockitoJUnitRunner.Silent.class)
public class CampaignSubObjectDefaultAccessCheckerTest {
    private static final long OPERATOR_UID = 367L;
    private static final ClientId CLIENT_ID = ClientId.fromLong(124L);
    private static final int SHARD = 12;

    private static final long CAMP1 = 111L;
    private static final long CAMP2 = 222L;
    private static final long CAMP3 = 333L;
    private static final long CAMP4 = 444L;

    private static final long OBJ_IN_CAMP1 = 1L;
    private static final long OBJ_IN_CAMP2 = 2L;
    private static final long OBJ_IN_CAMP3 = 3L;
    private static final long OBJ_IN_CAMP4 = 4L;

    private static final CampaignForAccessCheck CAMPAIGN1 =
            new CampaignForAccessCheckDefaultImpl().withId(CAMP1).withType(CampaignType.TEXT).withArchived(false);
    private static final CampaignForAccessCheck ARCHIVED_CAMPAIGN =
            new CampaignForAccessCheckDefaultImpl().withId(CAMP2).withType(CampaignType.TEXT).withArchived(true);
    private static final CampaignForAccessCheck UNALLOWED_UNSUPPORTED_CAMPAIGN =
            new CampaignForAccessCheckDefaultImpl().withId(CAMP3).withType(CampaignType.GEO).withArchived(false);
    private static final CampaignForAccessCheck UNSUPPORTED_CAMPAIGN =
            new CampaignForAccessCheckDefaultImpl().withId(CAMP4).withType(CampaignType.MCBANNER).withArchived(false);

    private static final Map<Long, CampaignForAccessCheck> SUB_OBJECT_TO_CAMP = ImmutableMap.of(
            OBJ_IN_CAMP1, CAMPAIGN1,
            OBJ_IN_CAMP2, ARCHIVED_CAMPAIGN,
            OBJ_IN_CAMP3, UNALLOWED_UNSUPPORTED_CAMPAIGN,
            OBJ_IN_CAMP4, UNSUPPORTED_CAMPAIGN
    );
    private static final Set<Long> SUB_OBJECT_IDS = SUB_OBJECT_TO_CAMP.keySet();
    private static final Set<Long> CAMPAIGN_IDS =
            EntryStream.of(SUB_OBJECT_TO_CAMP).values().map(CampaignForAccessCheck::getId).toSet();
    private static final CampaignAccessibiltyChecker<CampaignForAccessCheckDefaultImpl> ALLOWED_API_5_CAMPAIGN_TYPES =
            new AllowedTypesCampaignAccessibilityChecker(API5_EDIT, API5_VISIBLE, API_DEFAULT_CAMPAIGN_SOURCES, API_DEFAULT_CAMPAIGN_SOURCES);

    @Mock
    private RbacService rbacService;

    @Mock
    private CampaignSubObjectRetriever campaignSubObjectRetriever;

    @Mock
    private FeatureService featureService;

    private CampaignSubObjectAccessChecker<CampaignForAccessCheckDefaultImpl> checker;

    @Before
    public void setUp() throws Exception {
        when(campaignSubObjectRetriever.get(eq(SHARD), any(CampaignForAccessCheckRepositoryAdapter.class),
                eq(SUB_OBJECT_IDS))).thenReturn(SUB_OBJECT_TO_CAMP);
        when(rbacService.getVisibleCampaigns(eq(OPERATOR_UID), any())).thenReturn(CAMPAIGN_IDS);
        when(rbacService.getWritableCampaigns(eq(OPERATOR_UID), any())).thenReturn(CAMPAIGN_IDS);
        when(rbacService.getUidRole(eq(OPERATOR_UID))).thenReturn(RbacRole.CLIENT);
        checker = new CampaignSubObjectAccessChecker(rbacService, OPERATOR_UID, CLIENT_ID, SHARD,
                SUB_OBJECT_IDS, campaignSubObjectRetriever, new AffectedCampaignIdsContainer(),
                ALLOWED_API_5_CAMPAIGN_TYPES, featureService);
    }

    @Test
    public void getVisible() throws Exception {
        when(rbacService.getVisibleCampaigns(eq(OPERATOR_UID), any())).thenReturn(singleton(CAMP1));
        assertThat(checker.getVisible(), equalTo(singleton(OBJ_IN_CAMP1)));
    }

    @Test
    public void getWritableAndEditable() throws Exception {
        when(rbacService.getWritableCampaigns(eq(OPERATOR_UID), any())).thenReturn(singleton(CAMP2));
        assertThat(checker.getWritableAndEditable(), equalTo(singleton(OBJ_IN_CAMP2)));
    }

    @Test
    public void getAllowable() throws Exception {
        assertThat(checker.getAllowable(), not(hasItem(OBJ_IN_CAMP3)));
    }

    @Test
    public void getUnsupported() throws Exception {
        assertThat(checker.getUnsupported(ALLOWED_API_5_CAMPAIGN_TYPES), containsInAnyOrder(OBJ_IN_CAMP3, OBJ_IN_CAMP4));
    }

    @Test
    public void objectInVisibleCampaign() throws Exception {
        when(rbacService.getVisibleCampaigns(eq(OPERATOR_UID), any())).thenReturn(singleton(CAMP4));
        assertTrue(checker.objectInVisibleCampaign(OBJ_IN_CAMP4));
        assertFalse(checker.objectInVisibleCampaign(OBJ_IN_CAMP1));
    }

    @Test
    public void objectInWritableAndEditableCampaign() throws Exception {
        when(rbacService.getWritableCampaigns(eq(OPERATOR_UID), any())).thenReturn(singleton(CAMP2));
        assertFalse(checker.objectInWritableAndEditableCampaign(OBJ_IN_CAMP1));
        assertTrue(checker.objectInWritableAndEditableCampaign(OBJ_IN_CAMP2));
    }

    @Test
    public void objectInArchivedCampaign() throws Exception {
        assertTrue(checker.objectInArchivedCampaign(OBJ_IN_CAMP2));
        assertFalse(checker.objectInArchivedCampaign(OBJ_IN_CAMP1));
    }
}
