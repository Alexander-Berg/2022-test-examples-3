package ru.yandex.market.mbo.cms.api.view.converter.schema;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CmsPathUtilsTest {

    @Test
    public void getNodePath() {
        assertEquals("aaa[AAA]/bbb[BBB]",
                CmsPathUtils.getNodePath("aaa[AAA]/bbb[BBB]/NAME"));
        assertEquals("aaa[AAA]/bbb[BBB]",
                CmsPathUtils.getNodePath("aaa[AAA]/bbb[BBB]"));
    }
}
