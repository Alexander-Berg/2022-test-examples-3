package ru.yandex.direct.web.entity.retargetinglists.model;

import java.util.Collection;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.model.retargeting.WebGoalType;

@DirectWebTest
@RunWith(Parameterized.class)
public class WebGoalTypeTest {
    @Parameterized.Parameters(name = "Тип Goal: {0}")
    public static Collection<Object[]> data() {
        return StreamEx.of(GoalType.values())
                .map(ruleType -> new Object[]{ruleType})
                .toList();
    }

    @Parameterized.Parameter(0)
    public GoalType type;

    @Test
    public void fromMetrikaRetargetingConditionType() {
        WebGoalType.fromCoreType(type);
    }
}
