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
import ru.yandex.market.crm.core.domain.messages.EmailMessageConf;
import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateType;
import ru.yandex.market.crm.domain.Account;
import ru.yandex.market.crm.domain.CompositeUserRole;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MessageTemplatePermissionsServiceTest {
    @Mock
    public UserPermissionsService userPermissionsService;

    private MessageTemplatePermissionsService messageTemplatePermissionsServiceSpy;

    @Before
    public void before() {
        MessageTemplatePermissionsService messageTemplatePermissionsService = new MessageTemplatePermissionsService(
            userPermissionsService,
            new SimpleHasRoleChecker<>(),
            new AgentHasRoleChecker<>(userPermissionsService)
        );

        messageTemplatePermissionsServiceSpy = spy(messageTemplatePermissionsService);
        doReturn(0L).when(messageTemplatePermissionsServiceSpy).getUserUid();
    }

    @After
    public void resetMocks() {
        reset(userPermissionsService);
    }

    @Test
    public void testAgentCanEditHisMessageTemplates() {
        when(userPermissionsService.getUserRoles(0))
                .thenReturn(Set.of(new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.AGENT)));

        var emailSending = new MessageTemplate<EmailMessageConf>();
        emailSending.setAuthorUid(0L);
        boolean hasPermission = messageTemplatePermissionsServiceSpy.hasPermission(
            () -> emailSending, ObjectPermissions.UPDATE
        );

        assertTrue(hasPermission);
    }

    @Test
    public void testAgentCantEditNotHisSengings() {
        when(userPermissionsService.getUserRoles(1))
                .thenReturn(Set.of(new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.OPERATOR)));

        var emailSending = template(1);

        boolean hasPermission = messageTemplatePermissionsServiceSpy.hasPermission(
            () -> emailSending, ObjectPermissions.UPDATE
        );

        assertFalse(hasPermission);
    }

    @Test
    public void testAgentCantDeleteAnyMessageTemplates() {
        when(userPermissionsService.getUserRoles(0))
                .thenReturn(Set.of(new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.AGENT)));
        when(userPermissionsService.getUserRoles(1))
                .thenReturn(Set.of(new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.OPERATOR)));

        var emailSending = template(0);

        boolean hasPermissionOnAgentObject = messageTemplatePermissionsServiceSpy.hasPermission(
            () -> emailSending, ObjectPermissions.DELETE
        );
        assertFalse(hasPermissionOnAgentObject);

        emailSending.setAuthorUid(1L);
        boolean hasPermissionOnOperatorObject = messageTemplatePermissionsServiceSpy.hasPermission(
            () -> emailSending, ObjectPermissions.DELETE
        );
        assertFalse(hasPermissionOnOperatorObject);

    }

    @Test
    public void testOperatorCanEditAnyMessageTemplates() {
        when(userPermissionsService.getUserRoles(0))
                .thenReturn(Set.of(new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.OPERATOR)));
        when(userPermissionsService.getUserRoles(1))
                .thenReturn(Set.of(new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.AGENT)));

        var emailSending = template(1);

        boolean hasPermission = messageTemplatePermissionsServiceSpy.hasPermission(
            () -> emailSending, ObjectPermissions.UPDATE
        );

        assertTrue(hasPermission);
    }

    private static MessageTemplate<EmailMessageConf> template(long authorUid) {
        var template = new MessageTemplate<EmailMessageConf>();
        template.setType(MessageTemplateType.EMAIL);
        template.setAuthorUid(authorUid);
        return template;
    }
}
