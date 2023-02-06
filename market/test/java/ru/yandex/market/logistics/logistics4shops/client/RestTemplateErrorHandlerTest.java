package ru.yandex.market.logistics.logistics4shops.client;

import javax.annotation.Nonnull;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.logistics4shops.client.exception.Logistics4ShopsClientException;
import ru.yandex.market.logistics4shops.client.model.OrderBoxesRequestDto;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Обработчик ошибок для RestTemplate")
class RestTemplateErrorHandlerTest extends AbstractClientTest {

    @Test
    @DisplayName("Обработчик не требуется")
    void noNeedToHandle() {
        mockResponse(HttpStatus.OK, "response/ok_box_response.json");

        softly.assertThatCode(performRequest())
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Обработали ошибку валидации")
    void validationError() {
        mockResponse(HttpStatus.BAD_REQUEST, "response/validation_error.json");

        softly.assertThatThrownBy(performRequest())
            .isInstanceOf(Logistics4ShopsClientException.class)
            .hasFieldOrPropertyWithValue("statusCode", 400)
            .hasFieldOrPropertyWithValue("message", "Incorrect request. Validations: [...]");
    }

    @Test
    @DisplayName("Обработали клиентскую ошибку")
    void clientError() {
        mockResponse(HttpStatus.NOT_FOUND, "response/client_error.json");

        softly.assertThatThrownBy(performRequest())
            .isInstanceOf(Logistics4ShopsClientException.class)
            .hasFieldOrPropertyWithValue("statusCode", 404)
            .hasFieldOrPropertyWithValue("message", "Not found [ORDER] with id [1]");
    }

    @Test
    @DisplayName("Некорректное содержимое ответа")
    void incorrectErrorBody() {
        mockResponse(HttpStatus.BAD_REQUEST, "response/bad_error.json");

        softly.assertThatThrownBy(performRequest())
            .isInstanceOf(Logistics4ShopsClientException.class)
            .hasFieldOrPropertyWithValue("statusCode", 400)
            .hasFieldOrPropertyWithValue("message", "Error occurred while calling upstream");
    }

    @Test
    @DisplayName("Пятисотка")
    void internalServerError() {
        mockResponse(HttpStatus.INTERNAL_SERVER_ERROR, "response/internal_error.json");

        softly.assertThatThrownBy(performRequest())
            .isInstanceOf(Logistics4ShopsClientException.class)
            .hasFieldOrPropertyWithValue("statusCode", 500)
            .hasFieldOrPropertyWithValue("message", "Internal error: java.lang.NullPointerException");
    }

    @Test
    @DisplayName("Ошибка парсинга")
    void unparsableResponse() {
        mockResponse(HttpStatus.BAD_GATEWAY, "response/unparsable_error");

        softly.assertThatThrownBy(performRequest())
            .isInstanceOf(Logistics4ShopsClientException.class)
            .hasFieldOrPropertyWithValue("statusCode", 502)
            .hasFieldOrPropertyWithValue("message", "Error occurred while reading upstream error response");
    }

    private void mockResponse(HttpStatus status, String responsePath) {
        mockServer.expect(requestTo(getBaseUrl() + "/orders/1/boxes?mbiPartnerId=1"))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(
                withStatus(status)
                    .body(extractFileContent(responsePath))
                    .contentType(MediaType.APPLICATION_JSON)
            );
    }

    @Nonnull
    private ThrowingCallable performRequest() {
        return () -> orderBoxApi.putOrderBoxes("1", new OrderBoxesRequestDto(), 1L, null);
    }
}
