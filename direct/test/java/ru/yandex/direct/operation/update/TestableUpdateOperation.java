package ru.yandex.direct.operation.update;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import one.util.streamex.EntryStream;

import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.operation.testing.entity.RetargetingCondition;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

public class TestableUpdateOperation extends AbstractUpdateOperation<RetargetingCondition, Long> {

    public TestableUpdateOperation(Applicability applicability,
                                   List<ModelChanges<RetargetingCondition>> modelChanges,
                                   Function<Long, RetargetingCondition> modelStubCreator) {
        super(applicability, modelChanges, modelStubCreator);
    }

    @Override
    protected ValidationResult<List<ModelChanges<RetargetingCondition>>, Defect> validateModelChanges(
            List<ModelChanges<RetargetingCondition>> modelChanges) {
        return null;
    }

    @Override
    protected Collection<RetargetingCondition> getModels(Collection<Long> ids) {
        return null;
    }

    @Override
    protected ValidationResult<List<RetargetingCondition>, Defect> validateAppliedChanges(
            ValidationResult<List<RetargetingCondition>, Defect> validationResult) {
        return null;
    }

    @Override
    protected Map<Integer, Long> execute(ExecutionStep<RetargetingCondition> executionStep) {
        return EntryStream.of(executionStep.getAppliedChangesForExecutionWithIndex())
                .mapValues(ac -> 0L)
                .toMap();
    }
}
