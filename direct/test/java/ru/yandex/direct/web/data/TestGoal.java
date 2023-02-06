package ru.yandex.direct.web.data;

import java.util.List;

import ru.yandex.direct.web.core.model.retargeting.AbstractGoalWeb;
import ru.yandex.direct.web.core.model.retargeting.RetargetingConditionConverter;

import static java.util.stream.Collectors.toList;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoals;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultMetrikaGoals;

public class TestGoal {
    public static List<AbstractGoalWeb> defaultWebGoals() {
        return defaultGoals().stream().map(RetargetingConditionConverter::fromGoal).collect(toList());
    }

    public static List<AbstractGoalWeb> defaultWebMetrikaGoals() {
        return defaultMetrikaGoals().stream().map(RetargetingConditionConverter::fromGoal).collect(toList());
    }
}
