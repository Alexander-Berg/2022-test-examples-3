package ru.yandex.market.request;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.request.UrlSplitter.split;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class UrlSplitterTest {

    @Test
    public void shouldSplitRegularUrl() throws Exception {
        Triple<Protocol, String, String> triple = split("https://www.test.com/some/resource?p1=1&p2=abc");

        assertEquals(Protocol.HTTPS, triple.getLeft());
        assertEquals("www.test.com", triple.getMiddle());
        assertEquals("/some/resource?p1=1&p2=abc", triple.getRight());
    }

    @Test
    public void shouldSplitUrlWithoutResource() throws Exception {
        Triple<Protocol, String, String> triple = split("http://www.simple-resource.ru");

        assertEquals(Protocol.HTTP, triple.getLeft());
        assertEquals("www.simple-resource.ru", triple.getMiddle());
        assertEquals(null, triple.getRight());
    }

    @Test
    public void shouldSplitUrlWithoutResourceAndTrailingSlash() throws Exception {
        Triple<Protocol, String, String> triple = split("http://www.simple-resource.ru/");

        assertEquals(Protocol.HTTP, triple.getLeft());
        assertEquals("www.simple-resource.ru", triple.getMiddle());
        assertEquals(null, triple.getRight());
    }
}
