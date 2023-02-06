package ru.yandex.market.shopinfo;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author semin-serg
 */
public class MockHttpClientUtil {

    private MockHttpClientUtil() {
    }

    public static HttpClient mockHttpClient(HttpResponse httpResponse) throws IOException {
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.execute(any())).thenReturn(httpResponse);
        return httpClient;
    }

}
