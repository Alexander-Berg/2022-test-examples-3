package ru.yandex.calendar.util.xml;

import java.io.IOException;
import java.io.StringWriter;

import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.function.Function;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.util.base.AuxBase;
import ru.yandex.calendar.util.xml.TagReplacement.Action;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.xml.jdom.JdomUtils;

/**
 * @author ssytnik
 */
public class TagReplacementTest {
    private static final String RESULT = "result";
    private static final String A_YARU_HREF = "<a href=\"http://ya.ru\">href</a>";

    // Tests

    @Test
    public void justText() {
        final String src = "Just text";
        assertInnerTextEquals(src, trAll(src, Action.CONVERT_XML));
        assertInnerTextEquals(src, trAll(src, Action.TEXT_STRIP));
    }

    @Test
    public void simple() {
        final String src = "There is a <b>bold</b> text";
        final String itStrip = "There is a bold text";
        Assert.assertEquals(1, trAll(src, Action.CONVERT_XML).getChildren("b").size());
        assertInnerTextEquals(itStrip, trAll(src, Action.TEXT_STRIP));
    }

    @Test
    public void eitherXmlOrTextButNotBoth() {
        final String src = "<b>bold</b>";
        final String ixStrip = "bold";
        assertInnerXmlEquals(src, trAll(src, Action.CONVERT_XML));
        assertInnerXmlEquals(ixStrip, trAll(src, Action.TEXT_STRIP));
    }

    @Test
    public void textNotEqualsXml() {
        final String src = "<b>Bold</b> text with a " + A_YARU_HREF;
        final String itXml = " text with a ";
        Element e = trAll(src, Action.CONVERT_XML);
        String eInnerText = innerText(e);
        String eXml = xml(e);
        Assert.A.equals(itXml, eInnerText);
        Assert.A.equals(xmlFromInnerXml(src), eXml);
        //AssertF.assertFalse(eXml.equals(xmlFromInnerXml(eInnerText)));
    }

    @Test
    public void replaceWithBrs() {
        String src = "<unknown>\n</unknown>\n<a href=\"www.ru\">before\nafter</a>";
        String res = "&lt;unknown&gt;<br />&lt;/unknown&gt;<br /><a href=\"http://www.ru\">before<br />after</a>";

        Assert.equals(res, TagReplacement.processText(src, Action.CONVERT_XML, TagReplacement.values()));
    }

    @Test
    public void ignoreMalformed() {
        String src = "<a><x><b>text</b></x></a>";
        String res = "&lt;a&gt;&lt;x&gt;<b>text</b>&lt;/x&gt;&lt;/a&gt;";

        Assert.equals(res, TagReplacement.processText(src, Action.CONVERT_XML, TagReplacement.values()));

        src = "<a><x><b>text</x></b></a>";
        res = "&lt;a&gt;&lt;x&gt;<b>text&lt;/x&gt;</b>&lt;/a&gt;";

        Assert.equals(res, TagReplacement.processText(src, Action.CONVERT_XML, TagReplacement.values()));
    }

    @Test
    public void caseInsensitivity() {
        final String src = "<b>case</b>";
        assertInnerTextEquals("case", trAll(src, Action.TEXT_STRIP));
        assertInnerTextEquals("case", trAll(src.replace('b', 'B'), Action.TEXT_STRIP));
        assertInnerTextEquals("<x>case</x>", trAll(src.replace('b', 'x'), Action.TEXT_STRIP));
    }

    @Test
    public void allowedTags() {
        final String src = "<p>par</p>, " + A_YARU_HREF + ", <b>bold</b>, <br/>";
        for (int i = 0; i < 2; ++i) {
            TagReplacement[] allowedTags = i == 0 ? TagReplacement.values() : new TagReplacement[0];
            Element e = tr(src, Action.CONVERT_XML, allowedTags);
            int expectedCount = i == 0 ? 1 : 0;
            for (TagReplacement allowedTag : allowedTags) {
                String tagName = allowedTag.toString().toLowerCase();
                String msg = "i = " + i + ", tag = " + allowedTag;
                Assert.assertEquals(msg, expectedCount, e.getChildren(tagName).size());
            }
        }
    }

    @Test
    public void attributesMatchedAfterSingleQuotedConversion() {
        final String src = AuxBase.toSingleQuotedStringExceptQuot(A_YARU_HREF);
        Assert.assertNotEmpty(tr(src, Action.CONVERT_XML, TagReplacement.A).getChildren("a"));
    }

    @Test
    public void unknownTags() {
        final String src ="<i>italics</i>, <javascript>js</javascript>, <noscript />";
        Assert.assertTrue(trAll(src, Action.CONVERT_XML).getChildren().isEmpty());
    }

    @Test
    public void nestedTags() {
        final String src = "<a href=\"http://www.img.com\"><b>text</b></a>";
        final String itStrip = "text (http://www.img.com)";
        assertInnerTextEquals(itStrip, trAll(src, Action.TEXT_STRIP));
        Assert.assertEquals(1, trAll(src, Action.CONVERT_XML).getChild("a").getChildren("b").size());
    }

    @Test
    public void badNestedTagsTreatedAsText() {
        final String src = "<a href=\"http://www.img.com\">" + A_YARU_HREF + "</a>";
        // inner <a> will match as a part of contents, inner </a> - as outer </a>,
        // while outer </a> won't match and will be treated as a plain text.
        // That's strange, but we are not going to support same nested tags here.
        final String itStrip = "<a href=\"http://ya.ru\">href (http://www.img.com)</a>";
        assertInnerTextEquals(itStrip, trAll(src, Action.TEXT_STRIP));
        Assert.assertEquals(1, trAll(src, Action.CONVERT_XML).getChildren("a").size());
    }

    @Test
    public void badAttributes() {
        final String src = "<a /><img /><a some=\"value\">text</a>";
        final String itBothAsIsAndStrip = "<a /><img /><a some=\"value\">text</a>";
        Assert.assertTrue(trAll(src, Action.CONVERT_XML).getChildren().isEmpty());
        assertInnerTextEquals(itBothAsIsAndStrip, trAll(src, Action.TEXT_STRIP));
    }

    @Test
    public void aHrefEqAContents() {
        final String uri = "http://www.test.com/contents";
        final String src = "Link is: <a href=\"" + uri + "\">" + uri + "</a>.";
        final String itStrip = "Link is: http://www.test.com/contents.";
        assertInnerTextEquals(itStrip, trAll(src, Action.TEXT_STRIP));
    }

    @Test
    public void severalParagraphs() {
        final String src = "<p>Paragraph one.</p><p>Paragraph two, are there spaces between?</p>";
        final String itStrip = "Paragraph one. Paragraph two, are there spaces between?";
        assertInnerTextEquals(itStrip, trAll(src, Action.TEXT_STRIP));
    }

    @Test
    public void attributeValidation() {
        String[] srcArray = {
                "<a href=\"http://www.yandex.ru\">http-www-link</a>",
                "<a href=\"www.yandex.ru\">www-link</a>",
                "<a href=\"yandex.ru\">bad-unknown-link</a>",
                "<a href=\"javascript:alert(1);\">bad-js-link</a>",
                "<a href=\'www.yandex.ru\'>bad-quotes-link</a>"
        };
        String[] expectedResultArray = {
                "<a href=\"http://www.yandex.ru\">http-www-link</a>",
                "<a href=\"http://www.yandex.ru\">www-link</a>",
                "<a href=\"<a href=\"http://yandex.ru\">yandex.ru</a>\">bad-unknown-link</a>",
                "<a href=\"javascript:alert(1);\">bad-js-link</a>",
                "<a href='<a href=\"http://www.yandex.ru\">www.yandex.ru</a>'>bad-quotes-link</a>"
        };

        for (Tuple2<String, String> srcResult : Cf.x(srcArray).zip(Cf.x(expectedResultArray))) {
            assertInnerXmlEquals(srcResult.get2(), tr(srcResult.get1(), Action.CONVERT_XML, EventRoutines.TAGS));
        }
    }

    @Test
    public void bigText() {
        final String src =
            "<p>This is a paragraph with <b  ignore=\"sth\"  >bold text</b> and " +
            "<a href=\"www.yandex.ru\">link with <b>bold</b> text and invalid <p /></a>.</p>" +
            "<p>Also, there is a <b><b>(same inner tags are not supported!)</b></b> content </p>";
        final String itStrip =
            "This is a paragraph with bold text and " +
            "link with bold text and invalid <p /> (http://www.yandex.ru). " +
            "Also, there is a <b>(same inner tags are not supported!)</b> content ";

        Assert.assertEquals(2, trAll(src, Action.CONVERT_XML).getChildren("p").size());
        assertInnerTextEquals(itStrip, trAll(src, Action.TEXT_STRIP));
    }

    @Test
    public void bug_2010_10_18() {
        String link = "http://kompas.ru/webinar/register/?ev_id=460";
        String src = "<a href=\" " + link + "\">Регистрация тут.</a>";
        String result = "<a href=\" <a href=\"" + link + "\">" + link + "</a>\">Регистрация тут.</a>";

        assertInnerXmlEquals(result, tr(src, Action.CONVERT_XML, EventRoutines.TAGS));
    }

    @Test
    public void severalAttributes() { // mainly for manual debug / check that attributes are parsed well
        final String src =
            "<a" +
                " attone=\"value1\"" +
                " atttwo=\" v2_spaces \"" +
                " href=\"http://www.example.com\"" +
                " attthree=\"value3\"" +
            ">" +
                "link contents" +
            "</a>";
        final String itStrip = "link contents (http://www.example.com)";
        assertInnerTextEquals(itStrip, trAll(src, Action.TEXT_STRIP));
    }

    @Test
    public void replaceAsText() {
        String src = "<b>bold</b> text <unknown/>";
        String res = "<b>bold</b> text &lt;unknown/&gt;";
        Assert.equals(res, TagReplacement.processText(src, Action.CONVERT_XML, TagReplacement.values()));
    }

    @Test
    public void linkSign() {
        Function<String, String> processSign = key ->
                TagReplacement.processText(RESULT, A_YARU_HREF, Action.CONVERT_XML, Option.of(key), TagReplacement.A)
                        .getChild("a").getAttributeValue("data-sign");

        Assert.equals("75320f0f0287ad58358be1a9228974f2", processSign.apply("calendar:254df3b"));
        Assert.equals("b,996daf8f9fe0611d21fc4dfd1f9562dc", processSign.apply("b:254df3b"));
    }

    // Test aux

    private Element trAll(String text, Action action) {
        return TagReplacement.processTextAllTags(RESULT, text, action);
    }
    private Element tr(String text, Action action, TagReplacement... allowedTags) {
        return TagReplacement.processText(RESULT, text, action, allowedTags);
    }
    private String innerText(Element e) {
        return e.getText();
    }
    private void assertInnerTextEquals(String s, Element e) {
        Assert.A.equals(s, innerText(e));
    }
    private String xml(Element e) {
        return writeXmlNoEscapeData(e);
    }
    private String xmlFromInnerXml(String s) {
        return "<" + RESULT + ">" + s + "</" + RESULT + ">";
    }
    private void assertInnerXmlEquals(String s, Element e) {
        Assert.A.equals(xmlFromInnerXml(s), xml(e));
    }
    private String writeXmlNoEscapeData(Element element) {
        Format f = Format.getRawFormat();
        XMLOutputter out = new NoEscapeXmlOutputter(f);
        StringWriter writer = new StringWriter();
        try {
            out.output(element, writer);
        } catch (IOException e) {
            throw JdomUtils.I.translate(e);
        }
        return writer.toString();
    }
}
