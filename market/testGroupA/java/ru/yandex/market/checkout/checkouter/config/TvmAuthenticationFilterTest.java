package ru.yandex.market.checkout.checkouter.config;


import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.common.web.AbstractTvmAuthenticationFilter;
import ru.yandex.market.checkout.common.web.CheckoutHttpParameters;
import ru.yandex.passport.tvmauth.CheckedServiceTicket;
import ru.yandex.passport.tvmauth.TicketStatus;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.LOG_TVM_CHECK_ENABLED;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.TVM_REQUIRED;

public class TvmAuthenticationFilterTest extends AbstractWebTestBase {

    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private TvmAuthenticationFilter tvmAuthenticationFilter;
    @Autowired
    private WireMockServer abcMock;
    @Autowired
    private Tvm2 tvm2;
    @Autowired
    private CheckouterFeatureWriter checkouterFeatureWriter;

    private static final String EXPIRED_TVM_TICKET = "3:serv:CNCIARC2j-aFBiIRCNTXehDU13ogtLGskaXU_gE:El9C27cdGKe9Z" +
            "g7YccsVQqsPIOFuCi33kKWeCiROM5VTj4pC32gmv_lQT5WWgMrhU4hT1M1P9YfsjL5RdL5OvWCuPuVpQmQ4WFIx2Ow45mPetgX-d_" +
            "znQhh4g5jCvJFmc9bO15NLWR_aBOm7rmvEkxwBKU1BBWyafxB4lpYjcVZIzWGCoZnrDMkhkQdtUWPu3nRCCxINJ_bjtjypYOobMbM" +
            "NWCPupluBRfQHV9BlVGwwFhcKg2ourZ07dzwEU1tLbOJvVfp5wE8FNStXS-RujLPaoqnTZolizhCHpUUzUSs8EoQOShkxiqWF14wO" +
            "polSNvwSYPFF8f7OBgG8LTp6TQ";

    @Value("${market.checkouter.tvm.whiteList}")
    private Set<Integer> whiteListClients;

    @BeforeEach
    public void init() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(tvmAuthenticationFilter)
                .build();
    }

    @AfterEach
    public void destroy() {
        abcMock.resetAll();
    }

    @Test
    public void tvmIgnoredWithHeaderIsTrue() throws Exception {
        checkouterFeatureWriter.writeValue(TVM_REQUIRED, true);
        checkouterFeatureWriter.writeValue(LOG_TVM_CHECK_ENABLED, true);

        mockMvc.perform(get("/orders", RandomUtils.nextLong())
                        .header(CheckoutHttpParameters.IGNORE_TVM_CHECK_HEADER, true)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void tvmEnabledWithHeaderIsFalse() throws Exception {
        checkouterFeatureWriter.writeValue(TVM_REQUIRED, true);
        checkouterFeatureWriter.writeValue(LOG_TVM_CHECK_ENABLED, true);

        mockMvc.perform(get("/orders", RandomUtils.nextLong())
                        .header(CheckoutHttpParameters.IGNORE_TVM_CHECK_HEADER, false)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(header().string(CheckoutHttpParameters.SERVICE_TICKET_HEADER_VALIDATION_ERROR,
                        AbstractTvmAuthenticationFilter.TVM_IS_BLANK_ERROR));
    }

    @Test
    public void annotationIgnoreTvmIsOk() throws Exception {
        checkouterFeatureWriter.writeValue(TVM_REQUIRED, true);
        checkouterFeatureWriter.writeValue(LOG_TVM_CHECK_ENABLED, true);

        mockMvc.perform(get("/properties", RandomUtils.nextLong())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void whiteListIsFilledFromConfig() {
        Assertions.assertEquals(4, tvmAuthenticationFilter.getWhiteList().size());
    }

    @Test
    public void tvmEnabledAndWhiteListIsFilled() throws Exception {
        performRequestWhileTvmEnabled(null, status().isOk());
        performRequestWhileTvmEnabled(Integer.MAX_VALUE, status().isForbidden());
    }

    @Test
    public void expiredTvmTicketForbidden() throws Exception {
        checkouterFeatureWriter.writeValue(TVM_REQUIRED, true);

        mockTvm2(null, TicketStatus.EXPIRED, EXPIRED_TVM_TICKET);

        mockMvc.perform(get("/orders", RandomUtils.nextLong())
                        .header(CheckoutHttpParameters.SERVICE_TICKET_HEADER, EXPIRED_TVM_TICKET)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/swagger-ui.html",
            "/v2/api-docs",
            "/webjars/foo",
            "/swagger-resources/foo",
            "/favicon.ico",
            "/ping",
            "/ping-alive",
            "/swagger-ui-json-folding-plugin.js",
            "/properties"
    })
    public void checkIgnoredTvmPaths(String url) throws Exception {
        checkouterFeatureWriter.writeValue(TVM_REQUIRED, true);

        var mvcResult = mockMvc.perform(head(url, RandomUtils.nextLong())
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        Assertions.assertNotEquals(HttpServletResponse.SC_FORBIDDEN, mvcResult.getResponse().getStatus(), url);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/orders/by-uid/123",
            "/properties/myProperty"
    })
    public void checkNotIgnoredTvmPaths(String url) throws Exception {
        checkouterFeatureWriter.writeValue(TVM_REQUIRED, true);

        var mvcResult = mockMvc.perform(head(url, RandomUtils.nextLong())
                        .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        Assertions.assertEquals(HttpServletResponse.SC_FORBIDDEN, mvcResult.getResponse().getStatus(), url);
    }

    private void performRequestWhileTvmEnabled(Integer clientSrc, ResultMatcher expectedStatus) throws Exception {
        checkouterFeatureWriter.writeValue(TVM_REQUIRED, true);
        checkouterFeatureWriter.writeValue(LOG_TVM_CHECK_ENABLED, true);

        String tvmTicket = "some-service-ticket";

        mockTvm2(clientSrc, TicketStatus.OK, tvmTicket);

        mockMvc.perform(get("/orders", RandomUtils.nextLong())
                        .header(CheckoutHttpParameters.IGNORE_TVM_CHECK_HEADER, false)
                        .header(CheckoutHttpParameters.SERVICE_TICKET_HEADER, tvmTicket)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(expectedStatus);
    }

    private void mockTvm2(Integer clientSrc, TicketStatus status, String ticket) {
        if (clientSrc == null) {
            clientSrc = whiteListClients.stream().findFirst().orElse(null);
            Assertions.assertNotNull(clientSrc, "Не заполнен white-list в конфиге");
        }
        CheckedServiceTicket checkedServiceTicket = new CheckedServiceTicket(status, "", clientSrc, -1);
        when(tvm2.checkServiceTicket(ticket)).thenReturn(checkedServiceTicket);
    }
}
