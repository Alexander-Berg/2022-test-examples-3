package ru.yandex.market.mbisfintegration;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.javaframework.clients.client.ApiClientFactory;
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;
import ru.yandex.market.mbisfintegration.salesforce.SalesForceTestConfiguration;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                SpringApplicationConfig.class,
                CommonUnitTestConfig.class,
                SalesForceTestConfiguration.class,
                AbstractFunctionalTest.FunctionalTestConfiguration.class
        }
)
@TestPropertySource(//locations = "classpath:95_application_test.properties",
        properties = "spring.autoconfigure.exclude=ru.yandex.market.starter.yt.config.YtConfigAutoConfiguration," +
                "ru.yandex.market.starter.yt.config.YtSyncAutoConfiguration," +
                "ru.yandex.market.starter.yt.config.YtAsyncAutoConfiguration," +
                "ru.yandex.market.starter.yt.config.YtRpcClientAutoConfiguration," +
                "ru.yandex.market.starter.yt.config.multiclient.YtMultiClientAutoConfiguration," +
                "ru.yandex.market.starter.yt.config.multiclient.YtSyncProviderAutoConfiguration," +
                "ru.yandex.market.starter.yt.config.multiclient.YtAsyncProviderAutoConfiguration")
public abstract class AbstractFunctionalTest {

    public static class FunctionalTestConfiguration {
        @Bean
        public TestClientCreator testClientCreator(ApiClientFactory apiClientFactory) {
            return new TestClientCreator(apiClientFactory);
        }
    }
}

