package ru.yandex.direct.core.entity.retargeting.converter;

import java.util.Collection;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.metrika.client.model.request.RetargetingGoalGroup;

@RunWith(Parameterized.class)
public class RuleTypeConverterTest {
    @Parameterized.Parameters(name = "Тип Rule: {0}")
    public static Collection<Object[]> data() {
        return StreamEx.of(RuleType.values())
                .map(ruleType -> new Object[]{ruleType})
                .toList();
    }


    @Parameterized.Parameter(0)
    public RuleType ruleType;

    @Test
    public void toMetrikaGoalGroupType() {
        RuleTypeConverter.toSource(ruleType);
        RetargetingGoalGroup.Type.valueOf(ruleType.name());
    }
}
