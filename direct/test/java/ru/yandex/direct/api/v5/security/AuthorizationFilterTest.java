package ru.yandex.direct.api.v5.security;

import javax.servlet.FilterChain;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ws.transport.context.TransportContextHolder;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.security.exception.BadCredentialsException;
import ru.yandex.direct.api.v5.security.exception.InternalAuthenticationServiceException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthorizationFilterTest {
    private HttpServletRequest request;
    private ApiContextHolder apiContextHolder;
    private AuthorizationFilter authorizationFilter;
    private DirectApiAuthenticationManager authenticationManager;

    @Before
    public void setUp() {
        ApiContext apiContext = new ApiContext();
        apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(apiContext);

        authenticationManager = mock(DirectApiAuthenticationManager.class);
        authorizationFilter = new AuthorizationFilter(apiContextHolder, authenticationManager);

        request = mock(HttpServletRequest.class);
    }

    @After
    public void tearDown() {
        TransportContextHolder.setTransportContext(null);
        SecurityContextHolder.clearContext();
    }

    @Test
    public void setExceptionToApiContextWhenAuthenticationManagerStep1ThrowsException() throws Exception {
        RuntimeException someException = new RuntimeException();
        when(authenticationManager.checkApiAccessAllowed(any())).thenThrow(someException);
        authorizationFilter.doFilter(request, mock(ServletResponse.class), mock(FilterChain.class));
        assertThat(apiContextHolder.get().getAuthorizationException(),
                instanceOf(InternalAuthenticationServiceException.class));
    }

    @Test
    public void setExceptionToApiContextWhenAuthenticationManagerStep2ThrowsException() throws Exception {
        DirectApiPreAuthentication firstStepPreAuth = mock(DirectApiPreAuthentication.class);
        when(authenticationManager.checkApiAccessAllowed(any())).thenReturn(firstStepPreAuth);
        when(authenticationManager.checkClientLoginAccess(same(firstStepPreAuth))).thenThrow(new RuntimeException());
        authorizationFilter.doFilter(request, mock(ServletResponse.class), mock(FilterChain.class));
        assertThat(apiContextHolder.get().getAuthorizationException(),
                instanceOf(InternalAuthenticationServiceException.class));
    }

    @Test
    public void storeExceptionToApiContextWhenAuthenticationManagerStep1ThrowsAuthenticationException()
            throws Exception {
        BadCredentialsException someAuthException = new BadCredentialsException("");
        when(authenticationManager.checkApiAccessAllowed(any())).thenThrow(someAuthException);
        authorizationFilter.doFilter(request, mock(ServletResponse.class), mock(FilterChain.class));
        assertThat(apiContextHolder.get().getAuthorizationException(), sameInstance(someAuthException));
    }

    @Test
    public void storeExceptionToApiContextWhenAuthenticationManagerStep2ThrowsAuthenticationException()
            throws Exception {
        DirectApiPreAuthentication firstStepPreAuth = mock(DirectApiPreAuthentication.class);
        when(authenticationManager.checkApiAccessAllowed(any())).thenReturn(firstStepPreAuth);
        BadCredentialsException someAuthException = new BadCredentialsException("");
        when(authenticationManager.checkClientLoginAccess(same(firstStepPreAuth))).thenThrow(someAuthException);
        authorizationFilter.doFilter(request, mock(ServletResponse.class), mock(FilterChain.class));
        assertThat(apiContextHolder.get().getAuthorizationException(), sameInstance(someAuthException));
    }

    @Test
    public void storePreAuthToApiContextAfterStep1AndFailAtStep2() throws Exception {
        DirectApiPreAuthentication firstStepPreAuth = mock(DirectApiPreAuthentication.class);
        when(authenticationManager.checkApiAccessAllowed(any())).thenReturn(firstStepPreAuth);
        when(authenticationManager.checkClientLoginAccess(same(firstStepPreAuth)))
                .thenThrow(new BadCredentialsException(""));

        authorizationFilter.doFilter(request, mock(ServletResponse.class), mock(FilterChain.class));
        assertThat(apiContextHolder.get().getPreAuthentication(), sameInstance(firstStepPreAuth));
    }

    @Test
    public void storePreAuthToApiContextAfterSuccessfulStep1And2() throws Exception {
        DirectApiPreAuthentication firstStepPreAuth = mock(DirectApiPreAuthentication.class);
        when(authenticationManager.checkApiAccessAllowed(any())).thenReturn(firstStepPreAuth);

        DirectApiPreAuthentication secondStepPreAuth = mock(DirectApiPreAuthentication.class);
        when(secondStepPreAuth.isFullyAuthorized()).thenReturn(true);
        when(authenticationManager.checkClientLoginAccess(same(firstStepPreAuth))).thenReturn(secondStepPreAuth);

        authorizationFilter.doFilter(request, mock(ServletResponse.class), mock(FilterChain.class));
        assertThat(apiContextHolder.get().getPreAuthentication(), sameInstance(secondStepPreAuth));
    }
}
