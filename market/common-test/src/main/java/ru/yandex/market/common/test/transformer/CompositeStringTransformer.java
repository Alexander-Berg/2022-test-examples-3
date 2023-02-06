package ru.yandex.market.common.test.transformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.yandex.market.common.test.transformer.StringTransformer;

/**
 * Преобразует один sql-диалект в другой sql-диалект.
 *
 * @author zoom
 */
public abstract class CompositeStringTransformer implements StringTransformer {

    private List<StringTransformer> transformers;

    public CompositeStringTransformer() {
        List<StringTransformer> stringTransformers = new ArrayList<>();
        customizeTransformers(stringTransformers);
        this.transformers = Collections.unmodifiableList(stringTransformers);
    }

    protected void customizeTransformers(List<StringTransformer> transformers) {
    }

    @Override
    public String transform(String string) {
        for (StringTransformer transformer : transformers) {
            string = transformer.transform(string);
        }
        return string;
    }

}
