package ru.yandex.common.mining.tabext.mining;

import junit.framework.TestCase;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;
import ru.yandex.common.mining.tabext.mining.strategy.ShingleStrategy;
import ru.yandex.common.mining.tabext.util.ElementPrinter;
import ru.yandex.common.util.http.loader.PageProvider;
import ru.yandex.common.util.io.IOInterruptedException;
import ru.yandex.common.util.test.FilePageProvider;

import java.io.*;
import java.util.List;

/**
 * User: dkachmar@yandex-team.ru Dmitry Kachmar
 * Date: Dec 4, 2008 4:45:06 PM
 */
public class AddressTabExtTest extends TestCase {


    public void testOnRusGas() throws Exception {
        //http://www.russneft.ru/gas/
        //testFile(getClass().getResource("/tabext/rusgas.html").getPath(), "АЗС 46");

//       need to WORK like this one!
//        final ByteArrayOutputStream byteArrayOutputStream1 = new ByteArrayOutputStream();
//        ElementPrinter.print(byteArrayOutputStream1, finder.find(element).get(5).getElement());
//        assertTrue(byteArrayOutputStream1.toString().contains("АЗС 46"));
    }

    public void testOnAutoPortal() throws Exception {
//        http://remont.avtoportal.ru/firms/6.html
        //testFile(getClass().getResource("/tabext/avtoportal.html").getPath(), "ул.Голубинская");

    }


    private void testFile(final String path, final String s) throws IOException, SAXException, IOInterruptedException {
        final Node element = getDocumentElement(path);
        final DefaultDataRootFinder defaultDataRootFinder = getDataRootFinder();
        final List<Node> dataRoot = defaultDataRootFinder.findDataRootsInside(element);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (final Node node : dataRoot) {
          ElementPrinter.print(byteArrayOutputStream, node);
            System.out.println(byteArrayOutputStream.toString());
        }

        assertTrue(byteArrayOutputStream.toString().contains(s));
    }


    private DefaultDataRootFinder getDataRootFinder() {
        ShingleCandidateFinder finder = new ShingleCandidateFinder();
        ShingleStrategy shingleStrategy = new ShingleStrategy(3, 2);
        shingleStrategy.setDiffThreshold(0.1);
        finder.setStrategy(shingleStrategy);

        final DefaultDataRootFinder defaultDataRootFinder = new DefaultDataRootFinder(finder);
        defaultDataRootFinder.setCandidateComparator(CandidateComparators.ON_WEIGHT_DESC);
        return defaultDataRootFinder;
    }


    private Node getDocumentElement(final String path) throws IOException, SAXException, IOInterruptedException {
        final PageProvider pageProvider = new FilePageProvider();
        final Node child = pageProvider.fetch(new FileLocation(path)).getDocument().getDocumentElement();

        return child;
    }


}
