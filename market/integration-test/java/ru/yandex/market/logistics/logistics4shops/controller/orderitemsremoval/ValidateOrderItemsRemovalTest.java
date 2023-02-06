package ru.yandex.market.logistics.logistics4shops.controller.orderitemsremoval;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.client.api.OrderItemsRemovalApi;
import ru.yandex.market.logistics.logistics4shops.client.api.model.RemoveItemsValidationRequest;
import ru.yandex.market.logistics.logistics4shops.client.api.model.RemoveItemsValidationResponse;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ValidationError;
import ru.yandex.market.logistics.logistics4shops.client.api.model.ValidationViolation;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4shops.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Валидация возможности удаления товаров из заказа")
@ParametersAreNonnullByDefault
class ValidateOrderItemsRemovalTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Успех")
    void success() {
        RemoveItemsValidationResponse removeItemsValidationResponse = apiOperation()
            .body(new RemoveItemsValidationRequest().lastMilePartnerId(1L))
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(removeItemsValidationResponse).isEqualTo(
            new RemoveItemsValidationResponse().itemsRemovalIsAllowed(true)
        );
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Ошибка валидации запроса")
    void invalidRequest(
        @SuppressWarnings("unused") String displayName,
        RemoveItemsValidationRequest request,
        String field,
        String message
    ) {
        ValidationError validationError = apiOperation()
            .body(request)
            .execute(validatedWith(shouldBeCode(SC_BAD_REQUEST)))
            .as(ValidationError.class);

        softly.assertThat(validationError.getErrors()).containsExactly(
            new ValidationViolation().field(field).message(message)
        );
    }

    @Nonnull
    private static Stream<Arguments> invalidRequest() {
        return Stream.of(
            Arguments.of(
                "Не указан lastMilePartnerId",
                new RemoveItemsValidationRequest().lastMilePartnerId(null),
                "lastMilePartnerId",
                "must not be null"
            )
        );
    }

    @Nonnull
    private OrderItemsRemovalApi.ValidateOrderItemsRemovalOper apiOperation() {
        return apiClient.orderItemsRemoval()
            .validateOrderItemsRemoval();
    }
}
