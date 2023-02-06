package ru.yandex.market.mbo.db.params;

import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.ParameterOverride;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ParameterTestStorage {

    private final Map<Long, CategoryParam> realIdToParam = new HashMap<>();
    private final Map<Long, Set<Long>> breakInheritance = new HashMap<>();
    private final Map<Long, Set<Long>> directInclude = new HashMap<>();

    public Map<Long, CategoryParam> getRealIdToParam() {
        return realIdToParam;
    }

    public Map<Long, Set<Long>> getBreakInheritance() {
        return breakInheritance;
    }

    public Map<Long, Set<Long>> getDirectInclude() {
        return directInclude;
    }

    public CategoryParam getParameter(long categoryId, long paramId) {
        return realIdToParam.values().stream()
            .filter(p -> p.getCategoryHid() == categoryId)
            .filter(p -> getParamId(p) == paramId)
            .findFirst()
            .orElse(null);
    }

    public static long getParamId(CategoryParam categoryParam) {
        if (categoryParam instanceof ParameterOverride) {
            return ((ParameterOverride) categoryParam).getOverridenParameterId();
        }
        return categoryParam.getId();
    }
}
