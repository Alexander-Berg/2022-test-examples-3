package ru.yandex.market.logistics.tarifficator.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

import org.junit.jupiter.params.provider.Arguments;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.common.util.collections.Triple;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ParametersAreNonnullByDefault
public final class ValidationUtil {
    public static final String MUST_NOT_BE_EMPTY = "must not be empty";
    public static final String MUST_NOT_BE_NULL = "must not be null";
    public static final String MUST_BE_GREATER_THAN_0 = "must be greater than 0";
    public static final String MUST_NOT_BE_BLANK = "must not be blank";
    public static final String SIZE_MUST_BE_BETWEEN = "size must be between %d and %d";

    public static final Triple<Class<? extends Annotation>, String, Object> NULL_VALIDATION_INFO =
        Triple.of(NotNull.class, MUST_NOT_BE_NULL, null);
    public static final Triple<Class<? extends Annotation>, String, Object> NOT_BLANK_VALIDATION_INFO =
        Triple.of(NotBlank.class, MUST_NOT_BE_BLANK, "");

    private ValidationUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Проверяет, что в json'е в поле 'message' содержится ошибка валидации по указанному полю.
     *
     * @param fieldName  название поля
     * @param fieldError ошибка валидации поля
     */
    @Nonnull
    public static ResultMatcher fieldValidationError(String fieldName, String fieldError) {
        return jsonPath("message")
            .value(String.format(
                "Following validation errors occurred:\nField: '%s', message: '%s'",
                fieldName,
                fieldError
            ));
    }

    /**
     * Проверяет, что в json'е в поле 'errors' содержится ошибка валидации по указанному полю.
     *
     * @param fieldName  название поля
     * @param fieldError ошибка валидации поля
     */
    @Nonnull
    public static ResultMatcher fieldValidationFrontError(String fieldName, String fieldError) {
        return ResultMatcher.matchAll(
            jsonPath("errors[0].field").value(fieldName),
            jsonPath("errors[0].defaultMessage").value(fieldError)
        );
    }

    /**
     * Проверяет, что в json'е в поле 'message' содержится ошибка с указанным сообщением.
     *
     * @param message сообщение ошибки
     * @return матчер
     */
    @Nonnull
    public static ResultMatcher errorMessage(String message) {
        return jsonPath("message").value(message);
    }

    @Nonnull
    public static ResultMatcher missingParameter(String parameterName, String parameterType) {
        return errorMessage("Required request parameter '%s' for method parameter type %s is not present".formatted(
            parameterName,
            parameterType
        ));
    }

    @Nonnull
    public static String getValidationTestName(String fieldName, String annotationName) {
        return String.format("%s: %s", fieldName, annotationName);
    }

    /**
     * Создает аргументы для проверки валидации.
     *
     * @param validationInfo тройка аннотация - ожидаемое сообщение - значение, которое устанавливается для проверки
     * @param dtoClass       класс, для которого будет осуществляться проверка
     * @param dtoGenerator   функция для получения объекта проверки
     * @return матчер
     */
    @Nonnull
    public static Stream<Arguments> createArgumentsForValidation(
        Triple<Class<? extends Annotation>, String, Object> validationInfo,
        Class<?> dtoClass,
        BiFunction<Field, Object, Object> dtoGenerator
    ) {
        return Arrays.stream(dtoClass.getDeclaredFields())
            .filter(f -> f.getAnnotation(validationInfo.first) != null
                && (validationInfo.third == null || validationInfo.third.getClass() == f.getType())
            )
            .map(f -> Arguments.of(
                getValidationTestName(f.getName(), validationInfo.first.getSimpleName()),
                f.getName(),
                validationInfo.second,
                dtoGenerator.apply(f, validationInfo.third)
            ));
    }

    @Nonnull
    public static Triple<Class<? extends Annotation>, String, Object> getNotEmptyValidationInfo(
        Collection<?> collection
    ) {
        if (!collection.isEmpty()) {
            throw new UnsupportedOperationException();
        }
        return Triple.of(NotEmpty.class, MUST_NOT_BE_EMPTY, collection);
    }

    @Nonnull
    public static Triple<Class<? extends Annotation>, String, Object> getSizeBetweenValidationInfo(
        int from,
        int to,
        Object value
    ) {
        return Triple.of(Size.class, String.format(SIZE_MUST_BE_BETWEEN, from, to), value);
    }

    @Nonnull
    public static Triple<Class<? extends Annotation>, String, Object> getPositiveValidationInfo(Object value) {
        return Triple.of(Positive.class, MUST_BE_GREATER_THAN_0, value);
    }
}
