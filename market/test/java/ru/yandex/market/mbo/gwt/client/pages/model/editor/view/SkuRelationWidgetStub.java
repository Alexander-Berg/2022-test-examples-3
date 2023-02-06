package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.IsSkuCheckConfirmedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.SkuRelationCreationEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.ModelData;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ReadOnlySkuRelationWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.PartnerSkuRelationWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.sku.SkuRelationWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.sku.widget.SkuTableFilter;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.sku.widget.SkuTablePager;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.sku.columns.ParamSkuColumn;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.sku.widgets.SkuTableFilterStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.sku.widgets.SkuTablePagerStub;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.Param.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author s-ermakov
 */
public class SkuRelationWidgetStub extends EditorWidgetStub implements SkuRelationWidget, ReadOnlySkuRelationWidget,
    PartnerSkuRelationWidget {
    private List<CommonModel> skus = new ArrayList<>();
    private List<CategoryParam> params = new ArrayList<>();
    private Map<CategoryParam, SkuTableFilter> filters = new HashMap<>();
    private SkuTablePager pager = new SkuTablePagerStub();
    private boolean skuCreationConfirmationShown = false;
    private boolean isSkuCheckConfirmationShown = false;

    @Override
    public void setParams(List<CategoryParam> params) {
        this.params.clear();
        this.params.addAll(params);
        params.forEach(p -> filters.put(p, new SkuTableFilterStub(p, bus)));
    }

    @Override
    public void setCreateEnabled(boolean enabled) {

    }

    @Override
    public void setMoveEnabled(boolean enabled) {

    }

    @Override
    public List<CategoryParam> getParams() {
        return this.params;
    }

    @Override
    public void setSkus(List<CommonModel> skus, int page) {
        this.skus = skus;
        pager.setPage(page, false);
    }

    @Override
    public void addSku(CommonModel sku) {
        this.skus.add(sku);
    }

    @Override
    public void addSku(CommonModel sku, int position) {

    }

    @Override
    public void deleteSku(CommonModel sku) {
        this.skus.removeIf(model -> model == sku);
    }

    @Override
    public void updatePickerImagesInSku(CommonModel sku) {

    }

    @Override
    public void updatePickerImagesInSkus(Collection<CommonModel> skusToUpdate) {

    }

    @Override
    public void updateTitleAndPicturesInSku(CommonModel sku) {

    }

    @Override
    public void showSkuCreationConfirmation() {
        skuCreationConfirmationShown = true;
    }

    @Override
    public void showIsSkuCheckConfirmation() {
        isSkuCheckConfirmationShown = true;
    }

    @Override
    public void setMaxNumberOfPages(int maxNumberOfPages) {
        pager.setMaxPages(maxNumberOfPages);
    }

    @Override
    public int getPageNumber() {
        return pager.getCurrentPage();
    }

    @Override
    public void setOnPageChangeHandler(Consumer<Integer> onPageChangeAction) {
        pager.onPageChange(onPageChangeAction);
    }

    @Override
    public List<SkuTableFilter> getFilters() {
        return new ArrayList<>(filters.values());
    }

    @Override
    public ParamSkuColumn getParamSkuColumn(String xslName) {
        Optional<CategoryParam> param = params.stream().filter(p -> p.getXslName().equals(xslName)).findAny();
        if (param.isPresent()) {
            return new ParamSkuColumn(param.get(), new ViewFactoryStub(), new ModelData());
        } else {
            CategoryParam dummy = CategoryParamBuilder.newBuilder().setXslName(xslName).setType(Type.STRING).build();
            return new ParamSkuColumn(dummy, new ViewFactoryStub(), new ModelData());
        }
    }

    public boolean isSkuCreationConfirmationShown() {
        return skuCreationConfirmationShown;
    }

    public boolean isSkuCheckConfirmationShown() {
        return isSkuCheckConfirmationShown;
    }

    public void acceptSkuCreationConfirmation() {
        bus.fireEvent(new SkuRelationCreationEvent());
    }

    @Override
    public void addNewEnumOption(CategoryParam param, Option option) {

    }

    public void acceptIsSkuCheckConfirmation() {
        bus.fireEvent(new IsSkuCheckConfirmedEvent());
    }

    public List<CommonModel> getSkus() {
        return skus;
    }

    public void setPage(int page) {
        pager.setPage(page, true);
    }

    public SkuTableFilterStub getFilter(CategoryParam param) {
        return (SkuTableFilterStub) filters.get(param);
    }
}
