package ru.yandex.direct.grid.processing.util;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.grid.model.GdStatPreset;
import ru.yandex.direct.grid.model.GdStatRequirements;
import ru.yandex.direct.grid.model.findandreplace.SearchOptions;
import ru.yandex.direct.grid.processing.model.GdLimitOffset;

@ParametersAreNonnullByDefault
public class InputTestDataUtils {

    public static GdStatRequirements getDefaultStatRequirements() {
        return new GdStatRequirements()
                .withPreset(GdStatPreset.LAST_30DAYS);
    }

    public static GdLimitOffset getDefaultLimitOffset() {
        return new GdLimitOffset()
                .withLimit(100)
                .withOffset(0);
    }

    public static SearchOptions searchOptions(boolean isMatchCase, boolean isOnlyWholeWords) {
        return new SearchOptions()
                .withMatchCase(isMatchCase)
                .withOnlyWholeWords(isOnlyWholeWords);
    }
}
