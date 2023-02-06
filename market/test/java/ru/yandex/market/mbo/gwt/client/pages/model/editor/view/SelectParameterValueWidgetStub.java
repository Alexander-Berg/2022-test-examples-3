package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.SelectParameterValueWidget;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;

import java.util.Collection;
import java.util.List;

/**
 * @author s-ermakov
 */
public class SelectParameterValueWidgetStub extends EditorWidgetStub implements SelectParameterValueWidget {

    private Collection<CategoryParam> params;
    private Collection<ParameterValue> values;

    @Override
    public Collection<ParameterValue> getValues() {
        return values;
    }

    @Override
    public Collection<CategoryParam> getParams() {
        return params;
    }

    @Override
    public List<ParameterValue> getSelectedValues() {
        return null;
    }

    @Override
    public void addSaveHandlers(Runnable onSaveHandler) {

    }

    @Override
    public void setValues(Collection<ParameterValue> values) {
        this.values = values;
    }

    @Override
    public void setParams(Collection<CategoryParam> params) {
        this.params = params;
    }
}
