package ru.yandex.direct.api.v5.ws;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DelegatingWebServiceMessageReceiverHandlerAdapterTest {

    public static final String PATH1 = "/aaa";
    public static final String PATH2 = "/bbb";

    private DelegatingWebServiceMessageReceiverHandlerAdapter delegatingAdapterUnderTest;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Object handler;
    private ApiWsHandlerAdapter apiHandlerAdapter;

    @Before
    public void setUp() throws Exception {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        handler = mock(Object.class);
        apiHandlerAdapter = mock(ApiWsHandlerAdapter.class);
    }

    @Test
    public void setNotFoundWhenNoAnyDelegates() throws Exception {
        delegatingAdapterUnderTest = new DelegatingWebServiceMessageReceiverHandlerAdapter();
        delegatingAdapterUnderTest.handle(request, response, handler);
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void setNotFoundWhenNoSuitableDelegates() throws Exception {
        delegatingAdapterUnderTest = new DelegatingWebServiceMessageReceiverHandlerAdapter(
                new HandlerAdapterDelegateRule(PATH1, apiHandlerAdapter));
        when(request.getRequestURI()).thenReturn(PATH2);
        delegatingAdapterUnderTest.handle(request, response, handler);
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void delegateHandleToSuitableHandlerAdapter() throws Exception {
        delegatingAdapterUnderTest = new DelegatingWebServiceMessageReceiverHandlerAdapter(
                new HandlerAdapterDelegateRule(PATH1, apiHandlerAdapter));
        when(request.getRequestURI()).thenReturn(PATH1);
        delegatingAdapterUnderTest.handle(request, response, handler);
        verify(apiHandlerAdapter).handle(request, response, handler);
    }
}
