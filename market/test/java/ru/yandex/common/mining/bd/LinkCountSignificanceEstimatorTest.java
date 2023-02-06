package ru.yandex.common.mining.bd;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import static ru.yandex.common.util.XPathUtils.queryElement;
import static ru.yandex.common.util.html.HtmlUtils.parse;

/**
 * @author Eugene Kirpichov
 */
public class LinkCountSignificanceEstimatorTest {
    private static final String PAGE = "<table><tbody>\n" +
        "\t<tr>\n" +
        "\t\t<td><a href=\"first.html\">First</a></td>\n" +
        "\t\t<td><a href=\"second.html\">Second</a></td>\n" +
        "\t</tr>\n" +
        "\t<a href=\"bottom.html\"/>\n" +
        "\t<b>Some <i>italic</i> text</b> here\n" +
        "</tbody></table>\n" +
        "<a href=\"bar.html\"/>";

    private Document doc;

    @Before
    public void init() throws SAXException {
        doc = parse(PAGE);
    }

    @Test
    public void testCountLinks() throws Exception {
        LinkCounter estimator = new LinkCounter(5);

        assertEquals(4, estimator.countLinks(doc.getDocumentElement()));

        int count = estimator.countLinks(queryElement("//B", doc));
        assertEquals(0, count);
    }

    @Test
    public void testIsSignificant() throws Exception {
        LinkCounter estimator = new LinkCounter(2);
        assertTrue(estimator.isSignificant(doc.getDocumentElement()));
        assertTrue(estimator.isSignificant(queryElement("//TABLE", doc)));
        assertFalse(estimator.isSignificant(queryElement("//TR", doc)));
        assertFalse(estimator.isSignificant(queryElement("//B", doc)));
    }
}
