package ru.yandex.common.util.json;

import junit.framework.TestCase;

/**
 * Date: 1/17/11
 *
 * @author Alexander Astakhov (leftie@yandex-team.ru)
 */
public class ExtendedJsonParametersSourceTest extends TestCase {

    public void testGetParams() throws Exception {
        assertEquals("bar", new ExtendedJsonParametersSource("{foo:{\"baz\":\"bar\"}}").getParams("foo").getParam("baz"));
    }

    public void testGetParam() throws Exception {
        final ExtendedJsonParametersSource params = new ExtendedJsonParametersSource("{x:{idx:0},y0:{idx:1},type:PIE2D}");
        assertEquals("PIE2D", params.getParam("type", "LINE"));
    }

    public void testGetParamWithSpace() throws Exception {
        final ExtendedJsonParametersSource params = new ExtendedJsonParametersSource("{hide: 1 }");
        assertEquals("1", params.getParam("hide"));
        assertEquals(true, params.getParamAsInt("hide", 0) == 1);
    }

    public void testOrdering() throws Exception {
        final StringBuilder sb = new StringBuilder(256);
        sb.append("{");
        for (int i = 0; i < 99; i++) {
            sb.append("y" + i).append(":{");
            sb.append("foo:\"bar\"}, ");

        }
        sb.append("y100:{foo:\"bar\"}");

        sb.append("}");

        System.out.println(sb.toString());
        final ExtendedJsonParametersSource p = new ExtendedJsonParametersSource(sb.toString());

        System.out.println(p.getNames());
    }

}
