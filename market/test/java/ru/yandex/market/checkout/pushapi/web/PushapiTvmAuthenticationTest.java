package ru.yandex.market.checkout.pushapi.web;

import java.util.Set;

import javax.annotation.Nonnull;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;
import ru.yandex.market.checkout.pushapi.config.TvmAuthenticationFilter;
import ru.yandex.market.checkout.pushapi.providers.SettingsProvider;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.checkout.util.PushApiTestSerializationService;
import ru.yandex.passport.tvmauth.CheckedServiceTicket;
import ru.yandex.passport.tvmauth.TicketStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.pushapi.helpers.PushApiCartParameters.DEFAULT_SHOP_ID;

/**
 * @author ifilippov5
 */
public class PushapiTvmAuthenticationTest extends AbstractWebTestBase {

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
    private TvmAuthenticationFilter spyFilter;
    @Autowired
    private Tvm2 tvm;
    @Autowired
    private SettingsProvider settingsProvider;
    @Autowired
    private PushApiTestSerializationService testSerializationService;
    @Autowired
    private WireMockServer abcMock;

    @Value("${market.checkouter.tvm.whiteList:0}")
    private Set<Integer> whiteListClients;

    @BeforeEach
    public void init() {
        spyFilter = spy(tvmAuthenticationFilter);

        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(spyFilter)
                .build();
        when(tvm.checkServiceTicket(anyString()))
                .thenReturn(null);
    }

    @AfterEach
    public void destroy() {
        abcMock.resetAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/ping", "/environment"})
    public void whenTvmDisabledShouldIgnoreTvmHeader(String url) throws Exception {
        mockMvc.perform(get(url)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void whenTvmEnabledShouldCheckTvmHeader() throws Exception {
        mockMvc.perform(postShopSettingsRequest()
                        .header(TVM_TICKET_HEADER, "111"))
                .andExpect(status().is(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    public void checkUserRoles() throws Exception {
        checkUserRoles("{\"previous\":null,\"results\":[{\"service\":{\"id\":123}}],\"next\":null}",
                WRITE_ROLE_ID, postShopSettingsRequest(), MockMvcResultMatchers.status().is2xxSuccessful());
        checkUserRoles("{\"previous\":null,\"results\":[{\"service\":{\"id\":123}}],\"next\":null}",
                READ_ROLE_ID, getShopSettingsRequest(), MockMvcResultMatchers.status().isOk());
        checkUserRoles("{\"previous\":null,\"results\":[],\"next\":null}",
                READ_ROLE_ID, getShopSettingsRequest(), MockMvcResultMatchers.status().isForbidden());
        checkUserRoles("{\"previous\":null,\"results\":[],\"next\":null}",
                WRITE_ROLE_ID, postShopSettingsRequest(), MockMvcResultMatchers.status().isForbidden());
        checkUserRoles("{\"previous\":null,\"results\":[{\"service\":{\"id\":123}}],\"next\":null}",
                READ_ROLE_ID, postShopSettingsRequest(), MockMvcResultMatchers.status().isForbidden());
    }


    private void checkUserRoles(String abcResponse,
                                int[] roleId,
                                MockHttpServletRequestBuilder request,
                                ResultMatcher resultMatcher) throws Exception {

        Integer clientSrc = whiteListClients.stream().findFirst().orElse(null);
        Assertions.assertNotNull(clientSrc, "Не заполнен white-list в конфиге");
        CheckedServiceTicket checkedServiceTicket = new CheckedServiceTicket(TicketStatus.OK, "", clientSrc,
                YANDEX_UID);
        when(tvm.checkServiceTicket("111")).thenReturn(checkedServiceTicket);

        mockAbc(abcResponse, roleId);

        mockMvc.perform(request
                        .header(TVM_TICKET_HEADER, "111")
                        .contentType(MediaType.APPLICATION_XML))
                .andExpect(resultMatcher);
    }

    @Nonnull
    private MockHttpServletRequestBuilder getShopSettingsRequest() {
        return get("/shops/{shopId}/settings", DEFAULT_SHOP_ID)
                .contentType(MediaType.APPLICATION_XML);
    }

    @Nonnull
    private MockHttpServletRequestBuilder postShopSettingsRequest() {
        Settings settings = settingsProvider.buildXmlSettings();
        mockPostSettings(DEFAULT_SHOP_ID, settings);
        return post("/shops/{shopId}/settings", DEFAULT_SHOP_ID)
                .content(testSerializationService.serialize(settings))
                .contentType(MediaType.APPLICATION_XML);
    }

    private void mockAbc(String result, int[] roleIds) {
        abcMock.stubFor(WireMock.get(urlPathEqualTo("/api/v4/services/members"))
                .withQueryParam("person__uid", WireMock.equalTo(Long.toString(YANDEX_UID)))
                .withQueryParam("role_in", WireMock.equalTo(StringUtils.join(roleIds, ',')))
                .withQueryParam("service", WireMock.equalTo(Integer.toString(ABC_SERVICE_ID)))
                .willReturn(WireMock.okJson(result)));
    }

}
