package ru.yandex.common.mining.scheme.pager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.Test;
import ru.yandex.common.mining.bd.PageNumberDetector;
import ru.yandex.common.mining.bd.domex.DomExtractor;
import ru.yandex.common.mining.bd.domex.NonEmptyLinkDomExtractor;
import ru.yandex.common.mining.bd.miner.DomLinksMiner;
import ru.yandex.common.mining.scheme.*;
import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.http.*;
import ru.yandex.common.util.http.loader.PageProvider;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Created on 21.08.2007 17:56:51
 *
 * @author Eugene Kirpichov jkff@yandex-team.ru
 */
public class PagerTest {
    private PageProvider pp = new PageProvider() {
        @Nonnull
        public Page fetch(Location location) throws IOException {
            return new Page(location, IOUtils.readWholeFile(location.unsafeGetAbsoluteURL()));
        }

        public ActionResolver getActionResolver() {
            return new FileActionResolver();
        }
    };

    @Test
    public void testPager() throws Exception {
        PagerScheme scheme = new PagerScheme(new DomLinksMiner(new NonEmptyLinkDomExtractor()));

        final int[] discoveryCount = new int[1];
        Coverage coverage = new Coverage() {
            public void discovery(Node where, @Nonnull Scheme who, @Nonnull Node what, @Nonnull NodeClass clazz) {
                super.discovery(where, who, what, clazz);
                discoveryCount[0]++;
            }
        };

        Graph graph = new Graph(pp);
        Node node = graph.addIsolatedNode(new TestFileLocation(
            "1", new File(getClass().getResource("/pager/1.html").toURI()
        ).getCanonicalPath()), NodeClass.LINK);


        final Set<Location> res = new HashSet<Location>();
        Scheme collector = new AbstractScheme() {
            public void run(Node input, @Nonnull Coverage coverage) throws IOException {
                res.add(input.getLocation());
            }
        };
        scheme.addNext(collector);

        scheme.run(node, coverage);
/*
        assertEquals(new HashSet<Location>(Arrays.asList(
                pp.absolute("/pager/1.html"),
                pp.absolute("/pager/2.html"),
                pp.absolute("/pager/3.html"),
                pp.absolute("/pager/4.html"),
                pp.absolute("/pager/5.html")
        )), res);

        assertEquals(5, graph.allNodes().size());
        assertEquals(4, discoveryCount[0]);
*/
    }

    @Test
    public void testPagerWhereThereIsNone() throws Exception {
        PagerScheme scheme = new PagerScheme(new DomLinksMiner(new NonEmptyLinkDomExtractor()));

        Graph graph = new Graph(pp);
        Node node = graph.addIsolatedNode(new HttpGetLocation("0", "/pager/no-pager.html"), NodeClass.LINK);


        final Set<Location> res = new HashSet<Location>();
        Scheme collector = new AbstractScheme() {
            public void run(Node input, @Nonnull Coverage coverage) throws IOException {
                res.add(input.getLocation());
            }
        };
        scheme.addNext(collector);

//        scheme.run(node, new Coverage());
/*
        assertEquals(new HashSet<Location>(Arrays.asList(
                new HttpGetLocation("0", "/pager/no-pager.html")
        )), res);
        */

//        assertEquals(1, graph.allNodes().size());
    }

    @Test
    public void testFalseOnSony() throws Exception {
        String url = "http://www.sony.ru/view/ShowProduct.action?product=5DPW30A&site=odw_ru_RU&pageType=Overview&imageType=Main&category=8cm+DVD+RW";
        String fileName = "/pager/sony_5dpw30a.htm";
        assertNoPagerOn(url, fileName);
    }

    @Test
    public void testOnMForum() throws Exception {
        String url = "http://www.mforum.ru/news/archive.htm?c=news/tests&sn=50";
        String fileName = "/pager/mforum.htm";


        Page page = new Page(new HttpGetLocation(fileName, url), IOUtils.readInputStream(
            getClass().getResourceAsStream(fileName)
        ));
        List<DomExtractor> detectors = new PageNumberDetector().suggest(page.getDocument());
        assertEquals(1, detectors.size());
        assertEquals(12, detectors.get(0).extractElements(page.getDocument()).size());
    }

    private void assertNoPagerOn(String url, String fileName) throws IOException {
        Page page = new Page(new HttpGetLocation(fileName, url), IOUtils.readInputStream(
            getClass().getResourceAsStream(fileName)
        ));
        List<DomExtractor> detectors = new PageNumberDetector().suggest(page.getDocument());
        assertEquals(0, detectors.size());
    }

    @Test
    public void testFalseOnSamsung1() throws Exception {
        assertNoPagerOn("http://www.samsung.ru/products/home/refrigerator/standard/rt44mbsw/",
                "/pager/samsung1.htm");
    }


    @Test
    public void testFalseOnSamsung2() throws Exception {
        assertNoPagerOn("http://www.samsung.ru/support/download-center/?productID=16769&resourceType=6",
                "/pager/samsung2.htm");
    }

    @Test
    public void testFalseOnAdrenalin() throws Exception {
        //assertNoPagerOn("http://www.adrenalin.ru/index.php?CID=21","/pager/adrenalin_raymarine.html");
    }


    private static class TestFileLocation extends HttpGetLocation {
        public TestFileLocation(@Nullable String refererUrl, @Nonnull String name, @Nonnull String url, Map<String, String> extraParameters) {
            super(refererUrl, name, url, extraParameters);
        }

        public TestFileLocation(String name, String url) {
            super(name, url);
        }

        @Nonnull
        public String getAbsoluteURL() {
            return getUrl();
        }
    }

}
