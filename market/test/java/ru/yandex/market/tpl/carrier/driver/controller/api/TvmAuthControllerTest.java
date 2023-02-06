package ru.yandex.market.tpl.carrier.driver.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams;
import ru.yandex.market.tpl.common.web.tvm.filter.TvmAuthenticationFilter;
import ru.yandex.passport.tvmauth.CheckedServiceTicket;
import ru.yandex.passport.tvmauth.TicketStatus;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.carrier.core.domain.user.UserUtil.TAXI_ID;
import static ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams.BASE_PATH;

@ActiveProfiles(value = "tests_tvm")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class TvmAuthControllerTest extends BaseDriverApiIntTest {

    private final static String SERVICE_TICKET = "123";

    private final TestUserHelper testUserHelper;

    private User user;

    @Autowired
    private TvmClient tvmClient;

    @BeforeAll
    static void setUpAll() {
        // To fix YamlPropertiesEnvironmentOverridingProcessor
        System.clearProperty("environment");
    }

    @BeforeEach
    void setUp() {
        user = testUserHelper.findOrCreateUser(TAXI_ID, UID);
    }

    @Test
    @SneakyThrows
    void testTvmFailAuthOk() {

        Mockito.when(tvmClient.checkServiceTicket(Mockito.anyString()))
                .thenReturn(new CheckedServiceTicket(TicketStatus.INVALID_DST, "", 123, 123L));

        mockMvc.perform(get(BASE_PATH + "/test")
                        .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PASSPORT_UID_HEADER, TAXI_UID_HEADER_VALUE)
                        .header(TvmAuthenticationFilter.SERVICE_TICKET_HEADER, SERVICE_TICKET)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @SneakyThrows
    void testTvmFailAuthFail() {

        Mockito.when(tvmClient.checkServiceTicket(Mockito.anyString()))
                .thenReturn(new CheckedServiceTicket(TicketStatus.INVALID_DST, "", 123, 123L));

        mockMvc.perform(get(BASE_PATH + "/test")
                        .header(TvmAuthenticationFilter.SERVICE_TICKET_HEADER, SERVICE_TICKET)
                )
                .andExpect(status().isUnauthorized());

    }

    @Test
    @SneakyThrows
    void testTvmOkAuthFail() {

        Mockito.when(tvmClient.checkServiceTicket(Mockito.anyString()))
                .thenReturn(new CheckedServiceTicket(TicketStatus.OK, "", 123, 123L));

        mockMvc.perform(get(BASE_PATH + "/test")
                        .header(TvmAuthenticationFilter.SERVICE_TICKET_HEADER, SERVICE_TICKET)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @SneakyThrows
    void testTvmOkAuthOk() {

        Mockito.when(tvmClient.checkServiceTicket(Mockito.anyString()))
                .thenReturn(new CheckedServiceTicket(TicketStatus.OK, "", 123, 123L));

        mockMvc.perform(get(BASE_PATH + "/test")
                        .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PASSPORT_UID_HEADER, TAXI_UID_HEADER_VALUE)
                        .header(TvmAuthenticationFilter.SERVICE_TICKET_HEADER, SERVICE_TICKET)
                )
                .andExpect(status().isOk());
    }

}
