package ru.yandex.market.mbi.tariffs.mvc;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.tariffs.FunctionalTest;

import static ru.yandex.market.common.test.spring.FunctionalTestHelper.get;

/**
 * Тесты для {@link ru.yandex.market.mbi.tariffs.mvc.controller.SwaggerController}
 */
public class SwaggerControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Тест на получение swagger.yaml")
    void testGetSwagger() throws Exception {
        ResponseEntity<String> response = get(baseUrl() + "/swagger");
        assertOk(response);

        String actual = JsonTestUtil.parseJson(response.getBody())
                .getAsJsonObject().get("result")
                .getAsJsonArray().get(0)
                .getAsString();
        String expected = Files.readString(Paths.get(getClass().getResource("/tariff-tech-api.yaml").toURI()));
        Assert.assertEquals(expected, actual);
    }
}
