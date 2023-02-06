package ru.yandex.market.mbo.gwt.client.pages.model.editor.test;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.ModelDataLoadedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.ModelUIGeneratedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableModel;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

import java.util.Date;

/**
 * @author gilmulla
 */
@SuppressWarnings("checkstyle:magicNumber")
public abstract class AbstractModelTest extends AbstractTest {

    protected CommonModel model;

    @Override
    public void run() {
        modelData = data.getModelData();

        bus.subscribe(ModelUIGeneratedEvent.class, event -> {
            editableModel = event.getEditableModel();
            onModelLoaded(editableModel);
        });

        bus.fireEvent(new PlaceShowEvent(
            EditorUrlStub.of("modelEditor", "entity-id=" + modelData.getModel().getId())));
        bus.fireEvent(new ModelDataLoadedEvent(modelData));
    }

    @Override
    public void model() {
        data.startModel()
            .title("Test model")
            .published(false)
            .publishedOnBlue(false)
            .id(10).category(11).currentType(CommonModel.Source.GURU)
            .modificationDate(new Date(1000000))
            .endModel();
    }

    protected void onModelLoaded(EditableModel editableModel) {
        this.model = editableModel.getModel();
    }
}
