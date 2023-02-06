package ru.yandex.direct.operation.operationwithid;

import java.util.List;

import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

public class TestableOperationWithId extends AbstractOperationWithId {

    public TestableOperationWithId(Applicability applicability, List<Long> modelIds) {
        super(applicability, modelIds);
    }

    @Override
    protected ValidationResult<List<Long>, Defect> validate(List<Long> modelIds) {
        return null;
    }

    @Override
    protected void execute(List<Long> ids) {
    }
}
