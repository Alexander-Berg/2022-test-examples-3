package ru.yandex.direct.core.entity.adgroup.service.validation;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.duplicatedObject;
import static ru.yandex.direct.core.entity.retargeting.Constants.MAX_GOALS_PER_RULE;
import static ru.yandex.direct.core.entity.retargeting.Constants.MAX_RULES_PER_CONDITION;
import static ru.yandex.direct.core.entity.retargeting.Constants.MIN_GOALS_PER_RULE;
import static ru.yandex.direct.core.entity.retargeting.Constants.MIN_RULES_PER_CONDITION;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.AB_SEGMENT_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CONTENT_CATEGORY_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CONTENT_GENRE_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.unsupportedGoalId;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.collectionSizeIsValid;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.emptyPath;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@ParametersAreNonnullByDefault
public class ContentCategoriesRetargetingConditionRulesValidatorTest {

    private final Goal genreGoal = (Goal) new Goal().withId(CONTENT_GENRE_UPPER_BOUND - 1);
    private final Goal contentGoal = (Goal) new Goal().withId(CONTENT_CATEGORY_UPPER_BOUND - 1);

    @Test
    public void validate_ValidRules() {
        var actual = ContentCategoriesRetargetingConditionRulesValidator.validate(List.of(getValidRule()));

        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_InvalidGoalId() {
        var rules = List.of(getValidRule().withGoals(List.of((Goal) new Goal().withId(AB_SEGMENT_UPPER_BOUND))));
        var actual = ContentCategoriesRetargetingConditionRulesValidator.validate(rules);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(Rule.GOALS.name()), index(0), field(Goal.ID.name())), unsupportedGoalId()))));
    }

    @Test
    public void validate_InvalidRuleType() {
        var rules = List.of(getValidRule().withType(RuleType.NOT));
        var actual = ContentCategoriesRetargetingConditionRulesValidator.validate(rules);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(Rule.TYPE.name())), invalidValue()))));
    }

    @Test
    public void validate_DuplicatedGoals() {
        var rules = List.of(getValidRule().withGoals(List.of(genreGoal, genreGoal)));
        var actual = ContentCategoriesRetargetingConditionRulesValidator.validate(rules);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(Rule.GOALS.name()), index(1)), duplicatedObject()))));
    }

    @Test
    public void validate_EmptyRulesList() {
        var actual = ContentCategoriesRetargetingConditionRulesValidator.validate(emptyList());

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(emptyPath(), collectionSizeIsValid(MIN_RULES_PER_CONDITION, MAX_RULES_PER_CONDITION)))));
    }

    @Test
    public void validate_EmptyGoalsList() {
        var rules = List.of(getValidRule().withGoals(emptyList()));
        var actual = ContentCategoriesRetargetingConditionRulesValidator.validate(rules);

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(Rule.GOALS.name())), collectionSizeIsValid(MIN_GOALS_PER_RULE, MAX_GOALS_PER_RULE)))));
    }

    private Rule getValidRule() {
        return new Rule()
                .withGoals(List.of(contentGoal, genreGoal))
                .withType(RuleType.OR);
    }
}
