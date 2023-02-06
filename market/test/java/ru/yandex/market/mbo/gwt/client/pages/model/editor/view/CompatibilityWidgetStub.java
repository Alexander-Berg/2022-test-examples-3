package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import java.util.List;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.CompatibilityWidget;
import ru.yandex.market.mbo.gwt.models.compatibility.CompatibilityModel;

/**
 * @author gilmulla
 *
 */
public class CompatibilityWidgetStub extends EditorWidgetStub implements CompatibilityWidget {

    @Override
    public List<CompatibilityModel> getModels() {
        return null;
    }

    @Override
    public void setModels(List<CompatibilityModel> models) {

    }

    @Override
    public long getMainCategoryId() {
        return 0;
    }

    @Override
    public void setMainCategoryId(long categoryId) {

    }

}
