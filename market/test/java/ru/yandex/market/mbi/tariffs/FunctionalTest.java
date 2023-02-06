package ru.yandex.market.mbi.tariffs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.junit.JupiterDbUnitTest;
import ru.yandex.market.mbi.tariffs.config.EmbeddedPostgresConfig;
import ru.yandex.market.mbi.tariffs.config.FunctionalTestConfig;
import ru.yandex.market.mbi.tariffs.config.SpringApplicationConfig;

/**
 * Базовый класс для всех тестов
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = SpringApplicationConfig.class
)
@SpringJUnitConfig(
        classes = {
                FunctionalTestConfig.class,
                EmbeddedPostgresConfig.class
        }
)
@ActiveProfiles(profiles = {"functionalTest", "development"})
@TestPropertySource({
        "classpath:functional-test.properties",
        "classpath:app.properties",
        "classpath:quartz.properties"
})
@DbUnitDataSet(before = {
        "classpath:ru/yandex/market/mbi/tariffs/categories.csv",
        "classpath:ru/yandex/market/mbi/tariffs/partners.csv"
})
public class FunctionalTest extends JupiterDbUnitTest {
    private static final String BASE_URL = "http://localhost:";

    @LocalServerPort
    private int port;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String baseUrl() {
        return BASE_URL + port;
    }

    protected String convertToJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected void assertOk(ResponseEntity<?> response) {
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    protected HttpHeaders jsonHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return httpHeaders;
    }

    protected HttpEntity<String> createHttpRequestEntity(Object body) {
        String jsonBody = body instanceof String s ? s : convertToJson(body);
        return new HttpEntity<>(jsonBody, jsonHeaders());
    }
}
