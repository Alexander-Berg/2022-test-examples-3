package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.PickerImageEditorWidget;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.params.PickerImage;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author s-ermakov
 */
public class PickerImageEditorWidgetStub extends EditorWidgetStub implements PickerImageEditorWidget {
    @Override
    public PickerImage getPickerImage() {
        return null;
    }

    @Override
    public ModificationSource getPickerImageSource() {
        return null;
    }

    @Override
    public void setPickerImage(PickerImage pickerImage, ModificationSource pickerSource) {

    }

    @Override
    public void setOnSaveHandler(BiConsumer<PickerImage, ModificationSource> saveHandler) {

    }

    @Override
    public void setImportPictures(List<Picture> importPictures) {

    }
}
