package ru.yandex.direct.intapi.entity.balanceclient.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyClientParameters;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class NotifyClient2MigrateToCurrencyDoneTest {

    @Autowired
    private Steps steps;

    @Autowired
    private BalanceClientController controller;

    @Autowired
    private ClientService clientService;


    private MockMvc mockMvc;

    private MockHttpServletRequestBuilder requestBuilder;

    private UserInfo userInfo;

    @Before
    public void before() {
        userInfo = steps.userSteps().createDefaultUser();
        steps.currencySteps().createCurrencyConversionQueueEntry(userInfo.getClientInfo(), 0L);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        requestBuilder = post(BalanceClientServiceConstants.NOTIFY_CLIENT2_PREFIX)
                .contentType(MediaType.APPLICATION_JSON);
    }

    /**
     * Тест проверяет, что флаг MigrateToCurrencyDone принимается даже в тех случаях,
     * когда запрос не может обработан быть
     */
    @Test
    public void validatePostmigrateChanges() throws Exception {
        ClientId clientId = userInfo.getClientInfo().getClientId();
        assertThat(clientService.isBalanceSideConvertFinished(clientId), is(false));
        NotifyClientParameters params = new NotifyClientParameters()
                .withClientId(clientId.asLong())
                .withMigrateToCurrencyDone(true);
        String requestBody = toJson(singletonList(params));
        requestBuilder.content(requestBody);
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest());
        assertThat(clientService.isBalanceSideConvertFinished(clientId), is(true));
    }
}
