package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import com.google.gwt.user.client.ui.Widget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.SelectModelSuccessorsWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.sku.widget.ModelSelectionWidget;
import ru.yandex.market.mbo.gwt.models.IdAndName;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelInfo;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRemovalType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author anmalysh
 */
public class SelectModelSuccessorsWidgetStub implements SelectModelSuccessorsWidget {

    private final ModelInfo removedModel;
    private List<Runnable> onContinueHandlers = new ArrayList<>();
    private List<Runnable> onCancelHandlers = new ArrayList<>();
    boolean hidden = false;
    private List<IdAndName> successors = new ArrayList<>();
    private ModelRemovalType modelRemovalType = ModelRemovalType.SPLIT;

    public SelectModelSuccessorsWidgetStub(ModelInfo removedModel) {
        this.removedModel = removedModel;
    }

    @Override
    public ModelRemovalType getRemovalType() {
        return modelRemovalType;
    }

    public void setRemovalType(ModelRemovalType modelRemovalType) {
        this.modelRemovalType = modelRemovalType;
    }

    @Override
    public void addContinueHandler(Runnable handler) {
        onContinueHandlers.add(handler);
    }

    public void addCancelHandler(Runnable handler) {
        onCancelHandlers.add(handler);
    }

    @Override
    public void addRequestModelByIdHandler(BiConsumer<Long, ModelSelectionWidget> handler) {
    }

    @Override
    public ModelInfo getRemovedModel() {
        return removedModel;
    }

    @Override
    public IdAndName getPrimarySuccessor() {
        return successors.stream()
            .findFirst().orElse(null);
    }

    @Override
    public List<IdAndName> getSuccessors() {
        return successors;
    }

    @Override
    public void setProcessing(boolean processing) {

    }

    public void hide() {
        hidden = true;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void clickContinue(List<IdAndName> successors) {
        this.successors = successors;
        onContinueHandlers.forEach(Runnable::run);
    }

    public void clickCancel() {
        onCancelHandlers.forEach(Runnable::run);
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
}
