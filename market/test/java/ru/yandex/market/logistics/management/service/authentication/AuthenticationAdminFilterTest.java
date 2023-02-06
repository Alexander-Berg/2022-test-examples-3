package ru.yandex.market.logistics.management.service.authentication;

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

import ru.yandex.market.logistics.management.blackbox.BlackBoxClient;
import ru.yandex.market.logistics.management.blackbox.BlackBoxProfile;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Если вызов BlackBoxClient.getProfileInfo() возвращает UserTicket, то тикет попадает в RequestContext")
public class AuthenticationAdminFilterTest {

    private static final String TEST_USER_TICKET = "test-user-ticket";
    private static final String TEST_SESSION_ID_COOKIE_NAME = "Session_id";
    private static final String TEST_SESSION_ID_MAPPED_COOKIE_NAME = "sessionid";
    private static final String TEST_SESSION_ID = "test-session-id";
    private static final String IP_HEADER_NAME = "X-Real-IP";
    private static final String IP = "192.168.0.1";

    @Mock
    private BlackBoxClient blackBoxClient;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Test
    @DisplayName("Проверка вызова BlackBoxClient.getProfileInfo() и что юзер тикет после этого попадает в контекст")
    void blackBoxClientGetProfileInfoTest() throws ServletException, IOException {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(TEST_SESSION_ID_COOKIE_NAME, TEST_SESSION_ID)});
        when(request.getHeader(IP_HEADER_NAME)).thenReturn(IP);
        Map<String, String> map = Map.of(TEST_SESSION_ID_MAPPED_COOKIE_NAME, TEST_SESSION_ID);
        when(blackBoxClient.getProfileInfo(eq(IP), eq(map))).thenReturn(new BlackBoxProfile(
            "login",
            111L,
            TEST_USER_TICKET
        ));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        AuthenticationAdminFilter authenticationAdminFilter = new AuthenticationAdminFilter(blackBoxClient);
        authenticationAdminFilter.setAuthenticationManager(authenticationManager);
        authenticationAdminFilter.doFilterInternal(request, response, chain);
        assertThat(RequestContextHolder.getContext().getUserTicket()).isEqualTo(TEST_USER_TICKET);
    }
}
