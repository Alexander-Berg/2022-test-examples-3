package ru.yandex.market.logistic.gateway.utils;

import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static ru.yandex.market.logistic.gateway.service.util.RequestFactoryWrapper.wrapInBufferedRequestFactory;

public class MockServerUtils {

    private MockServerUtils() { }

    public static MockRestServiceServer createMockRestServiceServer(RestTemplate restTemplate) {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);

        wrapInBufferedRequestFactory(restTemplate);

        return mockServer;
    }
}
