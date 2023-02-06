package ru.yandex.mbo.tool.jira.MBO15473;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.db.params.ParameterLoaderService;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Parameter;

/**
 * @author commince
 * @date 28.05.2018
 */
public class AddSkuParametersToCategoryFormsTest {
    private AddSkuParametersToCategoryForms addSkuParametersToCategoryForms;

    @Before
    public void setUp() throws Exception {
        addSkuParametersToCategoryForms = new AddSkuParametersToCategoryForms(makeMock(), null, null);
    }

    @Test
    public void testReplaceMarker() {

        String xml = "<tab name=\"Параметры модели\">\n" +
            "    <block name=\"Общие характеристики\">\n" +
            "        <property name=\"type\"/>\n" +
            "        <property name=\"description\"/>" +
            "        <property name=\"filter_type\"/>\n" +
            "    </block>" +
            "    <block name=\"Характеристики\">\n" +
            "        <property name=\"type\"/>\n" +
            "        <property name=\"filter_type\"/>\n" +
            "        <property name=\"performance\"/>\n" +
            "        <property name=\"max_volume\"/>\n" +
            "        <property name=\"lenght\"/>\n" +
            "    </block>\n" +
            "    <block name=\"Алиасы\">\n" +
            "        <property name=\"aliases\"/>\n" +
            "    </block>\n" +
            "    <block name=\"Служебные поля\">\n" +
            "        <property name=\"name\"/>\n" +
            "        <property name=\"url\"/>\n" +
            "        <property name=\"additional_url\"/>\n" +
            "        <property name=\"InstructionLink\"/>\n" +
            "        <property name=\"InstrLang\"/>\n" +
            "        <property name=\"Preview\"/>\n" +
            "        <property name=\"IsSku\"/>\n" +
            "        <property name=\"NotFound\"/>\n" +
            "        <property name=\"DBFilledOK\"/>\n" +
            "    </block>\n" +
            "</tab>\n" +
            "<tab name=\"Картинки\">\n" +
            "    <block name=\"Картинки модели\">\n" +
            "        <property name=\"XL-Picture\"/>\n" +
            "        <property name=\"XLPictureUrl\"/>\n" +
            "        <property name=\"XL-Picture_2\"/>\n" +
            "        <property name=\"XLPictureUrl_2\"/>\n" +
            "    </block>\n" +
            "</tab>\n";

        String replaced = addSkuParametersToCategoryForms.replaceMarker(xml, 1L);

        Assert.assertThat(replaced, CoreMatchers.containsString("SKU параметры"));
        Assert.assertThat(replaced, CoreMatchers.containsString("Служебные поля"));
        Assert.assertEquals("<tab name=\"Параметры модели\">\n" +
            "    <block name=\"Общие характеристики\">\n" +
            "        <property name=\"type\"/>\n" +
            "        <property name=\"description\"/>" +
            "        <property name=\"filter_type\"/>\n" +
            "    </block>" +
            "    <block name=\"Характеристики\">\n" +
            "        <property name=\"type\"/>\n" +
            "        <property name=\"filter_type\"/>\n" +
            "        <property name=\"performance\"/>\n" +
            "        <property name=\"max_volume\"/>\n" +
            "        <property name=\"lenght\"/>\n" +
            "    </block>\n" +
            "    <block name=\"Алиасы\">\n" +
            "        <property name=\"aliases\"/>\n" +
            "    </block>\n" +
            "    <block name=\"SKU параметры\">\n" +
            "        <property name=\"test1\"/>\n" +
            "        <property name=\"test2\"/>\n" +
            "        <property name=\"test3\"/>\n" +
            "    </block>\n" +
            "    <block name=\"Служебные поля\">\n" +
            "        <property name=\"name\"/>\n" +
            "        <property name=\"url\"/>\n" +
            "        <property name=\"additional_url\"/>\n" +
            "        <property name=\"InstructionLink\"/>\n" +
            "        <property name=\"InstrLang\"/>\n" +
            "        <property name=\"Preview\"/>\n" +
            "        <property name=\"IsSku\"/>\n" +
            "        <property name=\"NotFound\"/>\n" +
            "        <property name=\"DBFilledOK\"/>\n" +
            "    </block>\n" +
            "</tab>\n" +
            "<tab name=\"Картинки\">\n" +
            "    <block name=\"Картинки модели\">\n" +
            "        <property name=\"XL-Picture\"/>\n" +
            "        <property name=\"XLPictureUrl\"/>\n" +
            "        <property name=\"XL-Picture_2\"/>\n" +
            "        <property name=\"XLPictureUrl_2\"/>\n" +
            "    </block>\n" +
            "</tab>\n", replaced);
    }

    private IParameterLoaderService makeMock() {
        return new ParameterLoaderService(null, null, null, null, null, null, -1) {
            @Override
            public CategoryEntities loadCategoryEntitiesByHid(long hid) {
                CategoryParam p1 = new Parameter();
                p1.setXslName("test1");
                p1.setLevel(CategoryParam.Level.OFFER);
                p1.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
                CategoryParam p2 = new Parameter();
                p2.setXslName("test2");
                p2.setLevel(CategoryParam.Level.OFFER);
                p2.setSkuParameterMode(SkuParameterMode.SKU_INFORMATIONAL);
                p2.setMandatory(true);
                CategoryParam p3 = new Parameter();
                p3.setXslName("test3");
                p3.setLevel(CategoryParam.Level.OFFER);
                p3.setShowOnSkuTab(true);
                CategoryParam p4 = new Parameter();
                p4.setXslName("test4");
                p4.setLevel(CategoryParam.Level.OFFER);
                p2.setSkuParameterMode(SkuParameterMode.SKU_INFORMATIONAL);

                CategoryEntities result = new CategoryEntities();
                result.addParameter(p1);
                result.addParameter(p2);
                result.addParameter(p3);
                result.addParameter(p4);
                return result;
            }
        };
    }
}
