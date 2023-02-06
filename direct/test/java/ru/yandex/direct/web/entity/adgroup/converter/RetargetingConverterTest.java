package ru.yandex.direct.web.entity.adgroup.converter;

import java.lang.reflect.Field;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.reflections.ReflectionUtils;

import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingFilter;
import ru.yandex.direct.web.core.model.retargeting.CryptaInterestTypeWeb;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroupRetargeting;
import ru.yandex.direct.web.entity.adgroup.model.WebRetargetingGoal;
import ru.yandex.direct.web.entity.adgroup.model.WebRetargetingRule;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

public class RetargetingConverterTest {
    @Test
    public void convertWebCpmRetargetingToCoreRetargetingCondition() {
        WebRetargetingGoal webRetargetingGoal = new WebRetargetingGoal()
                .withId(123L)
                .withGoalType(GoalType.GOAL)
                .withTime(90);
        WebRetargetingRule webRetargetingRule = new WebRetargetingRule()
                .withInterestType(CryptaInterestTypeWeb.all)
                .withRuleType(RuleType.OR)
                .withGoals(singletonList(webRetargetingGoal));
        WebCpmAdGroupRetargeting retargeting = new WebCpmAdGroupRetargeting()
                .withId(1L)
                .withRetargetingConditionId(2L)
                .withPriceContext(50.0)
                .withName("condition name")
                .withDescription("condition description")
                .withConditionType(ConditionType.interests)
                .withGroups(singletonList(webRetargetingRule));

        RetargetingCondition expected = (RetargetingCondition) new RetargetingCondition()
                .withId(retargeting.getRetargetingConditionId())
                .withName(retargeting.getName())
                .withDescription(retargeting.getDescription())
                .withType(retargeting.getConditionType())
                .withRules(singletonList(new Rule()
                        .withInterestType(webRetargetingRule.getInterestType().toCoreType())
                        .withType(webRetargetingRule.getRuleType())
                        .withGoals(singletonList((Goal) new Goal()
                                .withId(webRetargetingGoal.getId())
                                .withType(webRetargetingGoal.getGoalType())
                                .withTime(webRetargetingGoal.getTime())))));

        RetargetingCondition retargetingCondition =
                RetargetingConverter.webCpmRetargetingToCoreRetargetingCondition(retargeting);

        assertThat(retargetingCondition, beanDiffer(expected));
    }

    @Test
    public void convertWebCpmRetargetingToCoreRetargeting() {
        WebCpmAdGroupRetargeting retargeting = new WebCpmAdGroupRetargeting()
                .withId(1L)
                .withRetargetingConditionId(2L)
                .withPriceContext(50.0);

        TargetInterest expected = new TargetInterest()
                .withId(retargeting.getId())
                .withRetargetingConditionId(retargeting.getRetargetingConditionId());

        TargetInterest targetInterest = RetargetingConverter.webCpmRetargetingToCore(retargeting);

        assertThat(targetInterest, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    /**
     * Проверяем что в модель GdRetargetingFilter не было добавленно новых полей
     * <p>
     * Если были измененны поля и по ним происходит фильтрация условий показа в коде, то нужно учесть их в методе
     * {@link ru.yandex.direct.grid.processing.service.showcondition.converter.RetargetingConverter#hasAnyCodeFilter}
     */
    @Test
    public void testForNewFilterFields() {
        String[] expectFieldsName = {"retargetingIdIn", "retargetingIdNotIn", "retargetingConditionIdIn",
                "retargetingConditionIdNotIn", "campaignIdIn", "adGroupIdIn", "nameContains", "nameNotContains",
                "statusIn", "typeIn", "typeNotIn", "minPriceContext", "maxPriceContext", "stats", "interest",
                "reasonsContainSome"};

        @SuppressWarnings("unchecked")
        Set<Field> fields = ReflectionUtils.getAllFields(GdRetargetingFilter.class);

        Set<String> actualFieldsName = StreamEx.of(fields)
                .map(Field::getName)
                .filter(fieldName -> !Character.isUpperCase(fieldName.charAt(0)))
                .toSet();

        assertThat(actualFieldsName)
                .as("Нет новых полей в GdShowConditionFilter")
                .containsExactlyInAnyOrder(expectFieldsName);
    }
}
