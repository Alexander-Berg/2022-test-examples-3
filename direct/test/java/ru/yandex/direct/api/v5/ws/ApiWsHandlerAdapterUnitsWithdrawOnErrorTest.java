package ru.yandex.direct.api.v5.ws;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ws.InvalidXmlException;
import org.springframework.ws.NoEndpointFoundException;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.WebServiceMessageException;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.transport.WebServiceMessageReceiver;

import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.api.v5.ws.exceptionresolver.ApiExceptionResolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты проверяют, что заданные исключения приводя к вызову метода apiExceptionResolver.resolveException,
 * который в конечном счете списывает баллы в случае ошибок
 */
public class ApiWsHandlerAdapterUnitsWithdrawOnErrorTest {
    private WebServiceMessage requestMessage;
    private ApiExceptionResolver apiExceptionResolver;
    private ApiWsHandlerAdapter adapter;

    @Before
    public void setUp() throws IOException {
        requestMessage = mock(WebServiceMessage.class);

        WebServiceMessageFactory messageFactory = mock(WebServiceMessageFactory.class);
        when(messageFactory.createWebServiceMessage())
                .thenReturn(requestMessage);

        apiExceptionResolver = mock(ApiExceptionResolver.class);
        when(apiExceptionResolver.resolveException(any(), any()))
                .thenReturn(true);

        adapter = new ApiWsHandlerAdapter(
                messageFactory, apiExceptionResolver, mock(ApiUnitsService.class));
    }

    private void testIfException(Exception exception) throws Exception {
        WebServiceMessageReceiver handler = mock(WebServiceMessageReceiver.class);

        // Эмулируем ошибку при обработке сообщения
        doThrow(exception)
                .when(handler)
                .receive(any());

        adapter.handlePostMethod(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                handler);

        // В силу того, что exception может подменяться просто проверяем,
        // что метод вызывается
        verify(apiExceptionResolver).resolveException(any(), any());
    }

    @Test
    public void testIfInvalidXmlException() throws Exception {
        testIfException(
                new InvalidXmlException("Invalid xml", null));
    }

    @Test
    public void testIfWebServiceMessageException() throws Exception {
        testIfException(
                new WebServiceMessageException("Error") {{
                }});
    }

    @Test
    public void testIfNoEndpointFoundException() throws Exception {
        testIfException(
                new NoEndpointFoundException(requestMessage));
    }

    @Test
    public void testIfSomeRuntimeException() throws Exception {
        testIfException(new RuntimeException("Error"));
    }
}
