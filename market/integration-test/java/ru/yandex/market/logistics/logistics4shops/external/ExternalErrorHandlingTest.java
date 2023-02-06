package ru.yandex.market.logistics.logistics4shops.external;

import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.utils.RestAssuredFactory;
import ru.yandex.market.logistics.test.integration.matchers.XmlMatcher;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Обработка ошибок для внешнего апи")
class ExternalErrorHandlingTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Внутренняя ручка не существует")
    void notFoundExternal() {
        RestAssuredFactory.assertGetXml(
            "external/path/does/not/exist",
            "external/errorHandling/response/not_found.xml"
        );
    }

    @Test
    @DisplayName("Ручка не существует")
    void notFound() {
        RestAssured
            .get("path/does/not/exist")
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Неподдерживаемый тип запроса")
    void invalidMediaType() {
        RestAssured.given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(extractFileContent("external/errorHandling/request/invalid_media.json"))
            .post("/external/orders/getOrder")
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(MediaType.TEXT_XML_VALUE)
            .body(new XmlMatcher(extractFileContent("external/errorHandling/response/invalid_media.xml")));
    }
}
