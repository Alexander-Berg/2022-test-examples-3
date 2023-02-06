package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.sku.SkuFullPicturesEditor;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;

import java.util.List;

/**
 * @author s-ermakov
 */
public class SkuFullPicturesEditorStub extends EditorWidgetStub implements SkuFullPicturesEditor {
    private List<CategoryParam> params;

    @Override
    public void init(CommonModel parentModel, List<CommonModel> skus) {

    }

    @Override
    public void init(CommonModel parentModel, CommonModel editableSku, List<CommonModel> skus) {

    }

    @Override
    public void setParams(List<CategoryParam> params) {
        this.params = params;
    }

    @Override
    public List<CategoryParam> getParams() {
        return params;
    }

    @Override
    public void onAllModelPicturesLoadSuccess(List<Picture> pictures) {

    }

    @Override
    public void onAllModelPicturesLoadFailure(String message) {

    }

    @Override
    public void setReadOnly(boolean readOnly) {

    }
}
