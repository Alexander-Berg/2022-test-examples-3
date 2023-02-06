package ru.yandex.market.api.integration;

import javax.inject.Inject;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import ru.yandex.market.api.RestServer;
import ru.yandex.market.api.config.CategoryConfig;
import ru.yandex.market.api.config.CheckouterConfigPatched;
import ru.yandex.market.api.config.ClientConfig;
import ru.yandex.market.api.config.CurrencyConfig;
import ru.yandex.market.api.config.GeoConfig;
import ru.yandex.market.api.config.HttpClientConfig;
import ru.yandex.market.api.config.JettyConfig;
import ru.yandex.market.api.config.MapiConfig;
import ru.yandex.market.api.config.PropertiesConfig;
import ru.yandex.market.api.config.UrlConfig;
import ru.yandex.market.api.internal.distribution.DistributionTestConfig;
import ru.yandex.market.api.server.sec.SecurityTestConfig;
import ru.yandex.market.api.util.SerializationConfiguration;
import ru.yandex.market.api.util.httpclient.spi.HttpExpectations;
import ru.yandex.market.api.util.httpclient.spi.MockRequestProcessorFactory;
import ru.yandex.market.api.util.parser2.ParsersConfig;
import ru.yandex.market.http.RequestProcessorFactory;

/**
 * @author zoom
 */
@Configuration
@ImportResource(locations = {
        "classpath:/application-test.xml"
})
@Import({
        PropertiesConfig.class,
        SerializationConfiguration.class,
        JettyConfig.class,
        CheckouterConfigPatched.class,
        HttpClientConfig.class,
        ClientConfig.class,
        ParsersConfig.class,
        UrlConfig.class,
        GeoConfig.class,
        CurrencyConfig.class,
        CategoryConfig.class,
        MapiConfig.class,
        DistributionTestConfig.class,
        SecurityTestConfig.class,
})
@ComponentScan("ru.yandex.market.api")
@PropertySource("classpath:/mock-http.properties")
public class TestAppConfig {

    @Inject
    private Server server;
    private RequestProcessorFactory commonRequestProcessorFactory;

    @Bean
    @DependsOn("server")
    public String baseUrl() {
        startServerForTests();

        final Connector[] connectors = server.getConnectors();
        final Connector firstConnector = connectors[0];

        final NetworkConnector networkConnector = (NetworkConnector) firstConnector;
        final int actualPort = networkConnector.getLocalPort();

        return "http://localhost:" + actualPort;
    }

    /**
     * Для продакшн кода сервер стартует не в контексте, см {@link RestServer#runApplication()}.
     */
    private void startServerForTests() {
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RequestProcessorFactory commonRequestProcessorFactory(Environment propertySource,
                                                            HttpExpectations httpExpectations,
                                                            ApplicationContext appContext) {
        if (null == commonRequestProcessorFactory) {
            commonRequestProcessorFactory = new MockRequestProcessorFactory(propertySource, httpExpectations, appContext);
        }
        return commonRequestProcessorFactory;
    }

    @Bean(name = HttpClientConfig.DEFAULT_REQUEST_PROCESSOR_BEAN_NAME)
    public RequestProcessorFactory requestProcessorFactory(Environment propertySource,
                                                           HttpExpectations httpExpectations,
                                                           ApplicationContext appContext) {
        return commonRequestProcessorFactory(propertySource, httpExpectations, appContext);
    }

    @Bean(name = MapiConfig.MAPI_REQUEST_FACTORY)
    public RequestProcessorFactory mapiRequestProcessorFactory(Environment propertySource,
                                                           HttpExpectations httpExpectations,
                                                           ApplicationContext appContext) {
        return commonRequestProcessorFactory(propertySource, httpExpectations, appContext);
    }
}
