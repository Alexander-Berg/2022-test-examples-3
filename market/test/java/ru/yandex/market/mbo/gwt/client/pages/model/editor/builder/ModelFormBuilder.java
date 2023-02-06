package ru.yandex.market.mbo.gwt.client.pages.model.editor.builder;

import ru.yandex.market.mbo.gwt.models.forms.model.ModelForm;
import ru.yandex.market.mbo.gwt.models.forms.model.ModelFormTab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * @author gilmulla
 */
public class ModelFormBuilder<FormT> {
    private Function<ModelForm, FormT> callback;
    private ModelForm modelForm = new ModelForm();

    public ModelFormBuilder() {
        this(null);
    }

    public ModelFormBuilder(Function<ModelForm, FormT> callback) {
        this.callback = callback;
    }

    public TabBuilder<FormT> startTab() {
        return startTab(null);
    }
    public TabBuilder<FormT> startTab(String name) {
        return new TabBuilder<>(this)
            .name(name);
    }

    public ModelForm getModelForm() {
        return modelForm;
    }

    public FormT endForm() {
        return this.callback.apply(modelForm);
    }

    public class BlockBuilder<BlockT> {
        private TabBuilder<BlockT> tabBuilder;
        private String name;
        private List<String> properties = new ArrayList<>();

        public BlockBuilder(TabBuilder<BlockT> tabBuilder) {
            this.tabBuilder = tabBuilder;
        }

        public BlockBuilder<BlockT> name(String name) {
            this.name = name;
            return this;
        }

        public BlockBuilder<BlockT> property(String props) {
            this.properties.add(props);
            return this;
        }

        public BlockBuilder<BlockT> properties(String... props) {
            this.properties = Arrays.asList(props);
            return this;
        }

        public BlockBuilder<BlockT> addAllProperties(Collection<String> properties) {
            if (this.properties == null) {
                this.properties = new ArrayList<>();
            }
            this.properties.addAll(properties);
            return this;
        }

        public TabBuilder<BlockT> endBlock() {
            tabBuilder.tab.addBlock(name, properties);
            tabBuilder.blockBuilder = null;
            return tabBuilder;
        }
    }

    public class TabBuilder<TabT> {
        private ModelFormBuilder<TabT> modelFormBuilder;
        private ModelFormTab tab = new ModelFormTab();
        private BlockBuilder<TabT> blockBuilder;

        public TabBuilder(ModelFormBuilder<TabT> modelFormBuilder) {
            this.modelFormBuilder = modelFormBuilder;
        }

        public TabBuilder<TabT> name(String name) {
            tab.setName(name);
            return this;
        }

        public BlockBuilder<TabT> startBlock() {
            return startBlock(null);
        }
        public BlockBuilder<TabT> startBlock(String name) {
            if (blockBuilder != null) {
                throw new IllegalStateException("Another block started");
            }
            return blockBuilder = new BlockBuilder<TabT>(this)
                .name(name);
        }

        public ModelFormBuilder<TabT> endTab() {
            modelForm.addTab(tab);
            return modelFormBuilder;
        }
    }
}
