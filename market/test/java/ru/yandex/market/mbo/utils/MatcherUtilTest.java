package ru.yandex.market.mbo.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author amaslak
 */
public class MatcherUtilTest {

    @Test
    public void testGetVendorCode() {
        assertEquals("", MatcherUtil.getVendorCode(null));
        assertEquals("", MatcherUtil.getVendorCode(""));
        assertEquals("", MatcherUtil.getVendorCode("|Описание: aaa || bbb"));
        assertEquals("", MatcherUtil.getVendorCode("|Описание: aaa|Комментарий: bbb"));
        assertEquals("ccc", MatcherUtil.getVendorCode(
            "|Описание: aaa|Комментарий: bbb|Код производителя: ccc|Комментарий-2: ddd"
        ));
        assertEquals("ccc", MatcherUtil.getVendorCode(
            "|Описание: aaa|Комментарий: bbb|Код производителя: ccc"
        ));
        assertEquals("ccc", MatcherUtil.getVendorCode(
            "|Описание: aaa|Комментарий: bbb|Код производителя: ccc|Код производителя: aaa"
        ));
        assertEquals("ccc       ", MatcherUtil.getVendorCode(
            "|Описание: aaa|Комментарий: bbb|Код производителя: ccc       "
        ));
        assertEquals("ccc", MatcherUtil.getVendorCode(
            "|Описание: aaa|Комментарий: bbb\n|Код производителя: ccc"
        ));
    }


}
