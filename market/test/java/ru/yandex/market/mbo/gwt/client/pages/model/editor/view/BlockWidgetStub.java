package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.BlockWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamWidget;

import java.util.List;

/**
 * @author gilmulla
 */
public class BlockWidgetStub extends EditorWidgetStub implements BlockWidget {

    private String name;
    private List<ParamWidget<?>> widgets;

    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public List<ParamWidget<?>> getWidgets() {
        return widgets;
    }

    @Override
    public void setWidgets(List<ParamWidget<?>> widgets) {
        this.widgets = widgets;
    }

    @Override
    public void addWidget(ParamWidget<?> paramWidget) {
        widgets.add(paramWidget);
    }
}
