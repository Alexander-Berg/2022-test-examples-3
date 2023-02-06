package ru.yandex.direct.core.entity.adgroup.service.complex.text.update;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupUpdateOperation;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

public class ComplexAdGroupUpdateOperationValidationTestBase extends ComplexAdGroupUpdateOperationTestBase {

    protected Long adGroupId;

    protected final Logger validationErrorLogger = LogManager.getLogger("validationError");

    @Before
    public void before() {
        super.before();
        adGroupId = adGroupInfo1.getAdGroupId();
    }

    protected ValidationResult<?, Defect> updateAndCheckResultIsFailed(ComplexTextAdGroup... complexAdGroups) {
        ComplexAdGroupUpdateOperation updateOperation = createOperation(asList(complexAdGroups));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, not(isFullySuccessful()));
        return result.getValidationResult();
    }

    protected ValidationResult<?, Defect> updateAndCheckResultIsSuccess(ComplexTextAdGroup... complexAdGroups) {
        ComplexAdGroupUpdateOperation updateOperation = createOperation(asList(complexAdGroups));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        return result.getValidationResult();
    }
}
