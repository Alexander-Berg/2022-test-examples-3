package ru.yandex.direct.intapi.entity.balanceclient.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.entity.agency.model.AgencyAdditionalCurrency;
import ru.yandex.direct.core.entity.agency.service.AgencyService;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.intapi.configuration.IntApiTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientResponseMatcher.ncAnswerOk;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class NotifyAgencyAdditionalCurrenciesTest {
    private static final LocalDate TEST_DATE_ONE = LocalDate.now().plusDays(1);
    private static final LocalDate TEST_DATE_TWO = LocalDate.now().minusDays(1);
    private static final LocalDate TEST_DATE_THREE = LocalDate.now().minusDays(2);
    private static final LocalDate TEST_DATE_FOUR = LocalDate.now().minusDays(4);

    @Autowired
    private BalanceClientController controller;
    @Autowired
    private AgencyService agencyService;
    @Autowired
    private ClientSteps clientSteps;

    private Long clientId;
    private MockMvc mockMvc;
    private MockHttpServletRequestBuilder requestBuilder;

    @Before
    public void before() throws Exception {
        clientId = clientSteps.createDefaultClient().getClientId().asLong();

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        requestBuilder =
                post("/BalanceClient/NotifyAgencyAdditionalCurrencies")
                        .contentType(MediaType.APPLICATION_JSON);
    }

    private String getRequestBody(Long clientId) {
        return String.format("[{\"ClientID\":%s,\"AdditionalCurrencies\":"
                        + "[{\"Currency\":\"%s\",\"ExpireDate\":\"%s\"},"
                        + "{\"Currency\":\"%s\",\"ExpireDate\":\"%s\"},"
                        + "{\"Currency\":\"%s\",\"ExpireDate\":\"%s\"}]}]", clientId,
                CurrencyCode.BYN.name(), TEST_DATE_ONE,
                CurrencyCode.CHF.name(), TEST_DATE_FOUR,
                CurrencyCode.RUB.name(), TEST_DATE_ONE);
    }

    @Test
    public void testAdditionalCurrencies() throws Exception {
        List<AgencyAdditionalCurrency> oldCurrencies = new ArrayList<>();
        oldCurrencies.add(new AgencyAdditionalCurrency()
                .withClientId(clientId)
                .withCurrencyCode(CurrencyCode.BYN)
                .withExpirationDate(TEST_DATE_ONE));
        oldCurrencies.add(new AgencyAdditionalCurrency()
                .withClientId(clientId)
                .withCurrencyCode(CurrencyCode.CHF)
                .withExpirationDate(TEST_DATE_TWO));
        oldCurrencies.add(new AgencyAdditionalCurrency()
                .withClientId(clientId)
                .withCurrencyCode(CurrencyCode.RUB)
                .withExpirationDate(TEST_DATE_THREE));

        agencyService.addAdditionalCurrencies(oldCurrencies);

        requestBuilder.content(getRequestBody(clientId));

        Date nowDate = DateUtils.round(new Date(), Calendar.SECOND);
        LocalDateTime checkTime = nowDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(ncAnswerOk());
        List<AgencyAdditionalCurrency> newCurrencies = agencyService.getAllAdditionalCurrencies(clientId);

        SoftAssertions soft = new SoftAssertions();
        for (AgencyAdditionalCurrency newCurrency : newCurrencies) {
            if (newCurrency.getCurrencyCode() == CurrencyCode.BYN) {
                soft.assertThat(newCurrency.getLastChange()).isBeforeOrEqualTo(checkTime);
            } else {
                soft.assertThat(newCurrency.getLastChange()).isAfterOrEqualTo(checkTime);
            }
        }
        soft.assertAll();

        newCurrencies.forEach(c -> c.setLastChange(null));
        assertThat(newCurrencies)
                .containsExactlyInAnyOrder(
                        new AgencyAdditionalCurrency()
                                .withClientId(clientId)
                                .withCurrencyCode(CurrencyCode.BYN)
                                .withExpirationDate(TEST_DATE_ONE),
                        new AgencyAdditionalCurrency()
                                .withClientId(clientId)
                                .withCurrencyCode(CurrencyCode.CHF)
                                .withExpirationDate(TEST_DATE_FOUR),
                        new AgencyAdditionalCurrency()
                                .withClientId(clientId)
                                .withCurrencyCode(CurrencyCode.RUB)
                                .withExpirationDate(TEST_DATE_ONE)
                );
    }

    @Test
    public void testClientIdNotExist() throws Exception {
        requestBuilder.content(getRequestBody(Long.MAX_VALUE));

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(ncAnswerOk());
        List<AgencyAdditionalCurrency> newCurrencies = agencyService.getAllAdditionalCurrencies(clientId);
        assertThat(newCurrencies)
                .isEmpty();
    }
}
