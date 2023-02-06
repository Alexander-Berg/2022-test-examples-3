package ru.yandex.market.wms.auth.controller;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.PresentFilter;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.auth.core.model.LdapUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class LdapUserControllerTest extends AuthIntegrationTest {

    @Autowired
    @MockBean
    private LdapTemplate ldapTemplate;

    @AfterEach
    public void resetLdapTemplate() {
        Mockito.reset(ldapTemplate);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getUserListByLogin() throws Exception {
        String login = "AD1";
        List<LdapUser> expected = List.of(new LdapUser(login, "ou=" + login, login + "name"));
        when(ldapTemplate.search(any(), (AttributesMapper<Object>) any())).thenAnswer(
                (InvocationOnMock invocationOnMock) -> {
                    LdapQuery argument = invocationOnMock.getArgument(0);
                    assertEquals(argument.filter(), new EqualsFilter("sAMAccountName", login));
                    return expected;
                }
        );

        ResultActions result = mockMvc.perform(get("/ldap/users/" + login)
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/ldap/response/get-users-by-login.json")));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getUserList() throws Exception {
        String login1 = "AD1";
        String login2 = "AD2";
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

        ResultActions result = mockMvc.perform(get("/ldap/users")
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/ldap/response/get-users.json")));
    }
}
