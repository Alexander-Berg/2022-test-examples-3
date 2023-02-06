package ru.yandex.direct.core.validation.assertj;

import java.util.List;

import one.util.streamex.StreamEx;
import org.assertj.core.api.Condition;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;

/**
 * todo javadoc
 */
public class ValidationResultHasDefect<T> extends Condition<ValidationResult<T, Defect>> {

    private final Defect definition;

    public ValidationResultHasDefect(Defect definition) {
        super("Defect " + definition);
        this.definition = definition;
    }

    @Override
    public boolean matches(ValidationResult<T, Defect> value) {
        if (value == null) {
            return false;
        }
        List<DefectInfo<Defect>> defects = value.flattenWarnings();
        defects.addAll(value.flattenErrors());
        return StreamEx.of(defects).filter(t -> t.getDefect().equals(definition)).count() == 1;
    }
}
