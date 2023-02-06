package ru.yandex.market.checkout.checkouter.web;

import java.util.Set;

import javax.annotation.Nonnull;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.config.TvmAuthenticationFilter;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.passport.tvmauth.CheckedServiceTicket;
import ru.yandex.passport.tvmauth.TicketStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ROLE;

/**
 * @author jkt
 */
@Disabled //MARKETCHECKOUT-17119
public class CheckouterTvmAuthenticationTest extends AbstractWebTestBase {

    private static final String TVM_TICKET_HEADER = "X-Ya-Service-Ticket";
    private static final long YANDEX_UID = 1120000000135414L;
    private static final int[] READ_ROLE_ID = new int[]{631};
    private static final int[] WRITE_ROLE_ID = new int[]{2300};
    private static final int ABC_SERVICE_ID = 1851;

    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private TvmAuthenticationFilter tvmAuthenticationFilter;
    private TvmAuthenticationFilter filter;
    @Autowired
    private Tvm2 tvm;
    @Autowired
    private WireMockServer abcMock;

    @Value("${market.checkouter.tvm.whiteList}")
    private Set<Integer> whiteListClients;

    @BeforeEach
    public void init() {
        filter = spy(tvmAuthenticationFilter);
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(filter)
                .build();
        when(tvm.checkServiceTicket(anyString()))
                .thenReturn(null);
        checkouterFeatureWriter.writeValue(BooleanFeatureType.TVM_REQUIRED, false);
    }

    @AfterEach
    public void destroy() {
        abcMock.resetAll();
        checkouterFeatureWriter.writeValue(BooleanFeatureType.TVM_REQUIRED, false);
    }

    @Test
    public void whenTvmDisabledShouldIgnoreTvmHeader() throws Exception {
        mockMvc.perform(ordersRequest()
                .header(TVM_TICKET_HEADER, "111")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("orders")));
    }

    @Test
    public void whenTvmDisabledShouldNotRequireTvmHeader() throws Exception {
        mockMvc.perform(ordersRequest()
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("orders")));
    }

    @Test
    public void whenTvmEnabledShouldCheckTvmHeader() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.TVM_REQUIRED, true);

        mockMvc.perform(ordersRequest()
                .header(TVM_TICKET_HEADER, "111")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    public void whenTvmEnabledShouldCheckTvmHeader2() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.TVM_REQUIRED, true);
        mockMvc.perform(ordersRequest()
                .header(TVM_TICKET_HEADER, "111")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    public void whenTvmEnabledShouldRequireTvmHeader() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.TVM_REQUIRED, true);

        mockMvc.perform(ordersRequest()
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    public void whenMonitoringRequestShouldNotRequireTvm() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.TVM_REQUIRED, true);

        mockMvc.perform(get("/properties")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/pagematch")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void checkUserRoles() throws Exception {
        checkUserRoles("{\"previous\":null,\"results\":[{\"service\":{\"id\":123}}],\"next\":null}",
                READ_ROLE_ID, ordersRequest(), MockMvcResultMatchers.status().isOk());
        checkUserRoles("{\"previous\":null,\"results\":[],\"next\":null}",
                READ_ROLE_ID, ordersRequest(), MockMvcResultMatchers.status().isForbidden());
        checkUserRoles("{\"previous\":null,\"results\":[{\"service\":{\"id\":123}}],\"next\":null}",
                WRITE_ROLE_ID, postRequest(), MockMvcResultMatchers.status().is2xxSuccessful());
        checkUserRoles("{\"previous\":null,\"results\":[],\"next\":null}",
                WRITE_ROLE_ID, postRequest(), MockMvcResultMatchers.status().isForbidden());
        checkUserRoles("{\"previous\":null,\"results\":[{\"service\":{\"id\":123}}],\"next\":null}",
                READ_ROLE_ID, postRequest(), MockMvcResultMatchers.status().isForbidden());
    }

    private void checkUserRoles(String abcResponse,
                                int[] roleIds,
                                MockHttpServletRequestBuilder request,
                                ResultMatcher resultMatcher) throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.TVM_REQUIRED, true);

        Integer clientSrc = whiteListClients.stream().findFirst().orElse(null);
        Assertions.assertNotNull(clientSrc, "Не заполнен white-list в конфиге");
        CheckedServiceTicket checkedServiceTicket = new CheckedServiceTicket(TicketStatus.OK, "", clientSrc,
                YANDEX_UID);
        when(tvm.checkServiceTicket("111")).thenReturn(checkedServiceTicket);

        mockAbc(abcResponse, roleIds);

        mockMvc.perform(request
                .header(TVM_TICKET_HEADER, "111")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(resultMatcher);
    }

    @Nonnull
    private MockHttpServletRequestBuilder ordersRequest() {
        return get("/orders")
                .param(CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param("rgb", "BLUE");
    }

    @Nonnull
    private MockHttpServletRequestBuilder postRequest() {
        return post("/queuedcalls/stop-processing/" + CheckouterQCType.ORDER_CREATE_CASH_PAYMENT.name());
    }

    private void mockAbc(String result, int[] roleIds) {
        abcMock.stubFor(WireMock.get(urlPathEqualTo("/api/v4/services/members"))
                .withQueryParam("person__uid", WireMock.equalTo(Long.toString(YANDEX_UID)))
                .withQueryParam("role_in", WireMock.equalTo(StringUtils.join(roleIds, ',')))
                .withQueryParam("service", WireMock.equalTo(Integer.toString(ABC_SERVICE_ID)))
                .willReturn(WireMock.okJson(result)));
    }

}
