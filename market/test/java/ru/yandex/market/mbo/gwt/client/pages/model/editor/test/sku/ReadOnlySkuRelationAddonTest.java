package ru.yandex.market.mbo.gwt.client.pages.model.editor.test.sku;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.sku.BaseSkuAddonTest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.ModelUIGeneratedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditableModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.Param;

public class ReadOnlySkuRelationAddonTest extends BaseSkuAddonTest {

    @Test
    public void testSubscribeModelUIGenerated() throws Exception {
        data.clearParameters()
            .startParameters()
            .startParameter()
            .xsl("param0").type(Param.Type.NUMERIC).name("param0")
            .skuParameterMode(SkuParameterMode.SKU_NONE)
            .endParameter()
            .startParameter()
            .xsl("param1").type(Param.Type.NUMERIC).name("param1")
            .skuParameterMode(SkuParameterMode.SKU_DEFINING)
            .endParameter()
            .startParameter()
            .xsl("param2").type(Param.Type.NUMERIC).name("param2")
            .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL)
            .endParameter()
            .startParameter()
            .xsl("param3").type(Param.Type.NUMERIC).name("param3")
            .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL)
            .mandatory(true)
            .endParameter()
            .endParameters();
        editableModel = new EditableModel(bus);
        editableModel.setOriginalModel(data.getModel());
        editableModel.setModel(new CommonModel(data.getModel()));
        bus.fireEvent(new ModelUIGeneratedEvent(editableModel));
        Assert.assertEquals(2, viewFactory.getSkuRelationWidget().getParams().size());
    }
}
