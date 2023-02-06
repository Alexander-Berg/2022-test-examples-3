package ru.yandex.market.jmf.module.transfer.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Function;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.jmf.http.HttpResponse;
import ru.yandex.market.jmf.http.test.ResponseBuilder;
import ru.yandex.market.jmf.http.test.impl.HttpEnvironment;
import ru.yandex.market.jmf.http.test.impl.HttpRequest;
import ru.yandex.market.jmf.module.transfer.YandexDataTransferClient;


@ExtendWith(SpringExtension.class)
@SpringJUnitConfig(classes = ModuleDataTransferTestConfiguration.class)
@TestPropertySource("classpath:yc_test.properties")
public class HttpYandexDataTransferClientTest {

    private static final String ENDPOINT_ID = "fakeEndpointId";

    @Inject
    protected YandexDataTransferClient yandexDataTransferClient;
    @Inject
    private HttpEnvironment environment;
    @Inject
    private ResourceLoader resourceLoader;

    private static HttpResponse withResponse(Function<ResponseBuilder, ResponseBuilder> builderSpec) {
        return builderSpec.apply(ResponseBuilder.newBuilder()).build();
    }

    @BeforeEach
    public void setUp() throws IOException {
        var token = readFileContent("classpath:response/token.json");
        var endpoint = readFileContent("classpath:response/endpoint.json");
        environment.setUp();
        environment.when(HttpRequest.post("https://example.iam:443/iam/v1/tokens")).then(withResponse(b ->
                b.body(token)));
        environment.when(HttpRequest.get("https://example.transfer:443/v1/endpoint/" + ENDPOINT_ID)).then(withResponse(b
                -> b.body(endpoint)));
        environment.when(HttpRequest.patch("https://example.transfer:443/v1/endpoint/" + ENDPOINT_ID)).then(withResponse(b
                -> b.body("")));
    }

    @AfterEach
    public void tearDown() {
        environment.tearDown();
    }

    private String readFileContent(String location) throws IOException {
        return IOUtils.readInputStream(resourceLoader.getResource(location)
                .getInputStream());
    }

    @Test
    public void addNewTableToEndpoint() {
        Assertions.assertDoesNotThrow(() -> {
            yandexDataTransferClient.addTableToEndpoint("test table", ENDPOINT_ID);
        });
        Assertions.assertThrows(Exception.class, () -> yandexDataTransferClient.addTableToEndpoint("test table",
                "FailEndpoint"));
    }

    @Test
    public void addNewTables() {
        var tables = new ArrayList<String>();
        tables.add("test table1");
        tables.add("test table2");
        tables.add("test table3");
        Assertions.assertDoesNotThrow(() -> {
            yandexDataTransferClient.addTablesToEndpoint(tables, ENDPOINT_ID);
        });
    }

    @Test
    public void getEndpoint() {
        var endpoint = yandexDataTransferClient.getEndpoint(ENDPOINT_ID);
        Assertions.assertNotNull(endpoint);
        Assertions.assertFalse(endpoint.getSettings().getPostgresSource().getIncludeTables().isEmpty());
    }

    @Test
    public void updateEndpoint() {
        var oldEndpoint = yandexDataTransferClient.getEndpoint(ENDPOINT_ID);
        Assertions.assertDoesNotThrow(() -> {
            yandexDataTransferClient.updateEndpoint(oldEndpoint);
        });
    }
}
