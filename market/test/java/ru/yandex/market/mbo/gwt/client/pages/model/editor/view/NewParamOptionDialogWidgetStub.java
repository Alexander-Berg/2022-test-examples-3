package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import com.google.gwt.user.client.ui.Widget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorEventBus;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableValues;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.NewParamOptionDialogWidget;

/**
 * Стаба виджета на добавление новой ENUM-опции с табы SKU. Общий инстанс нужен для того, чтобы из теста удобно было
 * достать тот же виджет, что и менялся в аддонах.
 */
public class NewParamOptionDialogWidgetStub implements NewParamOptionDialogWidget {

    private String stringValue = "";
    protected EditorEventBus bus;
    private static NewParamOptionDialogWidgetStub instance = null;

    private NewParamOptionDialogWidgetStub() { }

    public static NewParamOptionDialogWidgetStub getInstance() {
        return instance == null ? (instance = new NewParamOptionDialogWidgetStub()) : instance;
    }

    @Override
    public void show() {
    }

    @Override
    public void setEditableValues(EditableValues editableValues) {
    }

    @Override
    public void setParamId(Long paramId) {
    }

    @Override
    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getStringValue() {
        return stringValue.trim();
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
        this.bus = bus;
    }

    @Override
    public Widget asWidget() {
        return null;
    }
}
