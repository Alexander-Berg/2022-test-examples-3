package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.sku;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.SkuRelationWidgetStub;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;

import java.util.List;

/**
 * Тест, что для read-only моделей создается read-only виджет.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ReadOnlySkuRelationAddonTest extends BaseSkuAddonTest {

    @Override
    public void model() {
        // создаем модель и связанные с ней sku
        data.startModel()
            .title("Test model")
            .id(1).category(666).vendorId(777).currentType(CommonModel.Source.GENERATED)
            .picture("XL-Picture", "pic://1_2")
            .picture("XL-Picture_2", "pic://1_3")
            .param("XL-Picture").setString("pic://1_2")
            .param("XL-Picture_2").setString("pic://1_3")
            .startModelRelation()
                .id(3).categoryId(666).type(ModelRelation.RelationType.SKU_MODEL)
                .startModel()
                    // sku model содержит определяющие параметры
                    .id(3).category(666).currentType(CommonModel.Source.GENERATED_SKU)
                    .param("param1").setOption(1).modificationSource(ModificationSource.OPERATOR_FILLED)
                    .param("param2").setOption(4).modificationSource(ModificationSource.OPERATOR_FILLED)
                    .picture("pic://2_1")
                    .picture("pic://2_2")
                    .startModelRelation()
                        .id(1).categoryId(666).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                    .endModelRelation()
                .endModel()
            .endModelRelation()
            .endModel();
    }

    @Test
    public void testTableContainsSkus() {
        SkuRelationWidgetStub widget = (SkuRelationWidgetStub) viewFactory.getGskuRelationWidget();
        List<CommonModel> skus = widget.getSkus();
        Assertions.assertThat(skus).isNotEmpty();
    }
}
