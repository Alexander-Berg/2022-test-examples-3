package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.sku.MoveSkuEditorWidget;
import ru.yandex.market.mbo.gwt.client.utils.dialogs.ClosableDialogBox;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelInfo;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;

import java.util.List;
import java.util.Set;

public class MoveSkuEditorWidgetStub extends EditorWidgetStub implements MoveSkuEditorWidget {

    private List<CategoryParam>  params;
    private CommonModel fromModel;
    private Set<CommonModel> skuIds;

    public MoveSkuEditorWidgetStub(List<CategoryParam> params, CommonModel fromModel, Set<CommonModel> skuIds) {
        this.params = params;
        this.fromModel = fromModel;
        this.skuIds = skuIds;
    }

    @Override
    public ModelInfo getTargetModel() {
        return null;
    }

    @Override
    public void startLoader() {

    }

    @Override
    public void finishLoader() {

    }

    @Override
    public void clearErrors() {

    }

    @Override
    public void showModifiedWarning() {

    }

    @Override
    public void showError(Throwable error) {

    }

    @Override
    public void showError(String error) {

    }

    @Override
    public ClosableDialogBox createDialogBox() {
        return null;
    }
}
