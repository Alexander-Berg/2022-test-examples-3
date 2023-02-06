package ru.yandex.common.mining.tabext.mining;

import junit.framework.TestCase;
import org.w3c.dom.Node;
import ru.yandex.common.mining.tabext.mining.strategy.ShingleStrategy;
import ru.yandex.common.mining.tabext.util.ElementUtils;
import ru.yandex.common.util.http.Page;
import ru.yandex.common.util.http.loader.PageProvider;
import ru.yandex.common.util.io.IOInterruptedException;
import ru.yandex.common.util.test.FilePageProvider;

import java.io.IOException;
import java.util.List;

/**
 * Date: 09.01.2007
 * Time: 0:48:53
 *
 * @author nmalevanny@yandex-team.ru
 */
public class ShingleStrategyTest extends TestCase {
    private static final String SIMPLE_ELEMENT = "<table>"
            + "<tr><td>Ivan</td><td>29</td></tr>"
            + "<tr/>"
            + "<tr><td>Petr</td><td>39</td></tr>"
            + "<tr><td>Stepan</td><td>49</td></tr>"
            + "</table>";

    /*  public void testSimpleTable() throws Exception {
        final Element element = TagSoupParser.parseString(SIMPLE_ELEMENT);
        Element tableElement = element.getDescendantOnFirstChildWithName("table");
        Strategy strategy = new ShingleStrategy(2, 7);
        final List<List<Element>> blocks = strategy.selectBlocks(tableElement);
        assertEquals(3, blocks.size());
    }
*/


    public Node getDescendantOnFirstChildWithName(final Node element, final String elementName) {
        final List<Node> children = ElementUtils.getChildren(element);
        if (!children.isEmpty()) {
            Node firstChild = children.get(0);
            return elementName.equals(firstChild.getNodeName())
                    ? firstChild
                    : getDescendantOnFirstChildWithName(firstChild, elementName);
        }
        return null;
    }


    public void testJobList() throws IOInterruptedException, IOException {
        //final Element element = TagSoupParser.parseString(SIMPLE_ELEMENT);
        final PageProvider pageProvider = new FilePageProvider();
        final Node node = new Page(pageProvider.absolute(""), SIMPLE_ELEMENT).getDocument().getDocumentElement()
                .getFirstChild().getNextSibling();

        final Node tableElement = getDescendantOnFirstChildWithName(node, "TABLE");
        //     ElementPrinter.print(System.out, tableElement);
        Strategy strategy = new ShingleStrategy(2, 7);
        final List<Block> blocks = strategy.selectBlocks(ElementUtils.getChildren(tableElement));
        //     System.out.println(blocks.size());
        //     System.out.println(blocks.get(0).size());
        //assertEquals(3, blocks.size());
        //assertEquals(2, blocks.get(0).getElements().size());
    }

}
