package ru.yandex.market.fulfillment.wrap.core.processing;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;

import ru.yandex.market.fulfillment.wrap.core.api.RequestType;
import ru.yandex.market.fulfillment.wrap.core.processing.validation.TokenContextHolder;
import ru.yandex.market.fulfillment.wrap.core.processing.validation.TokenValidator;
import ru.yandex.market.logistic.api.model.common.ErrorCode;
import ru.yandex.market.logistic.api.model.common.ErrorPair;
import ru.yandex.market.logistic.api.model.fulfillment.exception.FulfillmentApiException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.noMoreInteractions;
import static org.mockito.internal.verification.VerificationModeFactory.times;

class RequestProcessingServiceTest {

    private final RequestValidator requestValidator = mock(RequestValidator.class);
    private final ProcessorProvider processorProvider = mock(ProcessorProvider.class);
    private final TokenValidator tokenValidator = mock(TokenValidator.class);
    private final TokenContextHolder tokenContextHolder = mock(TokenContextHolder.class);
    private final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    private RequestProcessingService requestProcessingService = new RequestProcessingService(
        createDefaultMapper(),
        requestValidator,
        processorProvider,
        tokenValidator,
        tokenContextHolder
    );

    @Test
    void tokenWasSetAndCleared() throws IOException {
        String token = "xxxxxxxxxxxxxxxxxxxxxxmarschrouteTokenxxxxxxxxxxxxxxxxxxxxxxxxxx";

        when(httpServletRequest.getInputStream())
            .thenReturn(getServletInputStream(getByteArrayInputStream("/request.xml")));
        when(httpServletRequest.getContentType()).thenReturn(MediaType.APPLICATION_XML_VALUE);

        requestProcessingService.process(httpServletRequest);

        verify(tokenContextHolder, times(1)).setToken(token);
        verify(tokenContextHolder, times(1)).clearToken();
        verify(tokenContextHolder, noMoreInteractions()).setToken(any());
    }

    @Test
    void whenTokenIsInvalidThenContextTokenHolderWasNotSet() throws IOException {
        String token = "xxxxxxxxxxxxxxxxxxxxxxxxxxinvalidTokenxxxxxxxxxxxxxxxxxxxxxxxxxx";

        when(httpServletRequest.getInputStream())
            .thenReturn(getServletInputStream(getByteArrayInputStream("/invalid_request.xml")));
        when(httpServletRequest.getContentType()).thenReturn(MediaType.APPLICATION_XML_VALUE);

        Mockito.doThrow(new FulfillmentApiException(new ErrorPair(
            ErrorCode.BAD_REQUEST,
            String.format("RequestType %s doesn't support token %s", RequestType.GET_ORDER, token)
        ))).when(tokenValidator).assertIsValid(any(), any());

        requestProcessingService.process(httpServletRequest);

        verify(tokenContextHolder, never()).setToken(any());
        verify(tokenContextHolder, times(1)).clearToken();
    }


    private ByteArrayInputStream getByteArrayInputStream(String request) throws IOException {
        return new ByteArrayInputStream(getBytes(request));
    }

    private byte[] getBytes(String request) throws IOException {
        return IOUtils.toByteArray(this.getClass().getResourceAsStream(request));
    }

    private ServletInputStream getServletInputStream(ByteArrayInputStream byteArrayInputStream) {
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }

            public int read() {
                return byteArrayInputStream.read();
            }
        };
    }

    private XmlMapper createDefaultMapper() {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return xmlMapper;
    }
}
