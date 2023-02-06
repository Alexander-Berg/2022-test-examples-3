package ru.yandex.market.crm.campaign.services.security;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.campaign.services.security.checkers.AgentHasRoleChecker;
import ru.yandex.market.crm.campaign.services.security.checkers.SimpleHasRoleChecker;
import ru.yandex.market.crm.core.domain.AuthorDto;
import ru.yandex.market.crm.core.domain.templates.BlockTemplate;
import ru.yandex.market.crm.domain.Account;
import ru.yandex.market.crm.domain.CompositeUserRole;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BlockTemplatePermissionsServiceTest {
    @Mock
    public UserPermissionsService userPermissionsService;

    private BlockTemplatePermissionsService blockTemplatePermissionsServiceSpy;

    @Before
    public void before() {
        BlockTemplatePermissionsService blockTemplatePermissionsService = new BlockTemplatePermissionsService(
            userPermissionsService,
            new SimpleHasRoleChecker<>(),
            new AgentHasRoleChecker<>(userPermissionsService)
        );

        blockTemplatePermissionsServiceSpy = spy(blockTemplatePermissionsService);
        doReturn(0L).when(blockTemplatePermissionsServiceSpy).getUserUid();
    }

    @After
    public void reset_mocks() {
        reset(userPermissionsService);
    }


    @Test
    public void testAgentCanEditAndDeleteHisBlockTemplates() {
        when(userPermissionsService.getUserRoles(0))
                .thenReturn(Set.of(new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.AGENT)));

        BlockTemplate blockTemplate = new BlockTemplate();
        blockTemplate.setAuthor(new AuthorDto(0L));
        boolean hasPermissionOnEdit = blockTemplatePermissionsServiceSpy.hasPermission(
            () -> blockTemplate, ObjectPermissions.UPDATE);

        assertTrue(hasPermissionOnEdit);

        boolean hasPermissionOnDelete = blockTemplatePermissionsServiceSpy.hasPermission(
            () -> blockTemplate, ObjectPermissions.DELETE);

        assertTrue(hasPermissionOnDelete);
    }

    @Test
    public void testAgentCantEditAndDeleteNotHisBlockTemplates() {
        when(userPermissionsService.getUserRoles(0))
                .thenReturn(Set.of(new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.AGENT)));
        when(userPermissionsService.getUserRoles(1))
                .thenReturn(Set.of(new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.OPERATOR)));

        BlockTemplate blockTemplate = new BlockTemplate();
        blockTemplate.setAuthor(new AuthorDto(1L));

        boolean hasPermissionOnUpdate = blockTemplatePermissionsServiceSpy.hasPermission(
            () -> blockTemplate, ObjectPermissions.UPDATE);
        assertFalse(hasPermissionOnUpdate);

        boolean hasPermissionOnDelete = blockTemplatePermissionsServiceSpy.hasPermission(
            () -> blockTemplate, ObjectPermissions.UPDATE);
        assertFalse(hasPermissionOnDelete);
    }

    @Test
    public void testOperatorCanEditAgentBlockTemplates() {
        when(userPermissionsService.getUserRoles(0))
                .thenReturn(Set.of(new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.OPERATOR)));
        when(userPermissionsService.getUserRoles(1))
                .thenReturn(Set.of(new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.AGENT)));

        BlockTemplate blockTemplate = new BlockTemplate();
        blockTemplate.setAuthor(new AuthorDto(1L));

        boolean hasPermissionOnUpdate = blockTemplatePermissionsServiceSpy.hasPermission(
            () -> blockTemplate, ObjectPermissions.UPDATE);

        assertTrue(hasPermissionOnUpdate);

        boolean hasPermissionOnDelete = blockTemplatePermissionsServiceSpy.hasPermission(
            () -> blockTemplate, ObjectPermissions.DELETE);

        assertTrue(hasPermissionOnDelete);
    }
}
