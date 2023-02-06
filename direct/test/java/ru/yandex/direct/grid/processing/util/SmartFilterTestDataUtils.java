package ru.yandex.direct.grid.processing.util;

import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterFilter;
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFiltersContainer;

import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultLimitOffset;
import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultStatRequirements;

public class SmartFilterTestDataUtils {
    public static GdSmartFiltersContainer getDefaultGdSmartFiltersContainer() {
        return new GdSmartFiltersContainer()
                .withFilter(new GdSmartFilterFilter())
                .withStatRequirements(getDefaultStatRequirements())
                .withLimitOffset(getDefaultLimitOffset());
    }
}
