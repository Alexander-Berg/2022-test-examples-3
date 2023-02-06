package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.SelectPicturesList;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;

import java.util.Collections;
import java.util.List;

/**
 * @author s-ermakov
 */
public class SelectPicturesListStub extends EditorWidgetStub implements SelectPicturesList {
    private List<Picture> pictures = Collections.emptyList();
    private List<Picture> selectedPictures = Collections.emptyList();
    private boolean multiSelection;
    private String title;

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public boolean isMultiSelect() {
        return multiSelection;
    }

    @Override
    public void setMultiSelection(boolean multiSelection) {
        this.multiSelection = multiSelection;
    }

    @Override
    public void setPictures(List<Picture> pictures) {
        this.pictures = pictures;
    }

    @Override
    public List<Picture> getPictures() {
        return pictures;
    }

    public void setSelectedPictures(List<Picture> selectedPictures) {
        this.selectedPictures = selectedPictures;
    }

    @Override
    public List<Picture> getSelectedPictures() {
        return selectedPictures;
    }
}
