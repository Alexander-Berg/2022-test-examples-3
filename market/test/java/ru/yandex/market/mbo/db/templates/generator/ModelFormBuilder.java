package ru.yandex.market.mbo.db.templates.generator;

import ru.yandex.market.mbo.gwt.models.forms.model.ModelForm;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 10.07.2019
 */
public class ModelFormBuilder {
    private final ModelForm form = new ModelForm();

    public static ModelFormBuilder create() {
        return new ModelFormBuilder();
    }

    public ModelFormTabBuilder<ModelFormBuilder> startTab() {
        return ModelFormTabBuilder.create(tab -> {
            form.addTab(tab);
            return this;
        });
    }

    public ModelFormTabBuilder<ModelFormBuilder> startTab(String name) {
        return startTab().name(name);
    }

    public ModelForm endForm() {
        return form;
    }
}
