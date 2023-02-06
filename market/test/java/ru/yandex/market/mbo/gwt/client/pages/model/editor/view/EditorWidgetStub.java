package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import com.google.gwt.user.client.ui.Widget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorEventBus;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.EditorWidget;

/**
 * @author gilmulla
 *
 */
public class EditorWidgetStub implements EditorWidget {

    private boolean visible;
    protected EditorEventBus bus;

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void setEventBus(EditorEventBus bus) {
        this.bus = bus;
    }

    @Override
    public Widget asWidget() {
        return null;
    }
}
