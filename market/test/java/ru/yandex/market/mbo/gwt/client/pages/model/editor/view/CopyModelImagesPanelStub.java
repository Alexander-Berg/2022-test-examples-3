package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.ModelImages;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.CopyModelImagesPanel;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.EditorWidget;

import java.util.List;

/**
 * @author gilmulla
 *
 */
public class CopyModelImagesPanelStub extends EditorWidgetStub implements CopyModelImagesPanel {

    private List<ModelImages> modelImages;

    public CopyModelImagesPanelStub(List<ModelImages> modelImages) {
        this.modelImages = modelImages;
    }

    public List<ModelImages> getModelImages() {
        return modelImages;
    }

    @Override
    public EditorWidget getEditorWidget() {
        return this;
    }

    @Override
    public void updateSize() {
    }

    @Override
    public boolean isDynamicSized() {
        return true;
    }
}
