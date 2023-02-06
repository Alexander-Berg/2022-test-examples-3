package ru.yandex.market.mbo.cms.core.models;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

public class CmsPagesFilterTest extends TestCase {
    @Test
    public void testSearchStringParsingSimple() {
        String searchString = "abc def";

        CmsPagesFilter filter = new CmsPagesFilter();
        filter.parseSearchString(searchString);

        Assert.assertEquals(searchString, filter.getTextQuery());
        Assert.assertEquals(true, filter.getExportParams().isEmpty());
    }

    @Test
    public void testSearchStringParsingExportParams() {
        String searchString = "aaa abc:def ab:";

        CmsPagesFilter filter = new CmsPagesFilter();
        filter.parseSearchString(searchString);

        Assert.assertEquals("aaa ab:", filter.getTextQuery());
        Assert.assertEquals(1, filter.getExportParams().size());
        Assert.assertEquals(true, filter.getExportParams().containsKey("abc"));
        Assert.assertEquals(1, filter.getExportParams().get("abc").size());
        Assert.assertEquals(true, filter.getExportParams().get("abc").contains("def"));
    }

    @Test
    public void testSearchStringParsingExportParams2() {
        String searchString = " aaa abc:def ab:  abc:123 dd:qq    ";

        CmsPagesFilter filter = new CmsPagesFilter();
        filter.parseSearchString(searchString);

        Assert.assertEquals("aaa ab:", filter.getTextQuery());
        Assert.assertEquals(2, filter.getExportParams().size());
        Assert.assertEquals(true, filter.getExportParams().containsKey("abc"));
        Assert.assertEquals(2, filter.getExportParams().get("abc").size());
        Assert.assertEquals(true, filter.getExportParams().get("abc").contains("def"));
        Assert.assertEquals(true, filter.getExportParams().get("abc").contains("123"));

        Assert.assertEquals(1, filter.getExportParams().get("dd").size());
        Assert.assertEquals(true, filter.getExportParams().get("dd").contains("qq"));
    }
}
