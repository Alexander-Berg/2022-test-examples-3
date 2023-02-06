package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import com.google.gwt.user.client.ui.Widget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.CardApiModelWidget;

import java.util.function.Consumer;

public class CardApiModelWidgetStub implements CardApiModelWidget {
    @Override
    public void setRequestText(String text) {
    }

    @Override
    public void setResponseText(String text) {

    }

    @Override
    public void center() {
    }

    @Override
    public void show() {
    }

    @Override
    public void setAutoHideEnabled(boolean autoHideEnabled) {
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public void setVisible(boolean visible) {
    }

    @Override
    public Widget asWidget() {
        return null;
    }

    @Override
    public void addButton(String title, Consumer<String> widgetTextAction) {
    }

    @Override
    public void setLoading(boolean isLoading) {

    }

    @Override
    public void setStatusMessage(String text) {

    }
}
