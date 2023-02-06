package ru.yandex.market.checkout.carter;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.market.checkout.carter.client.HttpCarterClient;
import ru.yandex.market.checkout.carter.config.TestCarterConfig;
import ru.yandex.market.checkout.carter.context.YdbReadWriteContainerContextInitializer;
import ru.yandex.market.checkout.carter.storage.dao.ydb.CarterYdbDao;
import ru.yandex.market.checkout.common.time.TestableClock;
import ru.yandex.market.checkout.test.MemCachedAgentMockFactory;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {
        TestCarterConfig.class,
        CarterContainerTestBase.ClientConfiguration.class
})
@ContextConfiguration(initializers = YdbReadWriteContainerContextInitializer.class)
@TestPropertySource({
        "classpath:checkout-storage.properties",
        "classpath:carter-client.properties"
})
public abstract class CarterContainerTestBase {

    protected final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    @Autowired
    protected CarterYdbDao ydbDao;
    @Autowired
    private MemCachedAgentMockFactory mockFactory;
    @Autowired
    private MemCachedAgent memCachedAgentMock;
    @Autowired
    @Qualifier("clock")
    private TestableClock testableClock;

    @BeforeEach
    public void cleanDatabase() {
        ydbDao.truncateTable();
    }

    @BeforeEach
    public void resetCache() {
        Mockito.reset(memCachedAgentMock);
        mockFactory.resetMemCachedAgentMock(memCachedAgentMock);
    }

    @AfterEach
    public void tearDown() {
        testableClock.clearFixed();
    }

    @Configuration
    static class ClientConfiguration {

        @Component
        public static class ServletContainerInitListener implements ApplicationListener<WebServerInitializedEvent> {

            private final HttpCarterClient carterClient;

            ServletContainerInitListener(
                    HttpCarterClient carterClient
            ) {
                this.carterClient = carterClient;
            }

            @Override
            public void onApplicationEvent(WebServerInitializedEvent event) {
                String serviceUrl = "http://localhost:" + event.getWebServer().getPort();
                carterClient.setServiceURL(serviceUrl);
            }
        }
    }
}
