package ru.yandex.direct.ess.router.testutils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.core.entity.recommendation.RecommendationType;
import ru.yandex.direct.ess.logicobjects.recomtracer.RecomTracerLogicObject;

import static ru.yandex.direct.binlog.model.Operation.DELETE;
import static ru.yandex.direct.binlog.model.Operation.INSERT;

public class TestUtils {
    public static void fillChangedInRow(Map<String, Object> before,
                                        Map<String, Object> after,
                                        BaseTableChange changeToFill,
                                        Operation operation) {
        fillChangedInRow(before, after, changeToFill.getChangedColumns(), operation);
    }

    public static void fillChangedInRow(Map<String, Object> before,
                                        Map<String, Object> after,
                                        Map<String, ColumnChange> changesToFill,
                                        Operation operation) {
        for (Map.Entry<String, ColumnChange> columnChangeEntry : changesToFill.entrySet()) {
            if (!operation.equals(INSERT)) {
                before.put(columnChangeEntry.getKey(), columnChangeEntry.getValue().before);
            }
            if (!operation.equals(DELETE)) {
                after.put(columnChangeEntry.getKey(), columnChangeEntry.getValue().after);
            }
        }
    }

    public static List<RecommendationType> getRecommendationsTypes(List<RecomTracerLogicObject> recomTracerObjects) {
        return recomTracerObjects.stream()
                .map(RecomTracerLogicObject::getRecommendationTypeId)
                .map(RecommendationType::fromId)
                .collect(Collectors.toList());
    }


}
