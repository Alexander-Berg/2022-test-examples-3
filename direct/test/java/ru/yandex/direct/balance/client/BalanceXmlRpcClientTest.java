package ru.yandex.direct.balance.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClientRequestImpl;
import org.apache.xmlrpc.client.XmlRpcHttpTransportException;
import org.apache.xmlrpc.common.XmlRpcHttpRequestConfigImpl;
import org.apache.xmlrpc.common.XmlRpcLoadException;
import org.apache.xmlrpc.common.XmlRpcWorker;
import org.apache.xmlrpc.common.XmlRpcWorkerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.balance.client.exception.BalanceClientException;
import ru.yandex.direct.balance.client.model.method.BalanceExtendedStatusMethodSpec;
import ru.yandex.direct.balance.client.model.method.BaseBalanceMethodSpec;
import ru.yandex.direct.balance.client.model.request.BalanceRpcRequestParam;
import ru.yandex.direct.balance.client.model.response.BalanceExtendedStatusResponse;
import ru.yandex.direct.balance.client.model.response.BalanceRpcResponse;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class BalanceXmlRpcClientTest {
    private static final Long RESPONSE = 3334442L;
    private static final String TEST_METHOD_NAME = "Balance.Test";
    private static final Object[] TEST_PARAM = new Object[]{"Param"};
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(1L);
    private static final int TEST_RETRIES_NUM = 1;
    private static final Object TEST_RESPONSE = new Object[]{0, "SUCCESS", RESPONSE};

    private BalanceXmlRpcClient clientUnderTest;
    @Mock
    private BaseBalanceMethodSpec<BalanceRpcResponse> methodSpec;
    private BalanceRpcRequestParam requestParam;
    private XmlRpcWorker xmlRpcWorker;
    private XmlRpcClientRequestImpl clientRequest;

    @BeforeEach
    void setUp() throws XmlRpcLoadException, MalformedURLException {
        MockitoAnnotations.initMocks(this);
        when(methodSpec.getFullName()).thenReturn(TEST_METHOD_NAME);

        clientRequest = new XmlRpcClientRequestImpl(new XmlRpcHttpRequestConfigImpl(), TEST_METHOD_NAME, TEST_PARAM);

        requestParam = mock(BalanceRpcRequestParam.class);
        when(requestParam.asArray()).thenReturn(TEST_PARAM);
        when(requestParam.getTimeout()).thenReturn(TEST_TIMEOUT);
        when(requestParam.getMaxRetries()).thenReturn(TEST_RETRIES_NUM);

        XmlRpcWorkerFactory xmlRpcWorkerFactory = mock(XmlRpcWorkerFactory.class);
        xmlRpcWorker = mock(XmlRpcWorker.class);
        when(xmlRpcWorkerFactory.getWorker()).thenReturn(xmlRpcWorker);

        BalanceXmlRpcClientConfig config = new BalanceXmlRpcClientConfig(new URL("http://ya.ru"));
        clientUnderTest = new BalanceXmlRpcClient(config, xmlRpcWorkerFactory);
    }

    @Test
    void checkRawCallReturnsData() throws XmlRpcException {
        when(xmlRpcWorker.execute(refEq(clientRequest, "config")))
                .thenReturn(TEST_RESPONSE);
        Object response = clientUnderTest.rawCall(TEST_METHOD_NAME, TEST_PARAM, TEST_TIMEOUT);
        assertThat("получили ожидаемый ответ", response, equalTo(TEST_RESPONSE));
    }

    @Test
    void checkRawCallThrowsProvidedException() throws XmlRpcException {
        when(xmlRpcWorker.execute(refEq(clientRequest, "config")))
                .thenThrow(new XmlRpcHttpTransportException(404, "Not found"));
        assertThatCode(() -> clientUnderTest.rawCall(TEST_METHOD_NAME, TEST_PARAM, TEST_TIMEOUT))
                .isInstanceOf(XmlRpcHttpTransportException.class);
    }

    @Test
    void checkRetriedCallReturnsValueOnFirstRetry() throws XmlRpcException {
        when(xmlRpcWorker.execute(refEq(clientRequest, "config"))).thenReturn(TEST_RESPONSE);
        Object response = clientUnderTest.retriedCall(TEST_METHOD_NAME, TEST_PARAM, TEST_TIMEOUT, 2);
        assertThat("получили ожидаемый ответ", response, equalTo(TEST_RESPONSE));
        verify(xmlRpcWorker, times(1)).execute(any());
    }

    @Test
    void checkRetriedCallReturnsValueOnSecondRetry() throws XmlRpcException {
        when(xmlRpcWorker.execute(refEq(clientRequest, "config")))
                .thenThrow(new XmlRpcHttpTransportException(404, "Not found"))
                .thenReturn(TEST_RESPONSE);
        Object response = clientUnderTest.retriedCall(TEST_METHOD_NAME, TEST_PARAM, TEST_TIMEOUT, 2);
        assertThat("получили ожидаемый ответ", response, equalTo(TEST_RESPONSE));
        verify(xmlRpcWorker, times(2)).execute(any());
    }

    @Test
    void checkRetriedCallThrowsProvidedException() throws XmlRpcException {
        when(xmlRpcWorker.execute(refEq(clientRequest, "config")))
                .thenThrow(new XmlRpcHttpTransportException(404, "Not found"))
                .thenReturn(TEST_RESPONSE);
        assertThatCode(() -> clientUnderTest.retriedCall(TEST_METHOD_NAME, TEST_PARAM, TEST_TIMEOUT, 1))
                .isInstanceOf(BalanceClientException.class);
    }

    @Test
    void checkCallReturnsValidValue() throws XmlRpcException {
        when(xmlRpcWorker.execute(refEq(clientRequest, "config"))).thenReturn(TEST_RESPONSE);
        BalanceExtendedStatusMethodSpec<Long> extendedStatusMethodSpec =
                new BalanceExtendedStatusMethodSpec<>(TEST_METHOD_NAME, Long.class);
        BalanceExtendedStatusResponse<Long> response = clientUnderTest.call(extendedStatusMethodSpec, requestParam);
        assertThat("Получили корректные данные", response.getData(), equalTo(RESPONSE));
    }

    @Test
    void checkCallThrowsWrappedException() throws XmlRpcException {
        when(xmlRpcWorker.execute(refEq(clientRequest, "config"))).thenReturn(TEST_RESPONSE);
        when(methodSpec.convertResponse(TEST_RESPONSE)).thenThrow(new RuntimeException("Something strange happened"));
        assertThatCode(() -> clientUnderTest.call(methodSpec, requestParam))
                .isInstanceOf(BalanceClientException.class);
    }
}
