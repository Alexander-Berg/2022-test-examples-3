package ru.yandex.market.test;

import java.io.IOException;

import okhttp3.mockwebserver.MockWebServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FunctionalTestEnvironmentConfig {


    @Bean(destroyMethod = "shutdown")
    public MockWebServer biddingMockServer() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start(8889);
        return server;
    }

}
