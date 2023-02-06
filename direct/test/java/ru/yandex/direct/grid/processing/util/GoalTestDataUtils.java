package ru.yandex.direct.grid.processing.util;

import ru.yandex.direct.grid.processing.model.goal.GdGoalFilter;
import ru.yandex.direct.grid.processing.model.goal.GdGoalsContainer;

public class GoalTestDataUtils {
    public static GdGoalsContainer getDefaultGdGoalsContainerInput() {
        return new GdGoalsContainer()
                .withFilter(new GdGoalFilter());
    }
}
