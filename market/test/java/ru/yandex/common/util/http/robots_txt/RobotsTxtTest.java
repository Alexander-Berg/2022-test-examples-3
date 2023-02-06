package ru.yandex.common.util.http.robots_txt;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Test;

import ru.yandex.common.util.http.loader.PageBase;
import ru.yandex.common.util.http.loader.PageLoader;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;

/**
 * Created on 28.05.2007 20:28:35
 *
 * @author Eugene Kirpichov jkff@yandex-team.ru
 */
public class RobotsTxtTest {
    @Test
    public void testEmptyFile() {
        assertTrue(RobotsTxtRulesRepository.parse("").fits("http://www.example.com"));
    }

    @Test
    public void testAsksCorrectRobotTxt() {
        final boolean[] invoked = new boolean[1];
        PageLoader loader = new PageLoader() {
            public PageBase getPage(HttpUriRequest request) throws IOException {
                assertEquals("http://www.example.com/robots.txt", request.getURI().toString());
                invoked[0] = true;
                return new PageBase(null, "Nothing here.".getBytes(), Charset.defaultCharset());
            }
            public HttpResponse getResponse(HttpUriRequest request) throws IOException {
                throw new UnsupportedOperationException();
            }
        };

        RobotsTxtRulesRepository repository = new RobotsTxtRulesRepository();
        repository.setInnerPageLoader(loader);
        repository.getRules("http://www.example.com");

        assertTrue(invoked[0]);
    }

    @Test
    public void testMissingFile() throws Exception {
        PageLoader loader = createMock(PageLoader.class);
        expect(loader.getPage(isA(HttpUriRequest.class))).andThrow(new IOException());

        replay(loader);

        RobotsTxtRulesRepository repository = new RobotsTxtRulesRepository();
        repository.setInnerPageLoader(loader);
        repository.setHostPollInterval(1000);
        RobotRules rules = repository.getRules("http://www.example.com");
        assertEquals(1000, rules.getMinFetchInterval());
        assertTrue(rules.isAllowed("http://www.example.com"));
    }

    @Test
    public void testSimpleFileAboutUs() throws Exception {
        PageLoader loader = createMock(PageLoader.class);
        expect(loader.getPage(isA(HttpUriRequest.class))).andReturn(
                new PageBase(null, ("User-agent: *\n" +
                "Disallow: /forum").getBytes(), Charset.defaultCharset()));

        replay(loader);

        RobotsTxtRulesRepository repository = new RobotsTxtRulesRepository();
        repository.setInnerPageLoader(loader);
        repository.setHostPollInterval(1000);
        RobotRules rules = repository.getRules("http://www.example.com");
        assertEquals(1000, rules.getMinFetchInterval());
        assertTrue(rules.isAllowed("http://www.example.com"));
        assertTrue(rules.isAllowed("http://www.example.com/content/"));
        assertTrue(rules.isAllowed("http://www.example.com/content/forum"));
        assertFalse(rules.isAllowed("http://www.example.com/forum"));
        assertFalse(rules.isAllowed("http://www.example.com/forum/"));
        assertFalse(rules.isAllowed("http://www.example.com/forums"));
        assertTrue(rules.isAllowed("http://www.example.com/for"));
    }

    @Test
    public void testSimpleFileNotAboutUs() throws Exception {
        PageLoader loader = createMock(PageLoader.class);
        expect(loader.getPage(isA(HttpUriRequest.class))).andReturn(new PageBase(null, ("User-agent: Googlebot\n" +
                "Disallow: /").getBytes(), Charset.defaultCharset()));

        replay(loader);

        RobotsTxtRulesRepository repository = new RobotsTxtRulesRepository();
        repository.setInnerPageLoader(loader);
        repository.setHostPollInterval(1000);

        RobotRules rules = repository.getRules("http://www.example.com");
        assertTrue(rules.isAllowed("http://www.example.com/file.html"));
        assertTrue(rules.isAllowed("http://www.example.com/"));
    }

    @Test
    public void testOnSamsung() throws Exception {
        PageLoader loader = createMock(PageLoader.class);
        expect(loader.getPage(isA(HttpUriRequest.class))).andReturn(new PageBase(null, ("User-agent: *\n" +
                "Disallow: /forum/\n" +
                "Disallow: /cgi-bin/\n" +
                "Disallow: /support/download-center/\n" +
                "Disallow: /products/comparation/\n" +
                "Disallow: /products/finder/").getBytes(), Charset.defaultCharset()));

        replay(loader);

        RobotsTxtRulesRepository repository = new RobotsTxtRulesRepository();
        repository.setInnerPageLoader(loader);
        repository.setHostPollInterval(1000);

        RobotRules rules = repository.getRules("http://www.samsung.ru");
        assertTrue(rules.isAllowed("http://www.samsung.ru/products/"));
        assertTrue(rules.isAllowed("http://www.samsung.ru/products/tv/lcd"));
        assertFalse(rules.isAllowed("http://www.samsung.ru/forum/"));
        assertFalse(rules.isAllowed("http://www.samsung.ru/products/finder/?param=1"));
        assertFalse(rules.isAllowed("http://www.samsung.ru/products/finder/"));
        assertFalse(rules.isAllowed("http://www.samsung.ru/products/finder/finder.html"));
        assertTrue(rules.isAllowed("http://www.samsung.ru/products/finder.html"));
    }
}
