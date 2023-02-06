package ru.yandex.mbo.tool.jira.MBO17108;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class AddMdmTabToCategoryFormsTest {

    private static final String TEST_FORM = "<root>\n" +
        "<tab name=\"Параметры модели\">\n" +
        "      <block name=\"Общие характеристики\">\n" +
        "         <property name=\"type\"/>\n" +
        "         <property name=\"fat_content\"/>\n" +
        "         <property name=\"milk\"/>\n" +
        "         <property name=\"termo\"/>\n" +
        "         <property name=\"package\"/>\n" +
        "         <property name=\"big_children\"/>\n" +
        "         <property name=\"grocery_shelflife\"/>\n" +
        "         <property name=\"Comment\"/>\n" +
        "         <property name=\"NDS\"/>\n" +
        "      </block>\n" +
        "      <block name=\"SKU параметры\">\n" +
        "         <property name=\"volume\"/>\n" +
        "         <property name=\"weight\"/>\n" +
        "      </block>\n" +
        "      <block name=\"Служебные поля\">\n" +
        "         <property name=\"name\"/>\n" +
        "         <property name=\"url\"/>\n" +
        "         <property name=\"additional_url\"/>\n" +
        "         <property name=\"LifeShelf\"/>\n" +
        "      </block>\n" +
        "   </tab>\n" +
        "   <tab name=\"Картинки\">\n" +
        "      <block name=\"Основная картинка\">\n" +
        "         <property name=\"BigPicture\"/>\n" +
        "         <property name=\"BigPictureUrl\"/>\n" +
        "      </block>\n" +
        "      <block name=\"Большие картинки\">\n" +
        "         <property name=\"XL-Picture\"/>\n" +
        "         <property name=\"XLPictureUrl\"/>\n" +
        "         <property name=\"XL-Picture_2\"/>\n" +
        "         <property name=\"XLPictureUrl_2\"/>\n" +
        "      </block>\n" +
        "   </tab></root>\n";

    private static final String EXPECTED_RESULT_FORM = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<root>\n" +
        "   <tab name=\"Параметры модели\">\n" +
        "      <block name=\"Общие характеристики\">\n" +
        "         <property name=\"type\"/>\n" +
        "         <property name=\"fat_content\"/>\n" +
        "         <property name=\"milk\"/>\n" +
        "         <property name=\"termo\"/>\n" +
        "         <property name=\"package\"/>\n" +
        "         <property name=\"big_children\"/>\n" +
        "         <property name=\"grocery_shelflife\"/>\n" +
        "         <property name=\"Comment\"/>\n" +
        "      </block>\n" +
        "      <block name=\"SKU параметры\">\n" +
        "         <property name=\"volume\"/>\n" +
        "         <property name=\"weight\"/>\n" +
        "      </block>\n" +
        "      <block name=\"Служебные поля\">\n" +
        "         <property name=\"name\"/>\n" +
        "         <property name=\"url\"/>\n" +
        "         <property name=\"additional_url\"/>\n" +
        "      </block>\n" +
        "   </tab>\n" +
        "   <tab name=\"Картинки\">\n" +
        "      <block name=\"Основная картинка\">\n" +
        "         <property name=\"BigPicture\"/>\n" +
        "         <property name=\"BigPictureUrl\"/>\n" +
        "      </block>\n" +
        "      <block name=\"Большие картинки\">\n" +
        "         <property name=\"XL-Picture\"/>\n" +
        "         <property name=\"XLPictureUrl\"/>\n" +
        "         <property name=\"XL-Picture_2\"/>\n" +
        "         <property name=\"XLPictureUrl_2\"/>\n" +
        "      </block>\n" +
        "   </tab>\n   <tab name=\"Мастер данные\">\n" +
        "      <block name=\"Мастер данные\">\n" +
        "         <property name=\"LifeShelf\"/>\n" +
        "         <property name=\"ShelfService\"/>\n" +
        "         <property name=\"WarrantyPeriod\"/>\n" +
        "         <property name=\"NDS\"/>\n" +
        "         <property name=\"HScode\"/>\n" +
        "      </block>\n" +
        "   </tab>\n" +
        "</root>\n";


    @Test
    public void testReplaceMarker() {
        AddMdmTabToCategoryForms tool = new AddMdmTabToCategoryForms(null, null);

        String replaced = tool.replaceMarker(TEST_FORM, 1L);
        Assert.assertThat(replaced, CoreMatchers.containsString("Мастер данные"));
        Assert.assertThat(replaced, CoreMatchers.containsString("<tab name=\"Мастер данные\">"));
        Assert.assertEquals(EXPECTED_RESULT_FORM, replaced);
    }

}
