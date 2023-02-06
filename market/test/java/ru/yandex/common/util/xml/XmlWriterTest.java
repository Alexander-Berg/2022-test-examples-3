package ru.yandex.common.util.xml;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;

/**
 * User: conterouz Date: 05.04.12 Time: 12:47
 */
public class XmlWriterTest {

    XmlWriter w;
    StringWriter _result;

    String result() {
        return _result.toString();
    }

    @Before
    public void setUp() throws Exception {
        w = new XmlWriter(_result = new StringWriter());
    }

    @Test
    public void testSimple() throws Exception {
        w.startTag("test");
        w.endTag();
        Assert.assertEquals("<test/>\n", result());
    }

    @Test
    public void testContent() throws Exception {
        w.startTag("test");
        w.text("hello");
        w.endTag();
        Assert.assertEquals("<test>hello</test>\n", result());
    }

    @Test
    public void testSubtag() throws Exception {
        w.startTag("test");
        w.tag("mustbe");
        w.endTag();
        Assert.assertEquals("<test>\n <mustbe/>\n</test>\n", result());
    }

    @Test
    public void testSubtag2() throws Exception {
        w.startTag("test");
        w.tag("mustbe", "id", "5");
        w.startTag("mustbe2");
        w.text("bla");
        w.endTag();
        w.endTag();
        Assert.assertEquals("<test>\n <mustbe id=\"5\"/>\n <mustbe2>bla</mustbe2>\n</test>\n", result());
    }

    @Test
    public void testSubtag2_1() throws Exception {
        w.startTag("test");
        w.tag("mustbe", "id", "5");
        w.tag("mustbe2");
        w.endTag();
        Assert.assertEquals("<test>\n <mustbe id=\"5\"/>\n <mustbe2/>\n</test>\n", result());
    }

    @Test
    public void testText() throws Exception {
        w.startTag("test");
        w.startTag("test");
        w.startTag("test");
        w.text("blah");
        w.endTag();
        w.endTag();
        w.endTag();
        Assert.assertEquals("<test>\n <test>\n  <test>blah</test>\n </test>\n</test>\n", result());
    }

    @Test
    public void testText2() throws Exception {
        w.startTag("categories");
            w.startTag("category", "id", "7812196");
                w.startTag("page", "name", "new", "id", "8228652");
                    w.startTag("layout", "id", "help1");
                        w.startTag("widget", "id" , "tables");
                            w.xml("<table>\n" +
                                "    <tr><td></td></tr>\n" +
                                "    <tr><td></td></tr>\n" +
                                "</table>");
                        w.endTag();
                        w.startTag("widget", "id", "text");
                            w.xml("just text with <b>bold</b> and other simple HTML");
                        w.endTag();
                        w.startTag("widget", "id", "banner");
                            w.xml("http://cs-elliptics01ft.yandex.ru:88/get/market/cms_resources/8228652/facebook.jpeg");
                        w.endTag();
                    w.endTag();
                w.endTag();
            w.endTag();
        w.endTag();
        Assert.assertEquals("<categories>\n" +
                " <category id=\"7812196\">\n" +
                "  <page name=\"new\" id=\"8228652\">\n" +
                "   <layout id=\"help1\">\n" +
                "    <widget id=\"tables\"><table>\n" +
                "    <tr><td></td></tr>\n" +
                "    <tr><td></td></tr>\n" +
                "</table></widget>\n" +
                "    <widget id=\"text\">just text with <b>bold</b> and other simple HTML</widget>\n" +
                "    <widget id=\"banner\">http://cs-elliptics01ft.yandex.ru:88/get/market/cms_resources/8228652/facebook.jpeg</widget>\n" +
                "   </layout>\n" +
                "  </page>\n" +
                " </category>\n" +
                "</categories>\n", result());
    }
}
