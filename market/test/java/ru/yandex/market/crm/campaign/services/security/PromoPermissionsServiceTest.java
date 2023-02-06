package ru.yandex.market.crm.campaign.services.security;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.campaign.domain.sending.EmailPlainSending;
import ru.yandex.market.crm.campaign.services.security.checkers.HasRoleToPromoByAccountChecker;
import ru.yandex.market.crm.campaign.services.security.checkers.SimpleHasRoleChecker;
import ru.yandex.market.crm.dao.AccountsDao;
import ru.yandex.market.crm.domain.Account;
import ru.yandex.market.crm.domain.CompositeUserRole;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PromoPermissionsServiceTest {
    @Mock
    public UserPermissionsService userPermissionsService;

    @Mock
    private AccountsDao accountsDao;

    private PromoPermissionsService promoPermissionsServiceSpy;


    @Before
    public void before() {
        PromoPermissionsService promoPermissionsService = new PromoPermissionsService(
            userPermissionsService,
            new SimpleHasRoleChecker<>(),
            new HasRoleToPromoByAccountChecker<>(accountsDao)
        );

        promoPermissionsServiceSpy = spy(promoPermissionsService);
        doReturn(0L).when(promoPermissionsServiceSpy).getUserUid();
    }

    @After
    public void reset_mocks() {
        reset(userPermissionsService);
    }

    @Test
    public void testAgentCanEditAnySendings() {
        when(userPermissionsService.getUserRoles(0))
                .thenReturn(Set.of(new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.AGENT)));
        when(userPermissionsService.getUserRoles(1))
                .thenReturn(Set.of(new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.OPERATOR)));

        EmailPlainSending emailSending = new EmailPlainSending();
        emailSending.setAuthorUid(1L);

        boolean hasPermission = promoPermissionsServiceSpy.hasPermission(
                () -> emailSending, ObjectPermissions.UPDATE);

        assertTrue(hasPermission);
    }

    @Test
    public void testAgentCantDeleteAnySendings() {
        when(userPermissionsService.getUserRoles(0))
                .thenReturn(Set.of(new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.AGENT)));
        when(userPermissionsService.getUserRoles(1))
                .thenReturn(Set.of(new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.OPERATOR)));

        EmailPlainSending emailSending = new EmailPlainSending();
        emailSending.setAuthorUid(0L);
        boolean hasPermissionOnAgentObject = promoPermissionsServiceSpy.hasPermission(
            () -> emailSending, ObjectPermissions.DELETE
        );
        assertFalse(hasPermissionOnAgentObject);

        emailSending.setAuthorUid(1L);
        boolean hasPermissionOnOperatorObject = promoPermissionsServiceSpy.hasPermission(
            () -> emailSending, ObjectPermissions.DELETE
        );
        assertFalse(hasPermissionOnOperatorObject);

    }

    @Test
    public void testOperatorCanEditAnySendings() {
        when(userPermissionsService.getUserRoles(0))
                .thenReturn(Set.of(new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.OPERATOR)));
        when(userPermissionsService.getUserRoles(1))
                .thenReturn(Set.of(new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.AGENT)));

        EmailPlainSending emailSending = new EmailPlainSending();
        emailSending.setAuthorUid(1L);

        boolean hasPermission = promoPermissionsServiceSpy.hasPermission(
            () -> emailSending, ObjectPermissions.UPDATE);

        assertTrue(hasPermission);
    }
}
