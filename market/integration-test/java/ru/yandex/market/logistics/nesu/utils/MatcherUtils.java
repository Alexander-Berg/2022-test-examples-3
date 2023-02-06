package ru.yandex.market.logistics.nesu.utils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.nesu.client.model.error.ResourceType;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

public final class MatcherUtils {

    private MatcherUtils() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static ResultMatcher validationErrorMatcher(ValidationErrorData error) {
        return content().json(
            "{\"message\":\"Validation error\","
                + "\"errors\": [" + error.getValidationMessage() + "],"
                + "\"type\":\"VALIDATION_ERROR\""
                + "}",
            true
        );
    }

    @Nonnull
    public static ResultMatcher validationErrorMatcher(List<ValidationErrorData> errors) {
        String errorsString = errors.stream()
            .map(ValidationErrorData::getValidationMessage)
            .collect(Collectors.joining(",\n"));

        return content().json(
            "{\"message\":\"Validation error\",\"errors\": [" + errorsString + "],\"type\":\"VALIDATION_ERROR\"}",
            true
        );
    }

    @Nonnull
    public static ResultMatcher resourceNotFoundMatcher(ResourceType resourceType, Collection<Long> identifiers) {
        return content().json(
            "{\"message\":\"Failed to find [" + resourceType + "] with ids " + identifiers + "\","
                + "\"resourceType\":\"" + resourceType + "\","
                + "\"identifiers\":" + identifiers + ","
                + "\"type\":\"RESOURCE_NOT_FOUND\""
                + "}",
            true
        );
    }

    @Nonnull
    public static ResultMatcher shopValidationSettingErrorMatcher(String value) {
        return content().json(
            "{\"message\":\"Cannot parse constraint value '" + value + "'\","
                + "\"type\":\"SHOP_VALIDATION_SETTING_ERROR\""
                + "}",
            true
        );
    }
}
