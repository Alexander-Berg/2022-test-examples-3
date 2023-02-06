package ru.yandex.market.mbo.gwt.client.pages.model.editor.test;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelDataBuilder;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.SaveModelRequest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.save.PopulateModelSaveSyncEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.EditorUrl;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.TitleOperatorHintViewStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.BlockWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.EditorWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.NameSamplesView;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamWidget;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.ParamsTab;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.SourcesWidget;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.visual.CategoryWiki;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author gilmulla
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TestModelCreation extends AbstractTest {

    @Test
    public void testEmptyParametersString() {
        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelCreator", "")));

        assertError("Невозможно создать модель, так как не указан vendorId и/или parentId");
    }

    @Test
    public void testNullParametersString() {
        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelCreator", null)));

        assertError("Невозможно создать модель, так как не указан vendorId и/или parentId");
    }

    @Test
    public void testMissingCategory() {
        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelCreator", "vendor-id=11")));

        assertError("Невозможно создать модель, так как не указан categoryId");
    }

    @Test
    public void testMissingVendor() {
        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelCreator", "category-id=1")));

        assertError("Невозможно создать модель, так как не указан vendorId и/или parentId");
    }

    @Test
    public void testSampleNames() {
        rpc.setSampleNames(Arrays.asList("name2", "name1", "name3"), null);

        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelCreator", "vendor-id=11&category-id=1")));

        NameSamplesView nsView = view.getNameSamplesView();
        Assert.assertEquals(nsView.getNames().size(), 3);
        Assert.assertEquals(nsView.getNames().get(0), "name1");
        Assert.assertEquals(nsView.getNames().get(1), "name2");
        Assert.assertEquals(nsView.getNames().get(2), "name3");
    }

    @Test
    public void testTitleOperatorHints() {
        String includedHint = "included hint";
        String excludedHint = "excluded hint";
        String modelNameComment = "model name comment";

        CategoryWiki categoryWiki = new CategoryWiki();
        categoryWiki.setIncludedHint(includedHint);
        categoryWiki.setExcludedHint(excludedHint);
        categoryWiki.setModelNameComment(modelNameComment);

        rpc.setCategoryWiki(categoryWiki, null);
        bus.fireEvent(new PlaceShowEvent(
            EditorUrlStub.of("modelCreator", "vendor-id=11&category-id=1")));

        TitleOperatorHintViewStub operatorHintView = view.getTitleOperatorHintView();
        assertThat(operatorHintView).isNotNull();
        assertThat(operatorHintView.isVisible()).isTrue();
        assertThat(operatorHintView.getIncludedHint()).isEqualTo(includedHint);
        assertThat(operatorHintView.isIncludedHintVisible()).isTrue();
        assertThat(operatorHintView.getExcludedHint()).isEqualTo(excludedHint);
        assertThat(operatorHintView.isExcludedHintVisible()).isTrue();
        assertThat(operatorHintView.getModelNameComment()).isEqualTo(modelNameComment);
        assertThat(operatorHintView.isModelNameCommentHintVisible()).isTrue();
        assertThat(operatorHintView.getErrorText()).isEqualTo(null);
    }

    @Test
    public void testTitleOperatorHintsEmpty() {
        rpc.setCategoryWiki(new CategoryWiki(), null);

        bus.fireEvent(new PlaceShowEvent(
            EditorUrlStub.of("modelCreator", "vendor-id=11&category-id=1")));

        TitleOperatorHintViewStub operatorHintView = view.getTitleOperatorHintView();
        assertThat(operatorHintView).isNotNull();
        assertThat(operatorHintView.isVisible()).isFalse();
        assertThat(operatorHintView.isIncludedHintVisible()).isFalse();
        assertThat(operatorHintView.isExcludedHintVisible()).isFalse();
        assertThat(operatorHintView.isModelNameCommentHintVisible()).isFalse();
        assertThat(operatorHintView.getErrorText()).isEqualTo(null);
    }

    @Test
    public void testTitleIncludedHint() {
        CategoryWiki categoryWiki = new CategoryWiki();
        String includedHint = "included hint";
        categoryWiki.setIncludedHint(includedHint);

        rpc.setCategoryWiki(categoryWiki, null);

        bus.fireEvent(new PlaceShowEvent(
            EditorUrlStub.of("modelCreator", "vendor-id=11&category-id=1")));

        TitleOperatorHintViewStub operatorHintView = view.getTitleOperatorHintView();
        assertThat(operatorHintView).isNotNull();
        assertThat(operatorHintView.isVisible()).isTrue();
        assertThat(operatorHintView.getIncludedHint()).isEqualTo(includedHint);
        assertThat(operatorHintView.isIncludedHintVisible()).isTrue();
        assertThat(operatorHintView.isExcludedHintVisible()).isFalse();
        assertThat(operatorHintView.isModelNameCommentHintVisible()).isFalse();
        assertThat(operatorHintView.getErrorText()).isEqualTo(null);
    }

    @Test
    public void testTitleExcludedHint() {
        CategoryWiki categoryWiki = new CategoryWiki();
        String excludedHint = "excluded hint";
        categoryWiki.setExcludedHint(excludedHint);

        rpc.setCategoryWiki(categoryWiki, null);

        bus.fireEvent(new PlaceShowEvent(
            EditorUrlStub.of("modelCreator", "vendor-id=11&category-id=1")));

        TitleOperatorHintViewStub operatorHintView = view.getTitleOperatorHintView();
        assertThat(operatorHintView).isNotNull();
        assertThat(operatorHintView.isVisible()).isTrue();
        assertThat(operatorHintView.isIncludedHintVisible()).isFalse();
        assertThat(operatorHintView.getExcludedHint()).isEqualTo(excludedHint);
        assertThat(operatorHintView.isExcludedHintVisible()).isTrue();
        assertThat(operatorHintView.isModelNameCommentHintVisible()).isFalse();
        assertThat(operatorHintView.getErrorText()).isEqualTo(null);
    }

    @Test
    public void testTitleModelNameCommentHint() {
        CategoryWiki categoryWiki = new CategoryWiki();
        String modelNameComment = "crazy model name comment";
        categoryWiki.setModelNameComment(modelNameComment);

        rpc.setCategoryWiki(categoryWiki, null);

        bus.fireEvent(new PlaceShowEvent(
            EditorUrlStub.of("modelCreator", "vendor-id=11&category-id=1")));

        TitleOperatorHintViewStub operatorHintView = view.getTitleOperatorHintView();
        assertThat(operatorHintView).isNotNull();
        assertThat(operatorHintView.isVisible()).isTrue();
        assertThat(operatorHintView.isIncludedHintVisible()).isFalse();
        assertThat(operatorHintView.isExcludedHintVisible()).isFalse();
        assertThat(operatorHintView.isModelNameCommentHintVisible()).isTrue();
        assertThat(operatorHintView.getModelNameComment()).isEqualTo(modelNameComment);
        assertThat(operatorHintView.getErrorText()).isEqualTo(null);
    }

    @Test
    public void testTitleOperatorHintError() {
        String errorText = "error occurred";
        rpc.setCategoryWiki(null, new Exception(errorText));

        bus.fireEvent(new PlaceShowEvent(
            EditorUrlStub.of("modelCreator", "vendor-id=11&category-id=1")));

        TitleOperatorHintViewStub operatorHintView = view.getTitleOperatorHintView();
        assertThat(operatorHintView).isNotNull();
        assertThat(operatorHintView.isVisible()).isTrue();
        assertThat(operatorHintView.isIncludedHintVisible()).isFalse();
        assertThat(operatorHintView.isExcludedHintVisible()).isFalse();
        assertThat(operatorHintView.isModelNameCommentHintVisible()).isFalse();
        assertThat(operatorHintView.getErrorText()).isEqualTo(errorText);
    }

    @Test
    public void testOpeningNewModel() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("num1").type(Param.Type.NUMERIC).name("Num1").hidden(false).creation(true)
            .endParameter()
            .startParameter()
                .xsl("num2").type(Param.Type.NUMERIC).name("Num2").hidden(true).creation(true)
            .endParameter()
            .startParameter()
                .xsl("str1").type(Param.Type.STRING).name("Str1").hidden(false).creation(false)
            .endParameter()
            .startParameter()
                .xsl("str2").type(Param.Type.STRING).name("Str2").hidden(false).creation(true)
            .endParameter()
            .startParameter()
                .xsl(XslNames.OPERATOR_SIGN).type(Param.Type.BOOLEAN).name("Подпись оператора")
                .option(1, "TRUE")
                .option(2, "FALSE")
                .hidden(false).creation(false)
            .endParameter()
            .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
                .hidden(true)
                .option(1, "Vendor1")
                .option(2, "Vendor2")
                .option(3, "Vendor3")
            .endParameter()
        .endParameters()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("num1", "num2")
                .endBlock()
                .startBlock()
                    .name("block")
                    .properties("str1", "str2", XslNames.OPERATOR_SIGN)
                .endBlock()
            .endTab()
        .endForm()
        .startVendor()
            .source("http://source1", "ru", new Date())
            .source("http://source2", "en", new Date())
        .endVendor()
        .tovarCategory(1, 2);

        rpc.setSampleNames(Arrays.asList("name1", "name2"), null);
        rpc.setLoadModelData(data.getModelData(), null);

        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelCreator", "vendor-id=11&category-id=1")));

        // Панель копирования должна быть скрыта при создании модели
        Assert.assertFalse(view.isCopyPanelVisible());

        // Не должно быть никаких сообщений
        assertSuccessMessage("");
        assertWarningMessage("");
        assertWaitingMessage("");

        // Должно быть 2 вкладки - Параметры и Дополнительно (в которой содержится SourceWidget)
        Assert.assertEquals(2, view.getTabs().size());

        Iterator<EditorWidget> it = view.getTabs().iterator();
        ParamsTab params = (ParamsTab) it.next();
        ParamsTab additional = (ParamsTab) it.next();

        Assert.assertEquals("Параметры модели", params.getTabTitle());
        // При создании модели признак скрытости параметра игнорируется, они участвуют при создании
        Assert.assertEquals(1, params.getWidgetCountAtLeft());
        Assert.assertEquals(1, params.getWidgetCountAtRight());

        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        Assert.assertEquals(2, block.getWidgets().size());

        Assert.assertEquals(1, block.getWidgets().get(0).getParamMeta().getParamId());
        Assert.assertEquals("num1", block.getWidgets().get(0).getParamMeta().getXslName());

        Assert.assertEquals(2, block.getWidgets().get(1).getParamMeta().getParamId());
        Assert.assertEquals("num2", block.getWidgets().get(1).getParamMeta().getXslName());

        block = (BlockWidget) params.getWidgetsAtRight().get(0);
        Assert.assertEquals(1, block.getWidgets().size());

        Assert.assertEquals(4, block.getWidgets().get(0).getParamMeta().getParamId());
        Assert.assertEquals("str2", block.getWidgets().get(0).getParamMeta().getXslName());

        Assert.assertEquals("Дополнительно", additional.getTabTitle());
        Assert.assertTrue(SourcesWidget.class.isAssignableFrom(additional.getWidgetsAtLeft().get(0).getClass()));
        Assert.assertEquals(1, additional.getWidgetCountAtLeft());
        Assert.assertEquals(0, additional.getWidgetCountAtRight());
    }

    @Test
    public void testSavingNewModel() {
        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("num1").type(Param.Type.NUMERIC).name("Num1").creation(true)
            .endParameter()
            .startParameter()
                .xsl("num2").type(Param.Type.NUMERIC).name("Num2").creation(false)
            .endParameter()
            .startParameter()
                .xsl("num3").type(Param.Type.NUMERIC).name("Num3").creation(true)
            .endParameter()
            .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
                .hidden(true)
                .option(1, "Vendor1")
                .option(2, "Vendor2")
                .option(3, "Vendor3")
            .endParameter()
        .endParameters()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("num1", "num2", "num3")
                .endBlock()
            .endTab()
        .endForm()
        .startVendor()
            .source("http://source1", "ru", new Date())
            .source("http://source2", "en", new Date())
        .endVendor()
        .tovarCategory(1, 2);

        rpc.setSampleNames(Arrays.asList("name1", "name2"), null);
        rpc.setLoadModelData(data.getModelData(), null);

        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelCreator", "vendor-id=1&category-id=1")));

        ParamsTab params = (ParamsTab) view.getTabs().iterator().next();
        BlockWidget block = (BlockWidget) params.getWidgetsAtLeft().get(0);
        ParamWidget<Object> widget1 = (ParamWidget<Object>) block.getWidgets().get(0);

        // Задаем значение первого параметра, второй оставляем пустым
        widget1.getValuesWidget().getFirstValueWidget().setValue("5", true);

        // Сохраняем модель
        rpc.setSaveModel(1L, null);
        bus.fireEvent(
                new SaveModelRequest(false, false));

        // В сохраненной модели должны быть два параметра - num1 и автоматически проставленный вендор
        CommonModel savedModel = rpc.getSavedModel();
        Assert.assertEquals(2, savedModel.getParameterValues().size());
        ParameterValue num1 = savedModel.getSingleParameterValue("num1");
        Assert.assertNotNull(num1);
        Assert.assertEquals(Param.Type.NUMERIC, num1.getType());
        Assert.assertEquals(5, num1.getNumericValue().intValue());
        ParameterValue vendor = savedModel.getSingleParameterValue(XslNames.VENDOR);
        Assert.assertNotNull(vendor);
        Assert.assertEquals(Param.Type.ENUM, vendor.getType());
        Assert.assertEquals(1L, vendor.getOptionId().longValue());
    }

    @Test
    public void testOnPopulateModelSaveSyncEventFiredOnModelSave() {
        AtomicBoolean populateEventFired = new AtomicBoolean();
        bus.subscribe(PopulateModelSaveSyncEvent.class, event -> {
            populateEventFired.set(true);
        });

        ModelDataBuilder data = ModelDataBuilder.modelData()
        .startParameters()
            .startParameter()
                .xsl("num1").type(Param.Type.NUMERIC).name("Num1").creation(true)
            .endParameter()
            .startParameter()
                .xsl("num2").type(Param.Type.NUMERIC).name("Num2").creation(false)
            .endParameter()
            .startParameter()
                .xsl("num3").type(Param.Type.NUMERIC).name("Num3").creation(true)
            .endParameter()
            .startParameter()
                .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
                .hidden(true)
                .option(1, "Vendor1")
                .option(2, "Vendor2")
                .option(3, "Vendor3")
            .endParameter()
        .endParameters()
        .startForm()
            .startTab()
                .name(EditorTabs.PARAMETERS.getDisplayName())
                .startBlock()
                    .name("block")
                    .properties("num1", "num2", "num3")
                .endBlock()
            .endTab()
        .endForm()
        .startVendor()
            .source("http://source1", "ru", new Date())
            .source("http://source2", "en", new Date())
        .endVendor()
        .tovarCategory(1, 2);

        rpc.setSampleNames(Arrays.asList("name1", "name2"), null);
        rpc.setLoadModelData(data.getModelData(), null);

        bus.fireEvent(
                new PlaceShowEvent(
                        EditorUrlStub.of("modelCreator", "vendor-id=11&category-id=1")));

        // Сохраняем модель
        rpc.setSaveModel(1L, null);
        bus.fireEvent(
                new SaveModelRequest(false, false));

        Assert.assertTrue("PopulateModelSaveSyncEvent wasn't fire!", populateEventFired.get());
    }

    @Test
    public void testLocalVendorIdRedirect() {
        String expectedAnchor = "modelCreator";
        String[] expectedParams = {"category-id", "vendor-id"};
        String[] expectedValues = {"1", "2"};

        EditorUrl url = EditorUrlStub.of("modelCreator", "local-vendor-id=3");

        String token = url.withNewValueAndRemove(
            ImmutableMap.of(
                EditorUrl.CATEGORY_ID_PARAM, expectedValues[0],
                EditorUrl.VENDOR_ID_PARAM, expectedValues[1]
            ),
            Arrays.asList(EditorUrl.LOCAL_VENDOR_ID_PARAM));

        String anchor = token.split("/")[0];
        Assert.assertEquals(expectedAnchor, anchor);

        String[] params = token.split("/")[1].split("&");
        Map<String, String> mparams = new HashMap<>();
        for (String param : params) {
            String[] tokens = param.split("=");
            mparams.put(tokens[0], tokens[1]);
        }

        Assert.assertEquals(expectedParams.length, mparams.size());

        for (int i = 0; i < expectedParams.length; i++) {
            Assert.assertTrue(mparams.containsKey(expectedParams[i]));
            Assert.assertEquals(expectedValues[i], mparams.get(expectedParams[i]));
        }
    }
}
