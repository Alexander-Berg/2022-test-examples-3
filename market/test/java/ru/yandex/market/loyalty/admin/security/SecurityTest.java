package ru.yandex.market.loyalty.admin.security;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.core.AnyOf;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.loyalty.admin.config.BlackboxYandexTeam;
import ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdmin;
import ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdminMockConfigurer.BlackboxClientConfig;
import ru.yandex.market.loyalty.admin.controller.dto.CouponPromoDto;
import ru.yandex.market.loyalty.admin.controller.dto.PromoDto;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.model.security.AdminRole;
import ru.yandex.market.loyalty.core.trigger.actions.TriggerActionTypes;
import ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.loyalty.core.model.security.AdminRole.COUPON_GENERATOR_ROLE;
import static ru.yandex.market.loyalty.core.model.security.AdminRole.COUPON_PROMO_EDITOR_ROLE;
import static ru.yandex.market.loyalty.core.model.security.AdminRole.PROMO_TRIGGER_EDITOR_ROLE;
import static ru.yandex.market.loyalty.core.model.security.AdminRole.QA_ENGINEER_ROLE;
import static ru.yandex.market.loyalty.core.model.security.AdminRole.SUPERUSER_ROLE;
import static ru.yandex.market.loyalty.core.model.security.AdminRole.UNSAFE_TASK_SUBMITTER_ROLE;
import static ru.yandex.market.loyalty.core.model.security.AdminRole.VIEWER_ROLE;

/**
 * @author dinyat
 * 11/09/2017
 */
public class SecurityTest extends MarketLoyaltyAdminMockedDbTest {

    private static final AdminRole ANONYMOUS = null;
    private static final String INDEX_PAGE = "/";

    private static final AnyOf<Integer> OK_STATUSES = anyOf(
            equalTo(HttpServletResponse.SC_OK),
            equalTo(HttpServletResponse.SC_BAD_REQUEST),
            equalTo(HttpStatus.UNPROCESSABLE_ENTITY.value()),
            equalTo(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    @MarketLoyaltyAdmin
    private ObjectMapper objectMapper;
    @Autowired
    @BlackboxYandexTeam
    private RestTemplate restTemplate;

    @Before
    public void initSecurity() {
        securityService.addRole(BlackboxClientConfig.VIEWER_USER, VIEWER_ROLE);
        securityService.addRole(BlackboxClientConfig.COUPON_PROMO_EDITOR_USER, COUPON_PROMO_EDITOR_ROLE);
        securityService.addRole(BlackboxClientConfig.UNSAFE_TASK_SUBMITTER_ROLE, UNSAFE_TASK_SUBMITTER_ROLE);
        securityService.addRole(BlackboxClientConfig.QA_ENGINEER_ROLE, QA_ENGINEER_ROLE);
        securityService.addRole(BlackboxClientConfig.PROMO_TRIGGER_EDITOR_USER, PROMO_TRIGGER_EDITOR_ROLE);
        securityService.addRole(BlackboxClientConfig.COUPON_GENERATOR_USER, COUPON_GENERATOR_ROLE);
    }

    @Test
    public void testPing() throws Exception {
        urlChecker("/ping", GET)
                .allowed(VIEWER_ROLE)
                .allowed(COUPON_PROMO_EDITOR_ROLE)
                .allowed(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .allowed(ANONYMOUS);
        verifyZeroInteractions(restTemplate);
    }

    @Test
    public void testFavicon() throws Exception {
        urlChecker("/favicon.ico", GET)
                .allowed(VIEWER_ROLE)
                .allowed(COUPON_PROMO_EDITOR_ROLE)
                .allowed(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .allowed(ANONYMOUS);
        verifyZeroInteractions(restTemplate);
    }

    @Test
    public void testIndexPage() throws Exception {
        urlChecker(INDEX_PAGE, GET)
                .allowed(VIEWER_ROLE)
                .allowed(COUPON_PROMO_EDITOR_ROLE)
                .allowed(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .forbidden(ANONYMOUS);
    }

    @Test
    public void testSpendingHistory() throws Exception {
        urlChecker("/api/export/spendingHistory/0", GET)
                .allowed(VIEWER_ROLE)
                .allowed(COUPON_PROMO_EDITOR_ROLE)
                .allowed(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .forbidden(ANONYMOUS);
    }

    @Test
    public void testMonitor() throws Exception {
        urlChecker("/monitor/juggler", GET)
                .allowed(VIEWER_ROLE)
                .allowed(COUPON_PROMO_EDITOR_ROLE)
                .allowed(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .allowed(ANONYMOUS);
        verifyZeroInteractions(restTemplate);
    }

    @Test
    public void testGetAllPromos() throws Exception {
        urlChecker("/api/promo", GET)
                .allowed(VIEWER_ROLE)
                .allowed(COUPON_PROMO_EDITOR_ROLE)
                .allowed(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .forbidden(ANONYMOUS);
    }

    @Test
    public void testGetAllPagedPromos() throws Exception {
        urlChecker("/api/promo/paged?pageSize=1&currentPage=1", GET)
                .allowed(VIEWER_ROLE)
                .allowed(COUPON_PROMO_EDITOR_ROLE)
                .allowed(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .forbidden(ANONYMOUS);
    }

    @Test
    public void testGetPromoCommon() throws Exception {
        urlChecker("/api/promo/common/0", GET)
                .allowed(VIEWER_ROLE)
                .allowed(COUPON_PROMO_EDITOR_ROLE)
                .allowed(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .forbidden(ANONYMOUS);
    }

    @Test
    public void testGetAllTriggers() throws Exception {
        urlChecker("/api/trigger", GET)
                .allowed(VIEWER_ROLE)
                .allowed(COUPON_PROMO_EDITOR_ROLE)
                .allowed(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .forbidden(ANONYMOUS);
    }

    @Test
    public void testGetAllPagedTriggers() throws Exception {
        urlChecker("/api/trigger/paged?pageSize=1&currentPage=1", GET)
                .allowed(VIEWER_ROLE)
                .allowed(COUPON_PROMO_EDITOR_ROLE)
                .allowed(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .forbidden(ANONYMOUS);
    }

    @Test
    public void testGetTrigger() throws Exception {
        urlChecker("/api/trigger/0", GET)
                .allowed(VIEWER_ROLE)
                .allowed(COUPON_PROMO_EDITOR_ROLE)
                .allowed(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .forbidden(ANONYMOUS);
    }

    @Test
    public void testGetRestrictionFactories() throws Exception {
        urlChecker("/api/trigger/restriction/factories/" + TriggerRestrictionType.REGION_RESTRICTION.getFactoryName()
                , GET)
                .allowed(VIEWER_ROLE)
                .allowed(PROMO_TRIGGER_EDITOR_ROLE)
                .allowed(COUPON_PROMO_EDITOR_ROLE)
                .allowed(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .forbidden(ANONYMOUS);
    }

    @Test
    public void testGetActionFactories() throws Exception {
        urlChecker("/api/trigger/action/factories/" + TriggerActionTypes.CREATE_COIN_ACTION.getFactoryName() +
                "?forCoin=true", GET)
                .allowed(VIEWER_ROLE)
                .allowed(PROMO_TRIGGER_EDITOR_ROLE)
                .allowed(COUPON_PROMO_EDITOR_ROLE)
                .allowed(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .forbidden(ANONYMOUS);
    }

    @Test
    public void testCreateTrigger() throws Exception {
        urlChecker("/api/trigger/create", POST, new PromoDto())
                .forbidden(VIEWER_ROLE)
                .allowed(PROMO_TRIGGER_EDITOR_ROLE)
                .forbidden(COUPON_PROMO_EDITOR_ROLE)
                .forbidden(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .forbidden(ANONYMOUS);
    }

    @Test
    public void testDeleteTrigger() throws Exception {
        urlChecker("/api/trigger/0", DELETE, new PromoDto())
                .forbidden(VIEWER_ROLE)
                .allowed(PROMO_TRIGGER_EDITOR_ROLE)
                .forbidden(COUPON_PROMO_EDITOR_ROLE)
                .forbidden(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .forbidden(ANONYMOUS);
    }

    @Test
    public void testUpdateTrigger() throws Exception {
        urlChecker("/api/trigger/update", PUT, new PromoDto())
                .forbidden(VIEWER_ROLE)
                .allowed(PROMO_TRIGGER_EDITOR_ROLE)
                .forbidden(COUPON_PROMO_EDITOR_ROLE)
                .forbidden(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .forbidden(ANONYMOUS);
    }

    @Test
    public void testGetPromo() throws Exception {
        urlChecker("/api/promo/0", GET)
                .allowed(VIEWER_ROLE)
                .allowed(COUPON_PROMO_EDITOR_ROLE)
                .allowed(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .forbidden(ANONYMOUS);
    }

    @Test
    public void testUpdatePromo() throws Exception {
        urlChecker("/api/promo/update", PUT, new PromoDto())
                .forbidden(VIEWER_ROLE)
                .allowed(COUPON_PROMO_EDITOR_ROLE)
                .forbidden(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .forbidden(ANONYMOUS);
    }

    @Test
    public void testChangeStatusPromo() throws Exception {
        urlChecker("/api/promo/0/changeStatus/" + PromoStatus.ACTIVE.getCode(), PUT)
                .forbidden(VIEWER_ROLE)
                .allowed(COUPON_PROMO_EDITOR_ROLE)
                .forbidden(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .forbidden(ANONYMOUS);
    }

    @Test
    public void testCsrfWorks() throws Exception {
        urlChecker("/api/promo/0/changeStatus/" + PromoStatus.ACTIVE.getCode(), PUT)
                .csrfInvalidToken();
    }

    @Test
    public void testAddBudgetToPromo() throws Exception {
        urlChecker("/api/promo/0/add/budget", PUT)
                .forbidden(VIEWER_ROLE)
                .allowed(COUPON_PROMO_EDITOR_ROLE)
                .forbidden(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .forbidden(ANONYMOUS);
    }

    @Test
    public void testCreatePromo() throws Exception {
        urlChecker("/api/promo/create", PUT, new CouponPromoDto())
                .forbidden(VIEWER_ROLE)
                .allowed(COUPON_PROMO_EDITOR_ROLE)
                .forbidden(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .forbidden(ANONYMOUS);
    }

    @Test
    public void tmsList() throws Exception {
        urlChecker("/tms/list", GET)
                .forbidden(VIEWER_ROLE)
                .forbidden(COUPON_PROMO_EDITOR_ROLE)
                .forbidden(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .forbidden(ANONYMOUS);
    }

    @Test
    public void favicon() throws Exception {
        urlChecker("/favicon.ico", GET)
                .allowed(VIEWER_ROLE)
                .allowed(COUPON_PROMO_EDITOR_ROLE)
                .allowed(COUPON_GENERATOR_ROLE)
                .allowed(SUPERUSER_ROLE)
                .allowed(ANONYMOUS);
        verifyZeroInteractions(restTemplate);
    }

    private UrlChecker urlChecker(String url, HttpMethod httpMethod) {
        return new UrlChecker(url, httpMethod, mockMvc, null);
    }

    private UrlChecker urlChecker(String url, HttpMethod httpMethod, Object content) throws JsonProcessingException {
        return new UrlChecker(url, httpMethod, mockMvc, objectMapper.writeValueAsString(content));
    }

    private static class UrlChecker {
        private final String url;
        private final HttpMethod httpMethod;
        private final MockMvc mockMvc;
        private final String content;

        private UrlChecker(String url, HttpMethod httpMethod, MockMvc mockMvc, String content) {
            this.url = url;
            this.httpMethod = httpMethod;
            this.mockMvc = mockMvc;
            this.content = content;
        }

        UrlChecker allowed(AdminRole adminRole) throws Exception {
            if (!INDEX_PAGE.equals(url)) {
                checkRequest(createRequest(), adminRole, status().is(OK_STATUSES));
            } else {
                String response = checkRequest(createRequest(), adminRole, status().isOk());
                assertThat(response, containsString("<app-admin></app-admin>"));
            }
            return this;
        }

        @SuppressWarnings("UnusedReturnValue")
        UrlChecker csrfInvalidToken() throws Exception {
            if (!INDEX_PAGE.equals(url)) {
                checkRequest(createRequest(), null, status().isForbidden(), csrf().useInvalidToken());
            } else {
                String response = checkRequest(createRequest(), null, status().isForbidden(), csrf().useInvalidToken());
                assertThat(response, containsString("<app-admin></app-admin>"));
            }
            return this;
        }

        UrlChecker forbidden(AdminRole adminRole) throws Exception {
            if (!INDEX_PAGE.equals(url)) {
                checkRequest(createRequest(), adminRole, status().isForbidden());
            } else {
                String response = checkRequest(createRequest(), adminRole, status().isOk());
                assertThat(response, allOf(
                        containsString("Доступ запрещен"),
                        containsString("Возможно, Вам следует сменить"),
                        containsString("учетную запись")
                ));
            }
            return this;
        }

        private MockHttpServletRequestBuilder createRequest() {
            MockHttpServletRequestBuilder request = request(httpMethod, url);
            if (content != null) {
                request = request.content(content).contentType(MediaType.APPLICATION_JSON);
            }
            return request;
        }

        private String checkRequest(MockHttpServletRequestBuilder requestBuilder, AdminRole adminRole,
                                    ResultMatcher resultMatcher) throws Exception {
            return checkRequest(requestBuilder, adminRole, resultMatcher, csrf());
        }

        private String checkRequest(MockHttpServletRequestBuilder requestBuilder, AdminRole adminRole,
                                    ResultMatcher resultMatcher, RequestPostProcessor withProcessor) throws Exception {
            return mockMvc
                    .perform(
                            requestBuilder
                                    .cookie(new Cookie("Session_id", adminRole != null ? adminRole.getCode() : "ANONYMOUS"))
                                    .header("X-Real-IP", "127.0.0.1")
                                    .with(withProcessor)
                    )
                    .andDo(log())
                    .andExpect(resultMatcher)
                    .andReturn().getResponse().getContentAsString();
        }
    }
}
