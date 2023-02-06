package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import com.google.gwt.user.client.ui.Widget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.sku.SelectSkuSuccessorsWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.sku.widget.ModelSelectionWidget;
import ru.yandex.market.mbo.gwt.models.IdAndName;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelInfo;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRemovalType;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.BusinessSkuKey;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.MappingChangeType;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.MappingUpdateStatus;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.SupplierOffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author anmalysh
 */
public class SelectSkuSuccessorsWidgetStub implements SelectSkuSuccessorsWidget {

    private final ModelInfo removedSku;
    private List<Runnable> onContinueHandlers = new ArrayList<>();
    private List<Runnable> onCancelHandlers = new ArrayList<>();
    boolean hidden = false;
    private List<IdAndName> successors = new ArrayList<>();
    private List<SupplierOffer> mappings = new ArrayList<>();
    private ModelRemovalType modelRemovalType = ModelRemovalType.SPLIT;

    public SelectSkuSuccessorsWidgetStub(ModelInfo removedSku) {
        this.removedSku = removedSku;
    }


    @Override
    public ModelRemovalType getRemovalType() {
        return modelRemovalType;
    }

    @Override
    public void addContinueHandler(Runnable handler) {
        onContinueHandlers.add(handler);
    }

    public void addCancelHandler(Runnable handler) {
        onCancelHandlers.add(handler);
    }

    @Override
    public void addRequestSkuByIdHandler(BiConsumer<Long, ModelSelectionWidget> handler) {

    }

    @Override
    public ModelInfo getRemovedSku() {
        return removedSku;
    }

    @Override
    public List<IdAndName> getSuccessors() {
        return successors;
    }

    @Override
    public List<SupplierOffer> getPendingMappings() {
        return mappings.stream()
            .filter(m -> m.getApprovedMapping().getSkuId() != removedSku.getId())
            .collect(Collectors.toList());
    }

    @Override
    public List<String> getRelatedTickets() {
        return Collections.emptyList();
    }

    @Override
    public void applyMappingsUpdateStatuses(List<MappingUpdateStatus> statuses) {
        Map<BusinessSkuKey, MappingUpdateStatus> updatedMappingKeys = statuses.stream()
            .collect(Collectors.toMap(BusinessSkuKey::from, Function.identity()));
        mappings = mappings.stream()
            .filter(mapping -> {
                MappingUpdateStatus status = updatedMappingKeys.get(BusinessSkuKey.from(mapping));
                return status == null || status.isFailure();
            })
            .collect(Collectors.toList());
    }

    @Override
    public void applyReloadedMappings(List<SupplierOffer> mappings) {

    }

    @Override
    public void setProcessing(boolean processing) {

    }

    @Override
    public MappingChangeType getMappingChangeType() {
        return MappingChangeType.SKU_REMOVAL_DUPLICATE;
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

    public void setMappings(List<SupplierOffer> mappings) {
        this.mappings = mappings;
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public void setVisible(boolean visible) {

    }

    public void setModelRemovalType(ModelRemovalType modelRemovalType) {
        this.modelRemovalType = modelRemovalType;
    }

    @Override
    public Widget asWidget() {
        return null;
    }
}
