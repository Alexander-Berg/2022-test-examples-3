package ru.yandex.market.hrms.api.controller.user;

import java.security.Principal;
import java.util.List;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserControllerTest extends AbstractApiTest {

    @Test
    void validateUserReturnNull() throws Exception {
        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(principal.getName()).thenReturn("admin");
        mockMvc.perform(get("/lms/user")
                        .with(SecurityMockMvcRequestPostProcessors.user("USERNAME")
                                .authorities(List.of(
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_FUNC_BE_MULTIDOMAIN),
                                        new SimpleGrantedAuthority(HrmsPlugin.AUTHORITY_FUNC_BE_MULTIPARTNER)
                                ))))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "domainId":null,
                            "partnerId":null,
                            "isMultipartner":true,
                            "isMultidomain":true
                        }
                        """));
    }

    @Test
    @DbUnitDataSet(before = "UserControllerTest_base.before.csv")
    @DbUnitDataSet(before = "UserControllerTest_some_domains.before.csv")
    void someDomains() throws Exception {
        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(principal.getName()).thenReturn("admin");
        mockMvc.perform(get("/lms/user").cookie(new Cookie("yandex_login", "username"))
                        .param("domainId", "1")
                        .with(SecurityMockMvcRequestPostProcessors.user("username")))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "domainId":1,
                            "domainIds":[1,2,3],
                            "partnerId":null,
                            "isMultipartner":false,
                            "isMultidomain":true
                        }
                        """));
    }

    @Test
    @DbUnitDataSet(before = "UserControllerTest_base.before.csv")
    void oneDomain() throws Exception {
        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(principal.getName()).thenReturn("admin");
        mockMvc.perform(get("/lms/user").cookie(new Cookie("yandex_login", "username"))
                        .param("domainId", "1")
                        .with(SecurityMockMvcRequestPostProcessors.user("username")))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "domainId":1,
                            "partnerId":null,
                            "isMultipartner":false,
                            "isMultidomain":false
                        }
                        """));
    }


    @Test
    @DbUnitDataSet(before = "UserControllerTest_base.before.csv")
    void validateUserException() throws Exception {
        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(principal.getName()).thenReturn("admin");
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("X-Admin-Roles", "");
        mockMvc.perform(get("/lms/user").headers(httpHeaders))
                .andExpect(status().is4xxClientError());

    }
}
