package ru.yandex.direct.grid.processing.util;

import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetFilter;
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetsContainer;

import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultLimitOffset;
import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultStatRequirements;

public class DynamicTargetTestDataUtils {
    public static GdDynamicAdTargetsContainer getDefaultGdDynamicAdTargetsContainer() {
        return new GdDynamicAdTargetsContainer()
                .withFilter(new GdDynamicAdTargetFilter())
                .withStatRequirements(getDefaultStatRequirements())
                .withLimitOffset(getDefaultLimitOffset());
    }
}
