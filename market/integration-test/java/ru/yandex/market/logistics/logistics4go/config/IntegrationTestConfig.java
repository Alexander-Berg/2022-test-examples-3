package ru.yandex.market.logistics.logistics4go.config;

import java.time.Clock;

import javax.servlet.Filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.ErrorLoggingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.logistics4go.client.ApiClient;
import ru.yandex.market.logistics.logistics4go.client.CombinatorGrpcClient;
import ru.yandex.market.logistics.logistics4go.client.JacksonObjectMapper;
import ru.yandex.market.logistics.logistics4go.components.logbroker.consumer.LomOrderEventConsumer;
import ru.yandex.market.logistics.logistics4go.config.properties.LesProperties;
import ru.yandex.market.logistics.logistics4go.service.LesService;
import ru.yandex.market.logistics.logistics4go.service.LesServiceImpl;
import ru.yandex.market.logistics.logistics4go.service.logbroker.LomOrderEventService;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.zonky.EnableZonkyEmbeddedPostgres;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;
import ru.yandex.market.personal.client.api.DefaultPersonalMultiTypesStoreApi;
import ru.yandex.market.request.trace.RequestContextHolder;

import static io.restassured.RestAssured.config;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static ru.yandex.market.logistics.logistics4go.client.ApiClient.Config.apiConfig;

@Lazy
@Configuration
@EnableZonkyEmbeddedPostgres
@EnableAutoConfiguration(exclude = LiquibaseAutoConfiguration.class)
@ComponentScan({
    "ru.yandex.market.logistics.logistics4go.client",
    "ru.yandex.market.logistics.logistics4go.config.properties",
    "ru.yandex.market.logistics.logistics4go.controller",
    "ru.yandex.market.logistics.logistics4go.converter",
    "ru.yandex.market.logistics.logistics4go.facade",
    "ru.yandex.market.logistics.logistics4go.jobs",
    "ru.yandex.market.logistics.logistics4go.repository",
    "ru.yandex.market.logistics.logistics4go.service",
    "ru.yandex.market.logistics.logistics4go.utils",
    "ru.yandex.market.logistics.logistics4go.validator",
    "ru.yandex.market.logistics.logistics4go.queue",
})
@Import({
    DbUnitTestConfiguration.class,
    JsonConfig.class,
    LiquibaseConfig.class,
    RepositoryConfig.class,
    SecurityConfig.class,
    DbQueueConfig.class,
    CisParserConfig.class,
    ValidatorsConfig.class,
    CacheConfiguration.class,
    PersonalAddressConverterConfig.class,
})
@MockBean({
    LomClient.class,
    TvmClientApi.class,
    CombinatorGrpcClient.class,
    LMSClient.class,
    LesProducer.class,
    DefaultPersonalMultiTypesStoreApi.class,
})
public class IntegrationTestConfig {

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

    @Bean
    public TestableClock clock() {
        return new TestableClock();
    }

    @Bean
    public LomOrderEventConsumer lomOrderEventConsumer(
        LomOrderEventService lomOrderEventService,
        ObjectMapper objectMapper
    ) {
        return new LomOrderEventConsumer(lomOrderEventService, objectMapper);
    }

    @Bean
    public LesService lesService(
        LesProducer lesProducer,
        Clock clock,
        LesProperties lesProperties
    ) {
        return new LesServiceImpl(
            lesProducer,
            clock,
            lesProperties.getSource(),
            lesProperties.getQueueWrite()
        );
    }
}
