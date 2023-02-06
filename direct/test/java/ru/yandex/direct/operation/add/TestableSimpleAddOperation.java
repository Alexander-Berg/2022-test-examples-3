package ru.yandex.direct.operation.add;

import java.util.List;

import ru.yandex.direct.model.ModelWithId;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

public class TestableSimpleAddOperation<M extends ModelWithId> extends SimpleAbstractAddOperation<M, Long> {

    public TestableSimpleAddOperation(Applicability applicability, List<M> models) {
        super(applicability, models);
    }

    @Override
    protected void validate(ValidationResult<List<M>, Defect> preValidationResult) {
    }

    @Override
    protected List<Long> execute(List<M> validModelsToApply) {
        return null;
    }
}
