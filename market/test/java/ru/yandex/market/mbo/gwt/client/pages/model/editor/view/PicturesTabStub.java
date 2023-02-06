package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.PicturesTab;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;

import java.util.List;

/**
 * @author s-ermakov
 */
public class PicturesTabStub extends EditorWidgetStub implements PicturesTab {
    private List<Picture> pictures;
    private boolean readOnly;

    @Override
    public List<Picture> getPictures() {
        return pictures;
    }

    @Override
    public void setPictures(List<Picture> pictures) {
        this.pictures = pictures;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }
}
