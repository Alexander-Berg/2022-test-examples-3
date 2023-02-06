package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorEventBus;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.eventbus.events.ForceSaveTestEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ErrorsWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ForceSaveWidget;

/**
 * @author gilmulla
 *
 */
public class ForceSaveWidgetStub extends EditorWidgetStub implements ForceSaveWidget {
    private ErrorsWidget errorsWidget;
    private Runnable okHandler;

    public void subscribeToForceSaveEvent(EditorEventBus bus) {
        bus.subscribe(ForceSaveTestEvent.class, event -> {
            okHandler.run();
        });
    }

    @Override
    public ErrorsWidget getErrorsWidget() {
        return this.errorsWidget;
    }

    @Override
    public void setErrorsWidget(ErrorsWidget errorsWidget) {
        this.errorsWidget = errorsWidget;
    }

    @Override
    public void setOkHandler(Runnable okHandler) {
        this.okHandler = okHandler;
    }

    @Override
    public void setCancelHandler(Runnable cancelHandler) {
    }
}
