package ru.yandex.market.logistics.logistics4shops.config;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.Filter;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.ErrorLoggingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.consumer.StreamListener;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.ff4shops.client.FF4ShopsClient;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.logistics4shops.client.ApiClient;
import ru.yandex.market.logistics.logistics4shops.client.JacksonObjectMapper;
import ru.yandex.market.logistics.logistics4shops.config.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.zonky.EnableZonkyEmbeddedPostgres;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;
import ru.yandex.market.lrm.client.api.ReturnsApi;
import ru.yandex.market.order_service.client.api.OrdersLinesCommonApi;
import ru.yandex.market.order_service.client.api.OrdersLogisticsApi;
import ru.yandex.market.personal.PersonalClient;
import ru.yandex.market.request.trace.RequestContextHolder;
import ru.yandex.market.tpl.common.transferact.client.api.TransferApi;

import static io.restassured.RestAssured.config;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static ru.yandex.market.logistics.logistics4shops.client.ApiClient.Config.apiConfig;

@Lazy
@Configuration
@EnableZonkyEmbeddedPostgres
@EnableAutoConfiguration(exclude = LiquibaseAutoConfiguration.class)
@ComponentScan("ru.yandex.market.logistics.logistics4shops")
@Import({
    DbUnitTestConfiguration.class,
    SecurityConfiguration.class,
})
@MockBean({
    CheckouterAPI.class,
    FF4ShopsClient.class,
    LMSClient.class,
    LesProducer.class,
    LogbrokerClientFactory.class,
    LomClient.class,
    OrdersLinesCommonApi.class,
    OrdersLogisticsApi.class,
    PersonalClient.class,
    ReturnsApi.class,
    StreamListener.ReadResponder.class,
    TransferApi.class,
    TransportManagerClient.class,
    TvmClientApi.class,
    Yt.class,
    YtTables.class,
})
@SpyBean({
    FeatureProperties.class,
})
@ParametersAreNonnullByDefault
public class IntegrationTestConfiguration {

    public static final String TEST_REQUEST_ID = "test-request-id";

    @Bean
    public ApiClient apiClient(@Value("${local.server.port}") int localPort) {
        return ApiClient.api(
            apiConfig()
                .reqSpecSupplier(() ->
                    new RequestSpecBuilder()
                        .setConfig(
                            config()
                                .objectMapperConfig(
                                    objectMapperConfig().defaultObjectMapper(JacksonObjectMapper.jackson())
                                )
                        )
                        .addFilter(new ErrorLoggingFilter())
                        .setBaseUri("http://localhost:" + localPort)
                )
        );
    }

    @Bean
    public TestableClock clock() {
        return new TestableClock();
    }

    @Bean
    public TestExecutionListener clockCleaner() {
        return new TestExecutionListener() {
            @Override
            public void afterTestMethod(TestContext testContext) {
                clock().clearFixed();
            }
        };
    }

    @Bean
    public Filter requestContextIdFilter() {
        return (request, response, chain) -> {
            RequestContextHolder.createContext(TEST_REQUEST_ID);
            chain.doFilter(request, response);
        };
    }
}
