package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.DescriptionBlockView;

/**
 * @author gilmulla
 *
 */
public class DescriptionBlockViewStub implements DescriptionBlockView {

    private String text;
    private String description;

    @Override
    public String getText() {
        return text == null ? "" : text;
    }

    @Override
    public String getDescription() {
        return description == null ? "" : description;
    }

    @Override
    public void setText(String text, String description) {
        this.text = text;
        this.description = description;
    }

    @Override
    public void clear() {
        setText(null, null);
    }
}
