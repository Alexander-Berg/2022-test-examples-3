package ru.yandex.market.logistics.nesu.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.client.model.error.ClientError;
import ru.yandex.market.logistics.nesu.client.model.error.ErrorType;
import ru.yandex.market.logistics.nesu.client.model.error.PartnerShipmentConfirmationError;
import ru.yandex.market.logistics.nesu.client.model.error.PartnerShipmentConfirmationError.ExcludedOrderReason;
import ru.yandex.market.logistics.nesu.client.model.error.PartnerShipmentConfirmationError.PartnerShipmentConfirmationErrorType;
import ru.yandex.market.logistics.nesu.client.model.error.ResourceNotFoundError;
import ru.yandex.market.logistics.nesu.client.model.error.ResourceType;
import ru.yandex.market.logistics.nesu.client.model.error.ValidationError;
import ru.yandex.market.logistics.nesu.model.error.ApiError;
import ru.yandex.market.logistics.nesu.model.error.ConstraintApiError;
import ru.yandex.market.logistics.nesu.model.error.NotFoundError;
import ru.yandex.market.logistics.nesu.model.error.PartnerShipmentError;
import ru.yandex.market.logistics.nesu.model.error.SimpleApiError;
import ru.yandex.market.logistics.nesu.model.error.ValidationApiError;
import ru.yandex.market.logistics.nesu.model.error.validation.FieldValidationConditionViolation;
import ru.yandex.market.logistics.nesu.model.error.validation.ObjectValidationConditionViolation;

@DisplayName("Сериализация и десериализация объектов ошибок")
public class ClientErrorTest extends AbstractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @DisplayName("Успешная конвертация")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void success(
        @SuppressWarnings("unused") String caseName,
        ApiError internal,
        ClientError expected
    ) throws Exception {
        softly.assertThat(objectMapper.readValue(objectMapper.writeValueAsString(internal), ClientError.class))
            .usingRecursiveComparison()
            .isEqualTo(expected);
    }

    private static Stream<Arguments> success() {
        return Stream.of(
            Arguments.of(
                "NotFoundError",
                new NotFoundError("Order not found", ResourceType.ORDER, List.of(1L, 2L, 3L)),
                new ResourceNotFoundError()
                    .setResourceType(ResourceType.ORDER)
                    .setIdentifiers(List.of(1L, 2L, 3L))
                    .setType(ErrorType.RESOURCE_NOT_FOUND)
                    .setMessage("Order not found")
            ),
            Arguments.of(
                "ValidationApiError",
                new ValidationApiError(List.of(
                    new FieldValidationConditionViolation(
                        "someObject",
                        "someField",
                        "Some message",
                        "SomeCondition",
                        Map.of()
                    ),
                    new ObjectValidationConditionViolation(
                        "anotherObject",
                        "Another message",
                        "AnotherCondition",
                        Map.of("argument", 0)
                    )
                )),
                new ValidationError()
                    .setErrors(List.of(
                        new ValidationError.ValidationViolation()
                            .setErrorCode(ValidationError.ValidationErrorCode.FIELD_NOT_VALID)
                            .setObjectName("someObject")
                            .setField("someField")
                            .setMessage("Some message")
                            .setConditionCode("SomeCondition")
                            .setArguments(Map.of()),
                        new ValidationError.ValidationViolation()
                            .setErrorCode(ValidationError.ValidationErrorCode.OBJECT_NOT_VALID)
                            .setObjectName("anotherObject")
                            .setMessage("Another message")
                            .setConditionCode("AnotherCondition")
                            .setArguments(Map.of("argument", 0))
                    ))
                    .setType(ErrorType.VALIDATION_ERROR)
                    .setMessage("Validation error")
            ),
            Arguments.of(
                "SimpleApiError",
                new SimpleApiError("Something"),
                new ClientError()
                    .setType(ErrorType.UNKNOWN)
                    .setMessage("Something")
            ),
            Arguments.of(
                "ConstraintApiError",
                new ConstraintApiError(
                    "Own delivery restrictions validation failed",
                    ErrorType.OWN_DELIVERY_RESTRICTIONS_VALIDATION,
                    List.of()
                ),
                new ClientError()
                    .setType(ErrorType.OWN_DELIVERY_RESTRICTIONS_VALIDATION)
                    .setMessage("Own delivery restrictions validation failed")
            ),
            Arguments.of(
                "PartnerShipmentError",
                new PartnerShipmentError(
                    PartnerShipmentConfirmationErrorType.INVALID_ORDERS,
                    Map.of(ExcludedOrderReason.CANCELLED, List.of(123L))
                ),
                new PartnerShipmentConfirmationError()
                    .setError(PartnerShipmentConfirmationErrorType.INVALID_ORDERS)
                    .setInvalidOrders(Map.of(ExcludedOrderReason.CANCELLED, List.of(123L)))
                    .setType(ErrorType.PARTNER_SHIPMENT_CONFIRMATION_VALIDATION)
                    .setMessage("Partner shipment validation failed")
            )
        );
    }

}
