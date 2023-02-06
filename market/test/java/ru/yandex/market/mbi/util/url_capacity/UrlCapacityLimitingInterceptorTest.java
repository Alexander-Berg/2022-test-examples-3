package ru.yandex.market.mbi.util.url_capacity;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import ru.yandex.market.mbi.web.ActionNameProvider;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.util.url_capacity.UrlCapacityLimitingInterceptor.URL_CAPACITY_WAS_NOT_INCREMENTED;

class UrlCapacityLimitingInterceptorTest extends UrlCapacityLimitingTest {
    private static final String IGNORED_URL_1 = "/ignored-url-1";
    private static final String IGNORED_URL_2 = "/ignored-url-2";
    private ActionNameProvider actionNameProvider = mock(ActionNameProvider.class);


    //Бин для имитации вызова метода в интерцепторе
    private SomeBean someBean = new SomeBean();

    private UrlCapacityLimitingInterceptor interceptor;

    @BeforeEach
    void init() {
        interceptor = new UrlCapacityLimitingInterceptor(
                urlCapacityLimitFlags,
                urlCapacityLimiter,
                actionNameProvider,
                Set.of(
                        IGNORED_URL_1,
                        IGNORED_URL_2
                )
        );
    }


    @Test
    void testSimplePreHandle() throws Exception {
        setFlagEnabled(true);
        setCanProcessOneMoreUrl(true);
        setAction("someFreeUrl@GET");

        var passed = interceptor.preHandle(mockRequest("/some-free-url"), mockResponse(), mockSynchronousHandler());
        assertThat(passed).isTrue();
        verify(urlCapacityLimiter, times(1)).tryProcessOneMoreRequest(eq("someFreeUrl@GET"));
    }

    @Test
    void testSimplePreHandleWithDisabledFlag() throws Exception {
        setFlagEnabled(false);
        setCanProcessOneMoreUrl(true);
        setAction("someFreeUrl@GET");

        var passed = interceptor.preHandle(mockRequest("/some-free-url"), mockResponse(), mockSynchronousHandler());
        assertThat(passed).isTrue();
        verify(urlCapacityLimiter, never()).tryProcessOneMoreRequest(any());
    }

    @Test
    void testIgnoredUrl() throws Exception {
        setFlagEnabled(true);
        setCanProcessOneMoreUrl(false);

        var passed = interceptor.preHandle(mockRequest(IGNORED_URL_1), mockResponse(), mockSynchronousHandler());
        assertThat(passed).isTrue();
        verify(urlCapacityLimiter, never()).tryProcessOneMoreRequest(any());
    }

    @Test
    void test429OnDeny() throws Exception {
        setFlagEnabled(true);
        setCanProcessOneMoreUrl(false);
        setAction("someFreeUrl@GET");

        var response = mockResponse();

        var passed = interceptor.preHandle(mockRequest("/some-free-url"), response, mockSynchronousHandler());
        assertThat(passed).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS_429);
        verify(urlCapacityLimiter, times(1)).tryProcessOneMoreRequest(any());
    }

    @Test
    void testAsynchronousIgnored() throws Exception {
        setFlagEnabled(true);
        setCanProcessOneMoreUrl(false);
        setAction("someAsynchronousUrl@GET");

        var response = mockResponse();

        var passed = interceptor.preHandle(mockRequest("/some-async-url"), response, mockAsynchronousHandler());
        assertThat(passed).isTrue();
        verify(urlCapacityLimiter, never()).tryProcessOneMoreRequest(any());
    }

    @Test
    void testAfterCompletion() throws Exception {
        setFlagEnabled(true);
        setAction("someFreeUrl@GET");

        interceptor.afterCompletion(mockRequest("/some-free-url"), mockResponse(), mockSynchronousHandler(), null);
        verify(urlCapacityLimiter, times(1)).requestProcessed(eq("someFreeUrl@GET"));
    }

    @Test
    void testAfterCompletionIgnoredIfUrlCapacityNotIncremented() throws Exception {
        setFlagEnabled(false);
        setAction("someFreeUrl@GET");

        var request = mockRequest("/some-free-url");
        request.setAttribute(URL_CAPACITY_WAS_NOT_INCREMENTED, true);

        interceptor.afterCompletion(request, mockResponse(), mockSynchronousHandler(), null);
        verify(urlCapacityLimiter, never()).requestProcessed(any());
    }


    private void setAction(String actionName) {
        when(actionNameProvider.getActionName(any())).thenReturn(actionName);
    }

    private Object mockSynchronousHandler() {
        try {
            return new HandlerMethod(someBean, "someSynchronousMethod");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private Object mockAsynchronousHandler() {
        try {
            return new HandlerMethod(someBean, "someAsynchronousMethod");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpServletRequest mockRequest(String url) {
        var request = new MockHttpServletRequest(null, "GET", url);
        request.setPathInfo(url);
        return request;
    }

    private HttpServletResponse mockResponse() {
        return new MockHttpServletResponse();
    }

    private static class SomeBean {
        public String someSynchronousMethod() {
            return "123";
        }

        public CompletableFuture<String> someAsynchronousMethod() {
            return null;
        }
    }
}
