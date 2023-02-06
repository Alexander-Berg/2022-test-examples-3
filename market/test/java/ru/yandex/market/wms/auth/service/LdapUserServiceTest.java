package ru.yandex.market.wms.auth.service;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.PresentFilter;
import org.springframework.ldap.query.LdapQuery;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.auth.core.model.LdapUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class LdapUserServiceTest extends AuthIntegrationTest {

    @Autowired
    private LdapUserService ldapUserService;

    @Autowired
    @MockBean
    private LdapTemplate ldapTemplate;

    @AfterEach
    public void resetLdapTemplate() {
        Mockito.reset(ldapTemplate);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getUserListByLogin() {
        String login = "ad2";
        List<LdapUser> expected = List.of(new LdapUser(login, "ou=" + login, login + "name"));
        when(ldapTemplate.search(any(), (AttributesMapper<Object>) any())).thenAnswer(
                (InvocationOnMock invocationOnMock) -> {
                    LdapQuery argument = invocationOnMock.getArgument(0);
                    assertEquals(argument.filter(), new EqualsFilter("sAMAccountName", login));
                    return expected;
                }
        );
        List<LdapUser> actual = ldapUserService.getUserListByLogin(login);
        assertTrue(expected.size() == actual.size() && expected.containsAll(actual));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getUserList() {
        String login1 = "ad1";
        String login2 = "ad2";
        List<LdapUser> expected = List.of(
                new LdapUser(login1, "ou=" + login1, login1 + "name"),
                new LdapUser(login2, "ou=" + login2, login2 + "name")
        );
        when(ldapTemplate.search(any(), (AttributesMapper<Object>) any())).thenAnswer(
                (InvocationOnMock invocationOnMock) -> {
                    LdapQuery argument = invocationOnMock.getArgument(0);
                    assertEquals(argument.filter(), new PresentFilter("sAMAccountName"));
                    return expected;
                }
        );
        List<LdapUser> actual = ldapUserService.getUserListByLogin(null);
        assertTrue(expected.size() == actual.size() && expected.containsAll(actual));
    }
}
