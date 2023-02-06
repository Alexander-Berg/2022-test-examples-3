package ru.yandex.direct.web.auth;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;

import ru.yandex.direct.web.auth.blackbox.BlackboxCookieAuth;
import ru.yandex.direct.web.auth.blackbox.BlackboxCookieAuthRequest;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BestEffortAuthenticationManagerTest {

    private BestEffortAuthenticationManager testingAuthManager;

    private Authentication authRequest;
    private Authentication authResult;
    private AuthenticationProvider authProvider;

    @Before
    public void prepare() {
        authRequest = mock(BlackboxCookieAuthRequest.class);
        authResult = mock(BlackboxCookieAuth.class);

        authProvider = mock(AuthenticationProvider.class);
        when(authProvider.supports(authRequest.getClass())).thenReturn(true);
        when(authProvider.authenticate(authRequest)).thenReturn(authResult);

        testingAuthManager = new BestEffortAuthenticationManager(Collections.singletonList(authProvider));
    }

    @Test
    public void returnsValidAuthentication() {
        Authentication actualAuthResult = testingAuthManager.authenticate(authRequest);
        assertThat("manager has not return expected authentication", actualAuthResult, sameInstance(authResult));
    }

    @Test
    public void call_success() {
        testingAuthManager.authenticate(authRequest);
        verify(authProvider).authenticate(authRequest);
    }

    @Test
    public void supportsAuth_success() {
        testingAuthManager.authenticate(authRequest);
        verify(authProvider).supports(authRequest.getClass());
    }

    @Test
    public void exceptionFromProvider_success() {
        Exception e = new IllegalArgumentException();
        when(authProvider.authenticate(authRequest)).thenThrow(e);
        testingAuthManager.authenticate(authRequest);
    }

    @Test
    public void nullProvider_success() {
        when(authProvider.authenticate(authRequest)).thenReturn(null);
        testingAuthManager.authenticate(authRequest);
    }

    @Test(expected = AuthenticationServiceException.class)
    public void notSupportedProvider_fail() {
        when(authProvider.supports(authRequest.getClass())).thenReturn(false);
        testingAuthManager.authenticate(authRequest);
    }

}
