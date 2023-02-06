package ru.yandex.market.tpl.carrier.driver.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.carrier.core.domain.user.UserUtil.TAXI_ID;
import static ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams.BASE_PATH;
import static ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams.LEGACY_BASE_PATH;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AuthTestControllerTest extends BaseDriverApiIntTest {

    private final TestUserHelper testUserHelper;
    private User user;

    @BeforeEach
    void setUp() {
        user = testUserHelper.findOrCreateUser(TAXI_ID, UID);
    }

    @Test
    @SneakyThrows
    void testLegacyAuth() {
        mockMvc.perform(get(LEGACY_BASE_PATH + "/test")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        )
                .andExpect(status().isOk());

        mockMvc.perform(get(LEGACY_BASE_PATH + "/test")
                .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                .header(ApiParams.TAXI_PASSPORT_UID_HEADER, TAXI_UID_HEADER_VALUE)
        )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(LEGACY_BASE_PATH + "/test")
        )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @SneakyThrows
    void testNewAuth() {
        mockMvc.perform(get(BASE_PATH + "/test")
                .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                .header(ApiParams.TAXI_PASSPORT_UID_HEADER, TAXI_UID_HEADER_VALUE)
        )
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE_PATH + "/test")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(BASE_PATH + "/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @SneakyThrows
    void testUnknownPath() {
        mockMvc.perform(get("/test")
                .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                .header(ApiParams.TAXI_PASSPORT_UID_HEADER, TAXI_UID_HEADER_VALUE)
        )
                .andExpect(status().isOk());

        mockMvc.perform(get("/test")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/test"))
                .andExpect(status().isUnauthorized());
    }
}
