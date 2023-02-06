package ru.yandex.direct.core.entity.retargeting.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.retargeting.model.RuleType.ALL;
import static ru.yandex.direct.core.entity.retargeting.model.RuleType.NOT;
import static ru.yandex.direct.core.entity.retargeting.model.RuleType.OR;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@RunWith(Parameterized.class)
public class RetargetingConditionGetNegativeTest {
    @Parameterized.Parameters(name = "{2}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {Collections.singletonList(ALL), false, "1 положительное правило"},
                {Arrays.asList(ALL, OR), false, "2 положительных правила"},
                {Arrays.asList(ALL, NOT), false, "1 положительное правило и 1 not"},
                {Arrays.asList(NOT, ALL), false, "1 правило not и 1 положительное"},
                {Collections.emptyList(), false, "0 правил"},

                {Collections.singletonList(NOT), true, "1 правило not"},
                {Arrays.asList(NOT, NOT), true, "2 правила not"},
        });
    }

    @Parameterized.Parameter(0)
    public List<RuleType> ruleTypes;

    @Parameterized.Parameter(1)
    public boolean expectedNegative;

    @Parameterized.Parameter(2)
    public String desc;

    @Test
    public void getNegative_NoNegativeRules_False() {
        RetargetingCondition retargetingCondition = new RetargetingCondition();
        retargetingCondition.setRules(mapList(ruleTypes,
                ruleType -> {
                    Rule rule = new Rule();
                    rule.withType(ruleType);
                    return rule;
                }));
        assertThat(retargetingCondition.getNegative(), equalTo(expectedNegative));
    }
}
