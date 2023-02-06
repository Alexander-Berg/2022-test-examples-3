package ru.yandex.market.crm.core.services.cms;

import org.junit.Assert;
import org.junit.Test;

public class CmsArticleParserTest {
    @Test
    public void testIfArticleExists() {
        CmsArticleParser parser = new CmsArticleParser();
        CmsArticle response = parser.parse(getClass().getResourceAsStream("cms_response.json"));

        Assert.assertEquals("Томатный крем-суп за 20 минут", response.getName());
        Assert.assertEquals("blog", response.getType());
        Assert.assertEquals("56132-Tomatniy-kremsup-za-20-minut", response.getSemanticId());
        Assert.assertEquals("2018-11-01", response.getDatePublished());
    }

    @Test
    public void testIfNoArticle() {
        CmsArticleParser parser = new CmsArticleParser();
        CmsArticle response = parser.parse(getClass().getResourceAsStream("empty_cms_response.json"));

        Assert.assertNull(response);
    }
}
