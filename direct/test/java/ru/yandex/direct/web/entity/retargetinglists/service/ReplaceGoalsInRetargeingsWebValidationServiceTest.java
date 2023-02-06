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
import ru.yandex.direct.web.entity.retargetinglists.model.ReplaceGoal;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReplaceGoalsInRetargeingsWebValidationServiceTest {
    public static final long NEW_GOAL_ID = 1L;
    public static final long OLD_GOAL_ID = 2L;
    @Autowired
    private ReplaceGoalsInRetargetingsWebValidationService replaceGoalsInRetargetingsWebValidationService;

    @Test
    public void validateNullReplaceGoal() {
        List<ReplaceGoal> replaceGoalList = Collections.singletonList(null);
        ValidationResult<List<ReplaceGoal>, Defect>
                vr = replaceGoalsInRetargetingsWebValidationService.validate(replaceGoalList);
        assertThat("ошибка соответсвует ожиданиям", vr,
                hasDefectDefinitionWith(validationError(CollectionDefectIds.Gen.CANNOT_CONTAIN_NULLS)));
    }

    @Test
    public void validateNullReplaceGoalList() {
        List<ReplaceGoal> replaceGoalList = null;
        ValidationResult<List<ReplaceGoal>, Defect>
                vr = replaceGoalsInRetargetingsWebValidationService.validate(replaceGoalList);
        assertThat("ошибка соответсвует ожиданиям", vr,
                hasDefectDefinitionWith(validationError(DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void validateCorrectReplaceGoalList() {
        List<ReplaceGoal> replaceGoalList = Collections
                .singletonList(new ReplaceGoal().withNewGoalId(NEW_GOAL_ID).withOldGoalId(OLD_GOAL_ID));
        ValidationResult<List<ReplaceGoal>, Defect>
                vr = replaceGoalsInRetargetingsWebValidationService.validate(replaceGoalList);
        assertThat("ошибка отсутствует", vr, hasNoDefectsDefinitions());
    }
}
