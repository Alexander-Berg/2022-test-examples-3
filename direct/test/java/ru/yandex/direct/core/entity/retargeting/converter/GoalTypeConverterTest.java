package ru.yandex.direct.core.entity.retargeting.converter;

import java.util.Collection;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.metrika.client.model.response.RetargetingCondition;

@RunWith(Parameterized.class)
public class GoalTypeConverterTest {
    @Parameterized.Parameter(0)
    public RetargetingCondition.Type type;

    @Parameterized.Parameters(name = "Тип Goal: {0}")
    public static Collection<Object[]> data() {
        return StreamEx.of(RetargetingCondition.Type.values())
                .filter(t -> t != RetargetingCondition.Type.UNKNOWN)
                .map(ruleType -> new Object[]{ruleType})
                .toList();
    }

    @Test
    public void fromMetrikaRetargetingConditionType() {
        GoalTypeConverter.fromSource(type);
    }

}
