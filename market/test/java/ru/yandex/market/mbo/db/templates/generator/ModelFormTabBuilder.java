package ru.yandex.market.mbo.db.templates.generator;

import ru.yandex.market.mbo.gwt.models.forms.model.ModelFormTab;
import ru.yandex.market.mbo.gwt.utils.NestedBuilder;

import java.util.function.Function;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 10.07.2019
 */
public class ModelFormTabBuilder<Parent> extends NestedBuilder<Parent, ModelFormTab> {

    private final ModelFormTab tab = new ModelFormTab();

    private ModelFormTabBuilder(Function<ModelFormTab, Parent> toParent) {
        super(toParent);
    }

    public static <Parent> ModelFormTabBuilder<Parent> create(Function<ModelFormTab, Parent> toParent) {
        return new ModelFormTabBuilder<>(toParent);
    }

    public static ModelFormTabBuilder<ModelFormTab> create() {
        return new ModelFormTabBuilder<>(Function.identity());
    }

    public ModelFormBlockBuilder<ModelFormTabBuilder<Parent>> startBlock(String blockName) {
        return startBlock().name(blockName);
    }

    public ModelFormBlockBuilder<ModelFormTabBuilder<Parent>> startBlock() {
        return ModelFormBlockBuilder.create(block -> {
            tab.addBlock(block);
            return this;
        });
    }

    @Override
    protected ModelFormTab buildObject() {
        return tab;
    }

    public Parent endTab() {
        return end();
    }

    public ModelFormTabBuilder<Parent> name(String name) {
        tab.setName(name);
        return this;
    }
}
