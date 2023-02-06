package ru.yandex.market.passport.passport.spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.passport.blackbox2.Blackbox2;
import ru.yandex.inside.passport.blackbox2.BlackboxQueryable;
import ru.yandex.inside.passport.blackbox2.protocol.BlackboxMethod;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxCorrectResponse;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxResponseBuilder;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxSessionIdException;
import ru.yandex.market.passport.passport.PassportConstants;
import ru.yandex.market.passport.user.AuthUser;
import ru.yandex.market.passport.user.AuthUserDao;
import ru.yandex.misc.ip.IpAddress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author anmalysh
 * @since 10/26/2018
 */
public class PassportAuthServiceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private Blackbox2 blackbox2;

    @Mock
    private BlackboxQueryable blackboxQueryable;

    @Mock
    private AuthUserDao userDao;

    @Mock
    private AuthenticationFailureHandler failureHandler;

    @Mock
    private AuthenticationSuccessHandler successHandler;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpServletRequest request;

    @Test
    public void testSuccessfulHttpAuth() {
        mockUserDao("user1", "role1", "role2");
        mockBlackboxResponse("user1", false);
        PassportAuthService authService = createAuthService(false, false);
        PassportAuthentication authentication = runAuth(authService, "1234", null, null, "1.1.1.1");
        assertAuthSuccessful(authentication, "user1", "role1", "role2");
    }

    @Test
    public void testSuccessfulHttpsAuth() {
        mockUserDao("user1", "role1", "role2");
        mockBlackboxResponse("user1", false);
        PassportAuthService authService = createAuthService(false, true);
        PassportAuthentication authentication = runAuth(authService, "1234", "2345", null, "1.1.1.1");
        assertAuthSuccessful(authentication, "user1", "role1", "role2");
    }

    @Test
    public void testNoSessionidCookie() {
        mockUserDao("user1", "role1", "role2");
        mockBlackboxResponse("user1", false);
        PassportAuthService authService = createAuthService(false, false);
        PassportAuthentication authentication = runAuth(authService, null, null, null, "1.1.1.1");
        assertAuthFailed(authentication);
    }

    @Test
    public void testNoSessionid2Cookie() {
        mockUserDao("user1", "role1", "role2");
        mockBlackboxResponse("user1", false);
        PassportAuthService authService = createAuthService(false, true);
        PassportAuthentication authentication = runAuth(authService, "1234", null, null, "1.1.1.1");
        assertAuthFailed(authentication);
    }

    @Test
    public void testBlackBoxFailureResponse() {
        mockUserDao("user1", "role1", "role2");
        mockBlackboxResponse(null, false);
        PassportAuthService authService = createAuthService(false, true);
        PassportAuthentication authentication = runAuth(authService, "1234", "2345", null, "1.1.1.1");
        assertAuthFailed(authentication);
    }

    @Test
    public void testBlackBoxException() {
        mockUserDao("user1", "role1", "role2");
        mockBlackboxResponse(null, true);
        PassportAuthService authService = createAuthService(false, true);
        PassportAuthentication authentication = runAuth(authService, "1234", "2345", null, "1.1.1.1");
        assertAuthFailed(authentication);
    }

    @Test
    public void testNoUserInTheSystem() {
        when(userDao.getOrCreateUser(anyString()))
            .thenReturn(null);
        mockBlackboxResponse("user1", false);
        PassportAuthService authService = createAuthService(false, true);
        PassportAuthentication authentication = runAuth(authService, "1234", "2345", null, "1.1.1.1");
        assertAuthFailed(authentication);
    }

    @Test
    public void testCache() {
        mockUserDao("user1", "role1");
        mockBlackboxResponse("user1", false);
        PassportAuthService authService = createAuthService(true, true);
        PassportAuthentication authentication = runAuth(authService, "1234", "2345", null, "1.1.1.1");
        assertAuthSuccessful(authentication, "user1", "role1");

        // Note that role is not cached
        mockUserDao("user1", "role1", "role2");
        authentication = runAuth(authService, "1234", "2345", "1.1.1.1", null);
        assertAuthSuccessful(authentication, "user1", "role1", "role2");

        verify(blackboxQueryable, times(1)).sessionId(
            any(IpAddress.class), anyString(), anyString(), anyList(), anyBoolean(), any(Option.class));
    }

    @Test
    public void testNoCache() {
        mockUserDao("user1", "role1");
        mockBlackboxResponse("user1", false);
        PassportAuthService authService = createAuthService(false, true);
        PassportAuthentication authentication = runAuth(authService, "1234", "2345", null, "1.1.1.1");
        assertAuthSuccessful(authentication, "user1", "role1");

        authentication = runAuth(authService, "1234", "2345", null, "1.1.1.1");
        assertAuthSuccessful(authentication, "user1", "role1");

        verify(blackboxQueryable, times(2)).sessionId(
            any(IpAddress.class), anyString(), anyString(), anyList(), anyBoolean(), any(Option.class));
    }

    @Test
    public void testDebugPropertyAuth() {
        mockUserDao("user1", "role1");
        PassportAuthService authService = createDebugAuthService("user1", false, false);
        PassportAuthentication authentication = runDebugAuth(authService, null);
        assertAuthSuccessful(authentication, "user1", "role1");
        verify(blackboxQueryable, times(0)).sessionId(
            any(IpAddress.class), anyString(), anyString(), anyList(), anyBoolean(), any(Option.class));
    }

    @Test
    public void testDebugParamAuth() {
        mockUserDao("user1", "role1");
        PassportAuthService authService = createDebugAuthService("user2", false, false);
        PassportAuthentication authentication = runDebugAuth(authService, "user1");
        assertAuthSuccessful(authentication, "user1", "role1");
        verify(blackboxQueryable, times(0)).sessionId(
            any(IpAddress.class), anyString(), anyString(), anyList(), anyBoolean(), any(Option.class));
    }

    @Test
    public void testDebugRegularAuth() {
        mockUserDao("user1", "role1");
        mockBlackboxResponse("user1", false);
        PassportAuthService authService = createDebugAuthService(null, false, false);
        PassportAuthentication authentication = runAuth(authService, "1234", "2345", null, "1.1.1.1");
        assertAuthSuccessful(authentication, "user1", "role1");
        verify(blackboxQueryable, times(1)).sessionId(
            any(IpAddress.class), anyString(), anyString(), anyList(), anyBoolean(), any(Option.class));
    }

    @Test
    public void testDebugRegularAuthFails() {
        mockUserDao("user1", "role1");
        mockBlackboxResponse(null, true);
        PassportAuthService authService = createDebugAuthService(null, false, false);
        PassportAuthentication authentication = runAuth(authService, "1234", "2345", null, "1.1.1.1");
        assertAuthFailed(authentication);
        verify(blackboxQueryable, times(1)).sessionId(
            any(IpAddress.class), anyString(), anyString(), anyList(), anyBoolean(), any(Option.class));
    }


    private void assertAuthSuccessful(PassportAuthentication authentication, String userName, String... roles) {
        assertTrue(authentication.isAuthenticated());
        assertEquals(userName, authentication.getName());
        assertEquals(userName, authentication.getPrincipal());
        AuthUser user = authentication.getDetails();
        assertEquals(userName, user.getLogin());
        Assertions.assertThat(user.getRoles()).containsExactlyInAnyOrder(roles);
        try {
            verify(successHandler, times(1))
                .onAuthenticationSuccess(request, response, authentication);
        } catch (Exception e) {
            fail("Unexpected exception");
        }
    }

    private void assertAuthFailed(PassportAuthentication authentication) {
        assertFalse(authentication.isAuthenticated());
        assertNull(authentication.getName());
        assertNull(authentication.getPrincipal());
        assertNull(authentication.getDetails());
        try {
            verify(failureHandler, times(1))
                .onAuthenticationFailure(request, response, null);
        } catch (Exception e) {
            fail("Unexpected exception");
        }
    }

    private PassportAuthentication runDebugAuth(PassportAuthService service, String userParam) {
        Optional.ofNullable(userParam)
            .ifPresent(ip -> when(request.getHeader("debug_user")).thenReturn(userParam));
        return doRunAuth(service);
    }

    private PassportAuthentication runAuth(PassportAuthService service, String sessionId, String sessionId2,
                                           String ipHeader, String remoteIp) {
        List<Cookie> cookieList = new ArrayList<>();
        Optional.ofNullable(sessionId)
            .map(sid -> new Cookie(PassportConstants.SESSION_ID, sid))
            .ifPresent(cookieList::add);

        Optional.ofNullable(sessionId2)
            .map(sid2 -> new Cookie(PassportConstants.SESSION_ID2, sid2))
            .ifPresent(cookieList::add);

        when(request.getCookies())
            .thenReturn(cookieList.toArray(new Cookie[0]));

        Optional.ofNullable(ipHeader)
            .ifPresent(ip -> when(request.getHeader("X-Real-IP")).thenReturn(ipHeader));
        Optional.ofNullable(remoteIp)
            .ifPresent(ip -> when(request.getRemoteAddr()).thenReturn(remoteIp));

        return doRunAuth(service);
    }

    private PassportAuthentication doRunAuth(PassportAuthService service) {
        Authentication auth = service.autoLogin(request, response);
        if (service.supports(auth.getClass())) {
            try {
                auth = service.authenticate(auth);

                if (auth.isAuthenticated()) {
                    service.loginSuccess(request, response, auth);
                } else {
                    service.loginFail(request, response);
                }
            } catch (Exception e) {
                service.loginFail(request, response);
            }
        }
        return (PassportAuthentication) auth;
    }

    private void mockUserDao(String user, String... roles) {
        when(userDao.getOrCreateUser(anyString()))
            .thenReturn(createAuthUser(user, roles));
    }

    private AuthUser createAuthUser(String user, String... roles) {
        return new AuthUser() {
            @Override
            public boolean hasRole(String role) {
                return getRoles().contains(role);
            }

            @Override
            public String getLogin() {
                return user;
            }

            @Override
            public Set<String> getRoles() {
                return new HashSet<>(Arrays.asList(roles));
            }
        };
    }

    private void mockBlackboxResponse(String login, boolean exception) {
        when(blackbox2.query()).thenReturn(blackboxQueryable);

        if (login != null) {
            BlackboxCorrectResponse bbResponse =
                (BlackboxCorrectResponse) new BlackboxResponseBuilder(BlackboxMethod.SESSION_ID)
                    .setStatus(BlackboxSessionIdException.BlackboxSessionIdStatus.VALID.getId())
                    .setLogin(Option.of(login))
                    .build();
            when(blackboxQueryable.sessionId(
                any(IpAddress.class), anyString(), anyString(), anyList(), anyBoolean(), any(Option.class)))
                .thenReturn(bbResponse);
        } else {
            if (exception) {
                when(blackboxQueryable.sessionId(
                    any(IpAddress.class), anyString(), anyString(), anyList(), anyBoolean(), any(Option.class)))
                    .thenThrow(new BlackboxSessionIdException(
                        BlackboxSessionIdException.BlackboxSessionIdStatus.EXPIRED, "Wrong session ID", "url"));
            } else {
                BlackboxCorrectResponse bbResponse =
                    (BlackboxCorrectResponse) new BlackboxResponseBuilder(BlackboxMethod.SESSION_ID)
                        .setStatus(BlackboxSessionIdException.BlackboxSessionIdStatus.NOAUTH.getId())
                        .setLogin(Option.empty())
                        .build();
                when(blackboxQueryable.sessionId(
                    any(IpAddress.class), anyString(), anyString(), anyList(), anyBoolean(), any(Option.class)))
                    .thenThrow(new BlackboxSessionIdException(
                        BlackboxSessionIdException.BlackboxSessionIdStatus.EXPIRED, "Wrong session ID", "url"));
            }
        }
    }

    public PassportAuthService createAuthService(boolean useCache, boolean requireHttps) {
        return new PassportAuthServiceBuilder()
            .setBlackBoxClient(blackbox2)
            .setServiceHost("d")
            .setUserDao(userDao)
            .setUseCache(useCache)
            .setCacheMaxSize(1)
            .setCacheTimeoutMins(2)
            .setFailureHandler(failureHandler)
            .setSuccessHandler(successHandler)
            .setRequireHttpsAuth(requireHttps)
            .build();
    }

    public PassportAuthService createDebugAuthService(String debugUser, boolean hasRoles, boolean useCache) {
        return new PassportAuthServiceBuilder()
            .setBlackBoxClient(blackbox2)
            .setServiceHost("d")
            .setUserDao(userDao)
            .setUseCache(useCache)
            .setCacheMaxSize(1)
            .setCacheTimeoutMins(2)
            .setFailureHandler(failureHandler)
            .setSuccessHandler(successHandler)
            .setRequireHttpsAuth(false)
            .setDebug(true)
            .setDebugUser(debugUser)
            .build();
    }

}
