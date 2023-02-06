package ru.yandex.market.passport.passport.spring;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import ru.yandex.inside.passport.blackbox2.Blackbox2;
import ru.yandex.market.passport.user.AuthUserDao;

import static org.junit.Assert.assertEquals;

/**
 * @author anmalysh
 * @since 10/26/2018
 */
public class PassportAuthServiceBuilderTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    Blackbox2 blackbox2;

    @Mock
    private AuthUserDao userDao;

    @Mock
    private AuthenticationFailureHandler failureHandler;

    @Mock
    private AuthenticationSuccessHandler successHandler;

    @Test(expected = IllegalStateException.class)
    public void testNoBlacbox2Fails() {
        new PassportAuthServiceBuilder()
            .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testNoServiceHostFails() {
        new PassportAuthServiceBuilder()
            .setBlackBoxClient(blackbox2)
            .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testNoUserDaoFails() {
        new PassportAuthServiceBuilder()
            .setBlackBoxClient(blackbox2)
            .setServiceHost("d")
            .build();
    }

    @Test
    public void testMinimalPasses() {
        PassportAuthService passportAuthService = new PassportAuthServiceBuilder()
            .setBlackBoxClient(blackbox2)
            .setServiceHost("d")
            .setUserDao(userDao)
            .build();

        assertEquals(PassportAuthService.class, passportAuthService.getClass());
    }

    @Test
    public void testMinimalDebugPasses() {
        PassportAuthService passportAuthService = new PassportAuthServiceBuilder()
            .setBlackBoxClient(blackbox2)
            .setServiceHost("d")
            .setUserDao(userDao)
            .setFailureHandler(failureHandler)
            .setDebug(true)
            .build();

        assertEquals(DebugPassportAuthService.class, passportAuthService.getClass());
    }

    @Test
    public void testAllSetPasses() {
        PassportAuthService passportAuthService = new PassportAuthServiceBuilder()
            .setBlackBoxClient(blackbox2)
            .setServiceHost("d")
            .setUserDao(userDao)
            .setUseCache(true)
            .setCacheMaxSize(1)
            .setCacheTimeoutMins(2)
            .setSuccessHandler(successHandler)
            .setRequireHttpsAuth(false)
            .build();

        assertEquals(PassportAuthService.class, passportAuthService.getClass());
    }

    @Test
    public void testAllSetDebugPasses() {
        PassportAuthService passportAuthService = new PassportAuthServiceBuilder()
            .setBlackBoxClient(blackbox2)
            .setServiceHost("d")
            .setUserDao(userDao)
            .setUseCache(true)
            .setCacheMaxSize(1)
            .setCacheTimeoutMins(2)
            .setSuccessHandler(successHandler)
            .setRequireHttpsAuth(false)
            .setDebug(true)
            .setDebugUser("s")
            .build();

        assertEquals(DebugPassportAuthService.class, passportAuthService.getClass());
    }

}
