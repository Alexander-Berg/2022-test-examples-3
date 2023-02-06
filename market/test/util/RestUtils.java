package ru.yandex.market.test.util;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 18.05.16
 */
public class RestUtils {
    private static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 500;
    private static final int DEFAULT_CONNECT_TIMEOUT = 500;
    private static final int DEFAULT_READ_TIMEOUT = 1000;

    private RestUtils() {

    }

    public static RestTemplate buildDefaultRestTemplate() {
        return buildDefaultRestTemplate(DEFAULT_CONNECTION_REQUEST_TIMEOUT,
                DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    public static RestTemplate buildDefaultRestTemplate(int connectionRequestTimeout,
                                                        int connectTimeout,
                                                        int readTimeout) {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                HttpClientBuilder.create()
                        .setConnectionManager(new PoolingHttpClientConnectionManager())
                        .build()
        );
        requestFactory.setConnectionRequestTimeout(connectionRequestTimeout);
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        return new RestTemplate(requestFactory);
    }
}
