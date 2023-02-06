package ru.yandex.direct.operation.operationwithid;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;

import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectId;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class BaseAbstractOperationWithIdTest {

    protected List<Long> ids = new ArrayList<>();
    protected ValidationResult<List<Long>, Defect> validationResult = new ValidationResult<>(ids);

    protected Long retargetingId1;
    protected Long retargetingId2;

    protected TestableOperationWithId createOperation(Applicability applicability) {
        TestableOperationWithId operation = new TestableOperationWithId(applicability, ids);

        TestableOperationWithId mockedOperation = spy(operation);

        when(mockedOperation.validate(any())).thenReturn(validationResult);

        return mockedOperation;
    }

    protected void oneValidId() {
        oneId();
        addValidResultForId(validationResult, retargetingId1, 0);
    }

    protected void twoValidIds() {
        twoIds();
        addValidResultForId(validationResult, retargetingId1, 0);
        addValidResultForId(validationResult, retargetingId2, 1);
    }

    protected void oneInvalidIdOnValidation() {
        long invalidId = -1L;
        addResultWithErrorForId(validationResult, invalidId, 0);
    }

    protected void twoInvalidIdsOnValidation() {
        long invalidId1 = -1L;
        long invalidId2 = -2L;
        addResultWithErrorForId(validationResult, invalidId1, 0);
        addResultWithErrorForId(validationResult, invalidId2, 1);
    }

    protected void oneValidIdAndOneInvalidIdOnValidation() {
        oneValidId();

        retargetingId2 = 2L;
        ids.add(retargetingId2);
        addResultWithErrorForId(validationResult, retargetingId2, 1);
    }

    protected void firstWithErrorSecondValid() {
        twoIds();
        addResultWithErrorForId(validationResult, retargetingId1, 0);
        addValidResultForId(validationResult, retargetingId2, 1);
    }

    protected void oneIdWithError() {
        oneId();
        addResultWithErrorForId(validationResult, retargetingId1, 0);
    }

    protected void oneIdWithWarning() {
        oneId();
        addResultWithWarningId(validationResult, retargetingId1, 0);
    }

    private void oneId() {
        retargetingId1 = 1L;
        ids.add(retargetingId1);
    }

    private void twoIds() {
        retargetingId1 = 1L;
        retargetingId2 = 2L;

        ids.add(retargetingId1);
        ids.add(retargetingId2);
    }

    protected void addValidResultForId(ValidationResult<List<Long>, Defect> vr, Long id, int index) {
        vr.getOrCreateSubValidationResult(index(index), id);
    }

    protected void addResultWithErrorForId(ValidationResult<List<Long>, Defect> vr,
                                           Long id, int index) {
        ValidationResult<Long, Defect> idValidationResult =
                vr.getOrCreateSubValidationResult(index(index), id);
        idValidationResult.addError(new Defect<>(DefectIds.INVALID_VALUE));
    }

    protected void addResultWithWarningId(ValidationResult<List<Long>, Defect> vr,
                                          Long id, int index) {
        ValidationResult<Long, Defect> idValidationResult =
                vr.getOrCreateSubValidationResult(index(index), id);
        idValidationResult.addWarning(new Defect<>(new DefectId<Void>() {
            @Override
            public String getCode() {
                return "WARNING_DUPLICATED_RETARGETING_ID";
            }
        }));
    }

    protected static <T> Matcher<ValidationResult<T, Defect>> errorMatcher(int idIndex) {
        return hasDefectDefinitionWith(validationError(path(index(idIndex)), new Defect<>(DefectIds.INVALID_VALUE)));
    }
}
