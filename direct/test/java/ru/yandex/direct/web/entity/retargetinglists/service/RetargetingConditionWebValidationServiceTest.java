package ru.yandex.direct.web.entity.retargetinglists.service;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.retargeting.Condition;
import ru.yandex.direct.web.core.model.retargeting.MetrikaGoalWeb;
import ru.yandex.direct.web.core.model.retargeting.RetargetingConditionWeb;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingConditionWebValidationServiceTest {
    private static final Long DEFAULT_GOAL_ID = 1L;

    @Autowired
    private RetargetingConditionWebValidationService retargetingConditionWebValidationService;

    @Test
    public void validateNullRetargetingCondition() {
        ValidationResult<RetargetingConditionWeb, Defect> vr =
                retargetingConditionWebValidationService.validate(null);
        assertThat("ошибка соответсвует ожиданиям", vr,
                hasDefectDefinitionWith(validationError(DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void validateNullRetCondRule() {
        RetargetingConditionWeb retargetingConditionWeb = new RetargetingConditionWeb();
        retargetingConditionWeb.setConditions(Collections.singletonList(null));

        ValidationResult<RetargetingConditionWeb, Defect> vr =
                retargetingConditionWebValidationService.validate(retargetingConditionWeb);
        assertThat("ошибка соответсвует ожиданиям", vr,
                hasDefectDefinitionWith(validationError(DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void validateNullGoal() {
        RetargetingConditionWeb retargetingConditionWeb = new RetargetingConditionWeb();
        retargetingConditionWeb.setConditions(Collections.singletonList(new Condition().withConditionGoalWebs(null)));
        ValidationResult<RetargetingConditionWeb, Defect>
                vr = retargetingConditionWebValidationService.validate(retargetingConditionWeb);
        assertThat("ошибка соответсвует ожиданиям", vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateNullGoalId() {
        RetargetingConditionWeb retargetingConditionWeb = new RetargetingConditionWeb();
        retargetingConditionWeb.setConditions(Collections.singletonList(
                new Condition().withConditionGoalWebs(Collections.singletonList(new MetrikaGoalWeb().withId(null)))));
        ValidationResult<RetargetingConditionWeb, Defect>
                vr = retargetingConditionWebValidationService.validate(retargetingConditionWeb);
        assertThat("ошибка соответсвует ожиданиям", vr,
                hasDefectDefinitionWith(validationError(DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void validateCorrectRetargetingCondition() {
        RetargetingConditionWeb retargetingConditionWeb = new RetargetingConditionWeb();
        retargetingConditionWeb.setConditions(Collections.singletonList(
                new Condition()
                        .withConditionGoalWebs(Collections.singletonList(new MetrikaGoalWeb().withId(DEFAULT_GOAL_ID)))));
        ValidationResult<RetargetingConditionWeb, Defect>
                vr = retargetingConditionWebValidationService.validate(retargetingConditionWeb);
        assertThat("ошибка отсутствует", vr, hasNoDefectsDefinitions());
    }
}
