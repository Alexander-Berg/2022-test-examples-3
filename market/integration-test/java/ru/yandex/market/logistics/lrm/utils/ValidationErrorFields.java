package ru.yandex.market.logistics.lrm.utils;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ValidationErrorFields {
    public static final String DEFAULT_ERRORS_PREFIX = "errors[0].";

    String errorsPrefix;
    String objectName;
    String code;
    String message;
    String field;
}
