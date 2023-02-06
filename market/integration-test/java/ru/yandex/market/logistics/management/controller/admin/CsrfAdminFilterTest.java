package ru.yandex.market.logistics.management.controller.admin;

import java.time.Instant;
import java.time.ZoneId;

import javax.annotation.Nonnull;
import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.TestableClock;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PARTNER_RELATION;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PARTNER_RELATION_EDIT;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;

@DatabaseSetup("/data/controller/admin/partnerRelation/prepare_data.xml")
@TestPropertySource(properties = "csrf.token.check.disabled=false")
class CsrfAdminFilterTest extends AbstractContextualTest {
    private static final String CSRF_TOKEN_VALUE = "f264a8d020f460ea8b65993fc22e0adfcc6483c3:1578889361000";
    private static final Cookie YANDEXUID_COOKIE = new Cookie("yandexuid", "yandexuid-cookie-value");

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.ofEpochMilli(1578889363000L), ZoneId.systemDefault());
    }

    @AfterEach
    void teardown() {
        clock.clearFixed();
    }

    @Test
    @DisplayName("GET запрос отрабатывает без CSRF-токена")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION})
    void getWithoutCsrfToken() throws Exception {
        mockMvc.perform(get("/admin/lms/partner-relation")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST запрос без CSRF-токена возвращает ошибку авторизации")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void postWithoutCsrfToken() throws Exception {
        mockMvc.perform(postRequestBuilder())
            .andExpect(status().is(412))
            .andExpect(content().json(
                "{\"status\":\"CSRF_TOKEN_NOT_FOUND\",\"message\":\"X-CSRF-Token header not found\"}", true
            ));
    }

    @Test
    @DisplayName("POST запрос с CSRF-токеном неправильного формата")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void postWithInvalidFormatCsrfToken() throws Exception {
        mockMvc.perform(postRequestBuilder().header("X-CSRF-Token", "csrf-token-value"))
            .andExpect(status().is(412))
            .andExpect(content().json(
                "{\"status\":\"CSRF_TOKEN_INVALID_FORMAT\",\"message\":\"CSRF token has invalid format\"}", true
            ));
    }

    @Test
    @DisplayName("POST запрос с просроченным CSRF-токеном")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void postWithExpiredCsrfToken() throws Exception {
        clock.setFixed(Instant.ofEpochMilli(1578975763000L), ZoneId.systemDefault());
        mockMvc.perform(postRequestBuilder().header("X-CSRF-Token", CSRF_TOKEN_VALUE))
            .andExpect(status().is(412))
            .andExpect(content().json(
                "{\"status\":\"EXPIRED_CSRF_TOKEN\"," +
                    "\"message\":\"CSRF token expired, new CSRF token should be requested\"}",
                true
            ));
    }

    @Test
    @DisplayName("POST запрос с отсутствующей кукой yandexuid")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void postWithoutYandexuidCookie() throws Exception {
        mockMvc.perform(postRequestBuilder().header("X-CSRF-Token", CSRF_TOKEN_VALUE))
            .andExpect(status().is(412))
            .andExpect(content().json(
                "{\"status\":\"YANDEXUID_COOKIE_NOT_FOUND\"," +
                    "\"message\":\"Cookie 'yandexuid' not found\"}",
                true
            ));
    }

    @Test
    @DisplayName("POST запрос с неправильным CSRF-токеном")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void postWithInvalidCsrfToken() throws Exception {
        mockMvc.perform(
                postRequestBuilder()
                    .header("X-CSRF-Token", "this-is-incorrect-csrf-token:1578889361000")
                    .cookie(YANDEXUID_COOKIE)
            )
            .andExpect(status().is(412))
            .andExpect(content().json(
                "{\"status\":\"INVALID_CSRF_TOKEN\",\"message\":\"CSRF token is invalid\"}", true
            ));
    }

    @Test
    @DisplayName("POST запрос с правильным CSRF-токеном")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void postWithCorrectCsrfToken() throws Exception {
        mockMvc.perform(
                postRequestBuilder().header("X-CSRF-Token", CSRF_TOKEN_VALUE).cookie(YANDEXUID_COOKIE)
            )
            .andExpect(status().is2xxSuccessful());
        checkBuildWarehouseSegmentTask(1L);
    }

    @Nonnull
    private MockHttpServletRequestBuilder postRequestBuilder() {
        return post("/admin/lms/partner-relation")
            .contentType(MediaType.APPLICATION_JSON)
            .content(pathToJson("data/controller/admin/partnerRelation/newPartnerRelation.json"));
    }
}
