package ru.yandex.market.markup2.utils.cards;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.HashMap;
import java.util.function.Function;

/**
 * @author inenakhov
 */
public class CategoryParamsHelperMock extends CategoryParamsHelper {
    private Function<ModelStorage.Model, String> extractDescrFunc;
    private Function<ModelStorage.Model, String> extractTitleFunc;
    private Function<ModelStorage.Model, String> extractImageUrlFunc;

    public CategoryParamsHelperMock(Function<ModelStorage.Model, String> extractDescrFunc,
                                    Function<ModelStorage.Model, String> extractTitleFunc,
                                    Function<ModelStorage.Model, String> extractImageUrlFunc) {
        super(new HashMap<>(), new Long2ObjectArrayMap());

        this.extractDescrFunc = extractDescrFunc;
        this.extractTitleFunc = extractTitleFunc;
        this.extractImageUrlFunc = extractImageUrlFunc;
    }

    @Override
    public String generateDescription(ModelStorage.Model model) {
        return extractDescrFunc.apply(model);
    }

    @Override
    public String generateTitle(ModelStorage.Model model) {
        return extractTitleFunc.apply(model);
    }

    @Override
    public String extractImageUrl(ModelStorage.Model model) {
        return extractImageUrlFunc.apply(model);
    }
}
