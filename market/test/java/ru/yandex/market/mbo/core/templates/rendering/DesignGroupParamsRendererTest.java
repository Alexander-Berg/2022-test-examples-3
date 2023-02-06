package ru.yandex.market.mbo.core.templates.rendering;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.models.visual.templates.rendering.ErrorPositionInfo;
import ru.yandex.market.mbo.gwt.models.visual.templates.rendering.OutputTemplateRenderingResult;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * created on 11.11.2016
 */
@SuppressWarnings("checkstyle:magicNumber")
public class DesignGroupParamsRendererTest {

    @Test
    public void shouldOk() {
        String cookerTypeName = "CookerType";
        String ovenTypeName = "OvenType";
        String controlTypeName = "ControlType";

        long cookerTypeId = 1;
        long ovenTypeId = 2;
        long controlTypeId = 3;

        BigDecimal cookerTypeVal = new BigDecimal("123");
        BigDecimal ovenTypeVal = new BigDecimal("124");
        BigDecimal controlTypeVal = new BigDecimal("125");

        String xml = xml(cookerTypeName, ovenTypeName, controlTypeName);

        CommonModel model = new CommonModel();
        model.addParameterValue(new ParameterValue(
            cookerTypeId,
            cookerTypeName,
            Param.Type.NUMERIC,
            ParameterValue.ValueBuilder.newBuilder().setNumericValue(cookerTypeVal)));
        model.addParameterValue(new ParameterValue(
            ovenTypeId,
            ovenTypeName,
            Param.Type.NUMERIC,
            ParameterValue.ValueBuilder.newBuilder().setNumericValue(ovenTypeVal)));
        model.addParameterValue(new ParameterValue(
            controlTypeId,
            controlTypeName,
            Param.Type.NUMERIC,
            ParameterValue.ValueBuilder.newBuilder().setNumericValue(controlTypeVal)));
        model.addParameterValue(new ParameterValue(
            4,
            "unknown",
            Param.Type.NUMERIC,
            ParameterValue.ValueBuilder.newBuilder().setNumericValue(new BigDecimal("126"))));

        CategoryParam cookerTypeParam = new Parameter();
        cookerTypeParam.setId(cookerTypeId);
        cookerTypeParam.setXslName(cookerTypeName);

        CategoryParam ovenTypeParam = new Parameter();
        ovenTypeParam.setId(ovenTypeId);
        ovenTypeParam.setXslName(ovenTypeName);

        CategoryParam controlTypeParam = new Parameter();
        controlTypeParam.setId(controlTypeId);
        controlTypeParam.setXslName(controlTypeName);

        CategoryEntities categoryEntities = new CategoryEntities();
        categoryEntities.setParameters(Arrays.asList(cookerTypeParam, ovenTypeParam, controlTypeParam));

        DesignGroupParamsRenderer renderer = new DesignGroupParamsRenderer(model, xml, categoryEntities);

        OutputTemplateRenderingResult renderingResult = renderer.render();

        Assert.assertTrue(renderingResult.getErrors().isEmpty());
        Assert.assertEquals(
            xml.replace(cookerTypeName, cookerTypeVal.toPlainString())
               .replace(ovenTypeName, ovenTypeVal.toPlainString())
               .replace(controlTypeName, controlTypeVal.toPlainString()),
            renderingResult.getRenderingResult()
        );
    }

    @Test
    public void parameterNotExists() {
        long cookerTypeId = 1;
        BigDecimal cookerTypeVal = new BigDecimal("123");
        String cookerTypeName = "CookerType";
        String paramName = "123222";

        String xml = xml(paramName);

        CommonModel model = new CommonModel();
        model.addParameterValue(new ParameterValue(
            cookerTypeId,
            cookerTypeName,
            Param.Type.NUMERIC,
            ParameterValue.ValueBuilder.newBuilder().setNumericValue(cookerTypeVal)));

        CategoryParam param = new Parameter();
        param.setId(cookerTypeId);
        param.setXslName(cookerTypeName);

        CategoryEntities categoryEntities = new CategoryEntities();
        categoryEntities.setParameters(Collections.singletonList(param));

        DesignGroupParamsRenderer renderer = new DesignGroupParamsRenderer(model, xml, categoryEntities);

        OutputTemplateRenderingResult renderingResult = renderer.render();
        List<ErrorPositionInfo> renderingErrors = renderingResult.getErrors();

        Assert.assertFalse(renderingErrors.isEmpty());
        Assert.assertEquals("Parameter " + paramName + " not found", renderingErrors.get(0).getMessage());
    }

    @Test
    public void renderEnum() {
        long optionId = 2;
        String optionName = "gas";

        long paramId = 1;
        String paramName = "CookerType";

        String xml = xml(paramName);

        CommonModel model = new CommonModel();
        model.addParameterValue(new ParameterValue(
            paramId,
            paramName,
            Param.Type.ENUM,
            ParameterValue.ValueBuilder.newBuilder().setOptionId(optionId)));

        CategoryParam param = new Parameter();
        param.setId(paramId);
        param.setXslName(paramName);
        param.setOptions(Collections.singletonList(new OptionImpl(optionId, optionName)));

        CategoryEntities categoryEntities = new CategoryEntities();
        categoryEntities.setParameters(Collections.singletonList(param));

        DesignGroupParamsRenderer renderer =
            new DesignGroupParamsRenderer(model, xml, categoryEntities);

        OutputTemplateRenderingResult renderingResult = renderer.render();
        List<ErrorPositionInfo> errors = renderingResult.getErrors();

        String xmlResult = renderingResult.getRenderingResult();

        Assert.assertTrue(errors.isEmpty());
        Assert.assertEquals(xml.replace(paramName, optionName), xmlResult);
    }

    @Test
    public void renderText() {
        long paramId = 1;
        String paramName = "CookerType";

        String strVal = "газ";
        String strValEn = "gas";

        String xml = xml(paramName);

        CommonModel model = new CommonModel();
        model.addParameterValue(new ParameterValue(
            paramId,
            paramName,
            Param.Type.STRING,
            ParameterValue.ValueBuilder.newBuilder()
                .setStringValue(
                    Arrays.asList(
                        new Word(Language.RUSSIAN.getId(), strVal),
                        new Word(Language.ENGLISH.getId(), strValEn))))
        );

        CategoryParam param = new Parameter();
        param.setId(paramId);
        param.setXslName(paramName);

        CategoryEntities categoryEntities = new CategoryEntities();
        categoryEntities.setParameters(Collections.singletonList(param));

        DesignGroupParamsRenderer renderer = new DesignGroupParamsRenderer(model, xml, categoryEntities);

        OutputTemplateRenderingResult renderingResult = renderer.render();
        List<ErrorPositionInfo> errors = renderingResult.getErrors();

        String xmlResult = renderingResult.getRenderingResult();

        Assert.assertTrue(errors.isEmpty());
        Assert.assertEquals(xml.replace(paramName, strVal), xmlResult);
    }

    private static String xml(String... names) {
        StringBuilder builder = new StringBuilder("<category id=\"105942\">\n" +
            "<b-params name=\"Общие характеристики\">\n");

        for (String name : names) {
            builder.append("<value>").append(name).append("</value>\n");
        }
        return builder.append("</b-params>\n")
            .append("</category>\n")
            .toString();
    }
}
