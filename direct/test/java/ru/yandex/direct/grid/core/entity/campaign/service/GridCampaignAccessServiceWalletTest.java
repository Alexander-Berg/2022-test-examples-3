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

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.model.campaign.GdiCampaign;
import ru.yandex.direct.grid.model.campaign.GdiWalletAction;
import ru.yandex.direct.grid.model.campaign.GdiWalletActionsHolder;
import ru.yandex.direct.rbac.ClientPerm;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.rbac.model.RbacCampPerms;
import ru.yandex.direct.rbac.model.SubclientGrants;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.grid.core.util.GridCampaignTestUtil.defaultWallet;

@RunWith(JUnitParamsRunner.class)
public class GridCampaignAccessServiceWalletTest {
    private static final ClientId OPERATOR_CLIENT_ID = ClientId.fromLong(10L);
    private static final long OPERATOR_UID = 101L;

    private static final ClientId CLIENT_ID = ClientId.fromLong(20L);
    private static final long CLIENT_UID = 222L;

    private static final ClientId AGENCY_ID = ClientId.fromLong(30L);
    private static final long AGENCY_UID = 308L;

    @Mock
    private RbacService rbacService;

    @Mock
    private UserService userService;

    @InjectMocks
    private GridCampaignAccessService gridCampaignAccessService;

    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        "Самоход",
                        operator(RbacRole.CLIENT),
                        defaultWallet()
                                .withUserId(CLIENT_UID),

                        RbacCampPerms.ALL,
                        null,
                        false,
                        new GdiWalletActionsHolder()
                                .withActions(Set.of(GdiWalletAction.EDIT, GdiWalletAction.PAY))
                                .withSubclientAllowTransferMoney(null)
                                .withSubclientCanEdit(null),
                },
                {
                        "Самоход логин, которого заблокирован",
                        operator(RbacRole.CLIENT)
                                .withStatusBlocked(true),
                        defaultWallet()
                                .withUserId(CLIENT_UID),
                        RbacCampPerms.ALL,
                        null,
                        false,
                        new GdiWalletActionsHolder()
                                .withActions(Collections.emptySet())
                                .withSubclientAllowTransferMoney(null)
                                .withSubclientCanEdit(null),
                },
                {
                        "Субклиент со всеми правами",
                        operator(RbacRole.CLIENT),
                        defaultWallet()
                                .withUserId(CLIENT_UID)
                                .withAgencyId(AGENCY_ID.asLong())
                                .withAgencyUserId(AGENCY_UID),
                        RbacCampPerms.ALL,
                        SubclientGrants.buildWithAllGrants(AGENCY_UID, CLIENT_ID),
                        false,
                        new GdiWalletActionsHolder()
                                .withActions(Set.of(GdiWalletAction.EDIT, GdiWalletAction.PAY))
                                .withSubclientAllowTransferMoney(true)
                                .withSubclientCanEdit(true),
                },
                {
                        "Субклиент только с правами редактирования",
                        operator(RbacRole.CLIENT),
                        defaultWallet()
                                .withUserId(CLIENT_UID)
                                .withAgencyId(AGENCY_ID.asLong())
                                .withAgencyUserId(AGENCY_UID),
                        RbacCampPerms.ALL,
                        getSublientGrants(Set.of(ClientPerm.SUPER_SUBCLIENT)),
                        false,
                        new GdiWalletActionsHolder()
                                .withActions(Set.of(GdiWalletAction.EDIT, GdiWalletAction.PAY))
                                .withSubclientAllowTransferMoney(false)
                                .withSubclientCanEdit(true),
                },
                {
                        "Субклиент только с правами переноса средств.",
                        operator(RbacRole.CLIENT),
                        defaultWallet()
                                .withUserId(CLIENT_UID)
                                .withAgencyId(AGENCY_ID.asLong())
                                .withAgencyUserId(AGENCY_UID),
                        RbacCampPerms.ALL,
                        getSublientGrants(Set.of(ClientPerm.MONEY_TRANSFER)),
                        false,
                        new GdiWalletActionsHolder()
                                .withActions(Set.of(GdiWalletAction.EDIT, GdiWalletAction.PAY))
                                .withSubclientAllowTransferMoney(true)
                                .withSubclientCanEdit(false),
                },
                {
                        "Субклиент только с правами на импорт xls. Оплата недоступна",
                        operator(RbacRole.CLIENT),
                        defaultWallet()
                                .withUserId(CLIENT_UID)
                                .withAgencyId(AGENCY_ID.asLong())
                                .withAgencyUserId(AGENCY_UID),
                        RbacCampPerms.ALL,
                        getSublientGrants(Set.of(ClientPerm.XLS_IMPORT)),
                        false,
                        new GdiWalletActionsHolder()
                                .withActions(Set.of(GdiWalletAction.EDIT))
                                .withSubclientAllowTransferMoney(false)
                                .withSubclientCanEdit(false),
                },
                {
                        "Субклиент без дополнительных прав. Оплата недоступна",
                        operator(RbacRole.CLIENT),
                        defaultWallet()
                                .withUserId(CLIENT_UID)
                                .withAgencyId(AGENCY_ID.asLong())
                                .withAgencyUserId(AGENCY_UID),
                        RbacCampPerms.ALL,
                        getSublientGrants(Collections.emptySet()),
                        false,
                        new GdiWalletActionsHolder()
                                .withActions(Set.of(GdiWalletAction.EDIT))
                                .withSubclientAllowTransferMoney(false)
                                .withSubclientCanEdit(false),
                },
                // Главный представитель агентства. Субклиент со всеми правами.
                {
                        "Главный представитель агентства -> Субклиент со всеми правами",
                        operator(RbacRole.AGENCY, RbacRepType.CHIEF),
                        defaultWallet()
                                .withUserId(CLIENT_UID)
                                .withAgencyId(AGENCY_ID.asLong())
                                .withAgencyUserId(AGENCY_UID),
                        RbacCampPerms.ALL,
                        SubclientGrants.buildWithAllGrants(AGENCY_UID, CLIENT_ID),
                        false,
                        new GdiWalletActionsHolder()
                                .withActions(Set.of(GdiWalletAction.EDIT, GdiWalletAction.PAY))
                                .withSubclientAllowTransferMoney(true)
                                .withSubclientCanEdit(true),
                },
                {
                        "Основной представитель агентства -> Субклиент со всеми правами",
                        operator(RbacRole.AGENCY, RbacRepType.MAIN),
                        defaultWallet()
                                .withUserId(CLIENT_UID)
                                .withAgencyId(AGENCY_ID.asLong())
                                .withAgencyUserId(AGENCY_UID),
                        RbacCampPerms.ALL,
                        SubclientGrants.buildWithAllGrants(AGENCY_UID, CLIENT_ID),
                        false,
                        new GdiWalletActionsHolder()
                                .withActions(Set.of(GdiWalletAction.EDIT, GdiWalletAction.PAY))
                                .withSubclientAllowTransferMoney(true)
                                .withSubclientCanEdit(true),
                },
                {
                        "Ограниченный представитель агентства -> Субклиент со всеми правами",
                        operator(RbacRole.AGENCY, RbacRepType.LIMITED),
                        defaultWallet()
                                .withUserId(CLIENT_UID)
                                .withAgencyId(AGENCY_ID.asLong())
                                .withAgencyUserId(AGENCY_UID),
                        RbacCampPerms.ALL,
                        SubclientGrants.buildWithAllGrants(AGENCY_UID, CLIENT_ID),
                        false,
                        new GdiWalletActionsHolder()
                                .withActions(Set.of(GdiWalletAction.EDIT, GdiWalletAction.PAY))
                                .withSubclientAllowTransferMoney(true)
                                .withSubclientCanEdit(true),
                },
                {
                        "Ограниченный представитель агентства с запретом оплаты -> Субклиент со всеми правами. Оплата" +
                                " недоступна",
                        operator(RbacRole.AGENCY, RbacRepType.LIMITED),
                        defaultWallet()
                                .withUserId(CLIENT_UID)
                                .withAgencyId(AGENCY_ID.asLong())
                                .withAgencyUserId(AGENCY_UID),
                        RbacCampPerms.ALL,
                        SubclientGrants.buildWithAllGrants(AGENCY_UID, CLIENT_ID),
                        true,
                        new GdiWalletActionsHolder()
                                .withActions(Set.of(GdiWalletAction.EDIT))
                                .withSubclientAllowTransferMoney(true)
                                .withSubclientCanEdit(true),
                },
                {
                        "Вешальщик -> Субклиент со всеми правами. Оплата недоступна",
                        operator(RbacRole.PLACER),
                        defaultWallet()
                                .withUserId(CLIENT_UID)
                                .withAgencyId(AGENCY_ID.asLong())
                                .withAgencyUserId(AGENCY_UID),
                        RbacCampPerms.ALL,
                        SubclientGrants.buildWithAllGrants(AGENCY_UID, CLIENT_ID),
                        true,
                        new GdiWalletActionsHolder()
                                .withActions(Collections.emptySet())
                                .withSubclientAllowTransferMoney(true)
                                .withSubclientCanEdit(true),
                },
                {
                        "Superreader -> Субклиент со всеми правами. Оплата недоступна",
                        operator(RbacRole.SUPERREADER),
                        defaultWallet()
                                .withUserId(CLIENT_UID)
                                .withAgencyId(AGENCY_ID.asLong())
                                .withAgencyUserId(AGENCY_UID),
                        RbacCampPerms.ALL,
                        SubclientGrants.buildWithAllGrants(AGENCY_UID, CLIENT_ID),
                        true,
                        new GdiWalletActionsHolder()
                                .withActions(Collections.emptySet())
                                .withSubclientAllowTransferMoney(true)
                                .withSubclientCanEdit(true),
                },
                {
                        "Mediaplanner -> Субклиент со всеми правами. Оплата недоступна",
                        operator(RbacRole.MEDIA),
                        defaultWallet()
                                .withUserId(CLIENT_UID)
                                .withAgencyId(AGENCY_ID.asLong())
                                .withAgencyUserId(AGENCY_UID),
                        RbacCampPerms.ALL,
                        SubclientGrants.buildWithAllGrants(AGENCY_UID, CLIENT_ID),
                        true,
                        new GdiWalletActionsHolder()
                                .withActions(Collections.emptySet())
                                .withSubclientAllowTransferMoney(true)
                                .withSubclientCanEdit(true),
                }
        });
    }

    private static User operator(RbacRole role) {
        return operator(role, null);
    }

    private static User operator(RbacRole role, RbacRepType repType) {
        return new User()
                .withUid(role == RbacRole.CLIENT ? CLIENT_UID : OPERATOR_UID)
                .withChiefUid(role == RbacRole.CLIENT ? CLIENT_UID : OPERATOR_UID)
                .withClientId(role == RbacRole.CLIENT ? CLIENT_ID : OPERATOR_CLIENT_ID)
                .withRole(role)
                .withRepType(repType)
                .withSuperManager(false)
                .withDeveloper(false)
                .withStatusBlocked(false);
    }

    private static SubclientGrants getSublientGrants(Set<ClientPerm> perms) {
        return SubclientGrants.buildWithPermissions(AGENCY_UID, CLIENT_ID, perms);
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("{0}")
    public void testWalletAccess(@SuppressWarnings("unused") String testDescription, User operator, GdiCampaign wallet,
                                 RbacCampPerms campaignRights, SubclientGrants subclientGrants,
                                 boolean agencyRepIsNoPay, GdiWalletActionsHolder expectedAccess) {
        doReturn(Collections.singletonMap(wallet.getId(), campaignRights))
                .when(rbacService)
                .getCampaignsRights(eq(operator.getUid()), eq(Collections.singletonList(wallet.getId())));

        doReturn(agencyRepIsNoPay)
                .when(userService)
                .getUserAgencyIsNoPay(anyLong());

        doReturn(subclientGrants != null ? Collections.singletonList(subclientGrants) : Collections.emptyList())
                .when(rbacService)
                .getSubclientGrants(anyCollection(), any(ClientId.class));

        doReturn(AGENCY_UID)
                .when(rbacService)
                .getChief(anyLong());

        Map<Long, GdiWalletActionsHolder> walletActions =
                gridCampaignAccessService.getWalletsActions(operator, Collections.singletonList(wallet), operator);

        assertThat(walletActions)
                .containsOnly(entry(wallet.getId(), expectedAccess));
    }
}
