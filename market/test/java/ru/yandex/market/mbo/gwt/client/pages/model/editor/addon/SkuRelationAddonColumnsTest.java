package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelDataBuilder;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.AbstractModelTest;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.SkuRelationWidgetStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.view.api.EditorWidget;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Тест на проверку отображаемости параметров в колонках редактора модели на табе SKU.
 */
public class SkuRelationAddonColumnsTest extends AbstractModelTest {

    private static final List<String> BAD_XSL_NAMES = Arrays.asList("str", "agl", "mpw", "int", "chr");
    private static final List<String> GOOD_XSL_NAMES = Arrays.asList(XslNames.BAR_CODE, "A", "I", "G");

    @Test
    public void skuTabLoadedWithNoSkuColumns() {
        //конструируем модель с какими-то рандомными параметрами, которые не должны отображаться на табе SKU
        initializeModelWithoutShowOnSkuParams();

        //убедимся, что таба появилась
        EditorWidget tab = editor.getView().getTab(EditorTabs.SKU.getDisplayName());
        assertNotNull(tab);

        //проверим, что среди параметров (=колонок) SKU-таблицы нет ни одного из бестолковых параметров
        SkuRelationWidgetStub tabWidget = (SkuRelationWidgetStub) tab;
        List<CategoryParam> skuParams = tabWidget.getParams()
            .stream()
            .filter(p -> BAD_XSL_NAMES.contains(p.getXslName()))
            .collect(Collectors.toList());
        assertTrue(skuParams.isEmpty());

        //а вот Баркод всегда должен быть:
        Optional<CategoryParam> maybeBarcode = tabWidget.getParams()
            .stream()
            .filter(p -> XslNames.BAR_CODE.equals(p.getXslName()))
            .findAny();
        assertTrue(maybeBarcode.isPresent());
    }

    @Test
    public void skuTabLoadedWithSomeSkuColumns() {
        //конструируем модель с какими-то рандомными параметрами, некоторые из которых должны отображаться на табе SKU
        initializeModelWithShowOnSkuParams();

        //убедимся, что таба появилась
        EditorWidget tab = editor.getView().getTab(EditorTabs.SKU.getDisplayName());
        assertNotNull(tab);

        //проверим, что среди параметров (=колонок) SKU-таблицы есть только те, которые удовлетворяют одному из условий:
        //1. Определяющий параметр
        //2. Информационный + обязательный
        //3. Информационный + показывать в СКУ
        //4. "Баркод"
        // При этом баркод всегда должен быть первым (слева в UI).
        SkuRelationWidgetStub tabWidget = (SkuRelationWidgetStub) tab;
        List<CategoryParam> skuParams = tabWidget.getParams()
            .stream()
            .filter(p -> BAD_XSL_NAMES.contains(p.getXslName()))
            .collect(Collectors.toList());
        assertTrue(skuParams.isEmpty());

        Optional<CategoryParam> maybeBarcode = tabWidget.getParams()
            .stream()
            .filter(p -> XslNames.BAR_CODE.equals(p.getXslName()))
            .findAny();
        assertTrue(maybeBarcode.isPresent());

        assertEquals(tabWidget.getParams().get(0), maybeBarcode.get()); //первый в списке
        assertEquals(tabWidget.getParams().size(), GOOD_XSL_NAMES.size()); //проверяем состав
        List<CategoryParam> goodParams = tabWidget.getParams()
            .stream()
            .filter(p -> GOOD_XSL_NAMES.contains(p.getXslName()))
            .collect(Collectors.toList());
        assertEquals(tabWidget.getParams(), goodParams);
    }

    private void initializeModelWithShowOnSkuParams() {
        data = ModelDataBuilder.modelData()
            .startParameters()

            .startParameter()
            .xsl("str").type(Param.Type.NUMERIC).name("Абрам").showOnSkuTab(true)
            .endParameter()

            .startParameter()
            .xsl("A").type(Param.Type.NUMERIC).name("Авив").skuParameterMode(SkuParameterMode.SKU_DEFINING)
            .endParameter()

            .startParameter()
            .xsl("I").type(Param.Type.NUMERIC).name("Измаил").showOnSkuTab(true)
            .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL)
            .endParameter()

            .startParameter()
            .xsl(XslNames.BAR_CODE).type(Param.Type.NUMERIC).name("Баркод").showOnSkuTab(true)
            .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL)
            .endParameter()

            .startParameter()
            .xsl("agl").type(Param.Type.NUMERIC).name("Яков")
            .endParameter()

            .startParameter()
            .xsl("G").type(Param.Type.NUMERIC).name("George").mandatory(true)
            .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL)
            .endParameter()

            .startParameter()
            .xsl("mpw").type(Param.Type.NUMERIC).name("John").skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL)
            .endParameter()

            .endParameters()

            .startModel()
            .id(1).category(2).source(CommonModel.Source.GURU).currentType(CommonModel.Source.GURU)
            .endModel()
            .startVendor()
            .source("http://source1", "ru", new Date())
            .source("http://source2", "en", new Date())
            .endVendor()
            .tovarCategory(1, 2);

        editableModel.setOriginalModel(data.getModel());
        editableModel.setModel(data.getModel());
        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);
        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1")));
        super.run();
    }

    private void initializeModelWithoutShowOnSkuParams() {
        data = ModelDataBuilder.modelData()
            .startParameters()

            .startParameter()
            .xsl(XslNames.BAR_CODE).type(Param.Type.NUMERIC).name("Баркод").showOnSkuTab(true)
            .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL)
            .endParameter()

            .startParameter()
            .xsl("str").type(Param.Type.NUMERIC).name("STRENGTH")
            .endParameter()

            .startParameter()
            .xsl("agl").type(Param.Type.NUMERIC).name("AGILITY")
            .endParameter()

            .startParameter()
            .xsl("mpw").type(Param.Type.NUMERIC).name("MAGIC POWER").showOnSkuTab(false)
            .endParameter()

            .startParameter()
            .xsl("int").type(Param.Type.NUMERIC).name("INTELLIGENCE")
            .endParameter()

            .startParameter()
            .xsl("chr").type(Param.Type.NUMERIC).name("CHARISMA").showOnSkuTab(false)
            .endParameter()

            .endParameters()

            .startModel()
            .id(1).category(2).source(CommonModel.Source.GURU).currentType(CommonModel.Source.GURU)
            .endModel()
            .startVendor()
            .source("http://source1", "ru", new Date())
            .source("http://source2", "en", new Date())
            .endVendor()
            .tovarCategory(1, 2);

        editableModel.setOriginalModel(data.getModel());
        editableModel.setModel(data.getModel());
        rpc.setLoadModel(data.getModel(), null);
        rpc.setLoadModelData(data.getModelData(), null);
        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", "entity-id=1")));
        super.run();
    }

}
