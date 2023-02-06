package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.NameSamplesView;

/**
 * @author gilmulla
 *
 */
public class NameSamplesViewStub extends EditorWidgetStub implements NameSamplesView {

    private List<String> names = new ArrayList<>();
    private String error;

    @Override
    public List<String> getNames() {
        return names;
    }

    @Override
    public void setNames(List<String> names) {
        this.names = names;
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public void setError(String error) {
        this.error = error;
    }

}
