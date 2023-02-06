package ru.yandex.direct.api.v5.ws;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ws.NoEndpointFoundException;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.transport.WebServiceMessageReceiver;
import org.springframework.ws.transport.http.HttpTransportConstants;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.api.v5.ws.exceptionresolver.ApiExceptionResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class ApiWsHandlerAdapterTest {
    private WebServiceMessage requestMessage;
    private WebServiceMessage responseMessage;

    private WebServiceMessageFactory messageFactory;
    private ApiExceptionResolver exceptionResolver;
    private ApiUnitsService apiUnitsService;

    private ApiWsHandlerAdapter handlerAdapterUnderTest;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private WebServiceMessageReceiver receiver;
    private ServletOutputStream outputStream;

    @Before
    public void setUp() throws Exception {
        requestMessage = mock(WebServiceMessage.class);
        responseMessage = mock(WebServiceMessage.class);

        messageFactory = mock(WebServiceMessageFactory.class);
        when(messageFactory.createWebServiceMessage(any())).thenReturn(requestMessage);
        when(messageFactory.createWebServiceMessage()).thenReturn(responseMessage);

        exceptionResolver = mock(ApiExceptionResolver.class);
        when(exceptionResolver.resolveException(any(), any())).thenReturn(true);

        apiUnitsService = mock(ApiUnitsService.class);

        handlerAdapterUnderTest = new ApiWsHandlerAdapter(messageFactory, exceptionResolver, apiUnitsService);

        request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(HttpTransportConstants.METHOD_POST);
        response = mock(HttpServletResponse.class);
        outputStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(outputStream);
        receiver = mock(WebServiceMessageReceiver.class);
    }

    @Test
    public void handleSetMethodNotAllowedWhenNonPostRequest() throws Exception {
        when(request.getMethod()).thenReturn(HttpTransportConstants.METHOD_GET);
        handlerAdapterUnderTest.handle(request, response, receiver);
        verify(response).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void handleSuccessReceiveMessage() throws Exception {
        ArgumentCaptor<MessageContext> argument = ArgumentCaptor.forClass(MessageContext.class);
        handlerAdapterUnderTest.handle(request, response, receiver);
        verify(receiver).receive(argument.capture());
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(argument.getValue().getRequest()).isEqualTo(requestMessage);
        sa.assertThat(argument.getValue().getResponse()).isEqualTo(responseMessage);
        sa.assertAll();
    }

    @Test
    public void handleNoEndpointFoundException() throws Exception {
        NoEndpointFoundException exception = new NoEndpointFoundException(requestMessage);
        doThrow(exception).when(receiver).receive(any());
        handlerAdapterUnderTest.handle(request, response, receiver);

        ArgumentCaptor<NoEndpointFoundApiException> argument =
                ArgumentCaptor.forClass(NoEndpointFoundApiException.class);
        verify(exceptionResolver).resolveException(eq(responseMessage), argument.capture());
        NoEndpointFoundApiException actual = argument.getValue();
        NoEndpointFoundApiException expected = new NoEndpointFoundApiException(exception);
        assertThat(actual).is(matchedBy(beanDiffer(expected)
                .useCompareStrategy(translatableExceptionCompareStrategy(exception))));
    }

    @Test
    public void handleMessageCreationException() throws Exception {
        RuntimeException exception = new WebServiceMessageCreationException("xxx");
        doThrow(exception).when(receiver).receive(any());
        handlerAdapterUnderTest.handle(request, response, receiver);

        ArgumentCaptor<MessageCreationApiException> argument =
                ArgumentCaptor.forClass(MessageCreationApiException.class);
        verify(exceptionResolver).resolveException(eq(responseMessage), argument.capture());
        MessageCreationApiException expected = new MessageCreationApiException(exception);
        assertThat(argument.getValue()).is(matchedBy(
                beanDiffer(expected).useCompareStrategy(translatableExceptionCompareStrategy(exception))));
        verify(responseMessage).writeTo(outputStream);
    }

    @Test
    public void handleException() throws Exception {
        RuntimeException exception = new RuntimeException("xxx");
        doThrow(exception).when(receiver).receive(any());
        handlerAdapterUnderTest.handle(request, response, receiver);

        verify(exceptionResolver).resolveException(eq(responseMessage), eq(exception));
        verify(responseMessage).writeTo(outputStream);
    }

    private CompareStrategy translatableExceptionCompareStrategy(Throwable tr) {
        return onlyFields(newPath("cause"), newPath("code"), newPath("shortMessage"), newPath("detailedMessage"))
                .forFields(newPath("cause")).useMatcher(sameInstance(tr));
    }

}
