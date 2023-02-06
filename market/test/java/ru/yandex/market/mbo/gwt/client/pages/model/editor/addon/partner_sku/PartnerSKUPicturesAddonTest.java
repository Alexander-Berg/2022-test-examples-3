package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.partner_sku;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.sku.BaseSkuAddonTest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.PicturesTab;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author danfertev
 * @since 15.05.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class PartnerSKUPicturesAddonTest extends BaseSkuAddonTest {

    @Override
    public void model() {
        data.startModel()
            .title("Test model")
            .id(2).category(666).vendorId(777).currentType(CommonModel.Source.PARTNER_SKU)
            .picture("pic://2_1")
            .picture("pic://2_2")
            .parameterValues(1, "param1", 11)
            .parameterValues(101, "param101", 1011, 1012)
            .startModelRelation()
                .id(1).categoryId(666).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                .startModel()
                    .id(1).category(666).currentType(CommonModel.Source.PARTNER)
                    .picture("XL-Picture", "pic://1_2")
                    .picture("XL-Picture_2", "pic://1_3")
                    .startModelRelation()
                        .id(2).categoryId(666).type(ModelRelation.RelationType.SKU_MODEL)
                    .endModelRelation()
                .endModel()
            .endModelRelation()
            .endModel();
    }

    @Test
    public void testTabCreated() {
        PicturesTab tab = view.getTab(EditorTabs.PICTURES.getDisplayName(), PicturesTab.class);

        assertThat(tab.getPictures().stream().map(Picture::getUrl))
            .containsExactly("pic://2_1", "pic://2_2");
    }
}
