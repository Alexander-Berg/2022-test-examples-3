package ru.yandex.market.mbo.db.templates.generator;

import ru.yandex.market.mbo.gwt.models.forms.model.ModelFormBlock;
import ru.yandex.market.mbo.gwt.utils.NestedBuilder;

import java.util.function.Function;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 10.07.2019
 */
public class ModelFormBlockBuilder<Parent> extends NestedBuilder<Parent, ModelFormBlock> {

    private final ModelFormBlock block = new ModelFormBlock();

    private ModelFormBlockBuilder(Function<ModelFormBlock, Parent> toParent) {
        super(toParent);
    }

    public static ModelFormBlockBuilder<ModelFormBlock> create() {
        return new ModelFormBlockBuilder<>(Function.identity());
    }

    public static <Parent> ModelFormBlockBuilder<Parent> create(Function<ModelFormBlock, Parent> toParent) {
        return new ModelFormBlockBuilder<>(toParent);
    }

    public ModelFormBlockBuilder<Parent> property(String xslName) {
        block.addProperty(xslName);
        return this;
    }

    @Override
    protected ModelFormBlock buildObject() {
        return block;
    }

    public Parent endBlock() {
        return end();
    }

    public ModelFormBlockBuilder<Parent> name(String blockName) {
        block.setName(blockName);
        return this;
    }
}
