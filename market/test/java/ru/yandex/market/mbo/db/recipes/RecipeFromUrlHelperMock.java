package ru.yandex.market.mbo.db.recipes;

import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.recipe.url.RecipeFromUrlHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Using for testing.
 * Created by padme on 19.06.17.
 */
class RecipeFromUrlHelperMock extends RecipeFromUrlHelper {

    private Map<Long, Param.Type> types = new HashMap<>();

    @SuppressWarnings("checkstyle:magicNumber")
    RecipeFromUrlHelperMock() {
        types.put(10L, Param.Type.ENUM);
        types.put(20L, Param.Type.BOOLEAN);
        types.put(30L, Param.Type.NUMERIC);
    }

    @Override
    public Param.Type getType(long paramId) {
        if (types.containsKey(paramId)) {
            return types.get(paramId);
        } else {
            throw new IllegalArgumentException("Parameter " + paramId + " not found");
        }
    }
}
