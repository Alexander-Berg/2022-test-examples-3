package ru.yandex.market.wms.transportation.client;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import ru.yandex.market.wms.transportation.client.config.IntegrationTest;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class AbstractClientTest extends IntegrationTest {

    protected static WireMockServer server;

    protected final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
            .serializationInclusion(NON_NULL)
            .dateFormat(new StdDateFormat())
            .failOnUnknownProperties(false)
            .build();

    @BeforeAll
    protected static void initServer() {
        server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(19001));
        server.start();
    }

    @AfterAll
    protected static void stopServer() {
        server.stop();
    }

    protected <T> T readValue(String pathToFile, TypeReference<T> typeReference) {
        try {
            return mapper.readValue(extractFileContent(pathToFile), typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
