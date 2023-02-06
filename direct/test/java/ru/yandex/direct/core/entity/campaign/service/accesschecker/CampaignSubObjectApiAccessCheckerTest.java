package ru.yandex.direct.core.entity.campaign.service.accesschecker;

import java.util.Map;
import java.util.Set;

import one.util.streamex.EntryStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.campaign.container.AffectedCampaignIdsContainer;
import ru.yandex.direct.core.entity.campaign.model.CampaignForAccessCheck;
import ru.yandex.direct.core.entity.campaign.model.CampaignForAccessCheckApiImpl;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.api5.Api5CampaignAccessibilityChecker;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;

import static java.util.Collections.singleton;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CampaignSubObjectApiAccessCheckerTest {
    private static final long OPERATOR_UID = 367L;
    private static final ClientId CLIENT_ID = ClientId.fromLong(124L);
    private static final int SHARD = 12;

    private static final long CAMP1 = 111L;
    private static final long CAMP2 = 222L;
    private static final long CAMP3 = 333L;
    private static final long CAMP4 = 444L;
    private static final long CAMP5 = 555L;
    private static final long CAMP6 = 666L;
    private static final long CAMP7 = 777L;

    private static final long OBJ_IN_CAMP1 = 1L;
    private static final long OBJ_IN_CAMP2 = 2L;
    private static final long OBJ_IN_CAMP3 = 3L;
    private static final long OBJ_IN_CAMP4 = 4L;
    private static final long OBJ_IN_CAMP5 = 5L;
    private static final long OBJ_IN_CAMP6 = 6L;
    private static final long OBJ_IN_CAMP7 = 7L;

    private static final CampaignForAccessCheck CAMPAIGN1 = new CampaignForAccessCheckApiImpl()
            .withId(CAMP1).withType(CampaignType.TEXT).withArchived(false).withIsUniversal(false);
    private static final CampaignForAccessCheck ARCHIVED_CAMPAIGN = new CampaignForAccessCheckApiImpl()
            .withId(CAMP2).withType(CampaignType.TEXT).withArchived(true).withIsUniversal(false);
    private static final CampaignForAccessCheck UNALLOWED_UNSUPPORTED_CAMPAIGN = new CampaignForAccessCheckApiImpl()
            .withId(CAMP3).withType(CampaignType.GEO).withArchived(false).withIsUniversal(false);
    private static final CampaignForAccessCheck UNSUPPORTED_CAMPAIGN = new CampaignForAccessCheckApiImpl()
            .withId(CAMP4).withType(CampaignType.MCBANNER).withArchived(false).withIsUniversal(false);
    private static final CampaignForAccessCheck UNIVERSAL_CAMPAIGN = new CampaignForAccessCheckApiImpl()
            .withId(CAMP5).withType(CampaignType.TEXT).withArchived(false).withIsUniversal(true);
    private static final CampaignForAccessCheck UAC_SOURCE_CAMPAIGN = new CampaignForAccessCheckApiImpl()
            .withId(CAMP6).withType(CampaignType.TEXT).withArchived(false).withIsUniversal(false)
            .withSource(CampaignSource.UAC);
    private static final CampaignForAccessCheck EDA_SOURCE_CAMPAIGN = new CampaignForAccessCheckApiImpl()
            .withId(CAMP7).withType(CampaignType.TEXT).withArchived(false).withIsUniversal(false)
            .withSource(CampaignSource.EDA);

    private static final Map<Long, CampaignForAccessCheck> SUB_OBJECT_TO_CAMP = Map.of(
            OBJ_IN_CAMP1, CAMPAIGN1,
            OBJ_IN_CAMP2, ARCHIVED_CAMPAIGN,
            OBJ_IN_CAMP3, UNALLOWED_UNSUPPORTED_CAMPAIGN,
            OBJ_IN_CAMP4, UNSUPPORTED_CAMPAIGN,
            OBJ_IN_CAMP5, UNIVERSAL_CAMPAIGN,
            OBJ_IN_CAMP6, UAC_SOURCE_CAMPAIGN,
            OBJ_IN_CAMP7, EDA_SOURCE_CAMPAIGN
    );
    private static final Set<Long> SUB_OBJECT_IDS = SUB_OBJECT_TO_CAMP.keySet();
    private static final Set<Long> CAMPAIGN_IDS =
            EntryStream.of(SUB_OBJECT_TO_CAMP).values().map(CampaignForAccessCheck::getId).toSet();
    private static final CampaignAccessibiltyChecker<CampaignForAccessCheckApiImpl> ALLOWED_API_5_CAMPAIGN_TYPES =
            Api5CampaignAccessibilityChecker.getApi5AccessibilityChecker();

    @Mock
    private RbacService rbacService;

    @Mock
    private CampaignSubObjectRetriever campaignSubObjectRetriever;

    @Mock
    private FeatureService featureService;

    private CampaignSubObjectAccessChecker<CampaignForAccessCheckApiImpl> checker;

    @Before
    public void setUp() {
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
    public void getVisible() {
        when(rbacService.getVisibleCampaigns(eq(OPERATOR_UID), any())).thenReturn(singleton(CAMP1));
        assertThat(checker.getVisible(), equalTo(singleton(OBJ_IN_CAMP1)));
    }

    @Test
    public void getWritable() {
        when(rbacService.getWritableCampaigns(eq(OPERATOR_UID), any())).thenReturn(singleton(CAMP2));
        assertThat(checker.getWritableAndEditable(), equalTo(singleton(OBJ_IN_CAMP2)));
    }

    @Test
    public void getAllowable() {
        assertThat(checker.getAllowable(),
                not(containsInAnyOrder(OBJ_IN_CAMP3, OBJ_IN_CAMP5, OBJ_IN_CAMP6)));
    }

    @Test
    public void getUnsupported() {
        assertThat(checker.getUnsupported(ALLOWED_API_5_CAMPAIGN_TYPES),
                containsInAnyOrder(OBJ_IN_CAMP3, OBJ_IN_CAMP4, OBJ_IN_CAMP5, OBJ_IN_CAMP6, OBJ_IN_CAMP7));
    }

    @Test
    public void objectInVisibleCampaign() {
        when(rbacService.getVisibleCampaigns(eq(OPERATOR_UID), any())).thenReturn(singleton(CAMP4));
        assertTrue(checker.objectInVisibleCampaign(OBJ_IN_CAMP4));
        assertFalse(checker.objectInVisibleCampaign(OBJ_IN_CAMP1));
    }

    @Test
    public void objectInWritableAndEditableCampaign() {
        when(rbacService.getWritableCampaigns(eq(OPERATOR_UID), any())).thenReturn(Set.of(CAMP2, CAMP7));
        assertFalse(checker.objectInWritableAndEditableCampaign(OBJ_IN_CAMP1));
        assertTrue(checker.objectInWritableAndEditableCampaign(OBJ_IN_CAMP2));
        assertFalse(checker.objectInWritableAndEditableCampaign(OBJ_IN_CAMP7));
    }

    @Test
    public void objectInArchivedCampaign() {
        assertTrue(checker.objectInArchivedCampaign(OBJ_IN_CAMP2));
        assertFalse(checker.objectInArchivedCampaign(OBJ_IN_CAMP1));
    }
}
