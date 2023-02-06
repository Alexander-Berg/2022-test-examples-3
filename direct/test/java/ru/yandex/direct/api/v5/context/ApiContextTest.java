package ru.yandex.direct.api.v5.context;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.api.v5.security.DirectApiAuthentication;
import ru.yandex.direct.api.v5.security.DirectApiPreAuthentication;
import ru.yandex.direct.api.v5.security.exception.AuthenticationException;
import ru.yandex.direct.api.v5.security.exception.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiContextTest {

    private ApiContext apiContext;

    @Before
    public void setUp() {
        apiContext = new ApiContext();
    }

    @Test(expected = AuthenticationException.class)
    public void getAuthenticationOrFailThrowsStoredException() {
        apiContext.setAuthorizationException(new BadCredentialsException(""));
        apiContext.getDirectApiAuthenticationOrFail();
    }

    @Test(expected = AuthenticationException.class)
    public void getAuthenticationOrFailThrowsStoredExceptionEvenIfPreAuthenticationIsPresent() {
        apiContext.setPreAuthentication(mock(DirectApiPreAuthentication.class));
        apiContext.setAuthorizationException(new BadCredentialsException(""));
        apiContext.getDirectApiAuthenticationOrFail();
    }

    @Test
    public void getAuthenticationOrFailReturnsAuthFromStoredPreAuth() {
        DirectApiPreAuthentication preAuth = mock(DirectApiPreAuthentication.class);
        when(preAuth.isFullyAuthorized()).thenReturn(true);
        DirectApiAuthentication directApiAuthentication = mock(DirectApiAuthentication.class);
        when(preAuth.toDirectApiAuthentication()).thenReturn(directApiAuthentication);
        apiContext.setPreAuthentication(preAuth);
        DirectApiAuthentication auth = apiContext.getDirectApiAuthenticationOrFail();
        assertThat(auth)
                .usingRecursiveComparison()
                .isEqualTo(directApiAuthentication);
    }
}
