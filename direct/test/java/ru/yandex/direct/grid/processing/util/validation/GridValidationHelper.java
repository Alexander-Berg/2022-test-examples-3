package ru.yandex.direct.grid.processing.util.validation;

import java.util.Arrays;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;

import static java.util.Collections.emptyList;

@ParametersAreNonnullByDefault
public class GridValidationHelper {

    public static GdDefect toGdDefect(Path errorPath, Defect<?> defect) {
        return toGdDefect(errorPath, defect, false);
    }

    public static GdDefect toGdDefect(Path errorPath, Defect<?> defect, boolean convertDefectParamsToMap) {
        Object params = convertDefectParamsToMap
                ? GraphQlJsonUtils.convertValue(defect.params(), Map.class)
                : defect.params();

        return new GdDefect()
                .withPath(errorPath.toString())
                .withCode(defect.defectId().getCode())
                .withParams(params);
    }

    public static GdValidationResult toGdValidationResult(Path errorPath, Defect<?> defect) {
        return toGdValidationResult(errorPath, defect, false);
    }

    public static GdValidationResult toGdValidationResult(Path errorPath, Defect<?> defect,
                                                          boolean convertDefectParamsToMap) {
        return toGdValidationResult(toGdDefect(errorPath, defect, convertDefectParamsToMap));
    }

    public static GdValidationResult toGdValidationResult(GdDefect... errors) {
        return new GdValidationResult()
                .withErrors(Arrays.asList(errors))
                .withWarnings(emptyList());
    }

    public static GdValidationResult emptyValidationResult() {
        return new GdValidationResult()
                .withErrors(emptyList())
                .withWarnings(emptyList());
    }

}
