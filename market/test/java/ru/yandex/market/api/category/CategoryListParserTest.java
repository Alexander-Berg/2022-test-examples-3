package ru.yandex.market.api.category;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

public class CategoryListParserTest extends UnitTestBase {

    @Test
    public void categoryParseTest(){
        CategoryListParser parser = new CategoryListParser();
        List<Category> categoryList = parser.parse(ResourceHelpers.getResource("catalog_dump.xml"));

        Assert.assertEquals(CategoryType.gurulight, categoryList.get(0).getType());
        Assert.assertEquals(CategoryType.guru, categoryList.get(1).getType());
        Assert.assertEquals(CategoryType.nonguru, categoryList.get(2).getType());

        Assert.assertEquals(false, categoryList.get(0).isGuru());
        Assert.assertEquals(true, categoryList.get(1).isGuru());

        Assert.assertEquals(false, categoryList.get(1).isVisual());
        Assert.assertEquals(true, categoryList.get(2).isVisual());

    }
}
