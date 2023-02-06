package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.enumaliases.EnumValueAliasesChangedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.EnumValueAliasesEditor;
import ru.yandex.market.mbo.gwt.models.modelstorage.EnumValueAlias;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;

import java.util.List;

/**
 * @author s-ermakov
 */
public class EnumValueAliasesEditorStub extends EditorWidgetStub implements EnumValueAliasesEditor {

    private CategoryParam param;
    private List<Option> availableOptions;
    private ParameterValue parameterValue;
    private List<EnumValueAlias> enumValueAliases;

    @Override
    public CategoryParam getCategoryParam() {
        return param;
    }

    @Override
    public void setCategoryParam(CategoryParam param) {
        this.param = param;
    }

    @Override
    public List<Option> getAvailableOptions() {
        return availableOptions;
    }

    @Override
    public void setAvailableOptions(List<Option> options) {
        this.availableOptions = options;
    }

    @Override
    public ParameterValue getParameterValue() {
        return parameterValue;
    }

    @Override
    public void setParameterValue(ParameterValue parameterValue) {
        this.parameterValue = parameterValue;
    }

    @Override
    public List<EnumValueAlias> getAliases() {
        return enumValueAliases;
    }

    @Override
    public void setAliases(List<EnumValueAlias> enumValueAliases) {
        this.enumValueAliases = enumValueAliases;
    }

    public void fakeSaveClick() {
        bus.fireEventSync(new EnumValueAliasesChangedEvent(parameterValue, getAliases()));
    }
}
