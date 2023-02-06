package ru.yandex.market.request.jetty.trace;

import org.eclipse.jetty.server.Request;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.request.trace.RequestContextHolder;

public class JettyRequestIdPreHandlerTest {

    @Before
    @After
    public void clearContext() {
        RequestContextHolder.clearContext();
    }

    @Test
    public void testGeneratingReqIdHeader() {
        JettyRequestIdPreHandler handler = new JettyRequestIdPreHandler();
        handler.handle(null, mockRequest(), null, null);
        Assert.assertNotNull(RequestContextHolder.getContext().getRequestId());
    }

    private Request mockRequest() {
        Request request = Mockito.mock(Request.class);
        Mockito.when(request.getHeader(Mockito.anyString()))
                .thenReturn(null);
        return request;
    }

}
