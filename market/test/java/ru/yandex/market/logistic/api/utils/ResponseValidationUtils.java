package ru.yandex.market.logistic.api.utils;

public final class ResponseValidationUtils {
    public static final String CONSTRAINT_VIOLATION = "ConstraintViolationImpl{" +
        "interpolatedMessage='%s', " +
        "propertyPath=%s, " +
        "rootBeanClass=class ru.yandex.market.logistic.api.model.%s.response.%s, " +
        "messageTemplate='{javax.validation.constraints.%s.message}'}";

    private ResponseValidationUtils() {
        throw new UnsupportedOperationException();
    }

    public static String getNotNullErrorMessage(String apiType, String responseClassName, String propertyPath) {
        return getErrorMessage(apiType, responseClassName, propertyPath, "must not be null", "NotNull");
    }

    public static String getNotBlankErrorMessage(String apiType, String responseClassName, String propertyPath) {
        return getErrorMessage(apiType, responseClassName, propertyPath, "must not be blank", "NotBlank");
    }

    public static String getNotEmptyErrorMessage(String apiType, String responseClassName, String propertyPath) {
        return getErrorMessage(apiType, responseClassName, propertyPath, "must not be empty", "NotEmpty");
    }

    public static String getMaxErrorMessage(String apiType, String responseClassName, String propertyPath, int max) {
        return getErrorMessage(
            apiType,
            responseClassName,
            propertyPath,
            String.format("must be less than or equal to %d", max),
            "Max"
        );
    }

    public static String getMinErrorMessage(String apiType, String responseClassName, String propertyPath, int min) {
        return getErrorMessage(
            apiType,
            responseClassName,
            propertyPath,
            String.format("must be greater than or equal to %d", min),
            "Min"
        );
    }

    private static String getErrorMessage(
        String apiType,
        String responseClassName,
        String propertyPath,
        String interpolatedMessage,
        String errorConstraint
    ) {

        return String.format(
            CONSTRAINT_VIOLATION,
            interpolatedMessage,
            propertyPath,
            apiType,
            responseClassName,
            errorConstraint
        );
    }
}
