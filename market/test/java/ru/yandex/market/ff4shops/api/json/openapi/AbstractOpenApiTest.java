package ru.yandex.market.ff4shops.api.json.openapi;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.ErrorLoggingFilter;
import org.junit.jupiter.api.BeforeEach;

import ru.yandex.market.ff4shops.api.json.AbstractJsonControllerFunctionalTest;
import ru.yandex.market.ff4shops.client.ApiClient;
import ru.yandex.market.ff4shops.client.JacksonObjectMapper;

import static io.restassured.RestAssured.config;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;

public abstract class AbstractOpenApiTest extends AbstractJsonControllerFunctionalTest {
    protected ApiClient apiClient;

    @BeforeEach
    void init() {
        if (apiClient == null) {
            apiClient = apiClient(randomServerPort);
        }
    }

    ApiClient apiClient(int localPort) {
        return ApiClient.api(ApiClient.Config.apiConfig().reqSpecSupplier(() ->
                new RequestSpecBuilder().setConfig(config()
                                .objectMapperConfig(
                                        objectMapperConfig().defaultObjectMapper(JacksonObjectMapper.jackson())
                                )
                        )
                        .addFilter(new ErrorLoggingFilter())
                        .setBaseUri("http://localhost:" + localPort))
        );
    }

}
