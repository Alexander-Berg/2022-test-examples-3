package ru.yandex.market.logistics.nesu.admin.utils;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;

import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;

/**
 * Утилитный класс для работы с ошибками валидации при обращении к контроллерам админки.
 */
@UtilityClass
@ParametersAreNonnullByDefault
public class AdminValidationUtils {
    @Nonnull
    public ValidationErrorData createNullFieldError(String field, String objectName) {
        return fieldError(field, "Обязательно для заполнения", objectName, "NotNull");
    }
}
