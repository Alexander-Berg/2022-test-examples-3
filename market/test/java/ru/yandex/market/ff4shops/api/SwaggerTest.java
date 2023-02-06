package ru.yandex.market.ff4shops.api;

import java.util.List;

import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.SwaggerDeserializationResult;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.util.FF4ShopsUrlBuilder;
import ru.yandex.market.ff4shops.util.FunctionalTestHelper;

/**
 * Тест для проверки успешного ответа ручки /v2/api-docs Swagger'a
 */
class SwaggerTest extends FunctionalTest {

    @Test
    void validateSwaggerApiSpec() {
        String location = FF4ShopsUrlBuilder.getSwaggerUrl(randomServerPort);
        ResponseEntity<String> swaggerSpec = FunctionalTestHelper.getForEntity(location);

        SwaggerParser parser = new SwaggerParser();
        SwaggerDeserializationResult result = parser.readWithInfo(swaggerSpec.getBody());

        List<String> messageList = result.getMessages();
        Assertions.assertTrue(messageList.isEmpty(), StringUtils.join(messageList, ", "));
    }
}
