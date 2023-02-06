package ru.yandex.direct.operation.add;

import java.util.List;
import java.util.Map;

import ru.yandex.direct.model.ModelWithId;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

public class TestableAddOperation<M extends ModelWithId> extends AbstractAddOperation<M, Long> {
    public TestableAddOperation(Applicability applicability, List<M> models) {
        super(applicability, models);
    }

    @Override
    protected void validate(ValidationResult<List<M>, Defect> preValidationResult) {
    }

    @Override
    protected Map<Integer, Long> execute(Map<Integer, M> validModelsMapToApply) {
        return null;
    }
}
