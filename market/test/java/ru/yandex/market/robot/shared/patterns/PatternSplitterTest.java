package ru.yandex.market.robot.shared.patterns;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author Dmitriy Kotelnikov <a href="mailto:kotelnikov@yandex-team.ru"></a>
 * @date 13.02.12
 */
public class PatternSplitterTest extends Assert {
    @Test
    public void testSplit() throws Exception {
        PatternSplitter patternSplitter = new PatternSplitter();
        checkPatterns(patternSplitter.split("/A/B | /B/A"), "/A/B", "/B/A");
        checkPatterns(patternSplitter.splitWithRegExp("/A/B | /B/A"), "/a/b", "/b/a");
        checkPatterns(patternSplitter.splitWithRegExp("following-sibling::P/DIV/A"), "following-sibling::p/div/a");
        checkPatterns(patternSplitter.splitWithRegExp("*/DIV/A"), "*/div/a");
        checkPatterns(patternSplitter.splitWithRegExp("/*/* | /*/*"), "/*/*", "/*/*");
        checkPatterns(patternSplitter.splitWithRegExp("/A/B/@href | /B/A/@src"), "/a/b/@href", "/b/a/@src");
        checkPatterns(
            patternSplitter.splitWithRegExp("/A/B[@hello = 'ADB'] | /B/A[@u = 'pppPi']"),
            "/a/b[@hello = 'ADB']", "/b/a[@u = 'pppPi']"
        );
        checkPatterns(
            patternSplitter.splitWithRegExp("/A/B[@hello = \"ADB\"] | /B/A[@u = \"pppPi\"]"),
            "/a/b[@hello = \"ADB\"]", "/b/a[@u = \"pppPi\"]"
        );
        checkPatterns(
            patternSplitter.split("DIV[translate(@class,' ','') = 'grade-title']/" +
                "DIV[translate(@class,' ','') = 'user']/B[translate(@class,' ','') = 'b-user']/" +
                "A[translate(@class,' ','') = 'b-user__link']"),
            "DIV[translate(@class,' ','') = 'grade-title']/" +
                "DIV[translate(@class,' ','') = 'user']/B[translate(@class,' ','') = 'b-user']/" +
                "A[translate(@class,' ','') = 'b-user__link']");
        checkPatterns(
            patternSplitter.split("/A/B/@href | /B/A/@src"),
            new Part("/A/B", "@href", true),
            new Part("/B/A", "@src", true)
        );
        checkPatterns(patternSplitter.split("/A/B|/B/A"), "/A/B", "/B/A");
        checkPatterns(
            patternSplitter.split("concat(//DIV[translate(@class,' ','') = 'siparis_tamamla_icerik']/text()[4], " +
                "' ', substring-after(//DIV[translate(@class,' ','') = 'siparis_tamamla_icerik']/text()[2], ':'), " +
                "' ', //DIV[translate(@class,' ','') = 'siparis_f_baslik'][preceding-sibling::*[1][(name() = 'DIV' or name() = 'div') and translate(@style,' ','') = 'clear:left;']][position() = 1]/B/text()[1])"),
            new Part("//DIV[translate(@class,' ','') = 'siparis_tamamla_icerik']", "text()[4]", true),
            new Part("//DIV[translate(@class,' ','') = 'siparis_tamamla_icerik']", "substring-after(text()[2],':')", true),
            new Part("//DIV[translate(@class,' ','') = 'siparis_f_baslik']" +
                "[preceding-sibling::*[1][(name() = 'DIV' or name() = 'div') and translate(@style,' ','') = 'clear:left;']]" +
                "[position() = 1]/B", "text()[1]", true)
        );
        checkPatterns(
            patternSplitter.split("concat(substring-after(/A/B, '|'), ' ', substring-before(/B/A, '))'))"),
            new Part("/A/B", "substring-after(.,'|')", false),
            new Part("/B/A", "substring-before(.,'))')", false)
        );
        checkPatterns(
            patternSplitter.split("concat(substring-before(substring-after(/A/B,'|'),'*'), ' ', substring-after(substring-before(/B/A,'))'),'*'))"),
            new Part("/A/B", "substring-before(substring-after(.,'|'),'*')", false),
            new Part("/B/A", "substring-after(substring-before(.,'))'),'*')", false)
        );
        checkPatterns(
            patternSplitter.split("count(/A/B)"),
            new Part("/A/B", "count(.)", false)
        );
        checkPatterns(
            patternSplitter.splitWithRegExp(
                "concat(substring-after(/A/B, '|'), ' ', substring-before(/B/A, '))'))"
            ),
            new Part("/a/b", ".*?\\|(.*)", false),
            new Part("/b/a", "(.*?)\\)\\).*", false)
        );
        checkPatterns(
            patternSplitter.splitWithRegExp(
                "substring-after(substring-before(/B/A, 'abc $'), '*op')"
            ),
            new Part("/b/a", ".*?\\*op(.*?)abc\\s\\$.*", false)
        );
        checkPatterns(
            patternSplitter.splitWithRegExp(
                "substring-before(substring-after(/B/A, 'abc $'), '*op')"
            ),
            new Part("/b/a", ".*?abc\\s\\$(.*?)\\*op.*", false)
        );
        checkPatterns(
            patternSplitter.split("concat(substring-after(/A/B[contains(@href, '0')], '|'), ' ', substring-before(/B/A, '))'))"),
            new Part("/A/B[contains(@href, '0')]", "substring-after(.,'|')", false),
            new Part("/B/A", "substring-before(.,'))')", false)
        );
        checkPatterns(
            patternSplitter.split("concat(substring-after(/A/B/@href, '|'), ' ', substring-before(/B/A/@src, '))'))"),
            new Part("/A/B", "substring-after(@href,'|')", true),
            new Part("/B/A", "substring-before(@src,'))')", true)
        );
        checkPatterns(
            patternSplitter.split("concat(/A/B/text()[1], ' ', substring-before(/B/A, '))'))"),
            new Part("/A/B", "text()[1]", true),
            new Part("/B/A", "substring-before(.,'))')", false)
        );
        checkPatterns(
            patternSplitter.split("concat(/A/B/text()[1],' ',  substring-before(/B/A, '))'))"),
            new Part("/A/B", "text()[1]", true),
            new Part("/B/A", "substring-before(.,'))')", false)
        );
        checkPatterns(
            patternSplitter.split("substring-after(/A/B,')')"),
            new Part("/A/B", "substring-after(.,')')", false)
        );
    }

    @Test
    public void testLowerCase() throws Exception {
        PatternSplitter patternSplitter = new PatternSplitter();

        assertEquals(
            "concat(substring(., 1, 2),':',substring(., 1, 2))",
            patternSplitter.lowerCase("concat(substring(., 1, 2), ':', substring(., 1, 2))")
        );

        assertEquals("count(/a/b)", patternSplitter.lowerCase("count(/A/B)"));

        assertEquals("/b/a", patternSplitter.lowerCase("/B/A"));

        assertEquals(
            "concat(/b/a[@test = \"HELLO\"],\" \",substring-after(substring-before(/b/a,\"HELLO\"),\"TESTING\"))",
            patternSplitter.lowerCase("concat(/B/A[@test = \"HELLO\"], \" \", substring-after(substring-before(/B/A, \"HELLO\"), \"TESTING\"))")
        );
    }

    private void checkPatterns(List<Part> patterns, String... expectedElements) {
        assertEquals(expectedElements.length, patterns.size());
        for (int i = 0; i < expectedElements.length; i++) {
            checkPart(patterns.get(i), expectedElements[i]);
        }
    }

    private void checkPart(Part part, String expectedElement) {
        assertEquals(expectedElement, part.getElement());
    }

    private void checkPatterns(List<Part> patterns, Part... expectedParts) {
        assertEquals(expectedParts.length, patterns.size());
        for (int i = 0; i < expectedParts.length; i++) {
            checkPart(patterns.get(i), expectedParts[i]);
        }
    }

    private void checkPart(Part actualPart, Part expectedPart) {
        assertEquals(expectedPart.getElement(), actualPart.getElement());
        assertEquals(expectedPart.getText(), actualPart.getText());
        assertEquals(expectedPart.isContainsAdditionalPart(), actualPart.isContainsAdditionalPart());
    }

    @Test
    public void testMergePatterns() throws Exception {
        PatternSplitter patternSplitter = new PatternSplitter();

        assertEquals(
            "concat(" +
                "//DIV[translate(@id,\" \",\"\") = \"pdtab-1\"]," +
                "substring-before(//STRONG[contains(@class,\"pd_product_price_now\")]/text()[1],\"T\")" +
                ")",
            patternSplitter.mergePatterns(
                "//DIV[translate(@id,\" \",\"\") = \"pdtab-1\"]",
                "substring-before(//STRONG[contains(@class,\"pd_product_price_now\")]/text()[1],\"T\")"
            )
        );
    }
}
