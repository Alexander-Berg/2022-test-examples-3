package ru.yandex.direct.grid.core.entity.campaign.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
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
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.model.campaign.GdiCampaignActionsHolder;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.rbac.model.RbacCampPerms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.defaultCampaign;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.ACCEPT_SERVICING;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.ARCHIVE_CAMP;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.BAN_PAY;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.CAN_BE_AUTODELETED;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.COPY_CAMP_CLIENT;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.DELETE_CAMP;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.DISABLE_WEEKLY_BUDGET;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.EDIT_CAMP;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.EDIT_WEEKLY_BUDGET;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.EXPORT_IN_EXCEL;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.LOOKUP;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.MANAGE_PROMO_EXTENSION;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.MANAGE_VCARDS;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.OFFER_SERVICING;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.OTHER_MANAGER_SERVICED;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.PAY;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.REMODERATE_CAMP;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.RESET_FLIGHT_STATUS_APPROVE;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.RESUME_CAMP;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.SERVICED;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.SHOW_BS_LINK;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.SHOW_CAMP_SETTINGS;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.SHOW_CAMP_STAT;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.STOP_CAMP;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.UNARCHIVE_CAMP;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.UNBAN_PAY;
import static ru.yandex.direct.grid.model.campaign.GdiCampaignAction.VIEW_OFFLINE_REPORTS;

@RunWith(JUnitParamsRunner.class)
public class GridCampaignAccessServiceTest {

    private static final long TEST_OPERATOR_UID = 100500;

    private static final long TEST_CLIENT_ID = 1337;
    private static final long TEST_CLIENT_CHIEF_UID = 193202;
    private static final long UNBIND_CLIENT_ID = 1488;

    @Mock
    private RbacService rbacService;

    @Mock
    private AgencyClientRelationService agencyClientRelationService;

    @InjectMocks
    private GridCampaignAccessService gridCampaignAccessService;

    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        operator(RbacRole.SUPER),
                        client(),
                        defaultCampaign(),
                        RbacCampPerms.EMPTY,
                        false,
                        new GdiCampaignActionsHolder()
                                .withActions(Set
                                        .of(EDIT_CAMP, STOP_CAMP, RESUME_CAMP, OFFER_SERVICING, SHOW_CAMP_STAT, LOOKUP,
                                                COPY_CAMP_CLIENT, PAY, REMODERATE_CAMP, SHOW_BS_LINK,
                                                RESET_FLIGHT_STATUS_APPROVE, VIEW_OFFLINE_REPORTS, CAN_BE_AUTODELETED,
                                                BAN_PAY, UNBAN_PAY, MANAGE_PROMO_EXTENSION, EDIT_WEEKLY_BUDGET,
                                                DISABLE_WEEKLY_BUDGET))
                                .withCanEdit(true)
                                .withHasManager(false)
                                .withHasAgency(false),
                },
                {
                        operator(RbacRole.SUPER),
                        client(),
                        defaultCampaign(),
                        RbacCampPerms.ALL,
                        false,
                        new GdiCampaignActionsHolder()
                                .withActions(Set
                                        .of(EDIT_CAMP, STOP_CAMP, RESUME_CAMP, OFFER_SERVICING, SHOW_CAMP_STAT, LOOKUP,
                                                UNARCHIVE_CAMP, MANAGE_VCARDS, EXPORT_IN_EXCEL, ARCHIVE_CAMP,
                                                DELETE_CAMP, COPY_CAMP_CLIENT, PAY, REMODERATE_CAMP, SHOW_BS_LINK,
                                                RESET_FLIGHT_STATUS_APPROVE, VIEW_OFFLINE_REPORTS, CAN_BE_AUTODELETED,
                                                BAN_PAY, UNBAN_PAY, MANAGE_PROMO_EXTENSION, EDIT_WEEKLY_BUDGET,
                                                DISABLE_WEEKLY_BUDGET))
                                .withCanEdit(true)
                                .withHasManager(false)
                                .withHasAgency(false),
                },
                {
                        operator(RbacRole.SUPERREADER),
                        client(),
                        defaultCampaign(),
                        RbacCampPerms.EMPTY,
                        false,
                        new GdiCampaignActionsHolder()
                                .withActions(Set
                                        .of(OFFER_SERVICING, SHOW_CAMP_SETTINGS, SHOW_BS_LINK, CAN_BE_AUTODELETED,
                                                MANAGE_VCARDS, VIEW_OFFLINE_REPORTS))
                                .withCanEdit(false)
                                .withHasManager(false)
                                .withHasAgency(false),
                },
                {
                        operator(RbacRole.SUPERREADER),
                        client(),
                        defaultCampaign(),
                        RbacCampPerms.READONLY,
                        false,
                        new GdiCampaignActionsHolder()
                                .withActions(Set
                                        .of(OFFER_SERVICING, SHOW_CAMP_SETTINGS, SHOW_CAMP_STAT, LOOKUP, SHOW_BS_LINK,
                                                EXPORT_IN_EXCEL, CAN_BE_AUTODELETED, MANAGE_VCARDS,
                                                VIEW_OFFLINE_REPORTS))
                                .withCanEdit(false)
                                .withHasManager(false)
                                .withHasAgency(false),
                },
                {
                        operator(RbacRole.SUPPORT),
                        client(),
                        defaultCampaign(),
                        RbacCampPerms.READONLY,
                        false,
                        new GdiCampaignActionsHolder()
                                .withActions(Set
                                        .of(UNARCHIVE_CAMP, SHOW_CAMP_STAT, LOOKUP, REMODERATE_CAMP, EXPORT_IN_EXCEL,
                                                VIEW_OFFLINE_REPORTS, RESET_FLIGHT_STATUS_APPROVE, BAN_PAY,
                                                CAN_BE_AUTODELETED, UNBAN_PAY, ARCHIVE_CAMP, SHOW_BS_LINK))
                                .withCanEdit(false)
                                .withHasManager(false)
                                .withHasAgency(false),
                },
                {
                        operator(RbacRole.LIMITED_SUPPORT),
                        client(),
                        defaultCampaign(),
                        RbacCampPerms.READONLY,
                        false,
                        new GdiCampaignActionsHolder()
                                .withActions(Set
                                        .of(OFFER_SERVICING, SHOW_CAMP_STAT, LOOKUP, REMODERATE_CAMP, SHOW_BS_LINK,
                                                EXPORT_IN_EXCEL, CAN_BE_AUTODELETED, MANAGE_VCARDS,
                                                VIEW_OFFLINE_REPORTS, ARCHIVE_CAMP, UNARCHIVE_CAMP))
                                .withCanEdit(false)
                                .withHasManager(false)
                                .withHasAgency(false),
                },
                {
                        operator(RbacRole.MANAGER),
                        client(),
                        defaultCampaign().withManagerUserId(TEST_OPERATOR_UID),
                        RbacCampPerms.ALL,
                        false,
                        new GdiCampaignActionsHolder()
                                .withActions(Set
                                        .of(RESUME_CAMP, REMODERATE_CAMP, STOP_CAMP, ARCHIVE_CAMP, UNARCHIVE_CAMP,
                                                LOOKUP, OFFER_SERVICING, EDIT_CAMP, SERVICED, SHOW_CAMP_STAT, PAY,
                                                DELETE_CAMP, OTHER_MANAGER_SERVICED, SHOW_BS_LINK, EXPORT_IN_EXCEL,
                                                RESET_FLIGHT_STATUS_APPROVE, CAN_BE_AUTODELETED, MANAGE_VCARDS,
                                                MANAGE_PROMO_EXTENSION, EDIT_WEEKLY_BUDGET, DISABLE_WEEKLY_BUDGET))
                                .withCanEdit(true)
                                .withHasManager(true)
                                .withHasAgency(false),
                },
                {
                        operator(RbacRole.CLIENT),
                        client(),
                        defaultCampaign(),
                        RbacCampPerms.ALL,
                        false,
                        new GdiCampaignActionsHolder()
                                .withActions(Set
                                        .of(RESUME_CAMP, STOP_CAMP, ARCHIVE_CAMP, UNARCHIVE_CAMP, LOOKUP,
                                                OFFER_SERVICING, EDIT_CAMP, COPY_CAMP_CLIENT, SHOW_CAMP_STAT, PAY,
                                                DELETE_CAMP, SHOW_BS_LINK, EXPORT_IN_EXCEL, CAN_BE_AUTODELETED,
                                                MANAGE_VCARDS, MANAGE_PROMO_EXTENSION, EDIT_WEEKLY_BUDGET,
                                                DISABLE_WEEKLY_BUDGET))
                                .withCanEdit(true)
                                .withHasManager(false)
                                .withHasAgency(false),
                },
                {
                        operator(RbacRole.CLIENT)
                                .withStatusBlocked(true),
                        client(),
                        defaultCampaign(),
                        RbacCampPerms.ALL,
                        false,
                        new GdiCampaignActionsHolder()
                                .withActions(Set.of(SHOW_CAMP_STAT, SHOW_BS_LINK, OFFER_SERVICING, LOOKUP,
                                        EXPORT_IN_EXCEL, CAN_BE_AUTODELETED, MANAGE_VCARDS))
                                .withCanEdit(false)
                                .withHasManager(false)
                                .withHasAgency(false),
                },
                {
                        operator(RbacRole.CLIENT),
                        client(),
                        defaultCampaign(),
                        RbacCampPerms.EMPTY,
                        true,
                        new GdiCampaignActionsHolder()
                                .withActions(Set.of(ACCEPT_SERVICING, SHOW_BS_LINK, CAN_BE_AUTODELETED))
                                .withCanEdit(false)
                                .withHasManager(false)
                                .withHasAgency(false),
                },
        });
    }

    private static User operator(RbacRole role) {
        return new User()
                .withUid(TEST_OPERATOR_UID)
                .withRole(role)
                .withSuperManager(false)
                .withDeveloper(false)
                .withStatusBlocked(false);
    }

    private static GdiClientInfo client() {
        return new GdiClientInfo()
                .withId(TEST_CLIENT_ID)
                .withChiefUserId(TEST_CLIENT_CHIEF_UID);
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("{index}")
    public void testCampaignAccess(User operator, GdiClientInfo clientInfo, GdiCampaign campaign,
                                   RbacCampPerms campaignRights, boolean isWaitServicing,
                                   GdiCampaignActionsHolder expectedAccess) {
        doReturn(Collections.singletonMap(campaign.getId(), campaignRights))
                .when(rbacService)
                .getCampaignsRights(eq(TEST_OPERATOR_UID), eq(Collections.singletonList(campaign.getId())));
        doReturn(Collections.singletonMap(campaign.getId(), isWaitServicing))
                .when(rbacService).getCampaignsWaitForServicing(eq(Collections.singletonList(campaign.getId())));
        doReturn(Set.of(campaign.getAgencyId()))
                .when(agencyClientRelationService).getUnbindedAgencies(eq(ClientId.fromLong(UNBIND_CLIENT_ID)));

        Map<Long, GdiCampaignActionsHolder> actions = gridCampaignAccessService
                .getCampaignsActions(operator, clientInfo, Collections.singleton(campaign), operator);

        assertThat(actions)
                .containsOnly(entry(campaign.getId(), expectedAccess));
    }
}
