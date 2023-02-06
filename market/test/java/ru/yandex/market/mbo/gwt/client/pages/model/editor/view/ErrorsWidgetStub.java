package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import java.util.List;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ErrorsWidget;
import ru.yandex.market.mbo.common.processing.ProcessingResult;

/**
 * @author gilmulla
 *
 */
public class ErrorsWidgetStub extends EditorWidgetStub implements ErrorsWidget {

    private List<ProcessingResult> errors;
    private boolean showMoreErrorsVisible;

    @Override
    public List<ProcessingResult> getErrors() {
        return this.errors;
    }

    @Override
    public void setErrors(List<ProcessingResult> errors) {
        this.errors = errors;
    }

    @Override
    public void setNoneErrors() {

    }

    @Override
    public void setErrorStyleEnabled(boolean enabled) {

    }

    @Override
    public String getShowMoreErrorsText() {
        return null;
    }

    @Override
    public void setShowMoreErrorsText(String text) {

    }

    @Override
    public boolean isShowMoreErrorsVisible() {
        return this.showMoreErrorsVisible;
    }

    @Override
    public void setShowMoreErrorsVisible(boolean visible) {
        this.showMoreErrorsVisible = visible;
    }

}
