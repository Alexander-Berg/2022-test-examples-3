package ru.yandex.direct.web.configuration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FloatToIntDisabledTest {
    private MockMvc mockMvc;

    @Autowired
    TestAuthHelper testAuthHelper;

    @Autowired
    DirectWebAuthenticationSource directWebAuthenticationSource;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private UserInfo userInfo;

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        userInfo = testAuthHelper.createDefaultUser();
        TestAuthHelper.setSecurityContextWithAuthentication(directWebAuthenticationSource.getAuthentication());
    }

    @Test
    //проверяем, что дробный id в запросе вернет 400
    public void floatToIntField_badRequest() throws Exception {
        String param = "{\"id\": 1.5}";
        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/retargeting/condition/estimate?ulogin=" + userInfo.getClientInfo().getLogin())
                        .content(param)
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest());
    }

    @Test
    //проверяем, что целочисленный id вернет 200
    public void validRequest() throws Exception {
        String param = "{\"id\": 1}";
        mockMvc.perform(
                MockMvcRequestBuilders
                        .post("/retargeting/condition/estimate?ulogin=" + userInfo.getClientInfo().getLogin())
                        .content(param)
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());
    }

    @After
    public void after() {
        SecurityContextHolder.clearContext();
    }
}
