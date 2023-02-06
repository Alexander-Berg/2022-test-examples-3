package ru.yandex.market.mbi.partner_stat.mvc;

import java.util.List;

import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.SwaggerDeserializationResult;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.mbi.partner_stat.FunctionalTest;

/**
 * Тест доступности сваггера.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class SwaggerTest extends FunctionalTest {

    @Test
    @DisplayName("Работоспособность Swagger'а. Валидируем генерируемую OpenAPI-спеку.")
    void validateSwaggerApiSpec() {
        final String location = baseUrl() + "/v2/api-docs";
        final String swaggerSpec = FunctionalTestHelper.get(location).getBody();
        final SwaggerParser parser = new SwaggerParser();
        final SwaggerDeserializationResult result = parser.readWithInfo(swaggerSpec);
        final List<String> messageList = result.getMessages();

        final String errors = StringUtils.join(messageList, ", ");
        Assertions.assertTrue(messageList.isEmpty(), errors);
    }
}
