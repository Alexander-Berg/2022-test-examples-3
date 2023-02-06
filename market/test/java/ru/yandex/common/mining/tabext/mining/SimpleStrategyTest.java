package ru.yandex.common.mining.tabext.mining;

import org.w3c.dom.Node;
import ru.yandex.common.mining.tabext.mining.strategy.SimpleStrategy;
import ru.yandex.common.mining.tabext.util.ElementUtils;
import ru.yandex.common.util.http.Page;
import ru.yandex.common.util.http.loader.PageProvider;
import ru.yandex.common.util.test.FilePageProvider;

import java.util.List;

/**
 * Tests {@link SimpleStrategy} class.
 * 
 * @author kgeorgiy
 */
public class SimpleStrategyTest extends ShingleStrategyTest {
    private static final String SIMPLE_ELEMENT = "<table>"
        + "<tr><td>Ivan</td><td>29</td></tr>"
        + "<tr/>"
        + "<tr><td>Petr</td><td>39</td></tr>"
        + "<tr><td>Stepan</td><td>49</td></tr>"
        + "</table>";
    
	public void testEmpty() {
        final PageProvider pageProvider = new FilePageProvider();
        final Node node = new Page(pageProvider.absolute(""), SIMPLE_ELEMENT).getDocument().getDocumentElement()
                .getFirstChild().getNextSibling();

        final Node tableElement = getDescendantOnFirstChildWithName(node, "TABLE");
        
		SimpleStrategy strategy = new SimpleStrategy(3);
        final List<Block> blocks = strategy.selectBlocks(ElementUtils.getChildren(tableElement));

        //assertEquals(2, blocks.size());
        //assertEquals(3, blocks.get(0).getElements().size());
        //assertEquals(1, blocks.get(1).getElements().size());
	}
}
