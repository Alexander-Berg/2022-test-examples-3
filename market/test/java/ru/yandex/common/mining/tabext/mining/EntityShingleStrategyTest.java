package ru.yandex.common.mining.tabext.mining;

import junit.framework.TestCase;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;
import ru.yandex.common.mining.tabext.mining.strategy.EntityShingleStrategy;
import ru.yandex.common.mining.tabext.mining.strategy.ShingleStrategy;
import static ru.yandex.common.mining.tabext.util.ElementUtils.*;
import ru.yandex.common.util.http.loader.PageProvider;
import ru.yandex.common.util.io.IOInterruptedException;
import ru.yandex.common.util.test.FilePageProvider;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author tushkanchik
 *
 */
public class EntityShingleStrategyTest extends TestCase {
	private static final String FILE_NAME = "digitum.html";
	
	public void testDigitum() throws IOException, SAXException, IOInterruptedException {
        final String resource = getClass().getClassLoader().getResource(FILE_NAME).getPath();
        final PageProvider pageProvider = new FilePageProvider();
        final Node root = pageProvider.fetch(new FileLocation(resource)).getDocument().getDocumentElement();
		final ShingleCandidateFinder finder = new ShingleCandidateFinder();
		finder.setStrategy(new ShingleStrategy(3,2));
		finder.setShingleThreshold(50);
/*
        final Node dataRoot = new DefaultDataRootFinder(finder).findDataRootsInside(root).get(0);
		assertEquals("TABLE", dataRoot.getNodeName());

		final Node first = getChildren(dataRoot).get(5);
		final Node second = getChildren(dataRoot).get(6);
        assertEquals(3, new EntityShingleStrategy(3, 2).selectBlocks(Arrays.asList(first, second)).size());
        */
	}
}


