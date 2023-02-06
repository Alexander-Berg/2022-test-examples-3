package ru.yandex.market.logistics.test.integration.utils;

import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static ru.yandex.market.logistics.test.integration.utils.RequestFactoryWrapper.wrapInBufferedRequestFactory;

public final class MockServerUtils {

    private MockServerUtils() {
        throw new UnsupportedOperationException();
    }

    public static MockRestServiceServer createMockRestServiceServer(RestTemplate restTemplate) {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);

        wrapInBufferedRequestFactory(restTemplate);

        return mockServer;
    }
}
