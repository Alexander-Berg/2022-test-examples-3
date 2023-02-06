package ru.yandex.direct.intapi.entity.balanceclient.controller;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.balanceclient.BalanceClientResponseMatcher;
import ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyClientParameters;

import static java.util.Collections.singletonList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class NotifyClient2InvalidOverdraftFieldsTest {

    @Autowired
    private Steps steps;

    @Autowired
    private BalanceClientController controller;

    private MockMvc mockMvc;

    private ClientInfo clientInfo;

    @Before
    public void prepare() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

        clientInfo = steps.clientSteps().createDefaultClient();
    }

    private NotifyClientParameters requestBase() {
        return new NotifyClientParameters()
                .withClientId(clientInfo.getClientId().asLong())
                .withBalanceCurrency(clientInfo.getClient().getWorkCurrency().name());
    }

    private ResultActions performRequest(NotifyClientParameters params) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(BalanceClientServiceConstants.NOTIFY_CLIENT2_PREFIX)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(singletonList(params)));
        return mockMvc.perform(requestBuilder);
    }

    @Test
    public void absentOverdraftLimit() throws Exception {
        NotifyClientParameters params = requestBase();

        performRequest(params)
                .andExpect(status().isBadRequest())
                .andExpect(new BalanceClientResponseMatcher("ручка ответила ошибкой", 1010,
                        "Overdraft limit from balance: undef, must be greater than zero"));
    }

    @Test
    public void negativeOverdraftLimit() throws Exception {
        NotifyClientParameters params = requestBase()
                .withOverdraftLimit(new BigDecimal(-1));

        performRequest(params)
                .andExpect(status().isBadRequest())
                .andExpect(new BalanceClientResponseMatcher("ручка ответила ошибкой", 1010,
                        "Overdraft limit from balance: -1, must be greater than zero"));
    }

    @Test
    public void absentOverdraftSpent() throws Exception {
        NotifyClientParameters params = requestBase()
                .withOverdraftLimit(BigDecimal.TEN);

        performRequest(params)
                .andExpect(status().isBadRequest())
                .andExpect(new BalanceClientResponseMatcher("ручка ответила ошибкой", 1011,
                        "Debt from balance: undef, must be greater than zero"));
    }

    @Test
    public void negativeOverdraftSpent() throws Exception {
        NotifyClientParameters params = requestBase()
                .withOverdraftLimit(BigDecimal.TEN)
                .withOverdraftSpent(new BigDecimal(-1));

        performRequest(params)
                .andExpect(status().isBadRequest())
                .andExpect(new BalanceClientResponseMatcher("ручка ответила ошибкой", 1011,
                        "Debt from balance: -1, must be greater than zero"));
    }
}
