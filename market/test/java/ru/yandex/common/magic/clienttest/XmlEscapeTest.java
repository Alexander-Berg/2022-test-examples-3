/**
 * Created by IntelliJ IDEA.
 * User: AShevenkov
 * Date: 21.08.2007
 * Time: 16:45:30
 * To change this template use File | Settings | File Templates.
 */
package ru.yandex.common.magic.clienttest;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.common.magic.client.XmlEscapeUtils;

/**
 * @author ashevenkov
 */
public class XmlEscapeTest {

    @Test
    public void testEscapeOfEq() throws Exception {
        String s = XmlEscapeUtils.escapeXml("http://www.yandex.ru/?t=1");
        System.out.println("S = " + s);
        String us = XmlEscapeUtils.unescapeXml(s);
        System.out.println("US = " + us);
    }

    @Test
    public void testEscapeOfNotEq() throws Exception {
        String str = "select * from table where id != 1";
        String escaped = XmlEscapeUtils.escapeXml(str);
        System.out.println("S = " + escaped);
        String unescaped = XmlEscapeUtils.unescapeXml(escaped);
        System.out.println("US = " + unescaped);
        Assert.assertEquals(str, unescaped);
    }

}
