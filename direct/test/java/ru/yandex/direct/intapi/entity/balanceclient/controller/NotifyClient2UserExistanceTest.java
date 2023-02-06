package ru.yandex.direct.intapi.entity.balanceclient.controller;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestClientRepository;
import ru.yandex.direct.core.testing.repository.TestUserRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyClientParameters;

import static java.util.Collections.singletonList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientResponseMatcher.ncAnswerOk;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientResponseMatcher.ncAnswerWarning;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class NotifyClient2UserExistanceTest {
    @Autowired
    private Steps steps;

    @Autowired
    private BalanceClientController controller;

    @Autowired
    private TestClientRepository testClientRepository;

    @Autowired
    private TestUserRepository testUserRepository;

    private UserInfo userInfo;
    private MockMvc mockMvc;
    private MockHttpServletRequestBuilder requestBuilder;

    @Before
    public void before() {
        userInfo = steps.userSteps().createDefaultUser();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        requestBuilder = post(BalanceClientServiceConstants.NOTIFY_CLIENT2_PREFIX)
                .contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    public void nonExistentUserRequest() throws Exception {
        ClientId clientId = userInfo.getClientInfo().getClientId();

        testUserRepository.deleteUser(userInfo.getShard(), userInfo.getUid());
        testClientRepository.deleteClient(userInfo.getShard(), clientId);

        NotifyClientParameters requestParams = new NotifyClientParameters().withClientId(clientId.asLong());
        requestBuilder.content(toJson(singletonList(requestParams)));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(ncAnswerWarning(String.format("ClientID %s is not known", clientId)));
    }

    @Test
    public void existentUserRequest() throws Exception {
        NotifyClientParameters requestParams = new NotifyClientParameters()
                .withClientId(userInfo.getClientInfo().getClientId().asLong())
                .withTid(1L)
                .withOverdraftLimit(BigDecimal.ZERO)
                .withOverdraftSpent(BigDecimal.ZERO);
        requestBuilder.content(toJson(singletonList(requestParams)));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(ncAnswerOk());
    }
}
