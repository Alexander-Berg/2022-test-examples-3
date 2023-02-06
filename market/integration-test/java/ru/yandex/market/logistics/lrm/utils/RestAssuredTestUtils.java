package ru.yandex.market.logistics.lrm.utils;

import javax.annotation.ParametersAreNonnullByDefault;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

import ru.yandex.market.logistics.test.integration.matchers.JsonMatcher;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@UtilityClass
@ParametersAreNonnullByDefault
public class RestAssuredTestUtils {

    /**
     * Проверить, что в успешном ответе от сервера заданный json.
     */
    public void assertJsonResponse(Response response, String pathToJsonFile) {
        assertJsonResponse(response, HttpStatus.OK.value(), pathToJsonFile);
    }

    /**
     * Проверить, что в ответе от сервера заданный json и код.
     */
    public void assertJsonResponse(Response response, int expectedStatus, String pathToJsonFile) {
        response.then()
            .statusCode(expectedStatus)
            .contentType(ContentType.JSON)
            .body(new JsonMatcher(extractFileContent(pathToJsonFile)));
    }

    /**
     * Проверить, что в успешном ответе от сервера в поле json заданное значение.
     */
    public void assertJsonParameter(
        Response response,
        String parameterName,
        Object expectedValue
    ) {
        response.then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.JSON)
            .body(parameterName, equalTo(expectedValue));
    }

    /**
     * Проверка, что запрос вернул код 404 и заданное сообщение об ошибке.
     */
    public void assertNotFoundError(Response response, String errorMessage) {
        response.then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .body("message", equalTo(errorMessage));
    }

    /**
     * Проверка ошибок валидации, код 400.
     */
    public void assertValidationErrors(
        Response response,
        ValidationErrorFields validationErrorFields
    ) {
        response.then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .contentType(ContentType.JSON)
            .body(
                validationErrorFields.getErrorsPrefix() + "objectName",
                equalTo(validationErrorFields.getObjectName()),
                validationErrorFields.getErrorsPrefix() + "code",
                equalTo(validationErrorFields.getCode()),
                validationErrorFields.getErrorsPrefix() + "defaultMessage",
                equalTo(validationErrorFields.getMessage()),
                validationErrorFields.getErrorsPrefix() + "field",
                equalTo(validationErrorFields.getField())
            );
    }

    /**
     * Проверка, что запрос вернул код 500 и заданное сообщение об ошибке.
     */
    public void assertInternalServerError(Response response, String errorMessage) {
        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR.value(), errorMessage);
    }

    /**
     * Проверка, что запрос вернул код 422 и заданное сообщение об ошибке.
     */
    public void assertUnprocessableEntityServerError(Response response, String errorMessage) {
        assertError(response, HttpStatus.UNPROCESSABLE_ENTITY.value(), errorMessage);
    }

    /**
     * Проверка, что запрос вернул код 200 и пустое тело ответа.
     */
    public void assertSuccessResponseWithEmptyBody(Response response) {
        assertSuccessResponseWithBody(response, "");
    }

    /**
     * Проверка, что запрос вернул код 200 и заданное тело ответа.
     */
    public void assertSuccessResponseWithBody(Response response, String value) {
        response.then()
            .statusCode(HttpStatus.OK.value())
            .body(equalTo(value));
    }

    private void assertError(Response response, int expectedCode, String errorMessage) {
        response.then()
            .statusCode(expectedCode)
            .body("message", equalTo(errorMessage));
    }
}
