package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.sku;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.SaveModelRequest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.image.DeletePicturesRequestEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.PrepareSkusForImageEditorEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.sku.SkuRelationChangedEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.sku.SkuFullPicturesEditor;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тестирует {@link SkuPicturesEditorAddon} и другие тест-кейсы, связанные с обновлением картинок.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SkuPicturesEditorAddonTest extends BaseSkuAddonTest {

    @Override
    public void parameters() {
        data.startParameters()
            .startParameter()
                .xsl("param1").type(Param.Type.NUMERIC).name("Enum1")
            .endParameter()
            .startParameter()
                .xsl("param2").type(Param.Type.NUMERIC).name("Enum2")
                .skuParameterMode(SkuParameterMode.SKU_NONE)
            .endParameter()
            .startParameter()
                .xsl("param3").type(Param.Type.NUMERIC).name("Enum3")
                .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL)
            .endParameter()
            .startParameter()
                .xsl("param4").type(Param.Type.NUMERIC).name("Enum4")
                .skuParameterMode(SkuParameterMode.SKU_DEFINING)
            .endParameter()

            .startParameter()
                .xsl("param5").type(Param.Type.NUMERIC).name("Enum5")
                .mandatory(true)
            .endParameter()
            .startParameter()
                .xsl("param6").type(Param.Type.NUMERIC).name("Enum6")
                .skuParameterMode(SkuParameterMode.SKU_NONE).mandatory(true)
            .endParameter()
            .startParameter()
                .xsl("param7").type(Param.Type.NUMERIC).name("Enum7")
                .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL).mandatory(true)
            .endParameter()
            .startParameter()
                .xsl("param8").type(Param.Type.NUMERIC).name("Enum8")
                .skuParameterMode(SkuParameterMode.SKU_DEFINING).mandatory(true)
            .endParameter()

            .startParameter()
                .xsl("XL-Picture").type(Param.Type.STRING).name("XL-Picture")
            .endParameter()
            .startParameter()
                .xsl("XLPictureUrl").type(Param.Type.STRING).name("XLPictureUrl")
            .endParameter()
            .startParameter()
                .xsl("XLPictureOrig").type(Param.Type.STRING).name("XLPictureOrig")
            .endParameter()
            .startParameter()
                .xsl("XLPictureSizeX").type(Param.Type.NUMERIC).name("XLPictureSizeX")
            .endParameter()
            .startParameter()
                .xsl("XLPictureSizeY").type(Param.Type.NUMERIC).name("XLPictureSizeY")
            .endParameter()
            .startParameter()
                .xsl("XLPictureColorness").type(Param.Type.NUMERIC).name("XLPictureColorness")
            .endParameter()
            .startParameter()
                .xsl("XLPictureColornessAvg").type(Param.Type.NUMERIC).name("XLPictureColornessAvg")
            .endParameter()

            .startParameter()
                .xsl("XL-Picture_2").type(Param.Type.STRING).name("XL-Picture_2")
            .endParameter()
            .startParameter()
                .xsl("XLPictureUrl_2").type(Param.Type.STRING).name("XLPictureUrl_2")
            .endParameter()
            .startParameter()
                .xsl("XLPictureOrig_2").type(Param.Type.STRING).name("XLPictureOrig_2")
            .endParameter()
            .startParameter()
                .xsl("XLPictureSizeX_2").type(Param.Type.NUMERIC).name("XLPictureSizeX_2")
            .endParameter()
            .startParameter()
                .xsl("XLPictureSizeY_2").type(Param.Type.NUMERIC).name("XLPictureSizeY_2")
            .endParameter()
            .startParameter()
                .xsl("XLPictureColorness_2").type(Param.Type.NUMERIC).name("XLPictureColorness_2")
            .endParameter()
            .startParameter()
                .xsl("XLPictureColornessAvg_2").type(Param.Type.NUMERIC).name("XLPictureColornessAvg_2")
            .endParameter()
        .endParameters();
    }

    @Test
    public void testSettingOnlyMandatoryAndRequiredParams() {
        CommonModel sku = data.getModel().getRelation(2).get().getModel();
        bus.fireEvent(new PrepareSkusForImageEditorEvent(sku));

        SkuFullPicturesEditor widget = (SkuFullPicturesEditor) view.getDialogWidget();
        List<String> params = widget.getParams().stream()
            .map(CategoryParam::getXslName)
            .collect(Collectors.toList());

        List<String> expected = Arrays.asList("param3", "param4", "param7", "param8");
        Assert.assertEquals(expected, params);
    }

    @Test
    public void testPicturesMoveFromBaseModelToSkuWontResaveBaseModel() {
        rpc.setLoadModelFail(() -> new AssertionError("Load model don't expected to be call"));
        rpc.setSaveModelFail(() -> new AssertionError("Save model don't expected to be call"));
        rpc.setDeletePicturesFail(() -> new AssertionError("Delete pictures don't expected to be call"));

        CommonModel baseModel = data.getModel();

        Picture picture1 = baseModel.getPictures().get(0);
        Picture picture2 = baseModel.getPictures().get(1);
        List<Picture> modelPictures = Arrays.asList(picture1, picture2);

        // эмулируем события из SkuFullPicturesEditorImpl#onSave
        bus.fireEvent(new DeletePicturesRequestEvent(baseModel, modelPictures));
        sku.addAllPictures(modelPictures);
        bus.fireEvent(new SkuRelationChangedEvent(sku));

        // сохраняем модель
        rpc.setSaveModel(1L, null);
        rpc.setLoadModel(editableModel.getModel(), null);
        bus.fireEvent(new SaveModelRequest(false, false));
        CommonModel savedModel = rpc.getSavedModel();
        CommonModel skuModel = savedModel.getRelation(2L).get().getModel();

        assertThat(savedModel.getPictures().stream().map(Picture::getUrl)).isEmpty();
        assertThat(skuModel.getPictures().stream().map(Picture::getUrl))
            .containsExactlyInAnyOrder("pic://2_1", "pic://2_2", "pic://1_2", "pic://1_3");
    }
}
