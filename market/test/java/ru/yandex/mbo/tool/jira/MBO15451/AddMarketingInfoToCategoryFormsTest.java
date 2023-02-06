package ru.yandex.mbo.tool.jira.MBO15451;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author yuramalinov
 * @created 24.04.18
 */
public class AddMarketingInfoToCategoryFormsTest {

    private AddMarketingInfoToCategoryForms addMarketingInfoToCategoryForms;

    @Before
    public void setUp() throws Exception {
        addMarketingInfoToCategoryForms = new AddMarketingInfoToCategoryForms(null, null);
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
            "</tab>\n";

        String replaced = addMarketingInfoToCategoryForms.replaceMarker(xml, 1L);

        Assert.assertThat(replaced, CoreMatchers.containsString("Маркетинговое описание"));
        Assert.assertThat(replaced, CoreMatchers.containsString("Служебные поля"));
        Assert.assertEquals("<tab name=\"Параметры модели\">\n" +
            "    <block name=\"Общие характеристики\">\n" +
            "        <property name=\"type\"/>\n" +
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
            "    <block name=\"Маркетинговое описание\">\n" +
            "        <property name=\"description\"/>\n" +
            "        <property name=\"draft_description\"/>\n" +
            "        <property name=\"source_description\"/>\n" +
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
            "</tab>\n", replaced);
    }

}
