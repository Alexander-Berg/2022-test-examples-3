package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.partner_sku;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.AbstractModelTest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.partner_sku.SupplierTab;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

/**
 * Tests of {@link ShowSupplierIdTabAddon}.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ShowSupplierIdTabAddonTest extends AbstractModelTest {
    @Override
    public void model() {
        data.startModel()
            .title("Test model").id(10).category(11).vendorId(12)
            .currentType(CommonModel.Source.PARTNER_SKU)
            .supplierId(100)
            .endModel();
    }

    @Test
    public void testSupplierTabIsVisibleIfPartnerModelIsShown() {
        SupplierTab supplierTab = (SupplierTab) view.getTab(EditorTabs.SUPPLIER.getDisplayName());

        Assertions.assertThat(supplierTab.getSupplierId()).isEqualTo(100L);
    }
}
