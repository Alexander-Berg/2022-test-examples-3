package ru.yandex.direct.grid.core.entity.campaign.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.client.service.AgencyClientRelationService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.entity.model.client.GdiClientInfo;
import ru.yandex.direct.grid.model.campaign.GdiCampaignAction;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.rbac.model.RbacCampPerms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.defaultCampaign;

/**
 * Проверяет, что действия над кампанииями, доступные клиенту, будут доступны и limited support оператору.
 */
@RunWith(JUnitParamsRunner.class)
public class GridCampaignAccessServiceLimitedSupportTest {

    /**
     * Действия, которые доступны limited support оператору над кампаниями клиента, вне зависимости от того,
     * доступны они самому клиенту или нет
     */
    private static final Set<GdiCampaignAction> SPECIAL_LIMITED_SUPPORT_ACTIONS = Set.of(
            // Это действие вообще не доступно клиентам
            GdiCampaignAction.REMODERATE_CAMP,
            // Это действие вообще не доступно клиентам
            GdiCampaignAction.VIEW_OFFLINE_REPORTS,
            // Это действие всегда доступно limited_support (RbacCampPerms#READONLY),
            // но может быть не доступно клиенту, потому что не для всех клиентов canExportInExcel == true
            GdiCampaignAction.EXPORT_IN_EXCEL,
            // Это действие всегда доступно limited_support (hasOneOfRoles(c.operator, ..., LIMITED_SUPPORT),
            // но может быть не доступно клиенту, если у него нет прав на редактирование
            GdiCampaignAction.MANAGE_VCARDS
    );

    private static final long TEST_OPERATOR_UID = 9050;
    private static final long TEST_CLIENT_CHIEF_UID = 9302;
    private static final long TEST_CLIENT_ID = 1341;
    private static final long UNBIND_CLIENT_ID = 2308;

    @Mock
    private RbacService rbacService;

    @Mock
    private AgencyClientRelationService agencyClientRelationService;

    @InjectMocks
    private GridCampaignAccessService gridCampaignAccessService;

    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        RbacCampPerms.ALL,
                        false
                },
                {
                        RbacCampPerms.ALL,
                        true
                },
                {
                        RbacCampPerms.builder().withCanRead(true).build(),
                        false
                },
                {
                        RbacCampPerms.builder().withCanRead(true).withCanWrite(true).withCanDrop(true).build(),
                        false
                },
                {
                        RbacCampPerms.builder().withCanRead(true).withCanTransferMoney(true).build(),
                        false
                },
                {
                        RbacCampPerms.builder().withCanRead(true).withCanExportInExcel(true).build(),
                        false
                },
        });
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("{index}")
    public void testCampaignAccess(RbacCampPerms clientCampaignRights, boolean isWaitServicing) {
        var campaign = defaultCampaign();
        var campaignId = campaign.getId();
        doReturn(Map.of(campaignId, clientCampaignRights))
                .when(rbacService)
                .getCampaignsRights(eq(TEST_CLIENT_CHIEF_UID), eq(List.of(campaignId)));
        doReturn(Map.of(campaignId, RbacCampPerms.READONLY))
                .when(rbacService)
                .getCampaignsRights(eq(TEST_OPERATOR_UID), eq(List.of(campaignId)));
        doReturn(Map.of(campaignId, isWaitServicing))
                .when(rbacService)
                .getCampaignsWaitForServicing(eq(List.of(campaignId)));
        doReturn(Set.of(campaign.getAgencyId()))
                .when(agencyClientRelationService)
                .getUnbindedAgencies(eq(ClientId.fromLong(UNBIND_CLIENT_ID)));

        var limitedSupportOperator = new User()
                .withUid(TEST_OPERATOR_UID)
                .withRole(RbacRole.LIMITED_SUPPORT)
                .withSuperManager(false);

        var clientOperator = new User()
                .withUid(TEST_CLIENT_CHIEF_UID)
                .withRole(RbacRole.CLIENT)
                .withSuperManager(false);

        var clientInfo = new GdiClientInfo()
                .withId(TEST_CLIENT_ID)
                .withChiefUserId(TEST_CLIENT_CHIEF_UID);

        var clientAccess = gridCampaignAccessService
                .getCampaignsActions(clientOperator, clientInfo, List.of(campaign), clientOperator);

        var limitedSupportAccess = gridCampaignAccessService
                .getCampaignsActions(limitedSupportOperator, clientInfo, List.of(campaign), limitedSupportOperator);

        var clientHolder = clientAccess.get(campaignId);
        var limitedSupportHolder = limitedSupportAccess.get(campaignId);

        assertThat(limitedSupportHolder).isEqualToIgnoringGivenFields(clientHolder, "actions");

        var expectedLimitedSupportActions =
                StreamEx.of(clientHolder.getActions())
                        .append(SPECIAL_LIMITED_SUPPORT_ACTIONS)
                        .toSet();
        assertThat(limitedSupportHolder.getActions()).containsOnlyElementsOf(expectedLimitedSupportActions);
    }
}
