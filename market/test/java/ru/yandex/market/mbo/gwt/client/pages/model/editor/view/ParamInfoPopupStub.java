package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamInfoWidget;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;

import java.math.BigDecimal;

/**
 * @author gilmulla
 *
 */
public class ParamInfoPopupStub extends EditorWidgetStub implements ParamInfoWidget {

    private String description;
    private String comment;
    private BigDecimal[] bounds = new BigDecimal[2];
    private CategoryParam param;

    @Override
    public CategoryParam getParameterCategory() {
        return param;
    }

    @Override
    public void setParameterCategory(CategoryParam param, boolean createLinkToParam) {
        this.param = param;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getComment() {
        return this.comment;
    }

    @Override
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public BigDecimal[] getBounds() {
        return this.bounds;
    }

    @Override
    public void setBounds(BigDecimal min, BigDecimal max) {
        this.bounds[0] = min;
        this.bounds[1] = max;
    }

}
