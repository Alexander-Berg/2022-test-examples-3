package ru.yandex.common.mining.bd;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import ru.yandex.common.mining.bd.AttributeJudgeSchool.FactAboutAttribute;
import static ru.yandex.common.mining.bd.XmlElementFilter.Decision.BAD;
import static ru.yandex.common.mining.bd.XmlElementFilter.Decision.GOOD;
import ru.yandex.common.util.XPathUtils;
import static ru.yandex.common.util.XmlUtils.parseSource;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Kirpichov
 */
public class UnifyingJudgeSchoolTest {
    private AttributeJudgeSchool school = new AttributeJudgeSchool();


    @Before
    public void init() throws Exception {

    }

    @Test
    public void testClassify() throws Exception {
        String source =
            "<pack>\n" +
                "\t<item id=\"1\" false=\"false\" good=\"true\" constGood=\"true\" maybe1=\"yes\"/>\n" +
                "\t<item id=\"2\" false=\"false\" good=\"true\" constGood=\"true\" maybe1=\"yes\" maybe2=\"yes\"/>\n" +
                "\t<item id=\"3\" false=\"false\" good=\"true\" constGood=\"true\" maybe1=\"yes\"/>\n" +
                "\t<item id=\"4\" false=\"false\" good=\"true\" constGood=\"true\" maybe1=\"yes\" maybe2=\"yes\"/>\n" +
                "\t<item id=\"5\" false=\"false\" good=\"false\" constGood=\"bar\" maybe2=\"yes\"/>\n" +
                "\t<item id=\"6\" false=\"false\" good=\"false\" constGood=\"foo\"/>\n" +
                "\t<item id=\"7\" false=\"false\" good=\"false\" constGood=\"baz\"/>\n" +
                "\t<item id=\"8\" false=\"false\" good=\"false\" constGood=\"qux\" maybe2=\"yes\"/>\n" +
                "</pack>\n";
        Document doc = parseSource(source);
        List<Element> items = XPathUtils.queryElementList("//item", doc);

        assertEquals(FactAboutAttribute.USELESS, school.classify("false", items.subList(0, 3), items.subList(3, 6)));
        assertEquals(FactAboutAttribute.USELESS, school.classify("false", items.subList(0, 1), items.subList(3, 6)));
        assertEquals(FactAboutAttribute.USELESS, school.classify("false", items.subList(0, 3), items.subList(3, 4)));
        assertEquals(FactAboutAttribute.USELESS, school.classify("id", items.subList(0, 3), items.subList(3, 6)));
        assertEquals(FactAboutAttribute.CONSTANT_FOR_GOODS,
            school.classify("id", items.subList(0, 1), items.subList(3, 6)));
        assertEquals(FactAboutAttribute.CONSTANT_FOR_BADS,
            school.classify("id", items.subList(0, 3), items.subList(3, 4)));

        assertEquals(FactAboutAttribute.CONSTANT_FOR_GOODS,
            school.classify("constGood", items.subList(0, 4), items.subList(4, 8)));
        assertEquals(FactAboutAttribute.CONSTANT_FOR_BADS,
            school.classify("constGood", items.subList(4, 8), items.subList(0, 4)));

        assertEquals(FactAboutAttribute.CONSTANT_FOR_BADS,
            school.classify("maybe1", items.subList(0, 4), items.subList(4, 8)));
        assertEquals(FactAboutAttribute.CONSTANT_FOR_GOODS,
            school.classify("maybe1", items.subList(4, 8), items.subList(0, 4)));

        assertEquals(FactAboutAttribute.USELESS,
            school.classify("maybe2", items.subList(0, 4), items.subList(4, 8)));
        assertEquals(FactAboutAttribute.USELESS,
            school.classify("maybe2", items.subList(4, 8), items.subList(0, 4)));


        assertEquals(FactAboutAttribute.USELESS,
            school.classify("maybe2", items.subList(0, 4), items.subList(4, 7)));
        assertEquals(FactAboutAttribute.USELESS,
            school.classify("maybe2", items.subList(4, 7), items.subList(0, 4)));


        assertEquals(FactAboutAttribute.USELESS,
            school.classify("maybe2", items.subList(0, 4), items.subList(4, 7)));
        assertEquals(FactAboutAttribute.USELESS,
            school.classify("maybe2", items.subList(4, 7), items.subList(0, 4)));


        assertEquals(FactAboutAttribute.USELESS,
            school.classify("maybe2", items.subList(0, 4), items.subList(5, 7)));
        assertEquals(FactAboutAttribute.USELESS,
            school.classify("maybe2", items.subList(5, 7), items.subList(0, 4)));
    }

    @Test
    public void testBuildCluesOnIndistinguishableBecauseNoAttributes() throws Exception {
        List<Element> items = XPathUtils.queryElementList("//item", parseSource(
                "<pack>\n" +
                        "\t<item/>\n" +
                        "\t<item/>\n" +
                        "\t<item/>\n" +
                        "\t<item/>\n" +
                        "\t<item/>\n" +
                        "</pack>"));
        assertEquals(0, school.buildClues(items.subList(0, 3), items.subList(3, 5)).size());
    }

    @Test
    public void testBuildCluesOnIndistinguishableBecauseOfUselessAttributes() throws Exception {
        List<Element> items = XPathUtils.queryElementList("//item", parseSource(
                "<pack>\n" +
                        "\t<item id=\"1\" true=\"true\"/>\n" +
                        "\t<item id=\"2\" true=\"true\"/>\n" +
                        "\t<item id=\"3\" true=\"true\"/>\n" +
                        "\t<item id=\"4\" true=\"true\"/>\n" +
                        "\t<item id=\"5\" true=\"true\"/>\n" +
                        "</pack>"));
        assertEquals(0, school.buildClues(items.subList(0, 3), items.subList(3, 5)).size());
    }

    @Test
    public void testBuildCluesOnGoodOnly() throws Exception {
        List<Element> items = XPathUtils.queryElementList("//item", parseSource(
                "<pack>\n" +
                        "\t<item/>\n" +
                        "\t<item/>\n" +
                        "\t<item/>\n" +
                        "\t<item/>\n" +
                        "\t<item/>\n" +
                        "</pack>"));
        assertEquals(0, school.buildClues(items, new ArrayList<Element>()).size());
    }

    @Test
    public void testBuildCluesOnDistinguishableByNullableAttribute() throws Exception {
        List<Element> items = XPathUtils.queryElementList("//item", parseSource(
                "<pack>\n" +
                        "\t<item real=\"true\"/>\n" +
                        "\t<item real=\"true\"/>\n" +
                        "\t<item/>\n" +
                        "\t<item/>\n" +
                        "\t<item/>\n" +
                        "</pack>"));
        assertEquals("[real != null]",
            school.buildClues(items.subList(0, 2), items.subList(2, 5)).toString());
        assertEquals("[real = null]",
            school.buildClues(items.subList(2, 5), items.subList(0, 2)).toString());
    }

    @Test
    public void testBuildCluesOnDistinguishableBySingleAttributeValue() throws Exception {
        List<Element> items = XPathUtils.queryElementList("//item", parseSource(
                "<pack>\n" +
                        "\t<item real=\"true\"/>\n" +
                        "\t<item real=\"true\"/>\n" +
                        "\t<item real=\"false\"/>\n" +
                        "\t<item real=\"false\"/>\n" +
                        "\t<item real=\"false\"/>\n" +
                        "</pack>"));
        assertEquals("[real = true]",
            school.buildClues(items.subList(0, 2), items.subList(2, 5)).toString());
        assertEquals("[real != true]",
            school.buildClues(items.subList(2, 5), items.subList(0, 2)).toString());
    }

    @Test
    public void testBuildClues() throws Exception {
        List<Element> items = XPathUtils.queryElementList("//item", parseSource(
                "<pack>\n" +
                        "\t<item real=\"true\"/>\n" +
                        "\t<item real=\"true\"/>\n" +
                        "\t<item fake=\"false\"/>\n" +
                        "\t<item fake=\"false\"/>\n" +
                        "\t<item fake=\"false\"/>\n" +
                        "</pack>"));
        // buildClues returns alphabetical order
        assertEquals("[fake = null, real != null]",
            school.buildClues(items.subList(0, 2), items.subList(2, 5)).toString());


        List<Element> items2 = XPathUtils.queryElementList("//td", parseSource(
                "<tr valign=\"top\">\n" +
                        "\t<td width=\"145\"/>\n" +
                        "\t<td width=\"145\"/>\n" +
                        "\t<td width=\"145\"/>\n" +
                        "\t<td width=\"145\"/>\n" +
                        "\t<td width=\"10\" nowrap=\"\"/>\n" +
                        "\t<td width=\"10\" nowrap=\"\"/>\n" +
                        "\t<td width=\"10\" nowrap=\"\"/>\n" +
                        "</tr>"));
        assertEquals("[nowrap = null, width != 10]",
            school.buildClues(items2.subList(0, 4), items2.subList(4, 7)).toString());

        List<Element> items3 = XPathUtils.queryElementList("//table", parseSource(
                "<td width=\"610\">\n" +
                        "\t<table width=\"610\" height=\"30\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"/>\n" +
                        "\t<table width=\"610\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"/>\n" +
                        "</td>"));
        assertEquals("[height = null]",
            school.buildClues(items3.subList(1, 2), items3.subList(0, 1)).toString());

        List<Element> items4 = XPathUtils.queryElementList("//tr", parseSource(
                "<tbody>\n" +
                        "\t<tr bgcolor=\"#b4b4b4\"/>\n" +
                        "\t<tr bgcolor=\"#b4b4b4\"/>\n" +
                        "\t<tr bgcolor=\"#b4b4b4\"/>\n" +
                        "\t<tr valign=\"top\"/>\n" +
                        "\t<tr valign=\"top\"/>\n" +
                        "\t<tr valign=\"top\"/>\n" +
                        "</tbody>"));
        assertEquals("[bgcolor != null, valign = null]",
            school.buildClues(items4.subList(0, 3), items4.subList(3, 6)).toString());

    }

    @Test
    public void testTrainJudgeOnDistinguishableByTag() throws Exception {
        List<Element> items = XPathUtils.queryElementList("//pack/*", parseSource(
                "<pack>\n" +
                        "\t<item/>\n" +
                        "\t<item/>\n" +
                        "\t<bar/>\n" +
                        "\t<qux/>\n" +
                        "\t<gazonk/>\n" +
                        "</pack>"));
        /*
        check(school.trainJudge(items.subList(0, 2), items.subList(2, 5)), items,
            GOOD, GOOD, BAD, BAD, BAD);
        check(school.trainJudge(items.subList(2, 5), items.subList(0, 2)), items,
            BAD, BAD, GOOD, BAD, BAD);
        */
    }

    @Test
    public void testTrainJudgeOnDistinguishableByAttribute() throws Exception {
        List<Element> items = XPathUtils.queryElementList("//pack/*", parseSource(
                "<pack>\n" +
                        "\t<item real=\"true\"/>\n" +
                        "\t<item real=\"true\"/>\n" +
                        "\t<item fake=\"false\"/>\n" +
                        "\t<item fake=\"false\"/>\n" +
                        "\t<item fake=\"false\"/>\n" +
                        "</pack>"));
        check(school.trainJudge(items.subList(0, 2), items.subList(2, 5)), items,
            GOOD, GOOD, BAD, BAD, BAD);
        check(school.trainJudge(items.subList(2, 5), items.subList(0, 2)), items,
            BAD, BAD, GOOD, GOOD, GOOD);
        /*
        check(school.trainJudge(items.subList(0, 3), items.subList(3, 5)), items,
            GOOD, GOOD, GOOD, GOOD, GOOD);
        */
    }

    @Test
    public void testTrainJudgeOnAlwaysTrue() throws Exception {
        List<Element> items = XPathUtils.queryElementList("//pack/*", parseSource(
                "<pack>\n" +
                        "\t<foo real=\"true\"/>\n" +
                        "\t<bar real=\"true\"/>\n" +
                        "\t<qux fake=\"false\"/>\n" +
                        "\t<div fake=\"false\"/>\n" +
                        "\t<table fake=\"false\"/>\n" +
                        "</pack>"));
        check(school.trainJudge(items, new ArrayList<Element>()),
            items,
            GOOD, BAD, BAD, BAD, BAD);
    }

    @Test
    public void testTrainJudgeOnRealWorld() throws Exception {
        List<Element> items1 = XPathUtils.queryElementList("//td", parseSource(
                "<tr valign=\"top\">\n" +
                        "\t<td width=\"145\"/>\n" +
                        "\t<td width=\"145\"/>\n" +
                        "\t<td width=\"145\"/>\n" +
                        "\t<td width=\"145\"/>\n" +
                        "\t<td width=\"10\" nowrap=\"\"/>\n" +
                        "\t<td width=\"10\" nowrap=\"\"/>\n" +
                        "\t<td width=\"10\" nowrap=\"\"/>\n" +
                        "</tr>"));
        check(school.trainJudge(items1.subList(0, 4), items1.subList(4, 7)), items1,
            GOOD, GOOD, GOOD, GOOD, BAD, BAD, BAD);

        List<Element> items2 = XPathUtils.queryElementList("/td/*", parseSource(
                "<td width=\"145\">\n" +
                        "\t<table width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\"/>\n" +
                        "\t<a href=\"/products/audio-video\"/>\n" +
                        "\t<div style=\"margin: 0.3em 0pt 1em;\"/>\n" +
                        "</td>"));
        check(school.trainJudge(items2.subList(2, 3), items2.subList(0, 2)), items2,
            BAD, BAD, GOOD);
    }

    private void check(XmlElementFilter xmlElementFilter, List<Element> items, XmlElementFilter.Decision... expected) {
        for (int i = 0; i < items.size(); ++i) {
            assertEquals("At position " + i, expected[i], xmlElementFilter.classify(items.get(i)));
        }
    }
}
