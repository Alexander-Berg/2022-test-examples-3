package ru.yandex.direct.core.validation.assertj;

import one.util.streamex.StreamEx;
import org.assertj.core.api.Condition;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

public class ValidationResultHasError<T> extends Condition<ValidationResult<T, Defect>> {
    private final Defect definition;

    public ValidationResultHasError(Defect definition) {
        super("Error " + definition);
        this.definition = definition;
    }

    @Override
    public boolean matches(ValidationResult<T, Defect> value) {
        if (value == null) {
            return false;
        }
        return StreamEx.of(value.flattenErrors()).filter(t -> t.getDefect().equals(definition)).count() == 1;
    }
}
