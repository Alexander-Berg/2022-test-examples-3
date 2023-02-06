package ru.yandex.direct.core.entity.adgroup.service.complex.text.add;

import java.util.Optional;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.text.ComplexTextAdGroupAddOperation;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

public class ComplexTextAddValidationTestBase extends ComplexTextAddTestBase {

    protected final Logger validationErrorLogger = LogManager.getLogger("validationError");

    protected ValidationResult<?, Defect> prepareAndCheckResultIsFailed(ComplexTextAdGroup... complexAdGroups) {
        ComplexTextAdGroupAddOperation addOperation = createOperation(asList(complexAdGroups));
        Optional<MassResult<Long>> result = addOperation.prepare();
        assertTrue(result.isPresent());
        assertThat(result.get(), not(isFullySuccessful()));
        return result.get().getValidationResult();
    }

    protected ValidationResult<?, Defect> prepareAndCheckResultIsSuccess(ComplexTextAdGroup... complexAdGroups) {
        ComplexTextAdGroupAddOperation addOperation = createOperation(asList(complexAdGroups));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        return result.getValidationResult();
    }
}
