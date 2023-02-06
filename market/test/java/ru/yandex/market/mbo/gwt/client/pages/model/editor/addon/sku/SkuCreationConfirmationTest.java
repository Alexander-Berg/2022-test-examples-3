package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.sku;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.SkuRelationCreationRequestedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.SkuRelationWidgetStub;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import static org.junit.Assert.assertEquals;

/**
 * Тестируем подтверждение при создании SKU у модели с проставленным IsSku.
 *
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SkuCreationConfirmationTest extends BaseSkuAddonTest {

    @Override
    public void model() {
        // создаем модель и связанные с ней sku
        data.startModel()
            .title("Test model")
            .id(1).category(666).vendorId(777).currentType(CommonModel.Source.GURU)
            .startParameterValue()
                .xslName(XslNames.IS_SKU).paramId(7L).booleanValue(true, 1L)
            .endParameterValue()
            .endModel();
    }

    @Test
    public void testSkuCreationConfirmation() throws Exception {
        // добавляем новый sku
        bus.fireEvent(new SkuRelationCreationRequestedEvent());

        SkuRelationWidgetStub skuRelationWidget = (SkuRelationWidgetStub) viewFactory.getSkuRelationWidget();

        CommonModel editedModel = editableModel.getModel();

        assertEquals(true, skuRelationWidget.isSkuCreationConfirmationShown());
        assertEquals(0, editedModel.getRelations().size());

        skuRelationWidget.acceptSkuCreationConfirmation();

        assertEquals(1, editedModel.getRelations().size());
    }
}
