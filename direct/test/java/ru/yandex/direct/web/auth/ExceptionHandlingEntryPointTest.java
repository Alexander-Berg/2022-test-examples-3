package ru.yandex.direct.web.auth;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.core.AuthenticationException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExceptionHandlingEntryPointTest {

    private ExceptionHandlingEntryPoint testingEntryPoint;

    private ExceptionHandler exceptionHandler1;
    private ExceptionHandler exceptionHandler2;
    private ExceptionHandler defaultHandler;

    private HttpServletRequest request;
    private HttpServletResponse response;

    @Before
    public void prepare() {
        exceptionHandler1 = mock(ExceptionHandler.class);
        when(exceptionHandler1.supports(BadCredentialsException.class)).thenReturn(true);
        exceptionHandler2 = mock(ExceptionHandler.class);
        when(exceptionHandler2.supports(CredentialsExpiredException.class)).thenReturn(true);

        defaultHandler = mock(ExceptionHandler.class);
        when(defaultHandler.supports(AuthenticationServiceException.class)).thenReturn(true);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        testingEntryPoint = new ExceptionHandlingEntryPoint(
                defaultHandler, Arrays.asList(exceptionHandler1, exceptionHandler2));
    }

    // first handler supports exception

    @Test
    public void callsFirstHandlerWhenItSupportsException() {
        AuthenticationException ex = new BadCredentialsException("");
        testingEntryPoint.commence(request, response, ex);
        verify(exceptionHandler1).handle(request, response, ex);
    }

    @Test
    public void doesNotCallSecondHandlerWhenFirstSupportsException() {
        AuthenticationException ex = new BadCredentialsException("");
        testingEntryPoint.commence(request, response, ex);
        verify(exceptionHandler2, never()).handle(request, response, ex);
    }

    @Test
    public void doesNotCallDefaultHandlerWhenFirstSupportsException() {
        AuthenticationException ex = new BadCredentialsException("");
        testingEntryPoint.commence(request, response, ex);
        verify(defaultHandler, never()).handle(request, response, ex);
    }

    // second handler supports exception

    @Test
    public void callsSecondHandlerWhenItSupportsException() {
        AuthenticationException ex = new CredentialsExpiredException("");
        testingEntryPoint.commence(request, response, ex);
        verify(exceptionHandler2).handle(request, response, ex);
    }

    @Test
    public void doesNotCallFirstHandlerWhenSecondSupportsException() {
        AuthenticationException ex = new CredentialsExpiredException("");
        testingEntryPoint.commence(request, response, ex);
        verify(exceptionHandler1, never()).handle(request, response, ex);
    }

    @Test
    public void doesNotCallDefaulHandlerWhenSecondSupportsException() {
        AuthenticationException ex = new CredentialsExpiredException("");
        testingEntryPoint.commence(request, response, ex);
        verify(defaultHandler, never()).handle(request, response, ex);
    }

    // no handlers supports exception

    @Test
    public void callsDefaultHandlerWhenNoHandlersSupportException() {
        ProviderNotFoundException ex = new ProviderNotFoundException("");
        testingEntryPoint.commence(request, response, ex);
        verify(defaultHandler).handle(request, response, ex);
    }

    // check support() calls

    @Test
    public void checksFirstHandlerSupportsException() {
        testingEntryPoint.commence(request, response, new BadCredentialsException(""));
        verify(exceptionHandler1).supports(BadCredentialsException.class);
    }

    @Test
    public void doesNotCheckSecondHandlerSupportsExceptionWhenFirstSupports() {
        testingEntryPoint.commence(request, response, new BadCredentialsException(""));
        verify(exceptionHandler2, never()).supports(BadCredentialsException.class);
    }

    @Test
    public void checksSecondHandlerSupportsExceptionWhenFirstDoesNotSupport() {
        testingEntryPoint.commence(request, response, new CredentialsExpiredException(""));
        verify(exceptionHandler2).supports(CredentialsExpiredException.class);
    }

    @Test
    public void doesNotCheckDefaultHandlerSupportsExceptionWhenNoAnyHandlerSupports() {
        testingEntryPoint.commence(request, response, new AuthenticationServiceException(""));
        verify(defaultHandler, never()).supports(AuthenticationServiceException.class);
    }
}
