package ru.yandex.direct.web.entity.retargetinglists.service;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.configuration.DirectWebTest;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetRetCondWithGoalsValidationServiceTest {
    public static final long DEFAULT_RET_COND_ID = 1L;

    @Autowired
    private GetRetCondWithGoalsValidationService getRetCondWithGoalsValidationService;

    @Test
    public void validateNullRetCondId() {
        List<Long> retCondIds = Collections.singletonList(null);
        ValidationResult<List<Long>, Defect>
                vr = getRetCondWithGoalsValidationService.validate(retCondIds);
        assertThat("ошибка соответсвует ожиданиям", vr,
                hasDefectDefinitionWith(validationError(DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void validateCorrectRetCondList() {
        List<Long> retCondIds = Collections
                .singletonList(DEFAULT_RET_COND_ID);
        ValidationResult<List<Long>, Defect>
                vr = getRetCondWithGoalsValidationService.validate(retCondIds);
        assertThat("ошибка отсутствует", vr, hasNoDefectsDefinitions());
    }
}
