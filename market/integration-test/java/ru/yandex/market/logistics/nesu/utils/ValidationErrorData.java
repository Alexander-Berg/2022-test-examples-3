package ru.yandex.market.logistics.nesu.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import org.apache.commons.collections4.MapUtils;

import ru.yandex.market.logistics.nesu.client.validation.ValidShopExternalId;
import ru.yandex.market.logistics.nesu.model.error.validation.ValidationErrorCode;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ParametersAreNonnullByDefault
public class ValidationErrorData {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String validationMessage;
    private final String description;

    @Nonnull
    public static ValidationErrorData fieldError(
        String field,
        String message,
        String objectName,
        String code,
        Map<String, Object> arguments
    ) {
        ErrorDto errorDto = new ErrorDto(
            objectName,
            field,
            message,
            code,
            arguments,
            ValidationErrorCode.FIELD_NOT_VALID
        );

        return new ValidationErrorData(getMessageJson(errorDto), getDescription(errorDto));
    }

    @Nonnull
    public static ValidationErrorData fieldError(
        String field,
        String message,
        String objectName,
        String code
    ) {
        return fieldError(field, message, objectName, code, Map.of());
    }

    @Nonnull
    public static ValidationErrorData objectError(
        String objectName,
        String message,
        String code,
        Map<String, Object> arguments
    ) {
        ErrorDto errorDto = new ErrorDto(
            objectName,
            null,
            message,
            code,
            arguments,
            ValidationErrorCode.OBJECT_NOT_VALID
        );

        return new ValidationErrorData(getMessageJson(errorDto), getDescription(errorDto));
    }

    @Nonnull
    public static ValidationErrorData objectError(
        String objectName,
        String message,
        String code
    ) {
        return objectError(objectName, message, code, Map.of());
    }

    @Nonnull
    public static ValidationErrorDataBuilder fieldErrorBuilder(String field, String message, String code) {
        return new ValidationErrorDataBuilder(field, message, code);
    }

    @Nonnull
    public static ValidationErrorDataBuilder fieldErrorBuilder(String field, ErrorType errorType) {
        return new ValidationErrorDataBuilder(field, errorType);
    }

    @Nonnull
    public static ValidationErrorDataBuilder objectErrorBuilder(String message, String code) {
        return new ValidationErrorDataBuilder(null, message, code);
    }

    @Nonnull
    public static ValidationErrorDataBuilder objectErrorBuilder(ErrorType errorType) {
        return new ValidationErrorDataBuilder(null, errorType);
    }

    @Nonnull
    @SneakyThrows
    private static String getMessageJson(ErrorDto errorDto) {
        return OBJECT_MAPPER.writeValueAsString(errorDto);
    }

    @Nonnull
    private static String getDescription(ErrorDto errorDto) {
        return Optional.ofNullable(errorDto.getField()).orElseGet(errorDto::getObjectName)
            + " " + errorDto.getMessage();
    }

    @Nonnull
    public String getValidationMessage() {
        return this.validationMessage;
    }

    @Override
    public String toString() {
        return this.description;
    }

    @Value
    private static class ErrorDto {
        String objectName;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String field;
        String message;
        String conditionCode;
        Map<String, Object> arguments;
        ValidationErrorCode errorCode;
    }

    @Value
    @RequiredArgsConstructor
    public static class ErrorType {
        public static final ErrorType NOT_NULL = new ErrorType("must not be null", "NotNull");

        public static final ErrorType NOT_EMPTY = new ErrorType("must not be empty", "NotEmpty");

        public static final ErrorType NOT_BLANK = new ErrorType("must not be blank", "NotBlank");

        public static final ErrorType NOT_NULL_ELEMENTS = new ErrorType("must not contain nulls", "NotNullElements");

        public static final ErrorType POSITIVE = new ErrorType("must be greater than 0", "Positive");

        public static final ErrorType POSITIVE_OR_ZERO
            = new ErrorType("must be greater than or equal to 0", "PositiveOrZero");

        public static final ErrorType IS_UNIQUE = new ErrorType("must be unique in business", "UniqueInBusiness");

        public static final ErrorType VALID_EXTERNAL_ID = new ErrorType(
            "must contain only letters, digits, dashes, back and forward slashes, underscore and whitespace",
            "Pattern",
            Map.of("regexp", ValidShopExternalId.SHOP_EXTERNAL_ID_REGEXP)
        );

        String message;
        String code;
        Map<String, Object> arguments;

        public ErrorType(String message, String code) {
            this(message, code, null);
        }

        @Nonnull
        public static ErrorType min(int value) {
            return new ErrorType(
                "must be greater than or equal to " + value,
                "Min",
                Map.of("value", value)
            );
        }

        @Nonnull
        public static ErrorType max(int value) {
            return new ErrorType(
                "must be less than or equal to " + value,
                "Max",
                Map.of("value", value)
            );
        }

        @Nonnull
        public static ErrorType size(int min, int max) {
            return new ErrorType(
                String.format("size must be between %s and %s", min, max),
                "Size",
                Map.of("min", min, "max", max)
            );
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ValidationErrorDataBuilder {

        private final String field;
        private final String message;
        private final String code;
        private final Map<String, Object> arguments = new HashMap<>();

        private ValidationErrorDataBuilder(@Nullable String field, ErrorType errorType) {
            this(field, errorType.getMessage(), errorType.getCode());
            withArguments(errorType.getArguments());
        }

        public ValidationErrorDataBuilder withArguments(@Nullable Map<String, Object> arguments) {
            if (MapUtils.isNotEmpty(arguments)) {
                this.arguments.putAll(arguments);
            }
            return this;
        }

        public ValidationErrorData forObject(String objectName) {
            return field == null
                ? ValidationErrorData.objectError(objectName, message, code, arguments)
                : ValidationErrorData.fieldError(field, message, objectName, code, arguments);
        }

        @Override
        public String toString() {
            return Stream.of(field, message)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
        }
    }
}
