package ru.yandex.market.tsum.clients.solomon;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.asynchttpclient.RequestBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.request.netty.JsonNettyHttpClient;
import ru.yandex.market.request.netty.NettyHttpClientContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RunWith(MockitoJUnitRunner.class)
@Ignore("integration test")
public class ModernSolomonApiClientTest {

    private static final String TEST_TOKEN = "test-token";
    private static final String SOLOMON_API_URL = "https://solomon.yandex-team.ru/api";

    @Mock
    NettyHttpClientContext contextMock;


    @Test
    public void ensureAuthenticationHeaderPresentInRequestBuilder() {
        ModernSolomonApiClient modernSolomonApiClient = new ModernSolomonApiClient(contextMock, TEST_TOKEN,
            SOLOMON_API_URL);
        JsonNettyHttpClient delegateClient = modernSolomonApiClient.delegateClient;
        RequestBuilder requestBuilder = delegateClient.createRequest(HttpMethod.GET, "/someApiMethod");
        HttpHeaders headers = requestBuilder.build().getHeaders();
        String authorizationHeader = headers.get(AUTHORIZATION);
        assertNotNull(authorizationHeader);
        assertEquals("OAuth " + TEST_TOKEN, authorizationHeader);
    }


}
