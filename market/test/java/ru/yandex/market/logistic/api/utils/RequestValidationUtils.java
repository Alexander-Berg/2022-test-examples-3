package ru.yandex.market.logistic.api.utils;

public final class RequestValidationUtils {
    private static final String CONSTRAINT_TEMPLATE = "ConstraintViolationImpl{" +
        "interpolatedMessage='%s', " +
        "propertyPath=request.%s, " +
        "rootBeanClass=class ru.yandex.market.logistic.api.model.common.request.RequestWrapper, " +
        "messageTemplate='{javax.validation.constraints.%s.message}'}";

    private static final String VALIDATION_ERROR_TEMPLATE =
        "Some problems during ru.yandex.market.logistic.api.model.common.request.RequestWrapper " +
            "instance validating appeared:\n" + CONSTRAINT_TEMPLATE;

    private static final String NOT_NULL_VALIDATION_ERROR_TEMPLATE =
        String.format(VALIDATION_ERROR_TEMPLATE, "must not be null", "%s", "NotNull");

    private static final String NOT_BLANK_VALIDATION_ERROR_TEMPLATE =
        String.format(VALIDATION_ERROR_TEMPLATE, "must not be blank", "%s", "NotBlank");

    private static final String NOT_EMPTY_VALIDATION_ERROR_TEMPLATE =
        String.format(VALIDATION_ERROR_TEMPLATE, "must not be empty", "%s", "NotEmpty");

    private static final String POSITIVE_VALIDATION_ERROR_TEMPLATE =
        String.format(VALIDATION_ERROR_TEMPLATE, "must be greater than 0", "%s", "Positive");

    private static final String DECIMAL_MIN_ZERO_ERROR_TEMPLATE =
        String.format(VALIDATION_ERROR_TEMPLATE, "must be greater than or equal to 0", "%s", "DecimalMin");

    private static final String NOT_NULL_CONSTRAINT =
        String.format(CONSTRAINT_TEMPLATE, "must not be null", "%s", "NotNull");

    private static final String NOT_EMPTY_CONSTRAINT =
        String.format(CONSTRAINT_TEMPLATE, "must not be empty", "%s", "NotEmpty");

    private static final String NOT_BLANK_CONSTRAINT =
        String.format(CONSTRAINT_TEMPLATE, "must not be blank", "%s", "NotBlank");

    private RequestValidationUtils() {
        throw new UnsupportedOperationException();
    }

    public static String getNotNullErrorMessage(String property) {
        return String.format(NOT_NULL_VALIDATION_ERROR_TEMPLATE, property);
    }

    public static String getNotBlankErrorMessage(String property) {
        return String.format(NOT_BLANK_VALIDATION_ERROR_TEMPLATE, property);
    }

    public static String getNotNullConstraint(String property) {
        return String.format(NOT_NULL_CONSTRAINT, property);
    }

    public static String getNotBlankConstraint(String property) {
        return String.format(NOT_BLANK_CONSTRAINT, property);
    }

    public static String getNotEmptyConstraint(String property) {
        return String.format(NOT_EMPTY_CONSTRAINT, property);
    }

    public static String getNotEmptyErrorMessage(String property) {
        return String.format(NOT_EMPTY_VALIDATION_ERROR_TEMPLATE, property);
    }

    public static String getPositiveErrorMessage(String property) {
        return String.format(POSITIVE_VALIDATION_ERROR_TEMPLATE, property);
    }

    public static String getDecimalMinZeroErrorMessage(String property) {
        return String.format(DECIMAL_MIN_ZERO_ERROR_TEMPLATE, property);
    }
}
