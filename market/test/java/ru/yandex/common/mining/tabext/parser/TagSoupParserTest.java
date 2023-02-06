package ru.yandex.common.mining.tabext.parser;

import junit.framework.TestCase;
import ru.yandex.common.mining.tabext.model.Element;

import java.io.ByteArrayInputStream;

/**
 * Date: 08.01.2007
 * Time: 12:08:19
 *
 * @author nmalevanny@yandex-team.ru
 */
public class TagSoupParserTest extends TestCase {
    public void testBaseAndClear() throws Exception {
        TagSoupParser parser = new TagSoupParser();
        {
            String doc = "<html><p>test</html>";
            final Element element = parser.parse(new ByteArrayInputStream(doc.getBytes()));
            assertEquals("html", element.getName());
            final Element bodyElement = element.getChildren().get(0);
            assertEquals("body", bodyElement.getName());
            final Element pElement = bodyElement.getChildren().get(0);
            assertEquals("p", pElement.getName());
            assertEquals("test", pElement.getText());
        }
        {
            String doc = "<html><p>test</html>";
            final Element element = parser.parse(new ByteArrayInputStream(doc.getBytes()));
            assertEquals("html", element.getName());
            final Element bodyElement = element.getChildren().get(0);
            assertEquals("body", bodyElement.getName());
            final Element pElement = bodyElement.getChildren().get(0);
            assertEquals("p", pElement.getName());
            assertEquals("test", pElement.getText());
        }
    }

    public void testTagTextMix() throws Exception {
        String doc = "<html>test<p>test1<p>test2</p>test3</html>";
        TagSoupParser parser = new TagSoupParser();
        final Element element = parser.parse(new ByteArrayInputStream(doc.getBytes()));
        assertEquals("html", element.getName());
        final Element bodyElement = element.getChildren().get(0);
        assertEquals("body", bodyElement.getName());
        {
            final Element e = bodyElement.getChildren().get(0);
            assertEquals(Element.TEXT_ELEMENT, e.getName());
            assertEquals("test", e.getText());
        }
        {
            final Element e = bodyElement.getChildren().get(1);
            assertEquals("p", e.getName());
            assertEquals("test1", e.getText());
        }
        {
            final Element e = bodyElement.getChildren().get(2);
            assertEquals("p", e.getName());
            assertEquals("test2", e.getText());
        }
        {
            final Element e = bodyElement.getChildren().get(3);
            assertEquals(Element.TEXT_ELEMENT, e.getName());
            assertEquals("test3", e.getText());
        }
    }

    public void testAttributes() throws Exception {
        String doc = "<html><p class='aaa'>test</html>";
        TagSoupParser parser = new TagSoupParser();
        final Element element = parser.parse(new ByteArrayInputStream(doc.getBytes()));
        assertEquals("html", element.getName());
        final Element bodyElement = element.getChildren().get(0);
        assertEquals("body", bodyElement.getName());
        final Element pElement = bodyElement.getChildren().get(0);
        assertEquals("p", pElement.getName());
        assertEquals("test", pElement.getText());
        assertEquals("aaa", pElement.getAttributes().get("class"));
    }

}
