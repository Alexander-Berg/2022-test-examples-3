package ru.yandex.common.mining.tabext.mining;

import junit.framework.TestCase;
import ru.yandex.common.mining.tabext.mining.strategy.MetaShingleStrategy;
import ru.yandex.common.mining.tabext.model.Element;
import ru.yandex.common.mining.tabext.parser.TagSoupParser;

/**
 * Date: 09.01.2007
 * Time: 0:48:53
 *
 * @author nmalevanny@yandex-team.ru
 */
public class MetaShingleStrategyTest extends TestCase {
    private static final String COMPLEX_ELEMENT = "<table>"
            + "<a href='test'/>"
            + "<a href='test'/>"
            + "<a href='test'/>"
            + "<b>pre info</b>"
            + "<tr><td>Ivan</td><td>29</td></tr>"
            + "<b>after info</b>"
            + "<b>pre info</b>"
            + "<tr><td>Ivan</td><td>29</td></tr>"
            + "<b>after info</b>"
            + "<b>pre info</b>"
            + "<tr><td>Ivan</td><td>29</td></tr>"
            + "<b>after info</b>"
            + "<a href='test'/>"
            + "<a href='test'/>"
            + "<a href='test'/>"
            + "<a href='test'/>"
            + "<a href='test'/>"
            + "<a href='test'/>"
            + "<a href='test'/>"
            + "</table>";

    public void testJobList() {
        final Element element = TagSoupParser.parseString(COMPLEX_ELEMENT);
        Element tableElement = element.getDescendantOnFirstChildWithName("table");
        Strategy strategy = new MetaShingleStrategy();
        try {
//        	strategy.selectBlocks(tableElement.getChildren());
        	// Not implemented
        	//fail();
        } catch (UnsupportedOperationException e) {
			// Ok
		}
    }
}
