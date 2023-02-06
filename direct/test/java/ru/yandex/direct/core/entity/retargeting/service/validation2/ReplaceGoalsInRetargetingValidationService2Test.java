package ru.yandex.direct.core.entity.retargeting.service.validation2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.container.ReplaceRetargetingConditionGoal;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReplaceGoalsInRetargetingValidationService2Test {

    private static final boolean SKIP_GOAL_EXISTENCE_CHECK_FALSE = false;

    @Autowired
    ReplaceGoalsInRetargetingValidationService2 replaceGoalsInRetargetingValidationService2;

    @Test
    public void success() {
        List<ReplaceRetargetingConditionGoal> replaceRetargetingConditionGoals =
                Collections.singletonList(new ReplaceRetargetingConditionGoal()
                        .withOldGoalId(2L)
                        .withNewGoalId(1L)
                );
        ValidationResult<List<ReplaceRetargetingConditionGoal>, Defect>
                result =
                replaceGoalsInRetargetingValidationService2.validate(replaceRetargetingConditionGoals, singleton(1L),
                        SKIP_GOAL_EXISTENCE_CHECK_FALSE);
        assertThat("Ошибок нет в соответствии с ожиданиями", result, hasNoDefectsDefinitions());
    }

    @Test
    public void notFoundWhenSomeoneElseGoalId() {
        var result = notFoundWhenSomeoneElseGoalIdResult(false);

        assertThat("Ошибка соответствует ожиданиям",
                result,
                hasDefectDefinitionWith(validationError(DefectIds.OBJECT_NOT_FOUND))
        );
    }

    @Test
    public void skipNotFoundWhenSomeoneElseGoalId() {
        var result = notFoundWhenSomeoneElseGoalIdResult(true);

        assertThat("Ошибок нет в соответствии с ожиданиями",
                result,
                hasNoDefectsDefinitions()
        );
    }

    @Test
    public void duplicateObjectsWhenSameOldGoalsIds() {
        List<ReplaceRetargetingConditionGoal> replaceRetargetingConditionGoals =
                Arrays.asList(
                        new ReplaceRetargetingConditionGoal()
                                .withOldGoalId(1L)
                                .withNewGoalId(2L),
                        new ReplaceRetargetingConditionGoal()
                                .withOldGoalId(1L)
                                .withNewGoalId(3L)
                );
        ValidationResult<List<ReplaceRetargetingConditionGoal>, Defect> result =
                replaceGoalsInRetargetingValidationService2.validate(replaceRetargetingConditionGoals, singleton(1L),
                        SKIP_GOAL_EXISTENCE_CHECK_FALSE);
        assertThat("Ошибка соответствует ожиданиям",
                result,
                hasDefectDefinitionWith(validationError(CollectionDefectIds.Gen.MUST_NOT_CONTAIN_DUPLICATED_ELEMENTS)));
    }

    @Test
    public void notFoundNullGoalId() {
        List<ReplaceRetargetingConditionGoal> replaceRetargetingConditionGoals =
                Collections.singletonList(new ReplaceRetargetingConditionGoal()
                        .withOldGoalId(null)
                        .withNewGoalId(2L)
                );
        ValidationResult<List<ReplaceRetargetingConditionGoal>, Defect> result =
                replaceGoalsInRetargetingValidationService2.validate(replaceRetargetingConditionGoals, singleton(1L),
                        SKIP_GOAL_EXISTENCE_CHECK_FALSE);
        assertThat("Ошибка соответствует ожиданиям",
                result,
                hasDefectDefinitionWith(validationError(DefectIds.OBJECT_NOT_FOUND)));
    }


    private ValidationResult<List<ReplaceRetargetingConditionGoal>, Defect> notFoundWhenSomeoneElseGoalIdResult(boolean skipGoalExistenceCheck) {
        List<ReplaceRetargetingConditionGoal> replaceRetargetingConditionGoals =
                Collections.singletonList(new ReplaceRetargetingConditionGoal()
                        .withOldGoalId(1L)
                        .withNewGoalId(2L)
                );
        return replaceGoalsInRetargetingValidationService2.validate(
                replaceRetargetingConditionGoals,
                emptySet(),
                skipGoalExistenceCheck
        );
    }

}
