package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.sku;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableValue;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.SkuRelationWidgetStub;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import static org.junit.Assert.assertEquals;

/**
 * Тестируем подтверждение при простановке галки IsSku у модели с СКЮ.
 *
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class IsSkuCheckConfirmationTest extends BaseSkuAddonTest {

    @Override
    public void model() {
        // создаем модель и связанные с ней sku
        data.startModel()
            .title("Test model")
            .id(1).category(666).vendorId(777).currentType(CommonModel.Source.GURU)
            .startParameterValue()
                .xslName(XslNames.IS_SKU).paramId(7L).booleanValue(false, 1L)
            .endParameterValue()
            .startModelRelation()
                .id(2).categoryId(666).type(ModelRelation.RelationType.SKU_MODEL)
                .startModel()
                    // sku model содержит определяющие параметры
                    .id(2).category(666).currentType(CommonModel.Source.SKU)
                    .startModelRelation()
                       .id(1).categoryId(666).type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                    .endModelRelation()
                .endModel()
            .endModelRelation()
            .endModel();
    }

    @Test
    public void testIsSkuCheckConfirmation() throws Exception {
        // Запрашиваем установку галки IsSku
        EditableValue isSkuValue = editableModel.getEditableParameter(XslNames.IS_SKU)
            .getFirstEditableValue();
        isSkuValue.getValueWidget().setValue(true, true);

        SkuRelationWidgetStub skuRelationWidget = (SkuRelationWidgetStub) viewFactory.getSkuRelationWidget();

        assertEquals(true, skuRelationWidget.isSkuCheckConfirmationShown());
        assertEquals(false, isSkuValue.getParameterValue().getBooleanValue());

        skuRelationWidget.acceptIsSkuCheckConfirmation();

        assertEquals(true, isSkuValue.getParameterValue().getBooleanValue());
    }
}
