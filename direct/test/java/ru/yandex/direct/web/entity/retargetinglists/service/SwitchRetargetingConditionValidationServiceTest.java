package ru.yandex.direct.web.entity.retargetinglists.service;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.validation.defect.ids.CollectionDefectIds;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.retargetinglists.model.SwitchRetargetingWeb;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SwitchRetargetingConditionValidationServiceTest {
    public static final long DEFAULT_RET_COND_ID = 1L;
    @Autowired
    private SwitchRetargetingConditionValidationService switchRetargetingConditionValidationService;

    @Test
    public void validateNullSwitchRetargetingWeb() {
        List<SwitchRetargetingWeb> replaceGoalList = Collections.singletonList(null);
        ValidationResult<List<SwitchRetargetingWeb>, Defect>
                vr = switchRetargetingConditionValidationService.validate(replaceGoalList);
        assertThat("ошибка соответсвует ожиданиям", vr,
                hasDefectDefinitionWith(validationError(CollectionDefectIds.Gen.CANNOT_CONTAIN_NULLS)));
    }

    @Test
    public void validateNullSwitchRetargetingWebList() {
        List<SwitchRetargetingWeb> replaceGoalList = null;
        ValidationResult<List<SwitchRetargetingWeb>, Defect>
                vr = switchRetargetingConditionValidationService.validate(replaceGoalList);
        assertThat("ошибка соответсвует ожиданиям", vr,
                hasDefectDefinitionWith(validationError(DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void validateCorrectSwitchRetargetingWebList() {
        List<SwitchRetargetingWeb> replaceGoalList = Collections
                .singletonList(new SwitchRetargetingWeb().withRetCondId(DEFAULT_RET_COND_ID).withSuspended(true));
        ValidationResult<List<SwitchRetargetingWeb>, Defect>
                vr = switchRetargetingConditionValidationService.validate(replaceGoalList);
        assertThat("ошибка отсутствует", vr, hasNoDefectsDefinitions());
    }
}
