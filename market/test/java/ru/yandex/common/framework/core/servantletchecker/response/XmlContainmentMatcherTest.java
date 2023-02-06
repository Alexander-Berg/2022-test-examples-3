package ru.yandex.common.framework.core.servantletchecker.response;

import junit.framework.TestCase;


/**
 * @author agorbunov @ Oct 25, 2010
 */
public class XmlContainmentMatcherTest extends TestCase {

    public void testEqual() {
        String expected = "<response></response>";
        String actual = "<response></response>";
        assertEquals("", getDifference(expected, actual));
    }

    public void testElementMissing() {
        String expected = "<response><profile/></response>";
        String actual = "<response></response>";
        assertEquals("Missing element: /response/profile", getDifference(expected, actual));
    }

    public void testElementExtra() {
        String expected = "<response></response>";
        String actual = "<response><profile/></response>";
        assertEquals("", getDifference(expected, actual));
    }

    public void testElementMissingRecursive() {
        String expected = "<response><profile><project/></profile></response>";
        String actual = "<response><profile></profile></response>";
        assertEquals("Missing element: /response/profile/project", getDifference(expected, actual));
    }

    public void testElementExtraRecursive() {
        String expected = "<response><profile></profile></response>";
        String actual = "<response><profile><project/></profile></response>";
        assertEquals("", getDifference(expected, actual));
    }

    public void testTextInvalid() {
        String expected = "<response><status>200</status></response>";
        String actual = "<response><status>205</status></response>";
        assertEquals("Element /response/status expected:<200> but was:<205>", getDifference(expected, actual));
    }

    public void testTextMissing() {
        String expected = "<response><status>200</status></response>";
        String actual = "<response><status></status></response>";
        assertEquals("Element /response/status expected:<200> but was:<>", getDifference(expected, actual));
    }

    public void testTextExtra() {
        String expected = "<response><status></status></response>";
        String actual = "<response><status>205</status></response>";
        assertEquals("Element /response/status expected:<> but was:<205>", getDifference(expected, actual));
    }

    public void testTextExtraClosedNode() {
        String expected = "<response><status/></response>";
        String actual = "<response><status>205</status></response>";
        assertEquals("Element /response/status expected:<> but was:<205>", getDifference(expected, actual));
    }

    public void testAttributeMissing() {
        String expected = "<response><profile id='569'/></response>";
        String actual = "<response><profile/></response>";
        assertEquals("Missing attribute: /response/profile/@id", getDifference(expected, actual));
    }

    public void testAttributeExtra() {
        String expected = "<response><profile/></response>";
        String actual = "<response><profile id='215'/></response>";
        assertEquals("", getDifference(expected, actual));
    }

    public void testAttributeInvalid() {
        String expected = "<response><profile id='569'/></response>";
        String actual = "<response><profile id='215'/></response>";
        assertEquals("Attribute /response/profile/@id expected:<569> but was:<215>", getDifference(expected, actual));
    }

    public void testEqualSpace() {
        String expected = "<response> </response>";
        String actual = "<response></response>";
        assertEquals("", getDifference(expected, actual));
    }

    public void testEqualLineBreak() {
        String expected = "<response>\n\r\t<profile>\t</profile>\n</response>";
        String actual = "<response><profile/></response>";
        assertEquals("", getDifference(expected, actual));
    }

    public void testEqualLineBreakInText() {
        String expected = "<response>\n\r\t<profile>\n\r\tАква\n\r</profile>\n\r</response>";
        String actual = "<response><profile>Аква</profile></response>";
        assertEquals("", getDifference(expected, actual));
    }

    public void testEqualLineBreakInTextRecursive() {
        String expected = "<response>a\n\r<profile>b\n\r</profile></response>";
        String actual = "<response>a<profile>b</profile></response>";
        assertEquals("", getDifference(expected, actual));
    }

    public void testSameElementDifferentAttributes() {
        String expected = "<response><profile id='15'/><profile id='16'/></response>";
        String actual = "<response><profile id='15'/><profile id='16'/></response>";
        assertEquals("", getDifference(expected, actual));
    }

    public void testSameElementDifferentText() {
        String expected = "<response><status>200</status><status>205</status></response>";
        String actual = "<response><status>200</status><status>205</status></response>";
        assertEquals("", getDifference(expected, actual));
    }

    public void testSameElementDifferentTextRecursive() {
        String expected = "<response><status><code>200</code></status><status><code>205</code></status></response>";
        String actual = "<response><status><code>200</code></status><status><code>205</code></status></response>";
        assertEquals("", getDifference(expected, actual));
    }

    private String getDifference(String expected, String actual) {
        XmlContainmentMatcher matcher = new XmlContainmentMatcher(expected, actual);
        return matcher.getDifference();
    }
}
