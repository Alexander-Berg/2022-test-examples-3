package ru.yandex.market.pvz.internal.client;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

abstract class ClientTest {

    protected static final RestTemplate REST_TEMPLATE = new RestTemplate();
    protected static final String URL = "http://url";

    protected MockRestServiceServer mock;

    @BeforeEach
    public void setUp() {
        mock = MockRestServiceServer.createServer(REST_TEMPLATE);
    }

}
