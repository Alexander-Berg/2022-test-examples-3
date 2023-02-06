package ru.yandex.market.pricelabs.integration;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockWebServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestExecutionListeners;

import ru.yandex.market.pricelabs.AbstractSpringConfiguration;
import ru.yandex.market.pricelabs.MockMvcProxy;
import ru.yandex.market.pricelabs.api.AbstractApiSpringConfiguration;
import ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer;
import ru.yandex.market.pricelabs.integration.api.programs.AutocreationTestConfig;
import ru.yandex.market.pricelabs.integration.search.FilterQueryBuilderTest;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.TestControls;
import ru.yandex.market.pricelabs.tms.TmsResetListener;
import ru.yandex.market.pricelabs.tms.api.SharedApi;
import ru.yandex.market.pricelabs.tms.processing.ExecutorSources;


@ContextHierarchy({
        @ContextConfiguration(name = "tms", classes = {
                // Если тестам нужны дополнительные классы инициализации - их нужно расшарить
                // В противном случае для этих тестов будет подниматься отдельный контекст (а потом еще раз - для
                // оставшихся)
                AbstractTmsSpringConfiguration.TmsConfiguration.class,
                FilterQueryBuilderTest.TestClassInitializer.class,
                PublicApiTestInitializer.class
        }),
        @ContextConfiguration(name = "api", classes = {
                AutocreationTestConfig.class,
                AbstractApiSpringConfiguration.ApiConfiguration.class,
        }),
        @ContextConfiguration(classes = AbstractIntegrationSpringConfiguration.SharedConfiguration.class),
})
@TestExecutionListeners({TmsResetListener.class})
public class AbstractIntegrationSpringConfiguration extends AbstractSpringConfiguration {

    @Autowired
    protected TestControls testControls;

    @Autowired
    protected ExecutorSources executors;

    @Autowired
    @Qualifier("mockWebServerTms")
    protected MockWebServer mockWebServerTms;

    public String expectError(String message) {
        return message.replaceAll("(.+http://localhost:)(\\d+)(/.+)", "$1" + mockWebServerTms.getPort() + "$3");
    }

    @Slf4j
    public static class SharedConfiguration {

        @Autowired
        @Qualifier("mockWebServerTms")
        private MockWebServer mockWebServerTms;

        @Autowired
        private SharedApi sharedApi;

        @PostConstruct
        void init() {
            MockMvcProxy.registerProxy(mockWebServerTms, sharedApi);
        }

    }
}
