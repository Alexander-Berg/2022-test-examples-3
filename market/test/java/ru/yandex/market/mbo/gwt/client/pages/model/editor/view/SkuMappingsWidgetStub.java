package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import com.google.gwt.user.client.ui.Widget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.sku.SkuMappingsWidget;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelInfo;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.BusinessSkuKey;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.MappingUpdateStatus;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.SupplierOffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author anmalysh
 */
public class SkuMappingsWidgetStub implements SkuMappingsWidget {

    private ModelInfo sku;
    private List<SupplierOffer> mappings = new ArrayList<>();
    private Runnable hideControlsAction;

    @Override
    public void init(ModelInfo sku, List<ModelInfo> allVendorSkus) {
        this.sku = sku;
    }

    @Override
    public List<SupplierOffer> getPendingMappings() {
        return mappings.stream()
            .filter(m -> m.getApprovedMapping().getSkuId() != sku.getId())
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
        this.mappings = mappings;
    }

    @Override
    public void clearEmptySkuWithMappings() {

    }

    @Override
    public void setMappings(List<SupplierOffer> mappings) {
        this.mappings = new ArrayList<>(mappings);
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public void setVisible(boolean visible) {

    }

    public List<SupplierOffer> getMappings() {
        return mappings;
    }

    @Override
    public Widget asWidget() {
        return null;
    }

    @Override
    public void hideControls() {
        if (hideControlsAction != null) {
            hideControlsAction.run();
        }
    }

    public Runnable getHideControlsAction() {
        return hideControlsAction;
    }

    public void setHideControlsAction(Runnable hideControlsAction) {
        this.hideControlsAction = hideControlsAction;
    }
}
