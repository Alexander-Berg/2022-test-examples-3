package ru.yandex.market.logistics.lrm.config;

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
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterReturnApi;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lrm.client.ApiClient;
import ru.yandex.market.logistics.lrm.client.JacksonObjectMapper;
import ru.yandex.market.logistics.lrm.config.locals.UuidGenerator;
import ru.yandex.market.logistics.lrm.config.properties.FeatureProperties;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.zonky.EnableZonkyEmbeddedPostgres;
import ru.yandex.market.personal.client.api.DefaultPersonalMultiTypesRetrieveApi;
import ru.yandex.market.personal.client.api.DefaultPersonalMultiTypesStoreApi;
import ru.yandex.market.pvz.client.logistics.PvzLogisticsClient;
import ru.yandex.market.request.trace.RequestContextHolder;
import ru.yandex.market.tpl.internal.client.TplInternalClient;
import ru.yandex.market.ydb.integration.YdbTemplate;

import static io.restassured.RestAssured.config;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static ru.yandex.market.logistics.lrm.client.ApiClient.Config.apiConfig;

@Lazy
@Configuration
@EnableZonkyEmbeddedPostgres
@EnableAutoConfiguration(exclude = LiquibaseAutoConfiguration.class)
@ComponentScan("ru.yandex.market.logistics.lrm")
@Import({
    DbUnitTestConfiguration.class,
    SecurityConfiguration.class,
    CombinatorTestConfiguration.class,
    LocalsConfiguration.class,
})
@MockBean({
    CheckouterAPI.class,
    CheckouterReturnApi.class,
    LomClient.class,
    PvzLogisticsClient.class,
    TplInternalClient.class,
    LMSClient.class,
    LesProducer.class,
    DefaultPersonalMultiTypesRetrieveApi.class,
    DefaultPersonalMultiTypesStoreApi.class,
})
@MockBean(value = LogbrokerClientFactory.class, name = "returnEventsClientFactory")
@MockBean(value = AsyncProducerConfig.class, name = "returnEventsProducerConfig")
@MockBean(value = DataFieldMaxValueIncrementer.class, name = "returnEventsLogbrokerIdSequence")
@SpyBean({
    YdbTemplate.class,
    UuidGenerator.class,
    FeatureProperties.class
})
public class LrmTestConfiguration {

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
    public Filter requestContextIdFilter() {
        return (request, response, chain) -> {
            RequestContextHolder.createContext(TEST_REQUEST_ID);
            chain.doFilter(request, response);
        };
    }

}
