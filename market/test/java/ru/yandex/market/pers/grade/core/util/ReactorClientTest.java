package ru.yandex.market.pers.grade.core.util;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.pers.grade.core.util.reactor.ReactorClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.and;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.mockResponseWithString;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withHeader;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withMethod;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withPath;

public class ReactorClientTest {
    private final HttpClient httpClient = mock(HttpClient.class);
    private final ReactorClient reactorClient =
        new ReactorClient("https://localhost:1234", "naruto",
            new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient)));

    @Test
    public void testSendNotification() throws IOException {
        mockResponseWithString(httpClient, "OK");
        reactorClient.sendPostRequest("/api/v1/a/i/deprecate", "{}");

        ArgumentMatcher<HttpUriRequest> requestQueryMatcher = and(
            withMethod(HttpMethod.POST),
            withPath("/api/v1/a/i/deprecate"),
            withHeader(HttpHeaders.AUTHORIZATION, "OAuth naruto"),
            withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
        );
        verify(httpClient, times(1)).execute(argThat(requestQueryMatcher), any(HttpContext.class));
    }

}