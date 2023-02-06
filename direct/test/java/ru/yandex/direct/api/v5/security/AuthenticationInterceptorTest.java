package ru.yandex.direct.api.v5.security;

import javax.xml.transform.Source;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.transport.context.TransportContextHolder;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.security.exception.AuthenticationException;
import ru.yandex.direct.api.v5.security.exception.BadCredentialsException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthenticationInterceptorTest {
    private AuthenticationInterceptor authenticationInterceptor;
    private DirectApiAuthentication directApiAuthentication;
    private MessageContext messageContext;
    private ApiContext apiContext;

    @Before
    public void setUp() {
        WebServiceMessage requestMessage = mock(WebServiceMessage.class);
        when(requestMessage.getPayloadSource()).thenReturn(mock(Source.class));
        messageContext = mock(MessageContext.class);
        when(messageContext.getRequest()).thenReturn(requestMessage);

        apiContext = mock(ApiContext.class);
        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(apiContext);

        directApiAuthentication = mock(DirectApiAuthentication.class);

        authenticationInterceptor = new AuthenticationInterceptor(apiContextHolder);

        SecurityContextHolder.createEmptyContext();
    }

    @After
    public void tearDown() {
        TransportContextHolder.setTransportContext(null);
        SecurityContextHolder.clearContext();
    }

    @Test
    public void storeAuthenticationOnSuccess() throws Exception {
        when(apiContext.getDirectApiAuthenticationOrFail()).thenReturn(directApiAuthentication);
        authenticationInterceptor.handleRequest(messageContext, new Object());
        assertThat(SecurityContextHolder.getContext().getAuthentication(), sameInstance(directApiAuthentication));
    }

    @Test
    public void doNotStoreAuthenticationOnFail() {
        when(apiContext.getDirectApiAuthenticationOrFail()).thenThrow(new BadCredentialsException(""));
        try {
            authenticationInterceptor.handleRequest(messageContext, new Object());
        } catch (Exception ignore) {
        }
        assertThat(SecurityContextHolder.getContext().getAuthentication(), nullValue());
    }

    @Test(expected = AuthenticationException.class)
    public void throwExceptionOnFail() throws Exception {
        when(apiContext.getDirectApiAuthenticationOrFail()).thenThrow(new BadCredentialsException(""));
        authenticationInterceptor.handleRequest(messageContext, new Object());
    }
}
