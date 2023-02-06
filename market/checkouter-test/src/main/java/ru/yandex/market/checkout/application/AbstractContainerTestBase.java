package ru.yandex.market.checkout.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsClient;
import ru.yandex.market.checkout.checkouter.config.SwaggerConfiguration;
import ru.yandex.market.checkout.checkouter.config.web.WebContextConfig;
import ru.yandex.market.checkout.checkouter.test.config.services.IntTestCommonConfig;
import ru.yandex.market.checkout.common.CheckoutConfiguration;

@ContextHierarchy(
        @ContextConfiguration(name = "web", classes = AbstractContainerTestBase.TestConfiguration.class)
)
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = AbstractContainerTestBase.TestConfiguration.class
)
public abstract class AbstractContainerTestBase extends AbstractServicesTestBase {

    @Autowired
    protected TestRestTemplate testRestTemplate;
    @Autowired
    protected CheckouterAPI checkouterAPI;

    @Configuration
    @Import({IntTestCommonConfig.class, SwaggerConfiguration.class})
    @ImportResource("classpath:WEB-INF/checkouter-client-no-serialization.xml")
    @ComponentScan(basePackageClasses = {WebContextConfig.class})
    @CheckoutConfiguration
    public static class TestConfiguration {

        @Component
        public static class ServletContainerInitListener implements ApplicationListener<WebServerInitializedEvent> {

            private final CheckouterClient checkouterClient;
            private final CheckouterOrderHistoryEventsClient checkouterOrderHistoryEventsClient;

            public ServletContainerInitListener(
                    CheckouterClient checkouterClient,
                    CheckouterOrderHistoryEventsClient checkouterOrderHistoryEventsClient
            ) {
                this.checkouterClient = checkouterClient;
                this.checkouterOrderHistoryEventsClient = checkouterOrderHistoryEventsClient;
            }

            @Override
            public void onApplicationEvent(WebServerInitializedEvent event) {
                String serviceUrl = "http://localhost:" + event.getWebServer().getPort();
                checkouterClient.setServiceURL(serviceUrl);
                checkouterOrderHistoryEventsClient.setServiceURL(serviceUrl);
            }
        }
    }
}
