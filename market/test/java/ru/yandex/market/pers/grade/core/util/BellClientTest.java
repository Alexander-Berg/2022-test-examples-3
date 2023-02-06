package ru.yandex.market.pers.grade.core.util;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

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

import ru.yandex.market.pers.grade.core.util.bell.BellClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.and;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.mockResponseWithString;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withBody;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withHeader;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withMethod;
import static ru.yandex.market.pers.test.http.HttpClientMockUtils.withPath;

public class BellClientTest {
    private final HttpClient httpClient = mock(HttpClient.class);
    private final BellClient bellClient =
        new BellClient("https://localhost:1234", "naruto",
            new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient)));

    @Test
    public void testSendNotification() throws IOException {
        Map<String,String> data = Collections.singletonMap("path", "//some/path/current");

        mockResponseWithString(httpClient, "OK");
        bellClient.sendBellNotificationFromYt("//some/path/current");

        ArgumentMatcher<HttpUriRequest> requestQueryMatcher = and(
            withMethod(HttpMethod.POST),
            withPath("/pushes/batch"),
            withBody(data, Map.class),
            withHeader(HttpHeaders.AUTHORIZATION, "OAuth naruto"),
            withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
        );
        verify(httpClient).execute(argThat(requestQueryMatcher), any(HttpContext.class));
    }

}
