package ru.yandex.market.mbo.gwt.client.pages.model.editor.view.view.model_parameter_relation;

import com.google.gwt.user.client.ui.Widget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorEventBus;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.EditDependentOptionsWidget;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.DependentOptionsDto;
import ru.yandex.market.mbo.gwt.models.params.Option;

import java.util.Collection;
import java.util.List;

public class EditDependentOptionsWidgetStub implements EditDependentOptionsWidget {

    @Override
    public void setMasterParam(CategoryParam masterParam) {

    }

    @Override
    public void setMasterValue(Option masterValue) {

    }

    @Override
    public void setLinkedParams(Collection<CategoryParam> linkedParams) {

    }

    @Override
    public void setMediatorParamId(Long mediatorParamId) {

    }

    @Override
    public void setMediatorValueId(Long mediatorValueId) {

    }

    @Override
    public void setDependentOptions(List<DependentOptionsDto> options) {

    }

    @Override
    public void show() {

    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public void setVisible(boolean visible) {

    }

    @Override
    public void setEventBus(EditorEventBus bus) {

    }

    @Override
    public Widget asWidget() {
        return null;
    }
}
