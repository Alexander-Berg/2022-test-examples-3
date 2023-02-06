package ru.yandex.direct.core.validation.assertj;

import org.assertj.core.api.Condition;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

/**
 * todo javadoc
 */
public class ValidationResultConditions {
    public static <T> Condition<ValidationResult<T, Defect>> error(Defect<?> defect) {
        return new ValidationResultHasError<>(defect);
    }

    public static <T> Condition<ValidationResult<T, Defect>> warning(Defect defect) {
        return new ValidationResultHasWarning<>(defect);
    }

    public static <T> Condition<ValidationResult<T, Defect>> defect(Defect defect) {
        return new ValidationResultHasDefect<>(defect);
    }

}
